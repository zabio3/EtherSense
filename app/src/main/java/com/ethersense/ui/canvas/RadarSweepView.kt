package com.ethersense.ui.canvas

import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ethersense.data.model.WifiNetwork
import com.ethersense.ui.theme.CyanPrimary
import com.ethersense.ui.theme.EtherSenseTheme
import com.ethersense.ui.theme.GlowCyan
import com.ethersense.ui.theme.SignalExcellent
import com.ethersense.ui.theme.SignalFair
import com.ethersense.ui.theme.SignalPoor
import com.ethersense.ui.theme.SignalWeak
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RadarSweepView(
    networks: List<WifiNetwork>,
    modifier: Modifier = Modifier,
    isScanning: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar_sweep")

    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep_angle"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
    ) {
        val center = Offset(size.width / 2, size.height / 2)
        val maxRadius = minOf(size.width, size.height) / 2 - 16.dp.toPx()

        val ringCount = 4
        for (i in 1..ringCount) {
            val ringRadius = maxRadius * i / ringCount
            drawCircle(
                color = CyanPrimary.copy(alpha = 0.2f - i * 0.04f),
                radius = ringRadius,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )
        }

        for (i in 0 until 8) {
            val angle = (i * 45f) * PI.toFloat() / 180f
            val endX = center.x + maxRadius * cos(angle)
            val endY = center.y + maxRadius * sin(angle)
            drawLine(
                color = CyanPrimary.copy(alpha = 0.1f),
                start = center,
                end = Offset(endX, endY),
                strokeWidth = 1.dp.toPx()
            )
        }

        if (isScanning) {
            rotate(degrees = sweepAngle, pivot = center) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color.Transparent,
                            CyanPrimary.copy(alpha = 0.3f),
                            CyanPrimary.copy(alpha = 0.1f),
                            Color.Transparent
                        ),
                        center = center
                    ),
                    startAngle = -30f,
                    sweepAngle = 60f,
                    useCenter = true,
                    topLeft = Offset(center.x - maxRadius, center.y - maxRadius),
                    size = androidx.compose.ui.geometry.Size(maxRadius * 2, maxRadius * 2)
                )

                drawLine(
                    color = CyanPrimary.copy(alpha = 0.8f),
                    start = center,
                    end = Offset(center.x + maxRadius, center.y),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        networks.forEachIndexed { index, network ->
            val angle = (index * 360f / networks.size.coerceAtLeast(1)) * PI.toFloat() / 180f
            val normalizedDistance = ((-network.rssi - 30f) / 70f).coerceIn(0.1f, 0.95f)
            val distance = maxRadius * normalizedDistance

            val x = center.x + distance * cos(angle)
            val y = center.y + distance * sin(angle)

            val dotColor = getSignalColor(network.rssi)

            drawCircle(
                color = dotColor.copy(alpha = 0.3f),
                radius = 12.dp.toPx(),
                center = Offset(x, y)
            )

            drawCircle(
                color = dotColor,
                radius = 6.dp.toPx(),
                center = Offset(x, y)
            )

            if (network.isConnected) {
                drawCircle(
                    color = Color.White,
                    radius = 3.dp.toPx(),
                    center = Offset(x, y)
                )
            }
        }

        drawCircle(
            color = GlowCyan,
            radius = 8.dp.toPx(),
            center = center
        )
        drawCircle(
            color = CyanPrimary,
            radius = 4.dp.toPx(),
            center = center
        )
    }
}

private fun getSignalColor(rssi: Int): Color {
    return when {
        rssi >= -50 -> SignalExcellent
        rssi >= -60 -> SignalFair
        rssi >= -70 -> SignalPoor
        else -> SignalWeak
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D0D1A)
@Composable
private fun RadarSweepPreview() {
    EtherSenseTheme {
        RadarSweepView(
            networks = emptyList(),
            isScanning = true
        )
    }
}
