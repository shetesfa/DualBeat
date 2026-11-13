package com.tesfa.dualbeat.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.sin

@Composable
fun AnimatedAudioSpectrumVisualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 24
) {
    val infiniteTransition = rememberInfiniteTransition(label = "spectrum")

    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f, // 2 * PI
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val baseColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.tertiary

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until barCount) {
            val barFactor = if (isPlaying) {
                // Combine multi-frequency sine waves for authentic rhythm feel
                val wave1 = (sin(phase + (i * 0.45f)) + 1f) / 2f
                val wave2 = (sin((phase * 1.8f) + (i * 0.75f)) + 1f) / 2f
                val combined = (wave1 * 0.6f + wave2 * 0.4f).coerceIn(0.12f, 1.0f)
                combined
            } else {
                0.08f // resting low baseline
            }

            val targetHeight = (barFactor * 48).coerceAtLeast(6f).dp

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(targetHeight)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                secondaryColor,
                                baseColor
                            )
                        )
                    )
            )
        }
    }
}
