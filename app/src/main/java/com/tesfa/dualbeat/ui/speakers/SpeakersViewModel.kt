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
        isSamsungDualAudioActive = false,
        isMultiOutputSupportedByFirmware = false,
        hardwareLimitationNotice = "Analyzing hardware..."
    ),
    val bondedDevices: List<BluetoothAudioDevice> = emptyList(),
    val discoveredReceivers: List<ReceiverNode> = emptyList(),
    val isReceiverModeActive: Boolean = false,
    val isCastBroadcasting: Boolean = false,
    val isScanning: Boolean = false
)

class SpeakersViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as DualBeatApp
    private val btManager = app.bluetoothDeviceManager
    private val multiBtEngine = app.multiBluetoothEngine
    private val castServer = app.castServer
    private val castClient = app.castClient

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
                castServer.isBroadcasting,
                castClient.isListening
            ) { bonded, receiversMap, isBroadcasting, isReceiving ->
                _uiState.update { current ->
                    current.copy(
                        bondedDevices = bonded,
                        discoveredReceivers = receiversMap.values.toList(),
                        isCastBroadcasting = isBroadcasting,
                        isReceiverModeActive = isReceiving,
                        routeReport = multiBtEngine.analyzeRoutingCapabilities()
                    )
                }
            }.collect()
        }
    }

    fun refreshAll() {
        btManager.refreshDevices()
        _uiState.update { it.copy(routeReport = multiBtEngine.analyzeRoutingCapabilities()) }
    }

    fun startCastDiscovery() {
        _uiState.update { it.copy(isScanning = true) }
        castServer.startDiscovery()
    }

    fun stopCastDiscovery() {
        _uiState.update { it.copy(isScanning = false) }
        castServer.stopDiscovery()
    }

    fun toggleReceiverSync(node: ReceiverNode) {
        viewModelScope.launch {
            if (node.state == com.tesfa.dualbeat.domain.model.ReceiverState.CONNECTED_SYNCED) {
                castServer.disconnectReceiver(node.id)
            } else {
                castServer.connectToReceiver(node)
            }
        }
    }

    fun toggleReceiverMode(enable: Boolean) {
        if (enable) {
            castClient.startReceiverMode()
        } else {
            castClient.stopReceiverMode()
        }
    }
}
