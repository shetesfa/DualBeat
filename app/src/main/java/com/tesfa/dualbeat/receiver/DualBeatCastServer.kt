package com.tesfa.dualbeat.receiver

import android.content.Context
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

    private var discoveryJob: Job? = null
    private var tcpServerSocket: ServerSocket? = null
    private val connectedClients = mutableMapOf<String, Socket>()

    private var packetSequence: Long = 0L

    fun startDiscovery() {
        if (discoveryJob?.isActive == true) return
        discoveryJob = scope.launch {
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket().apply {
                    broadcast = true
                    soTimeout = 2000
                }

                val pingBuffer = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN)
                pingBuffer.putInt(DualBeatCastProtocol.MAGIC_HEADER)
                pingBuffer.putInt(DualBeatCastProtocol.TYPE_DISCOVERY_PING)
                val pingData = pingBuffer.array()

                val broadcastAddr = InetAddress.getByName("255.255.255.255")
                val pingPacket = DatagramPacket(pingData, pingData.size, broadcastAddr, DualBeatCastProtocol.DISCOVERY_PORT)

                val rxData = ByteArray(1024)
                val rxPacket = DatagramPacket(rxData, rxData.size)

                while (isActive) {
                    // Send discovery ping
                    try {
                        socket.send(pingPacket)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    // Listen for discovery responses
                    val endTime = System.currentTimeMillis() + 1500
                    while (System.currentTimeMillis() < endTime && isActive) {
                        try {
                            socket.receive(rxPacket)
                            val parsed = DualBeatCastProtocol.parseDiscoveryPacket(rxPacket.data, rxPacket.length)
                            if (parsed != null) {
                                val (name, id) = parsed
                                val ip = rxPacket.address.hostAddress ?: "Unknown IP"
                                val currentMap = _discoveredNodes.value.toMutableMap()
                                currentMap[id] = ReceiverNode(
                                    id = id,
                                    name = name,
                                    ipAddress = ip,
                                    port = DualBeatCastProtocol.STREAM_PORT,
                                    state = if (connectedClients.containsKey(id)) ReceiverState.CONNECTED_SYNCED else ReceiverState.DISCOVERED,
                                    lastSeenTimestamp = System.currentTimeMillis()
                                )
                                _discoveredNodes.value = currentMap
                            }
                        } catch (e: SocketTimeoutException) {
                            // Normal timeout, continue
                        } catch (e: Exception) {
                            break
                        }
                    }
                    delay(3000)
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
    }

    suspend fun connectToReceiver(node: ReceiverNode): Boolean = withContext(Dispatchers.IO) {
        try {
            val socket = Socket()
            socket.connect(InetSocketAddress(node.ipAddress, node.port), 3000)
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
    }
}
