package com.ethersense.ui.canvas

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ethersense.ui.theme.CyanPrimary
import com.ethersense.ui.theme.EtherSenseTheme
import com.ethersense.ui.theme.GlowCyan

@Composable
fun WaveformVisualizer(
    rssiHistory: List<Int>,
    modifier: Modifier = Modifier,
    lineColor: Color = CyanPrimary,
    showGrid: Boolean = true
) {
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)

    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val scanLinePosition by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scan_line"
    )

    val displayData = remember(rssiHistory) {
        if (rssiHistory.size < 2) {
            listOf(-70, -70)
        } else {
            rssiHistory.takeLast(50)
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        val width = size.width
        val height = size.height
        val padding = 16.dp.toPx()

        if (showGrid) {
            val gridLines = 5
            for (i in 0..gridLines) {
                val y = padding + (height - 2 * padding) * i / gridLines
                drawLine(
                    color = gridColor,
                    start = Offset(padding, y),
                    end = Offset(width - padding, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            val verticalLines = 10
            for (i in 0..verticalLines) {
                val x = padding + (width - 2 * padding) * i / verticalLines
                drawLine(
                    color = gridColor,
                    start = Offset(x, padding),
                    end = Offset(x, height - padding),
                    strokeWidth = 1.dp.toPx()
                )
            }
        }

        if (displayData.size >= 2) {
            val effectiveWidth = width - 2 * padding
            val effectiveHeight = height - 2 * padding
            val spacing = effectiveWidth / (displayData.size - 1)

            val path = Path()
            val fillPath = Path()

            displayData.forEachIndexed { index, rssi ->
                val normalizedY = ((rssi + 100) / 70f).coerceIn(0f, 1f)
                val x = padding + index * spacing
                val y = padding + effectiveHeight * (1 - normalizedY)

                if (index == 0) {
                    path.moveTo(x, y)
                    fillPath.moveTo(x, height - padding)
                    fillPath.lineTo(x, y)
                } else {
                    path.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }
            }

            fillPath.lineTo(padding + (displayData.size - 1) * spacing, height - padding)
            fillPath.close()

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        lineColor.copy(alpha = 0.3f),
                        lineColor.copy(alpha = 0.05f)
                    )
                )
            )

            drawPath(
                path = path,
                color = lineColor.copy(alpha = 0.4f),
                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
            )

            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )

            displayData.forEachIndexed { index, rssi ->
                val normalizedY = ((rssi + 100) / 70f).coerceIn(0f, 1f)
                val x = padding + index * spacing
                val y = padding + effectiveHeight * (1 - normalizedY)

                if (index == displayData.size - 1) {
                    drawCircle(
                        color = GlowCyan,
                        radius = 12.dp.toPx(),
                        center = Offset(x, y)
                    )
                    drawCircle(
                        color = lineColor,
                        radius = 6.dp.toPx(),
                        center = Offset(x, y)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 3.dp.toPx(),
                        center = Offset(x, y)
                    )
                }
            }
        }

        val scanX = padding + (width - 2 * padding) * scanLinePosition
        drawLine(
            brush = Brush.verticalGradient(
                colors = listOf(
                    lineColor.copy(alpha = 0f),
                    lineColor.copy(alpha = 0.5f),
                    lineColor.copy(alpha = 0f)
                )
            ),
            start = Offset(scanX, padding),
            end = Offset(scanX, height - padding),
            strokeWidth = 2.dp.toPx()
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D0D1A)
@Composable
private fun WaveformVisualizerPreview() {
    EtherSenseTheme {
        val sampleData = listOf(-50, -55, -52, -60, -58, -65, -62, -70, -68, -65, -60, -55, -50)
        WaveformVisualizer(rssiHistory = sampleData)
    }
}
