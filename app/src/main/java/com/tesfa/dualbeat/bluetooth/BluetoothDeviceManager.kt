package com.tesfa.dualbeat.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.tesfa.dualbeat.domain.model.AudioSinkType
import com.tesfa.dualbeat.domain.model.BluetoothAudioDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BluetoothDeviceManager(private val context: Context) {

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter
    private val audioManager: AudioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var a2dpProfile: BluetoothProfile? = null

    private val _bondedDevices = MutableStateFlow<List<BluetoothAudioDevice>>(emptyList())
    val bondedDevices: StateFlow<List<BluetoothAudioDevice>> = _bondedDevices.asStateFlow()

    private val _isBluetoothEnabled = MutableStateFlow(false)
    val isBluetoothEnabled: StateFlow<Boolean> = _isBluetoothEnabled.asStateFlow()

    init {
        initA2dpProfile()
        refreshDevices()
    }

    private fun initA2dpProfile() {
        bluetoothAdapter?.getProfileProxy(
            context,
            object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                    if (profile == BluetoothProfile.A2DP) {
                        a2dpProfile = proxy
                        refreshDevices()
                    }
                }

                override fun onServiceDisconnected(profile: Int) {
                    if (profile == BluetoothProfile.A2DP) {
                        a2dpProfile = null
                    }
                }
            },
            BluetoothProfile.A2DP
        )
    }

    fun hasBluetoothPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    @SuppressLint("MissingPermission")
    fun refreshDevices() {
        val adapter = bluetoothAdapter
        if (adapter == null) {
            _isBluetoothEnabled.value = false
            _bondedDevices.value = emptyList()
            return
        }

        _isBluetoothEnabled.value = adapter.isEnabled

        if (!hasBluetoothPermission() || !adapter.isEnabled) {
            _bondedDevices.value = emptyList()
            return
        }

        try {
            val bonded = adapter.bondedDevices ?: emptySet()
            val activeOutputs = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            val hasActiveBt = activeOutputs.any { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP }

            val deviceList = bonded.map { device ->
                val isConnected = a2dpProfile?.getConnectedDevices()?.contains(device) == true
                BluetoothAudioDevice(
                    address = device.address,
                    name = device.name ?: "Unknown Speaker",
                    isBonded = true,
                    isConnected = isConnected,
                    isAudioActive = isConnected && hasActiveBt,
                    sinkType = AudioSinkType.BLUETOOTH_A2DP
                )
            }
            _bondedDevices.value = deviceList
        } catch (e: SecurityException) {
            _bondedDevices.value = emptyList()
        }
    }

    fun getActiveAudioOutput(): Pair<AudioSinkType, String> {
        val outputs = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        val btOutput = outputs.firstOrNull { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP }
        if (btOutput != null) {
            return Pair(AudioSinkType.BLUETOOTH_A2DP, btOutput.productName.toString().ifEmpty { "Bluetooth Audio" })
        }
        val wiredOutput = outputs.firstOrNull {
            it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET || it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES
        }
        if (wiredOutput != null) {
            return Pair(AudioSinkType.WIRED_HEADSET, wiredOutput.productName.toString().ifEmpty { "Wired Headset" })
        }
        val usbOutput = outputs.firstOrNull { it.type == AudioDeviceInfo.TYPE_USB_DEVICE || it.type == AudioDeviceInfo.TYPE_USB_HEADSET }
        if (usbOutput != null) {
            return Pair(AudioSinkType.USB_AUDIO, usbOutput.productName.toString().ifEmpty { "USB Audio Device" })
        }
        return Pair(AudioSinkType.INTERNAL_SPEAKER, "Phone Speaker")
    }

    fun release() {
        a2dpProfile?.let {
            bluetoothAdapter?.closeProfileProxy(BluetoothProfile.A2DP, it)
        }
    }
}
