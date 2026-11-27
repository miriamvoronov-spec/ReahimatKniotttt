package com.reshimarket.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** גודל טקסט קבוע שלא מושפע מהגדרת גודל הכתב (ללוגו, לעגלה ולקבלה) */
@Composable
fun Int.fsp(): TextUnit = with(LocalDensity.current) { this@fsp.dp.toSp() }

fun Modifier.dashedBorder(color: Color, width: Dp, radius: Dp, dash: Dp = 6.dp): Modifier = drawBehind {
    val w = width.toPx()
    drawRoundRect(
        color = color,
        topLeft = androidx.compose.ui.geometry.Offset(w / 2, w / 2),
        size = androidx.compose.ui.geometry.Size(size.width - w, size.height - w),
        cornerRadius = CornerRadius(radius.toPx()),
        style = Stroke(width = w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash.toPx(), dash.toPx() * 0.7f))),
    )
}

enum class BtnKind { Primary, Ghost, Danger, WhatsApp }

@Composable
fun AppButton(text: String, kind: BtnKind = BtnKind.Primary, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = LocalAppColors.current
    val (bg, fg) = when (kind) {
        BtnKind.Primary -> c.ink to c.bg
        BtnKind.Ghost -> Color.Transparent to c.ink
        BtnKind.Danger -> c.red to Color.White
        BtnKind.WhatsApp -> Color(0xFF25A35A) to Color.White
    }
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier
            .heightIn(min = 46.dp)
            .clip(shape)
            .background(bg)
            .then(if (kind == BtnKind.Ghost) Modifier.border(1.dp, c.line, shape) else Modifier)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = fg, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ButtonRow(content: @Composable FlowRowScope.() -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}

@Composable
fun SheetTitle(text: String) {
    Text(text, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(bottom = 12.dp))
}

@Composable
fun Muted(text: String, modifier: Modifier = Modifier, size: TextUnit = 14.sp) {
    Text(text, color = LocalAppColors.current.mut, fontSize = size, modifier = modifier)
}

@Composable
fun Pill(text: String, bg: Color, fg: Color = Color.White, modifier: Modifier = Modifier) {
    Box(modifier.clip(RoundedCornerShape(999.dp)).background(bg).padding(horizontal = 10.dp, vertical = 4.dp)) {
        Text(text, color = fg, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

/** כפתור עגול-מלבני בגובה 40 כמו הלשוניות והצ'יפים בדמו */
@Composable
fun Chip(text: String, selected: Boolean = false, selectedBg: Color? = null, height: Dp = 42.dp, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = LocalAppColors.current
    val bg = if (selected) selectedBg ?: c.ink else c.card
    val fg = if (selected) (if (selectedBg != null) Color.White else c.bg) else c.ink
    val shape = RoundedCornerShape(999.dp)
    Box(
        modifier
            .heightIn(min = maxOf(height, 44.dp))
            .clip(shape)
            .background(bg)
            .border(1.dp, if (selected) bg else c.line, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = fg, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

@Composable
fun Seg(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val c = LocalAppColors.current
    val shape = RoundedCornerShape(14.dp)
    Row(Modifier.fillMaxWidth().clip(shape).border(1.dp, c.line, shape)) {
        options.forEachIndexed { i, o ->
            Box(
                Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
                    .background(if (i == selected) c.ink else Color.Transparent)
                    .clickable(role = Role.Tab) { onSelect(i) },
                contentAlignment = Alignment.Center,
            ) {
                Text(o, color = if (i == selected) c.bg else c.ink, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun ToggleRow(text: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val c = LocalAppColors.current
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(role = Role.Switch) { onChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = c.grn, checkedThumbColor = Color.White, uncheckedTrackColor = c.line, uncheckedThumbColor = c.mut, uncheckedBorderColor = c.line),
        )
    }
}

@Composable
fun Field(
    label: String?,
    value: String,
    onChange: (String) -> Unit,
    placeholder: String = "",
    singleLine: Boolean = true,
    minHeight: Dp = 46.dp,
    focusRequester: FocusRequester? = null,
    imeAction: ImeAction = ImeAction.Done,
    onDone: (() -> Unit)? = null,
) {
    val c = LocalAppColors.current
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        if (label != null) Text(label, fontWeight = FontWeight.Medium, fontSize = 15.sp, modifier = Modifier.padding(bottom = 6.dp))
        val shape = RoundedCornerShape(14.dp)
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = singleLine,
            textStyle = TextStyle(color = c.ink, fontFamily = Rubik, fontSize = 16.sp),
            cursorBrush = SolidColor(c.ink),
            keyboardOptions = KeyboardOptions(imeAction = if (singleLine) imeAction else ImeAction.Default),
            keyboardActions = KeyboardActions(onDone = { onDone?.invoke() }),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight)
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
            decorationBox = { inner ->
                Box(
                    Modifier.fillMaxWidth().heightIn(min = minHeight).clip(shape).background(c.card).border(1.dp, c.line, shape).padding(horizontal = 14.dp, vertical = 12.dp),
                    contentAlignment = if (singleLine) Alignment.CenterStart else Alignment.TopStart,
                ) {
                    if (value.isEmpty()) Text(placeholder, color = c.mut, fontSize = 16.sp)
                    inner()
                }
            },
        )
    }
}

/** שורת "אימוג'י: [🧀] לחיצה לבחירה" במסכי העריכה */
@Composable
fun EmojiButtonRow(emoji: String, onClick: () -> Unit) {
    val c = LocalAppColors.current
    val shape = RoundedCornerShape(14.dp)
    Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("אימוג׳י")
        Spacer(Modifier.width(10.dp))
        Box(
            Modifier.widthIn(min = 60.dp).heightIn(min = 52.dp).clip(shape).border(1.dp, c.line, shape).clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) { Text(emoji, fontSize = 30.fsp()) }
        Spacer(Modifier.width(10.dp))
        Muted("לחיצה לבחירה", size = 13.sp)
    }
}

@Composable
fun IconBox(contentDescription: String, onClick: () -> Unit, content: @Composable () -> Unit) {
    val c = LocalAppColors.current
    val shape = RoundedCornerShape(14.dp)
    Box(
        Modifier
            .size(44.dp)
            .clip(shape)
            .background(c.card)
            .border(1.dp, c.line, shape)
            .clickable(role = Role.Button, onClickLabel = contentDescription, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}
