package com.ethersense.ui.canvas

import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.tooling.preview.Preview
import com.ethersense.ui.theme.EtherSenseTheme
import com.ethersense.ui.theme.SignalExcellent
import com.ethersense.ui.theme.SignalFair
import com.ethersense.ui.theme.SignalPoor
import com.ethersense.ui.theme.SignalWeak

@Composable
fun GlowingOrbView(
    signalQuality: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_pulse")

    val pulseDuration = (1500 - signalQuality * 1000).toInt().coerceIn(300, 1500)

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = pulseDuration,
                easing = EaseInOutSine
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = pulseDuration,
                easing = EaseInOutSine
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    val orbColor = interpolateColor(signalQuality)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
    ) {
        val center = Offset(size.width / 2, size.height / 2)
        val baseRadius = minOf(size.width, size.height) / 4

        for (i in 6 downTo 1) {
            val layerRadius = baseRadius * pulseScale * (1 + i * 0.25f)
            val layerAlpha = glowAlpha / (i * 1.5f)

            drawCircle(
                color = orbColor.copy(alpha = layerAlpha),
                radius = layerRadius,
                center = center,
                style = Fill
            )
        }

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.9f),
                    orbColor.copy(alpha = 0.8f),
                    orbColor.copy(alpha = 0.4f),
                    orbColor.copy(alpha = 0.1f)
                ),
                center = center,
                radius = baseRadius * pulseScale
            ),
            radius = baseRadius * pulseScale,
            center = center
        )

        drawCircle(
            color = Color.White.copy(alpha = 0.8f),
            radius = baseRadius * pulseScale * 0.3f,
            center = Offset(
                center.x - baseRadius * 0.2f,
                center.y - baseRadius * 0.2f
            )
        )
    }
}

private fun interpolateColor(quality: Float): Color {
    return when {
        quality >= 0.75f -> {
            val t = (quality - 0.75f) / 0.25f
            lerp(SignalFair, SignalExcellent, t)
        }
        quality >= 0.5f -> {
            val t = (quality - 0.5f) / 0.25f
            lerp(SignalPoor, SignalFair, t)
        }
        quality >= 0.25f -> {
            val t = (quality - 0.25f) / 0.25f
            lerp(SignalWeak, SignalPoor, t)
        }
        else -> {
            SignalWeak
        }
    }
}

private fun lerp(start: Color, end: Color, fraction: Float): Color {
    val f = fraction.coerceIn(0f, 1f)
    return Color(
        red = start.red + (end.red - start.red) * f,
        green = start.green + (end.green - start.green) * f,
        blue = start.blue + (end.blue - start.blue) * f,
        alpha = start.alpha + (end.alpha - start.alpha) * f
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF0D0D1A)
@Composable
private fun GlowingOrbPreviewExcellent() {
    EtherSenseTheme {
        GlowingOrbView(signalQuality = 0.9f)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D0D1A)
@Composable
private fun GlowingOrbPreviewPoor() {
    EtherSenseTheme {
        GlowingOrbView(signalQuality = 0.3f)
    }
}
