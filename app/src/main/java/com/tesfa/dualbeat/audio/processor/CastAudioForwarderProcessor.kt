package com.tesfa.dualbeat.audio.processor

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import com.tesfa.dualbeat.DualBeatApp
import java.nio.ByteBuffer

@OptIn(UnstableApi::class)
class CastAudioForwarderProcessor : BaseAudioProcessor() {

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        return if (inputAudioFormat.encoding == C.ENCODING_PCM_16BIT) {
            inputAudioFormat
        } else {
            AudioProcessor.AudioFormat.NOT_SET
        }
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        val app = DualBeatApp.instance
        if (app.castServer.isBroadcasting.value) {
            val copy = ByteArray(remaining)
            val pos = inputBuffer.position()
            inputBuffer.get(copy)
            inputBuffer.position(pos) // Restore position so downstream local audio player keeps playing
            app.castServer.broadcastAudioPcm(copy, 0, remaining)
        }

        val output = replaceOutputBuffer(remaining)
        output.put(inputBuffer)
        output.flip()
    }
}
