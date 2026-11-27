package com.reshimarket.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

fun svgPath(d: String): Path = PathParser().parsePathString(d).toPath()

/** אייקון קווי קטן מתוך נתיבי SVG (viewBox ריבועי) */
@Composable
fun LineIcon(paths: List<String>, color: Color, modifier: Modifier = Modifier, size: Dp = 22.dp, viewBox: Float = 24f, strokeWidth: Float = 2f) {
    val parsed = remember(paths) { paths.map { svgPath(it) } }
    Canvas(modifier.size(size)) {
        val k = this.size.width / viewBox
        scale(k, k, pivot = Offset.Zero) {
            parsed.forEach {
                drawPath(it, color, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
    }
}

fun circlePath(cx: Float, cy: Float, r: Float) = "M${cx - r},$cy a$r,$r 0 1,0 ${2 * r},0 a$r,$r 0 1,0 ${-2 * r},0"

object Icons {
    val share = listOf(circlePath(18f, 5f, 3f), circlePath(6f, 12f, 3f), circlePath(18f, 19f, 3f), "M8.6 10.5l6.8-4M8.6 13.5l6.8 4")
    val settings = listOf("M4 6h10M18 6h2M4 12h4M12 12h8M4 18h12M20 18h0", circlePath(16f, 6f, 2f), circlePath(10f, 12f, 2f), circlePath(18f, 18f, 2f))
    val mic = listOf("M9 6a3 3 0 0 1 6 0v5a3 3 0 0 1 -6 0z", "M5 11a7 7 0 0 0 14 0M12 18v3")
    val cart = listOf("M3 6h5l4 20h20l4-14H10", circlePath(15f, 33f, 2.6f), circlePath(29f, 33f, 2.6f))
}

// ---------- רקע "מדפי סופר" ----------

private class ShelfShape(val path: Path?, val x: Float, val y: Float, val w: Float, val h: Float, val r: Float, val color: Color, val stroke: Color? = null)

private val shelfShapes: List<ShelfShape> by lazy {
    fun rect(x: Float, y: Float, w: Float, h: Float, r: Float, c: Long) = ShelfShape(null, x, y, w, h, r, Color(c))
    fun path(d: String, c: Long, s: Long? = null) = ShelfShape(svgPath(d), 0f, 0f, 0f, 0f, 0f, Color(c), s?.let { Color(it) })
    listOf(
        rect(10f, 78f, 30f, 46f, 4f, 0xFFFF8FA3),
        rect(46f, 64f, 22f, 60f, 4f, 0xFFFFD23F),
        path("M80 124v-38q0-8 6-12v-10h8v10q6 4 6 12v38z", 0xFF7BD389),
        rect(110f, 88f, 26f, 36f, 12f, 0xFF8EC5FF),
        rect(144f, 72f, 34f, 52f, 4f, 0xFFFB8500),
        rect(186f, 84f, 24f, 40f, 4f, 0xFFC9A4FF),
        path("M220 124v-34l12-14 12 14v34z", 0xFFFFFFFF, 0xFF8EC5FF),
        rect(254f, 70f, 28f, 54f, 4f, 0xFFFF5A5F),
        rect(290f, 90f, 30f, 34f, 14f, 0xFFFFD23F),
        path("M330 124v-40q0-8 6-12v-8h8v8q6 4 6 12v40z", 0xFF2A7DE1),
        rect(358f, 80f, 24f, 44f, 4f, 0xFF7BD389),
    )
}

/** מצייר את דוגמת המדפים לאורך כל המסך (אריח של 390×150) */
fun DrawScope.drawShelves(c: AppColors) {
    val k = density
    val tileW = 390f * k
    val tileH = 150f * k
    val alpha = if (c.dark) 0.22f else 0.42f
    val shadow = if (c.dark) Color(0x4D000000) else Color(0x145A320A)
    var ty = 0f
    while (ty < size.height) {
        var tx = 0f
        while (tx < size.width) {
            withTransform({ translate(tx, ty); scale(k, k, pivot = Offset.Zero) }) {
                for (s in shelfShapes) {
                    if (s.path != null) {
                        drawPath(s.path, s.color, alpha = alpha)
                        s.stroke?.let { drawPath(s.path, it, alpha = alpha, style = Stroke(3f)) }
                    } else {
                        drawRoundRect(s.color, Offset(s.x, s.y), Size(s.w, s.h), CornerRadius(s.r, s.r), alpha = alpha)
                    }
                }
                drawRect(c.shelf, Offset(0f, 124f), Size(390f, 12f))
                drawRect(shadow, Offset(0f, 136f), Size(390f, 6f))
            }
            tx += tileW
        }
        ty += tileH
    }
}

/** גרדיאנט בזווית 165° כמו בדמו */
fun DrawScope.drawGradient(top: Color, bottom: Color) {
    val a = Math.toRadians(165.0 - 90.0)
    val cx = size.width / 2
    val cy = size.height / 2
    val len = (size.width * kotlin.math.abs(cos(a)) + size.height * kotlin.math.abs(sin(a))).toFloat() / 2
    val dx = (cos(a) * len).toFloat()
    val dy = (sin(a) * len).toFloat()
    drawRect(Brush.linearGradient(listOf(top, bottom), start = Offset(cx - dx, cy - dy), end = Offset(cx + dx, cy + dy)))
}
