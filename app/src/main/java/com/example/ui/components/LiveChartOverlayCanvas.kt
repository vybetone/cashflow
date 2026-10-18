package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import com.example.data.TradingSignalEntity

@Composable
fun LiveChartOverlayCanvas(
    signal: TradingSignalEntity?,
    isAnalyzing: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .testTag("live_chart_overlay_canvas")
    ) {
        val w = size.width
        val h = size.height

        if (w <= 0 || h <= 0) return@Canvas

        // Draw crosshair grid lines
        val gridColor = Color(0xFF263238)
        val dashedEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)

        // Draw horizontal grid lines
        for (i in 1..4) {
            val y = h * (i / 5f)
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1f,
                pathEffect = dashedEffect
            )
        }

        // Draw vertical grid lines
        for (i in 1..4) {
            val x = w * (i / 5f)
            drawLine(
                color = gridColor,
                start = Offset(x, 0f),
                end = Offset(x, h),
                strokeWidth = 1f,
                pathEffect = dashedEffect
            )
        }

        // If currently analyzing, draw scanning laser line
        if (isAnalyzing) {
            val laserY = (System.currentTimeMillis() % 2000) / 2000f * h
            drawLine(
                color = Color(0xFF00E676),
                start = Offset(0f, laserY),
                end = Offset(w, laserY),
                strokeWidth = 3f
            )
        }

        if (signal == null) return@Canvas

        val isBuy = signal.action.uppercase() == "BUY"
        val isSell = signal.action.uppercase() == "SELL"

        val greenColor = Color(0xFF00E676)
        val redColor = Color(0xFFFF5252)

        // Resistance line (Upper)
        val resY = h * 0.28f
        drawLine(
            color = redColor,
            start = Offset(0f, resY),
            end = Offset(w, resY),
            strokeWidth = 2.5f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
        )

        // Support line (Lower)
        val supY = h * 0.72f
        drawLine(
            color = greenColor,
            start = Offset(0f, supY),
            end = Offset(w, supY),
            strokeWidth = 2.5f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
        )

        // Draw Target Zone Box
        if (isBuy) {
            // Take Profit Box above entry
            val tpTop = h * 0.15f
            val tpBottom = h * 0.45f
            drawRect(
                color = greenColor.copy(alpha = 0.12f),
                topLeft = Offset(w * 0.55f, tpTop),
                size = Size(w * 0.40f, tpBottom - tpTop)
            )
            drawRect(
                color = greenColor,
                topLeft = Offset(w * 0.55f, tpTop),
                size = Size(w * 0.40f, tpBottom - tpTop),
                style = Stroke(width = 2f)
            )

            // Stop Loss Box
            val slTop = h * 0.72f
            val slBottom = h * 0.88f
            drawRect(
                color = redColor.copy(alpha = 0.12f),
                topLeft = Offset(w * 0.55f, slTop),
                size = Size(w * 0.40f, slBottom - slTop)
            )
            drawRect(
                color = redColor,
                topLeft = Offset(w * 0.55f, slTop),
                size = Size(w * 0.40f, slBottom - slTop),
                style = Stroke(width = 2f)
            )
        } else if (isSell) {
            // Take Profit Box below entry
            val tpTop = h * 0.55f
            val tpBottom = h * 0.85f
            drawRect(
                color = greenColor.copy(alpha = 0.12f),
                topLeft = Offset(w * 0.55f, tpTop),
                size = Size(w * 0.40f, tpBottom - tpTop)
            )
            drawRect(
                color = greenColor,
                topLeft = Offset(w * 0.55f, tpTop),
                size = Size(w * 0.40f, tpBottom - tpTop),
                style = Stroke(width = 2f)
            )

            // Stop Loss Box
            val slTop = h * 0.12f
            val slBottom = h * 0.28f
            drawRect(
                color = redColor.copy(alpha = 0.12f),
                topLeft = Offset(w * 0.55f, slTop),
                size = Size(w * 0.40f, slBottom - slTop)
            )
            drawRect(
                color = redColor,
                topLeft = Offset(w * 0.55f, slTop),
                size = Size(w * 0.40f, slBottom - slTop),
                style = Stroke(width = 2f)
            )
        }

        // Draw Pattern Callout Anchor Circle
        val anchorX = w * 0.35f
        val anchorY = if (isBuy) h * 0.70f else h * 0.30f
        val circleColor = if (isBuy) greenColor else redColor

        drawCircle(
            color = circleColor,
            radius = 12f,
            center = Offset(anchorX, anchorY)
        )
        drawCircle(
            color = circleColor.copy(alpha = 0.3f),
            radius = 24f,
            center = Offset(anchorX, anchorY)
        )
    }
}
