package com.tesfa.dualbeat.receiver

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.tesfa.dualbeat.receiver.protocol.DualBeatCastProtocol
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.InputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID

class DualBeatCastClient(
    private val context: Context,
    private val clientName: String = "DualBeat Receiver ${android.os.Build.MODEL}",
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {

    private val clientId = UUID.randomUUID().toString().take(8)

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private var beaconJob: Job? = null
    private var streamJob: Job? = null
    private var serverSocket: ServerSocket? = null
    private var audioTrack: AudioTrack? = null

    fun startReceiverMode() {
        if (_isListening.value) return
        _isListening.value = true

        startBeaconResponder()
        startStreamServer()
    }

    private fun startBeaconResponder() {
        beaconJob = scope.launch {
            var datagramSocket: DatagramSocket? = null
            try {
                datagramSocket = DatagramSocket(DualBeatCastProtocol.DISCOVERY_PORT).apply {
                    broadcast = true
                }
                val rxBuffer = ByteArray(512)
                val rxPacket = DatagramPacket(rxBuffer, rxBuffer.size)

                while (isActive) {
                    datagramSocket.receive(rxPacket)
                    if (rxPacket.length >= 8) {
                        val buf = ByteBuffer.wrap(rxPacket.data, 0, rxPacket.length).order(ByteOrder.BIG_ENDIAN)
                        val magic = buf.getInt()
                        val type = buf.getInt()

                        if (magic == DualBeatCastProtocol.MAGIC_HEADER && type == DualBeatCastProtocol.TYPE_DISCOVERY_PING) {
                            val replyData = DualBeatCastProtocol.buildDiscoveryPongPacket(clientName, clientId)
                            val replyPacket = DatagramPacket(replyData, replyData.size, rxPacket.address, rxPacket.port)
                            datagramSocket.send(replyPacket)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                datagramSocket?.close()
            }
        }
    }

    private fun startStreamServer() {
        streamJob = scope.launch {
            try {
                serverSocket = ServerSocket(DualBeatCastProtocol.STREAM_PORT)
                initAudioTrack()

                while (isActive) {
                    val socket = serverSocket?.accept() ?: break
                    _isConnected.value = true
                    handleStreamingConnection(socket)
                    _isConnected.value = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                releaseAudioTrack()
            }
        }
    }

    private fun initAudioTrack() {
        val minBuffSize = AudioTrack.getMinBufferSize(
            DualBeatCastProtocol.SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(DualBeatCastProtocol.SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .build()
            )
            .setBufferSizeInBytes(minBuffSize * 4)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        track.play()
        audioTrack = track
    }

    private fun handleStreamingConnection(socket: Socket) {
        val input = socket.getInputStream()
        val headerBuffer = ByteArray(28) // 4 magic + 4 type + 8 seq + 8 pts + 4 size

        try {
            while (isActive && !socket.isClosed) {
                readFully(input, headerBuffer, 28)
                val buf = ByteBuffer.wrap(headerBuffer).order(ByteOrder.BIG_ENDIAN)
                val magic = buf.getInt()
                val type = buf.getInt()
                val seq = buf.getLong()
                val pts = buf.getLong()
                val payloadSize = buf.getInt()

                if (magic != DualBeatCastProtocol.MAGIC_HEADER || payloadSize <= 0 || payloadSize > 65536) {
                    break
                }

                val payload = ByteArray(payloadSize)
                readFully(input, payload, payloadSize)

                if (type == DualBeatCastProtocol.TYPE_AUDIO_DATA) {
                    // Jitter sync delay calculation
                    val now = System.currentTimeMillis()
                    val delayMs = pts - now
                    if (delayMs in 1..200) {
                        Thread.sleep(delayMs.coerceAtLeast(1L))
                    }
                    audioTrack?.write(payload, 0, payloadSize)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            runCatching { socket.close() }
        }
    }

    private fun readFully(inputStream: InputStream, buffer: ByteArray, length: Int) {
        var totalRead = 0
        while (totalRead < length) {
            val read = inputStream.read(buffer, totalRead, length - totalRead)
            if (read == -1) throw Exception("Stream closed prematurely")
            totalRead += read
        }
    }

    private fun releaseAudioTrack() {
        runCatching {
            audioTrack?.stop()
            audioTrack?.release()
        }
        audioTrack = null
    }

    fun stopReceiverMode() {
        _isListening.value = false
        _isConnected.value = false
        beaconJob?.cancel()
        streamJob?.cancel()
        runCatching { serverSocket?.close() }
        releaseAudioTrack()
    }
}
