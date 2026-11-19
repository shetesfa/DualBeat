package com.tesfa.dualbeat.ui.speakers

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tesfa.dualbeat.DualBeatApp
import com.tesfa.dualbeat.domain.model.AudioRouteReport
import com.tesfa.dualbeat.domain.model.AudioSinkType
import com.tesfa.dualbeat.domain.model.BluetoothAudioDevice
import com.tesfa.dualbeat.domain.model.ReceiverNode
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SpeakersUiState(
    val routeReport: AudioRouteReport = AudioRouteReport(
        currentActiveOutput = AudioSinkType.INTERNAL_SPEAKER,
        activeOutputName = "Phone Speaker",
        connectedBluetoothSinks = emptyList(),
        availableOutputs = emptyList(),
        dualAudioStatus = com.tesfa.dualbeat.domain.model.DualAudioDetectionStatus.NOT_CONNECTED,
        isPhysicalMultiOutputVerified = false,
        hardwareLimitationNotice = "Analyzing hardware..."
    ),
    val bondedDevices: List<BluetoothAudioDevice> = emptyList(),
    val discoveredReceivers: List<ReceiverNode> = emptyList(),
    val isReceiverModeActive: Boolean = false,
    val isCastBroadcasting: Boolean = false,
    val isScanning: Boolean = false,
    val isTransmitterRunning: Boolean = false,
    val localIpAddress: String = "Searching...",
    val manualIpInput: String = "",
    val connectionStatusMessage: String? = null
)

class SpeakersViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as DualBeatApp
    private val btManager = app.bluetoothDeviceManager
    private val multiBtEngine = app.multiBluetoothEngine
    val castServer = app.castServer
    val castClient = app.castClient

    private val _uiState = MutableStateFlow(SpeakersUiState())
    val uiState: StateFlow<SpeakersUiState> = _uiState.asStateFlow()

    init {
        observeData()
        refreshAll()
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                btManager.bondedDevices,
                castServer.discoveredNodes,
                combine(castServer.isBroadcasting, castServer.isDiscovering) { b, d -> Pair(b, d) },
                castClient.isListening,
                castClient.localIpAddress
            ) { bonded, receiversMap, castStatus, isReceiving, receiverIp ->
                val (isBroadcasting, isDiscovering) = castStatus
                _uiState.update { current ->
                    current.copy(
                        bondedDevices = bonded,
                        discoveredReceivers = receiversMap.values.toList(),
                        isCastBroadcasting = isBroadcasting,
                        isReceiverModeActive = isReceiving,
                        isTransmitterRunning = isDiscovering || isBroadcasting,
                        isScanning = isDiscovering,
                        localIpAddress = if (isReceiving) receiverIp else castServer.localIpAddress.value,
                        routeReport = multiBtEngine.analyzeRoutingCapabilities()
                    )
                }
            }.collect()
        }
    }

    fun setManualIpInput(ip: String) {
        _uiState.update { it.copy(manualIpInput = ip) }
    }

    fun refreshAll() {
        btManager.refreshDevices()
        castServer.updateLocalIp()
        castClient.updateLocalIp()
        _uiState.update {
            it.copy(
                routeReport = multiBtEngine.analyzeRoutingCapabilities(),
                localIpAddress = if (it.isReceiverModeActive) castClient.localIpAddress.value else castServer.localIpAddress.value
            )
        }
    }

    fun startCastDiscovery() {
        _uiState.update { it.copy(isScanning = true, connectionStatusMessage = "Broadcasting discovery beacons across Wi-Fi/Hotspot...") }
        castServer.startDiscovery()
    }

    fun stopCastDiscovery() {
        _uiState.update { it.copy(isScanning = false) }
        castServer.stopDiscovery()
    }

    fun toggleTransmitter() {
        if (_uiState.value.isTransmitterRunning) {
            castServer.release()
            stopCastDiscovery()
            _uiState.update { it.copy(isTransmitterRunning = false, connectionStatusMessage = "Transmitter stopped.") }
        } else {
            if (_uiState.value.isReceiverModeActive) {
                toggleReceiverMode(false)
            }
            startCastDiscovery()
            _uiState.update { it.copy(isTransmitterRunning = true, connectionStatusMessage = "Transmitter active. Sound from DualBeat will stream to connected receivers.") }
        }
    }

    fun scanReceivers() {
        startCastDiscovery()
    }

    fun toggleReceiverConnection(node: ReceiverNode) {
        viewModelScope.launch {
            if (node.state == com.tesfa.dualbeat.domain.model.ReceiverState.CONNECTED_SYNCED) {
                castServer.disconnectReceiver(node.id)
                _uiState.update { it.copy(connectionStatusMessage = "Disconnected from ${node.name}") }
            } else {
                _uiState.update { it.copy(connectionStatusMessage = "Connecting to ${node.name} (${node.ipAddress})...") }
                val success = castServer.connectToReceiver(node)
                _uiState.update {
                    it.copy(
                        connectionStatusMessage = if (success) "Successfully connected to ${node.name}! Audio streaming is active."
                        else "Failed to connect to ${node.name} at ${node.ipAddress}. Ensure Wi-Fi/Hotspot is active."
                    )
                }
            }
        }
    }

    fun connectDirectByIp() {
        val ip = _uiState.value.manualIpInput.trim()
        if (ip.isBlank()) {
            _uiState.update { it.copy(connectionStatusMessage = "Please enter receiver's IP address") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(connectionStatusMessage = "Connecting directly to IP $ip:8991...") }
            val success = castServer.connectToReceiverByIp(ip)
            _uiState.update {
                it.copy(
                    connectionStatusMessage = if (success) "Connected to $ip! Audio is now synchronized."
                    else "Connection failed to $ip. Check that Receiver Mode is ON on the second phone."
                )
            }
        }
    }

    fun toggleReceiverMode(enable: Boolean) {
        if (enable) {
            if (_uiState.value.isTransmitterRunning) {
                castServer.release()
                stopCastDiscovery()
            }
            castClient.startReceiverMode()
            _uiState.update {
                it.copy(
                    isTransmitterRunning = false,
                    isReceiverModeActive = true,
                    localIpAddress = castClient.localIpAddress.value,
                    connectionStatusMessage = "Receiver Mode Active! IP: ${castClient.localIpAddress.value} on port 8991"
                )
            }
        } else {
            castClient.stopReceiverMode()
            _uiState.update {
                it.copy(
                    isReceiverModeActive = false,
                    connectionStatusMessage = "Receiver Mode Stopped."
                )
            }
        }
    }
}
