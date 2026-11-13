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
import java.net.ServerSocket
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue

data class JitterBufferPacket(
    val seq: Long,
    val pts: Long,
    val data: ByteArray
)

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

    private val _bufferFillPercent = MutableStateFlow(0)
    val bufferFillPercent: StateFlow<Int> = _bufferFillPercent.asStateFlow()

    private var beaconJob: Job? = null
    private var streamJob: Job? = null
    private var playbackJob: Job? = null
    private var serverSocket: ServerSocket? = null
    private var audioTrack: AudioTrack? = null

    // Non-blocking Jitter Ring Buffer
    private val jitterBuffer = ConcurrentLinkedQueue<JitterBufferPacket>()
    private val TARGET_BUFFER_PACKETS = 4 // ~100ms target jitter depth
    private var lastSequenceNumber: Long = -1L

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
                // Log and continue on interrupt
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
                startJitterBufferPlayback()

                while (isActive) {
                    val socket = serverSocket?.accept() ?: break
                    _isConnected.value = true
                    handleStreamingConnection(socket)
                    _isConnected.value = false
                }
            } catch (e: Exception) {
                // Server closed or network error
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

    private fun startJitterBufferPlayback() {
        playbackJob = scope.launch {
            var isPrebuffered = false
            while (isActive) {
                val packetCount = jitterBuffer.size
                _bufferFillPercent.value = (packetCount.toFloat() / TARGET_BUFFER_PACKETS * 100).toInt().coerceIn(0, 100)

                if (!isPrebuffered) {
                    // Wait until initial jitter buffer depth is reached
                    if (packetCount >= TARGET_BUFFER_PACKETS) {
                        isPrebuffered = true
                    } else {
                        delay(10)
                        continue
                    }
                }

                val frame = jitterBuffer.poll()
                if (frame != null) {
                    audioTrack?.write(frame.data, 0, frame.data.size)
                } else {
                    // Buffer underrun: trigger re-buffering
                    isPrebuffered = false
                    delay(10)
                }
            }
        }
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

                // Robust packet validation
                if (magic != DualBeatCastProtocol.MAGIC_HEADER) {
                    break
                }
                if (payloadSize <= 0 || payloadSize > 65536) {
                    break
                }

                // Sequence validation (detect lost or out of order packets)
                if (lastSequenceNumber != -1L && seq <= lastSequenceNumber) {
                    // Stale or duplicate packet, skip payload
                    skipBytes(input, payloadSize)
                    continue
                }
                lastSequenceNumber = seq

                val payload = ByteArray(payloadSize)
                readFully(input, payload, payloadSize)

                if (type == DualBeatCastProtocol.TYPE_AUDIO_DATA) {
                    jitterBuffer.offer(JitterBufferPacket(seq, pts, payload))
                    // Cap buffer to prevent runaway memory usage
                    if (jitterBuffer.size > TARGET_BUFFER_PACKETS * 4) {
                        jitterBuffer.poll()
                    }
                }
            }
        } catch (e: Exception) {
            // Disconnection handled gracefully
        } finally {
            runCatching { socket.close() }
        }
    }

    private fun readFully(inputStream: InputStream, buffer: ByteArray, length: Int) {
        var totalRead = 0
        while (totalRead < length) {
            val read = inputStream.read(buffer, totalRead, length - totalRead)
            if (read == -1) throw Exception("Socket connection closed by transmitter")
            totalRead += read
        }
    }

    private fun skipBytes(inputStream: InputStream, length: Int) {
        var remaining = length.toLong()
        while (remaining > 0) {
            val skipped = inputStream.skip(remaining)
            if (skipped <= 0) break
            remaining -= skipped
        }
    }

    private fun releaseAudioTrack() {
        runCatching {
            audioTrack?.stop()
            audioTrack?.release()
        }
        audioTrack = null
        jitterBuffer.clear()
        lastSequenceNumber = -1L
    }

    fun stopReceiverMode() {
        _isListening.value = false
        _isConnected.value = false
        beaconJob?.cancel()
        streamJob?.cancel()
        playbackJob?.cancel()
        runCatching { serverSocket?.close() }
        releaseAudioTrack()
    }
}
