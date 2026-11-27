package com.reshimarket.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.reshimarket.data.Item
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

data class Flight(val emoji: String, val from: Offset, val to: Offset, val onEnd: () -> Unit, val id: Long = System.nanoTime())

/** מצב האנימציות של העגלה והאימוג'ים המעופפים */
class FxState(private val scope: CoroutineScope) {
    /** מרכז האימוג'י של כל מוצר, בקואורדינטות של המסך */
    val emojiPos = mutableMapOf<Long, Offset>()
    var cartCenter = Offset.Zero
    val flights = mutableStateListOf<Flight>()
    val hidden = mutableStateListOf<Long>()
    val cartScale = Animatable(1f)
    val cartRot = Animatable(0f)
    val cartDrive = Animatable(0f)

    fun bump() {
        scope.launch {
            cartScale.animateTo(1.18f, tween(160))
            cartScale.animateTo(1f, tween(240))
        }
        scope.launch {
            cartRot.animateTo(-6f, tween(160))
            cartRot.animateTo(0f, tween(240))
        }
    }
}

private val FlyEasing = CubicBezierEasing(0.35f, 0.1f, 0.35f, 1f)

/** האימוג'י שקופץ במסלול קשת עם סיבוב תלת-ממדי */
@Composable
fun FlightsLayer(fx: FxState) {
    Box(Modifier.fillMaxSize()) {
        for (f in fx.flights.toList()) {
            androidx.compose.runtime.key(f.id) {
                FlightView(f) { fx.flights.remove(f) }
            }
        }
    }
}

@Composable
private fun FlightView(f: Flight, onDone: () -> Unit) {
    val p = remember { Animatable(0f) }
    LaunchedEffect(f.id) {
        p.animateTo(1f, tween(780, easing = FlyEasing))
        f.onEnd()
        onDone()
    }
    val density = LocalDensity.current
    val half = with(density) { 22.dp.toPx() }
    val lift = with(density) { 150.dp.toPx() }
    Box(
        Modifier
            .offset {
                val t = p.value
                val s = sin(PI * t).toFloat()
                val x = f.from.x + (f.to.x - f.from.x) * t
                val y = f.from.y + (f.to.y - f.from.y) * t - s * lift
                IntOffset((x - half).roundToInt(), (y - half).roundToInt())
            }
            .size(44.dp)
            .graphicsLayer {
                val t = p.value
                val s = sin(PI * t).toFloat()
                rotationY = t * 540f
                rotationX = s * 35f
                val sc = 1f + s * 0.9f - (if (t > 0.85f) (t - 0.85f) * 3f else 0f)
                scaleX = sc
                scaleY = sc
                cameraDistance = 8f * density.density
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(f.emoji, fontSize = 30.fsp(), textAlign = TextAlign.Center)
    }
}

/** כפתור העגלה הצהוב בפינה השמאלית התחתונה */
@Composable
fun BoxScope.CartButton(fx: FxState, count: Int, onClick: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        Modifier
            .align(AbsoluteAlignment.BottomLeft)
            .navigationBarsPadding()
            .absolutePadding(left = 16.dp, bottom = 24.dp)
            .onGloballyPositioned { fx.cartCenter = it.boundsInRoot().center }
            .graphicsLayer {
                translationX = fx.cartDrive.value
                scaleX = fx.cartScale.value
                scaleY = fx.cartScale.value
                rotationZ = fx.cartRot.value
            }
            .size(72.dp),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .shadow(10.dp, shape, ambientColor = Color(0x4D783C00), spotColor = Color(0x4D783C00))
                .clip(shape)
                .background(Color(0xFFFFB703))
                .clickable(role = Role.Button, onClickLabel = "העגלה", onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            LineIcon(Icons.cart, Color(0xFF2A1A10), size = 40.dp, viewBox = 40f, strokeWidth = 2.4f)
        }
        Box(
            Modifier
                .align(AbsoluteAlignment.TopRight)
                .absoluteOffset(x = 6.dp, y = (-6).dp)
                .heightIn(min = 28.dp)
                .widthIn(min = 28.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0xFFFFB703))
                .padding(3.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(LocalAppColors.current.grn)
                .padding(horizontal = 5.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(count.toString(), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.fsp())
        }
    }
}

// ---------- קונפטי ----------

private class Piece(val x: Float, val color: Color, val dx: Float, val rot: Float, val dur: Float, val delay: Float)

private val ConfettiEasing = CubicBezierEasing(0.2f, 0.6f, 0.4f, 1f)

@Composable
fun Confetti(key: Int) {
    if (key == 0) return
    val pieces = remember(key) {
        val cols = listOf(0xFFFF5A5F, 0xFFFFB703, 0xFF1F9D55, 0xFF2A7DE1, 0xFFFF8FAB, 0xFFFB8500).map { Color(it) }
        List(90) { i ->
            Piece(Random.nextFloat(), cols[i % cols.size], Random.nextFloat() - 0.5f, Random.nextFloat() * 900f, 1600f + Random.nextFloat() * 1400f, Random.nextFloat() * 400f)
        }
    }
    val time = remember(key) { Animatable(0f) }
    var done by remember(key) { mutableStateOf(false) }
    LaunchedEffect(key) {
        time.animateTo(3400f, tween(3400, easing = LinearEasing))
        done = true
    }
    if (done) return
    Canvas(Modifier.fillMaxSize()) {
        val t = time.value
        val w = 10.dp.toPx()
        val h = 16.dp.toPx()
        val spread = 200.dp.toPx()
        for (p in pieces) {
            val raw = ((t - p.delay) / p.dur).coerceIn(0f, 1f)
            if (raw <= 0f || raw >= 1f) continue
            val e = ConfettiEasing.transform(raw)
            val x = p.x * size.width + p.dx * spread * e
            val y = -h + e * (size.height + h * 3)
            rotate(p.rot * e, pivot = Offset(x, y)) {
                drawRoundRect(p.color, Offset(x - w / 2, y - h / 2), Size(w, h), CornerRadius(2.dp.toPx()))
            }
        }
    }
}

// ---------- דלתות הזכוכית ----------

private val DoorEasing = CubicBezierEasing(0.6f, 0f, 0.3f, 1f)

@Composable
fun GlassDoors(shut: Boolean) {
    val open by animateFloatAsState(if (shut) 0f else 1.05f, tween(700, easing = DoorEasing), label = "doors")
    if (open >= 1.05f && !shut) return
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val half = constraints.maxWidth / 2f
        if (shut) {
            // חוסם לחיצות על הרשימה כשהדלתות סגורות
            Box(Modifier.fillMaxSize().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {})
        }
        Door(Modifier.align(AbsoluteAlignment.TopLeft).fillMaxWidth(0.5f).graphicsLayer { translationX = -open * half }, handleOnRight = true)
        Door(Modifier.align(AbsoluteAlignment.TopRight).fillMaxWidth(0.5f).graphicsLayer { translationX = open * half }, handleOnRight = false)
    }
}

@Composable
private fun Door(modifier: Modifier, handleOnRight: Boolean) {
    Box(
        modifier
            .fillMaxHeight()
            .background(Brush.linearGradient(listOf(Color(0xBFBEE6FF), Color(0x8C8CC8F0))))
            .border(10.dp, Color(0xFF8A96A3)),
    ) {
        Box(
            Modifier
                .align(if (handleOnRight) AbsoluteAlignment.CenterRight else AbsoluteAlignment.CenterLeft)
                .absolutePadding(left = if (handleOnRight) 0.dp else 22.dp, right = if (handleOnRight) 22.dp else 0.dp)
                .size(width = 10.dp, height = 70.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF5D6773)),
        )
    }
}

// ---------- הקבלה ----------

/** steps(9, end) כמו בדמו: הקבלה "מודפסת" בקפיצות */
private val Steps9 = Easing { x -> if (x >= 1f) 1f else floor(x * 9f) / 9f }

@Composable
fun BoxScope.ReceiptView(receipt: Receipt?, shown: Boolean, onClose: () -> Unit) {
    val r by animateFloatAsState(if (shown) 0f else 1.1f, tween(1100, easing = Steps9), label = "receipt")
    if (receipt == null) return
    val ink = Color(0xFF2A1A10)
    val time = remember(receipt) {
        SimpleDateFormat("d.M.yyyy · HH:mm", Locale.forLanguageTag("he-IL")).format(receipt.time)
    }
    Column(
        Modifier
            .align(Alignment.BottomCenter)
            .padding(horizontal = 16.dp)
            .widthIn(max = 330.dp)
            .fillMaxWidth()
            .graphicsLayer { translationY = r * size.height }
            .shadow(24.dp, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
            .background(Color(0xFFFFFEF8), RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 30.dp),
    ) {
        Text("רשימרקט", color = ink, fontWeight = FontWeight.ExtraBold, fontSize = 22.fsp(), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Text("${receipt.listName} · $time", color = ink, fontSize = 13.fsp(), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        DashedLine()
        Column(Modifier.heightIn(max = 260.dp).verticalScroll(rememberScrollState())) {
            for (x in receipt.items) ReceiptLine(x, ink)
        }
        DashedLine()
        Text("קנית ${receipt.items.size} מוצרים 🎉", color = ink, fontWeight = FontWeight.ExtraBold, fontSize = 20.fsp(), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(14.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .heightIn(min = 46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(ink)
                    .clickable(role = Role.Button, onClick = onClose)
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) { Text("תודה, להתראות", color = Color(0xFFFFF8EE), fontWeight = FontWeight.Bold, fontSize = 16.fsp()) }
        }
    }
}

@Composable
private fun ReceiptLine(x: Item, ink: Color) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("${x.emoji} ${x.name}", color = ink, fontSize = 15.fsp(), modifier = Modifier.weight(1f))
        Text(if (x.qty > 1) "×${x.qty}" else "✓", color = ink, fontSize = 15.fsp())
    }
}

@Composable
private fun DashedLine() {
    Canvas(Modifier.fillMaxWidth().padding(vertical = 10.dp).height(2.dp)) {
        drawLine(
            Color(0xFFCCBBBB), Offset(0f, size.height / 2), Offset(size.width, size.height / 2), strokeWidth = size.height,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())),
        )
    }
}

/** תנועת העגלה אל מחוץ למסך */
suspend fun FxState.driveAway(screenWidth: Float, px: Float) {
    kotlinx.coroutines.coroutineScope {
        launch {
            cartRot.animateTo(-3f, keyframes { durationMillis = 1100; 3f at 220 })
        }
        cartDrive.animateTo(-screenWidth * 1.3f, keyframes {
            durationMillis = 1100
            -12f * px at 220 using androidx.compose.animation.core.FastOutLinearInEasing
            -screenWidth * 1.3f at 1100
        })
    }
}
