package com.reshimarket.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

/** גלגל צבעים מלא (גוון ורוויה) עם פס בהירות */
@Composable
fun ColorWheel(color: Color, onChange: (Color) -> Unit, modifier: Modifier = Modifier) {
    val c = LocalAppColors.current
    val hsv = remember(color) { FloatArray(3).also { android.graphics.Color.colorToHSV(color.toArgb(), it) } }
    var hue by remember { mutableFloatStateOf(hsv[0]) }
    var sat by remember { mutableFloatStateOf(hsv[1]) }
    var value by remember { mutableFloatStateOf(hsv[2].coerceAtLeast(0.15f)) }
    val emit by rememberUpdatedState(onChange)

    fun pick(pos: Offset, w: Float, h: Float) {
        val r = min(w, h) / 2f
        val dx = pos.x - w / 2f
        val dy = pos.y - h / 2f
        var a = Math.toDegrees(atan2(dy, dx).toDouble()).toFloat()
        if (a < 0) a += 360f
        hue = a
        sat = (hypot(dx, dy) / r).coerceIn(0f, 1f)
        emit(Color.hsv(hue, sat, value))
    }

    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(200.dp)
                .pointerInput(Unit) {
                    detectTapGestures { pick(it, size.width.toFloat(), size.height.toFloat()) }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        pick(change.position, size.width.toFloat(), size.height.toFloat())
                    }
                },
        ) {
            Canvas(Modifier.size(200.dp)) {
                val r = size.minDimension / 2f
                val hues = (0..12).map { Color.hsv((it * 30f) % 360f, 1f, 1f) }
                drawCircle(Brush.sweepGradient(hues, center), r)
                drawCircle(Brush.radialGradient(listOf(Color.White, Color.White.copy(alpha = 0f)), center, r), r)
                drawCircle(Color.Black.copy(alpha = 1f - value), r)
                val rad = Math.toRadians(hue.toDouble())
                val p = Offset(center.x + (cos(rad) * sat * r).toFloat(), center.y + (sin(rad) * sat * r).toFloat())
                drawCircle(Color.hsv(hue, sat, value), 12.dp.toPx(), p)
                drawCircle(Color.White, 12.dp.toPx(), p, style = Stroke(3.dp.toPx()))
                drawCircle(Color.Black.copy(alpha = 0.3f), 13.5f.dp.toPx(), p, style = Stroke(1.dp.toPx()))
            }
        }
        Slider(
            value = value,
            onValueChange = {
                value = it.coerceAtLeast(0.05f)
                emit(Color.hsv(hue, sat, value))
            },
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(thumbColor = Color.hsv(hue, sat, value), activeTrackColor = c.ink, inactiveTrackColor = c.line),
            modifier = Modifier.fillMaxWidth(),
        )
        Muted("בהירות", size = androidx.compose.ui.unit.TextUnit.Unspecified)
    }
}
