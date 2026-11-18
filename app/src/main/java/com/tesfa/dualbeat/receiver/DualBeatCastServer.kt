package com.tesfa.dualbeat.receiver

import android.content.Context
import android.net.wifi.WifiManager
import com.tesfa.dualbeat.core.util.NetworkUtils
import com.tesfa.dualbeat.domain.model.ReceiverNode
import com.tesfa.dualbeat.domain.model.ReceiverState
import com.tesfa.dualbeat.receiver.protocol.DualBeatCastProtocol
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.*
import java.nio.ByteBuffer
import java.nio.ByteOrder

class DualBeatCastServer(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {

    private val _discoveredNodes = MutableStateFlow<Map<String, ReceiverNode>>(emptyMap())
    val discoveredNodes: StateFlow<Map<String, ReceiverNode>> = _discoveredNodes.asStateFlow()

    private val _isBroadcasting = MutableStateFlow(false)
    val isBroadcasting: StateFlow<Boolean> = _isBroadcasting.asStateFlow()

    private val _localIpAddress = MutableStateFlow("Searching...")
    val localIpAddress: StateFlow<String> = _localIpAddress.asStateFlow()

    private var multicastLock: WifiManager.MulticastLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    private var discoveryJob: Job? = null
    private val connectedClients = mutableMapOf<String, Socket>()

    private var packetSequence: Long = 0L

    fun updateLocalIp() {
        _localIpAddress.value = NetworkUtils.getPrimaryIpAddress()
    }

    private fun acquireNetworkLocks() {
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            if (wifiManager != null) {
                multicastLock = wifiManager.createMulticastLock("DualBeatTransmitterMulticast").apply {
                    setReferenceCounted(true)
                    acquire()
                }
                @Suppress("DEPRECATION")
                wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "DualBeatTransmitterWifi").apply {
                    setReferenceCounted(false)
                    acquire()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun releaseNetworkLocks() {
        try {
            if (multicastLock?.isHeld == true) multicastLock?.release()
            if (wifiLock?.isHeld == true) wifiLock?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        multicastLock = null
        wifiLock = null
    }

    fun startDiscovery() {
        if (discoveryJob?.isActive == true) return
        acquireNetworkLocks()
        updateLocalIp()

        discoveryJob = scope.launch {
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket().apply {
                    broadcast = true
                    soTimeout = 1500
                }

                val pingBuffer = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN)
                pingBuffer.putInt(DualBeatCastProtocol.MAGIC_HEADER)
                pingBuffer.putInt(DualBeatCastProtocol.TYPE_DISCOVERY_PING)
                val pingData = pingBuffer.array()

                val rxData = ByteArray(1024)
                val rxPacket = DatagramPacket(rxData, rxData.size)

                while (isActive) {
                    updateLocalIp()

                    // Send discovery ping to all local subnet broadcasts
                    val broadcasts = NetworkUtils.getBroadcastAddresses()
                    for (bcast in broadcasts) {
                        try {
                            val pingPacket = DatagramPacket(pingData, pingData.size, bcast, DualBeatCastProtocol.DISCOVERY_PORT)
                            socket.send(pingPacket)
                        } catch (e: Exception) {
                            // Ignored per interface
                        }
                    }

                    // Also ping hotspot clients range (192.168.43.2 to 192.168.43.20 if on hotspot)
                    val localIp = NetworkUtils.getPrimaryIpAddress()
                    if (localIp.startsWith("192.168.43.")) {
                        for (hostNum in 2..15) {
                            try {
                                val target = InetAddress.getByName("192.168.43.$hostNum")
                                val directPing = DatagramPacket(pingData, pingData.size, target, DualBeatCastProtocol.DISCOVERY_PORT)
                                socket.send(directPing)
                            } catch (e: Exception) {
                                // Ignore
                            }
                        }
                    }

                    // Listen for discovery responses and active beacons
                    val endTime = System.currentTimeMillis() + 1500
                    while (System.currentTimeMillis() < endTime && isActive) {
                        try {
                            socket.receive(rxPacket)
                            val parsed = DualBeatCastProtocol.parseDiscoveryPacket(rxPacket.data, rxPacket.length)
                            if (parsed != null) {
                                val (name, id) = parsed
                                val ip = rxPacket.address.hostAddress ?: "Unknown IP"
                                val currentMap = _discoveredNodes.value.toMutableMap()
                                val isAlreadyConnected = connectedClients.containsKey(id) || connectedClients.containsKey(ip)
                                currentMap[id] = ReceiverNode(
                                    id = id,
                                    name = name,
                                    ipAddress = ip,
                                    port = DualBeatCastProtocol.STREAM_PORT,
                                    state = if (isAlreadyConnected) ReceiverState.CONNECTED_SYNCED else ReceiverState.DISCOVERED,
                                    lastSeenTimestamp = System.currentTimeMillis()
                                )
                                _discoveredNodes.value = currentMap
                            }
                        } catch (e: SocketTimeoutException) {
                            // Normal timeout
                        } catch (e: Exception) {
                            break
                        }
                    }
                    delay(2000)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                socket?.close()
            }
        }
    }

    fun stopDiscovery() {
        discoveryJob?.cancel()
        discoveryJob = null
        releaseNetworkLocks()
    }

    suspend fun connectToReceiver(node: ReceiverNode): Boolean = withContext(Dispatchers.IO) {
        try {
            val socket = Socket()
            socket.connect(InetSocketAddress(node.ipAddress, node.port), 4000)
            socket.tcpNoDelay = true
            connectedClients[node.id] = socket

            val currentMap = _discoveredNodes.value.toMutableMap()
            currentMap[node.id] = node.copy(state = ReceiverState.CONNECTED_SYNCED)
            _discoveredNodes.value = currentMap
            _isBroadcasting.value = true
            true
        } catch (e: Exception) {
            val currentMap = _discoveredNodes.value.toMutableMap()
            currentMap[node.id] = node.copy(state = ReceiverState.ERROR)
            _discoveredNodes.value = currentMap
            false
        }
    }

    suspend fun connectToReceiverByIp(rawIp: String): Boolean = withContext(Dispatchers.IO) {
        val ip = rawIp.trim()
        if (ip.isBlank()) return@withContext false
        try {
            val socket = Socket()
            socket.connect(InetSocketAddress(ip, DualBeatCastProtocol.STREAM_PORT), 4000)
            socket.tcpNoDelay = true

            val id = "direct_$ip"
            connectedClients[id] = socket

            val node = ReceiverNode(
                id = id,
                name = "Remote Speaker ($ip)",
                ipAddress = ip,
                port = DualBeatCastProtocol.STREAM_PORT,
                state = ReceiverState.CONNECTED_SYNCED,
                lastSeenTimestamp = System.currentTimeMillis()
            )
            val currentMap = _discoveredNodes.value.toMutableMap()
            currentMap[id] = node
            _discoveredNodes.value = currentMap
            _isBroadcasting.value = true
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun disconnectReceiver(nodeId: String) {
        val socket = connectedClients.remove(nodeId)
        try {
            socket?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val currentMap = _discoveredNodes.value.toMutableMap()
        currentMap[nodeId]?.let {
            currentMap[nodeId] = it.copy(state = ReceiverState.DISCONNECTED)
            _discoveredNodes.value = currentMap
        }
        if (connectedClients.isEmpty()) {
            _isBroadcasting.value = false
        }
    }

    fun broadcastAudioPcm(pcmData: ByteArray, offset: Int, size: Int) {
        if (connectedClients.isEmpty()) return
        val presentationTime = System.currentTimeMillis() + 150L // 150ms buffer forward sync
        val packet = DualBeatCastProtocol.buildAudioPacket(
            seq = ++packetSequence,
            presentationTimestampMs = presentationTime,
            audioData = pcmData,
            offset = offset,
            size = size
        )

        scope.launch(Dispatchers.IO) {
            val deadClients = mutableListOf<String>()
            for ((id, socket) in connectedClients) {
                try {
                    val out = socket.getOutputStream()
                    out.write(packet)
                    out.flush()
                } catch (e: Exception) {
                    deadClients.add(id)
                }
            }
            deadClients.forEach { disconnectReceiver(it) }
        }
    }

    fun release() {
        stopDiscovery()
        connectedClients.values.forEach { runCatching { it.close() } }
        connectedClients.clear()
        _isBroadcasting.value = false
        releaseNetworkLocks()
    }
}
