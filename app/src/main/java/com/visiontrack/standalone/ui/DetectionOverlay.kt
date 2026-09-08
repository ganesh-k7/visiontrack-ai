package com.visiontrack.standalone.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.sp
import com.visiontrack.standalone.model.Detection

@Composable
fun DetectionOverlay(
    detections: List<Detection>,
    imageWidth: Int,
    imageHeight: Int,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        if (imageWidth <= 0 || imageHeight <= 0) return@Canvas

        val scale = minOf(size.width / imageWidth, size.height / imageHeight)
        val drawnWidth = imageWidth * scale
        val drawnHeight = imageHeight * scale
        val dx = (size.width - drawnWidth) / 2f
        val dy = (size.height - drawnHeight) / 2f

        detections.forEach { d ->
            val left = dx + d.left * scale
            val top = dy + d.top * scale
            val right = dx + d.right * scale
            val bottom = dy + d.bottom * scale

            drawRect(
                color = Color(0xFF00E676),
                topLeft = Offset(left, top),
                size = Size(right - left, bottom - top),
                style = Stroke(width = 4f)
            )

            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 15.sp.toPx()
                    isAntiAlias = true
                    setShadowLayer(4f, 0f, 0f, android.graphics.Color.BLACK)
                }
                drawText("${d.label} ${(d.confidence * 100).toInt()}%", left + 6f, top + 28f, paint)
            }
        }
    }
}
