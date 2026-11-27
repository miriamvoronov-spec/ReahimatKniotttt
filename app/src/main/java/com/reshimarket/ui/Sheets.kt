package com.reshimarket.ui

import android.app.backup.BackupManager
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reshimarket.data.CategoryWithItems
import com.reshimarket.data.EmojiTarget
import com.reshimarket.data.ShoppingList
import com.reshimarket.data.ThemeMode
import com.reshimarket.domain.Emojis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SheetHost(vm: MainViewModel, cats: List<CategoryWithItems>, lists: List<ShoppingList>, onBump: () -> Unit) {
    val s = vm.sheet ?: return
    val c = LocalAppColors.current
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = { vm.sheet = null },
        sheetState = state,
        containerColor = c.card,
        contentColor = c.ink,
        scrimColor = Color(0x73140A05),
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = c.line) },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 18.dp, end = 18.dp, bottom = 28.dp)
                .navigationBarsPadding(),
        ) {
            when (s) {
                is Sheet.EditItem -> ItemSheet(vm, s, cats)
                is Sheet.EditCategory -> CategorySheet(vm, s)
                is Sheet.EditList -> ListSheet(vm, s)
                is Sheet.PickEmoji -> EmojiPickerSheet(vm, s)
                is Sheet.EmojiManager -> EmojiManagerSheet(vm, s, cats, lists)
                is Sheet.Confirm -> ConfirmSheet(vm, s)
                Sheet.Settings -> SettingsSheet(vm)
                Sheet.Cart -> CartSheet(vm, cats, onBump)
                Sheet.Share -> ShareSheet(vm)
                Sheet.ImportText -> ImportTextSheet(vm)
            }
        }
    }
}

// ---------- עריכת מוצר ----------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ItemSheet(vm: MainViewModel, s: Sheet.EditItem, cats: List<CategoryWithItems>) {
    val d = s.draft
    val item = cats.flatMap { it.items }.firstOrNull { it.id == s.itemId }
    SheetTitle("${d.emoji} ${item?.name ?: d.name}")
    Field("שם", d.name, { d.name = it })
    EmojiButtonRow(d.emoji) {
        vm.sheet = Sheet.PickEmoji("מוצר ״${d.name}״", d.emoji, { e -> d.emoji = e; d.manualEmoji = true }, s)
    }
    Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("כמות", modifier = Modifier.weight(1f))
        QtyButton("−", "פחות") { d.qty = (d.qty - 1).coerceAtLeast(1) }
        Text(d.qty.toString(), fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp))
        QtyButton("＋", "יותר") { d.qty = (d.qty + 1).coerceAtMost(99) }
    }
    Text("קטגוריה", fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 14.dp, bottom = 8.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (cc in cats) {
            Chip("${cc.category.emoji} ${cc.category.name}", selected = cc.category.id == d.categoryId, height = 40.dp) { d.categoryId = cc.category.id }
        }
    }
    ButtonRow {
        AppButton("שמירה") { vm.saveItem(s) }
        AppButton("מחיקה", BtnKind.Danger) { vm.deleteItem(s.itemId) }
        AppButton("סגירה", BtnKind.Ghost) { vm.sheet = null }
    }
}

@Composable
private fun QtyButton(text: String, label: String, onClick: () -> Unit) {
    val c = LocalAppColors.current
    val shape = RoundedCornerShape(12.dp)
    Box(
        Modifier.size(44.dp).clip(shape).border(1.dp, c.line, shape).clickable(role = Role.Button, onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(text, fontSize = 20.sp) }
}

// ---------- עריכת קטגוריה ----------

@Composable
private fun CategorySheet(vm: MainViewModel, s: Sheet.EditCategory) {
    val d = s.draft
    val focus = remember { FocusRequester() }
    if (s.categoryId == null) LaunchedEffect(Unit) { delay(150); runCatching { focus.requestFocus() } }
    SheetTitle(if (s.categoryId == null) "קטגוריה חדשה" else "עריכת קטגוריה")
    Field("שם", d.name, { d.name = it }, placeholder = "למשל: קפואים", focusRequester = focus)
    Row(Modifier.padding(top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("צבע הכותרת", modifier = Modifier.weight(1f))
        Text(
            d.name.ifBlank { "דוגמה" },
            color = lerp(LocalAppColors.current.ink, Color(d.color), 0.82f),
            fontWeight = FontWeight.ExtraBold,
            fontSize = 18.sp,
        )
    }
    ColorWheel(Color(d.color), { d.color = it.toArgb() })
    EmojiButtonRow(d.emoji.ifEmpty { Emojis.forCategory(d.name) }) {
        vm.sheet = Sheet.PickEmoji(
            "קטגוריה" + (if (d.name.isNotBlank()) " ״${d.name}״" else ""),
            d.emoji.ifEmpty { Emojis.forCategory(d.name) },
            { e -> d.emoji = e },
            s,
        )
    }
    ButtonRow {
        AppButton("שמירה") { vm.saveCategory(s) }
        if (s.categoryId != null) AppButton("מחיקת קטגוריה", BtnKind.Danger) { vm.deleteCategory(s.categoryId) }
        AppButton("סגירה", BtnKind.Ghost) { vm.sheet = null }
    }
}

// ---------- עריכת רשימה ----------

@Composable
private fun ListSheet(vm: MainViewModel, s: Sheet.EditList) {
    val d = s.draft
    val isNew = s.listId == null
    SheetTitle(if (isNew) "רשימה חדשה" else "עריכת הרשימה")
    Field("שם", d.name, { d.name = it }, placeholder = "למשל: שוק", onDone = { vm.saveList(s) })
    val shown = d.emoji.ifEmpty { Emojis.forList(d.name) }
    EmojiButtonRow(shown) {
        vm.sheet = Sheet.PickEmoji("רשימה" + (if (d.name.isNotBlank()) " ״${d.name}״" else ""), shown, { e -> d.emoji = e }, s)
    }
    ButtonRow {
        AppButton(if (isNew) "יצירה" else "שמירה") { vm.saveList(s) }
        if (!isNew) AppButton("מחיקת הרשימה", BtnKind.Danger) { vm.deleteList(s.listId!!) }
        AppButton("סגירה", BtnKind.Ghost) { vm.sheet = null }
    }
}

// ---------- אישור מחיקה ----------

@Composable
private fun ConfirmSheet(vm: MainViewModel, s: Sheet.Confirm) {
    SheetTitle(s.message)
    Muted("אי אפשר לבטל את הפעולה הזו.")
    ButtonRow {
        AppButton("כן, להמשיך", BtnKind.Danger) {
            val f = s.onYes
            vm.sheet = null
            f()
        }
        AppButton("לא", BtnKind.Ghost) { vm.sheet = null }
    }
}

// ---------- חלון בחירת אימוג'י ----------

private fun firstGrapheme(s: String): String? {
    val t = s.trim()
    if (t.isEmpty()) return null
    val it = android.icu.text.BreakIterator.getCharacterInstance()
    it.setText(t)
    val end = it.next()
    return if (end == android.icu.text.BreakIterator.DONE) t else t.substring(0, end)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmojiPickerSheet(vm: MainViewModel, s: Sheet.PickEmoji) {
    val c = LocalAppColors.current
    var sel by remember(s) { mutableStateOf(s.current) }
    var group by rememberSaveable { mutableIntStateOf(0) }
    var typed by remember(s) { mutableStateOf("") }
    SheetTitle("בחירת אימוג׳י")
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
        Box(
            Modifier.size(72.dp).clip(RoundedCornerShape(20.dp)).background(c.sun.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center,
        ) { Text(firstGrapheme(typed) ?: sel, fontSize = 50.fsp()) }
        Spacer(Modifier.width(14.dp))
        Column {
            Text("ל${s.title}", fontWeight = FontWeight.Bold)
            Muted("עכשיו: ${s.current}")
        }
    }
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Emojis.GROUPS.forEachIndexed { i, (name, _) ->
            Chip(name, selected = i == group, height = 40.dp) { group = i }
        }
    }
    FlowRow(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (e in Emojis.GROUPS[group].second) {
            val on = e == sel && typed.isBlank()
            val shape = RoundedCornerShape(12.dp)
            Box(
                Modifier
                    .size(50.dp)
                    .clip(shape)
                    .background(if (on) c.sun.copy(alpha = 0.45f) else Color.Transparent)
                    .then(if (on) Modifier.border(2.dp, c.sun, shape) else Modifier)
                    .clickable(role = Role.Button) {
                        sel = e
                        typed = ""
                    },
                contentAlignment = Alignment.Center,
            ) { Text(e, fontSize = 28.fsp()) }
        }
    }
    Field("או להקליד כל אימוג׳י מהמקלדת", typed, { typed = it }, placeholder = "🙂")
    ButtonRow {
        AppButton("אישור") {
            val chosen = firstGrapheme(typed) ?: sel
            s.onPick(chosen)
            vm.sheet = s.back
            vm.showToast("האימוג׳י עודכן ל-$chosen")
        }
        AppButton("ביטול", BtnKind.Ghost) { vm.sheet = s.back }
    }
}

// ---------- מנהל האימוג'ים ----------

private data class EmojiRow(val id: Long, val emoji: String, val name: String, val unknown: Boolean)

@Composable
private fun EmojiManagerSheet(vm: MainViewModel, s: Sheet.EmojiManager, cats: List<CategoryWithItems>, lists: List<ShoppingList>) {
    val c = LocalAppColors.current
    SheetTitle("שינוי אימוג׳ים")
    Muted("למה רוצים לבחור אימוג׳י?", Modifier.padding(bottom = 8.dp))
    val targets = listOf(EmojiTarget.LIST, EmojiTarget.CATEGORY, EmojiTarget.ITEM)
    Seg(listOf("רשימה", "קטגוריה", "מוצר"), targets.indexOf(s.target)) { vm.sheet = Sheet.EmojiManager(targets[it]) }
    val rows = when (s.target) {
        EmojiTarget.LIST -> lists.map { EmojiRow(it.id, it.emoji, it.name, it.emoji == Emojis.DEFAULT_LIST) }
        EmojiTarget.CATEGORY -> cats.map { EmojiRow(it.category.id, it.category.emoji, it.category.name, it.category.emoji == Emojis.UNKNOWN_CATEGORY) }
        EmojiTarget.ITEM -> cats.flatMap { cc -> cc.sortedItems.map { EmojiRow(it.id, it.emoji, it.name, it.emoji == Emojis.UNKNOWN_ITEM) } }
    }.sortedBy { !it.unknown }
    val kind = when (s.target) {
        EmojiTarget.LIST -> "רשימה"
        EmojiTarget.CATEGORY -> "קטגוריה"
        EmojiTarget.ITEM -> "מוצר"
    }
    Spacer(Modifier.height(12.dp))
    if (rows.isEmpty()) Muted("אין כאן עדיין כלום")
    for (r in rows) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp)
                .heightIn(min = 52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(c.ink.copy(alpha = 0.05f))
                .clickable(role = Role.Button) { vm.pickEmojiFor(s.target, r.id, "$kind ״${r.name}״", r.emoji) }
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(r.emoji, fontSize = 26.sp, modifier = Modifier.width(44.dp))
            Text(r.name, modifier = Modifier.weight(1f))
            if (r.unknown) Pill("לא זוהה", c.sun, Color(0xFF2A1A10)) else Muted("שינוי ›")
        }
    }
    ButtonRow {
        AppButton("חזרה להגדרות", BtnKind.Ghost) { vm.sheet = Sheet.Settings }
        AppButton("סגירה") { vm.sheet = null }
    }
}

// ---------- העגלה ----------

@Composable
private fun CartSheet(vm: MainViewModel, cats: List<CategoryWithItems>, onBump: () -> Unit) {
    val c = LocalAppColors.current
    val items = cats.flatMap { it.sortedItems }.filter { !it.have || it.inCart }
    SheetTitle("העגלה שלי")
    Muted("לחיצה על מוצר מסמנת ״קניתי״, ולחיצה נוספת מחזירה אותו לרשימה כחסר", Modifier.padding(bottom = 8.dp))
    if (items.isEmpty()) Text("העגלה ריקה. כשמסמנים מוצר כ״קניתי״ הוא קופץ לכאן.")
    for (x in items) {
        val inCart = x.inCart
        Row(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp)
                .heightIn(min = 52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (inCart) c.grnBg else c.redBg)
                .clickable(role = Role.Button) {
                    if (!inCart) onBump()
                    vm.toggleInCart(x)
                }
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(x.emoji, fontSize = 26.sp, modifier = Modifier.width(44.dp))
            Text(
                x.name + if (x.qty > 1) " ×${x.qty}" else "",
                textDecoration = if (inCart) TextDecoration.LineThrough else null,
                color = if (inCart) c.ink.copy(alpha = 0.7f) else c.ink,
                modifier = Modifier.weight(1f),
            )
            Pill(if (inCart) "✓ קניתי" else "עוד לא", if (inCart) c.grn else c.red)
        }
    }
    ButtonRow { AppButton("סגירה") { vm.sheet = null } }
}

// ---------- שיתוף ----------

fun shareText(context: Context, text: String, whatsapp: Boolean) {
    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
    if (whatsapp) {
        try {
            context.startActivity(Intent(send).setPackage("com.whatsapp"))
            return
        } catch (_: ActivityNotFoundException) {
        }
    }
    context.startActivity(Intent.createChooser(send, "שיתוף הרשימה"))
}

fun copyText(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("רשימרקט", text))
}

@Composable
private fun ShareSheet(vm: MainViewModel) {
    val c = LocalAppColors.current
    val context = LocalContext.current
    val text = vm.shareText()
    SheetTitle("שיתוף לוואטסאפ")
    Seg(listOf("רק מה שחסר", "הכל"), if (vm.shareAll) 1 else 0) { vm.shareAll = it == 1 }
    Box(
        Modifier
            .padding(top = 12.dp)
            .fillMaxWidth()
            .heightIn(max = 320.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(c.ink.copy(alpha = 0.06f))
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
    ) { Text(text, fontSize = 15.sp, lineHeight = 22.sp) }
    ButtonRow {
        AppButton("וואטסאפ", BtnKind.WhatsApp) { shareText(context, text, whatsapp = true) }
        AppButton("שיתוף…", BtnKind.Ghost) { shareText(context, text, whatsapp = false) }
        AppButton("העתקה", BtnKind.Ghost) {
            copyText(context, text)
            vm.showToast("הרשימה הועתקה")
        }
        AppButton("סגירה", BtnKind.Ghost) { vm.sheet = null }
    }
}

// ---------- ייבוא מטקסט ----------

@Composable
private fun ImportTextSheet(vm: MainViewModel) {
    val context = LocalContext.current
    var text by rememberSaveable { mutableStateOf("") }
    SheetTitle("ייבוא רשימה מטקסט")
    Muted("מדביקים רשימה מוואטסאפ או מהפתקים. שורה שמסתיימת בנקודתיים היא קטגוריה, וכל שורה אחרת היא מוצר. המוצרים נכנסים לרשימה הפעילה.")
    Field(null, text, { text = it }, placeholder = "מוצרי חלב:\nחלב\nקוטג'\n\nירקות:\nעגבניות", singleLine = false, minHeight = 180.dp)
    ButtonRow {
        AppButton("ייבוא") { vm.importText(text) }
        AppButton("הדבקה", BtnKind.Ghost) {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = cm.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(context)?.toString()
            if (clip.isNullOrBlank()) vm.showToast("אין טקסט בלוח") else text = clip
        }
        AppButton("סגירה", BtnKind.Ghost) { vm.sheet = null }
    }
}

// ---------- הגדרות ----------

@Composable
private fun SectionTitle(text: String) {
    Text(text, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, modifier = Modifier.padding(top = 20.dp, bottom = 6.dp))
}

@Composable
private fun SettingsSheet(vm: MainViewModel) {
    val c = LocalAppColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val s by vm.settings.collectAsStateWithLifecycle()
    var editing by remember { mutableIntStateOf(0) } // 0 = סגור, 1 = עליון, 2 = תחתון, 3 = צבע כתב

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            val ok = runCatching {
                val json = vm.exportJson()
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) } ?: error("no stream")
                }
            }.isSuccess
            vm.showToast(if (ok) "הגיבוי נשמר" else "לא הצלחתי לשמור את הקובץ")
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            val text = runCatching {
                withContext(Dispatchers.IO) { context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() } }
            }.getOrNull()
            if (text == null) vm.showToast("לא הצלחתי לקרוא את הקובץ") else vm.importJson(text)
        }
    }
    fun backupName() = "reshimarket-${SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())}.json"

    SheetTitle("הגדרות")

    Text("מצב תצוגה", fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 8.dp))
    val modes = listOf(ThemeMode.AUTO, ThemeMode.DAY, ThemeMode.NIGHT)
    Seg(listOf("אוטומטי", "יום", "לילה"), modes.indexOf(s.mode)) { i -> vm.updateSettings { it.copy(mode = modes[i]) } }

    Text("גודל כתב: ${s.fontSize}", fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 16.dp))
    Slider(
        value = s.fontSize.toFloat(),
        onValueChange = { v -> vm.updateSettings { it.copy(fontSize = kotlin.math.round(v).toInt()) } },
        valueRange = 14f..24f,
        steps = 9,
        colors = SliderDefaults.colors(thumbColor = c.red, activeTrackColor = c.red, inactiveTrackColor = c.line),
    )
    Row(Modifier.fillMaxWidth()) {
        Muted("רגיל", size = 13.sp)
        Spacer(Modifier.weight(1f))
        Muted("גדול במיוחד לסופר", size = 13.sp)
    }

    Text("צבע כתב", fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Swatch("כתב", c.ink, editing == 3) { editing = if (editing == 3) 0 else 3 }
        Text("ככה ייראה הטקסט", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        AppButton("ברירת מחדל", BtnKind.Ghost) {
            if (editing == 3) editing = 0
            vm.updateSettings { it.copy(textColor = null) }
        }
    }
    if (editing == 3) {
        androidx.compose.runtime.key(3) {
            ColorWheel(c.ink, { col -> vm.updateSettings { it.copy(textColor = col.toArgb()) } }, Modifier.padding(top = 10.dp))
        }
    }

    SectionTitle("רקע")
    ToggleRow("מדפי סופר ברקע", s.shelves) { v -> vm.updateSettings { it.copy(shelves = v) } }
    Text("גרדיאנט בשני צבעים", fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Swatch("למעלה", s.gradientTop?.let { Color(it) } ?: c.bg, editing == 1) { editing = if (editing == 1) 0 else 1 }
        Swatch("למטה", s.gradientBottom?.let { Color(it) } ?: c.bg, editing == 2) { editing = if (editing == 2) 0 else 2 }
        AppButton("ברירת מחדל", BtnKind.Ghost) {
            editing = 0
            vm.updateSettings { it.copy(gradientTop = null, gradientBottom = null) }
        }
    }
    if (editing == 1 || editing == 2) {
        val start = (if (editing == 1) s.gradientTop else s.gradientBottom)?.let { Color(it) } ?: c.bg
        androidx.compose.runtime.key(editing) {
            ColorWheel(start, { col ->
                val v = col.toArgb()
                vm.updateSettings {
                    val top = it.gradientTop ?: c.bg.toArgb()
                    val bottom = it.gradientBottom ?: c.bg.toArgb()
                    if (editing == 1) it.copy(gradientTop = v, gradientBottom = bottom) else it.copy(gradientTop = top, gradientBottom = v)
                }
            }, Modifier.padding(top = 10.dp))
        }
    }

    SectionTitle("אימוג׳ים וצלילים")
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(RoundedCornerShape(14.dp)).background(c.ink.copy(alpha = 0.05f))
            .clickable(role = Role.Button) { vm.sheet = Sheet.EmojiManager(EmojiTarget.ITEM) }.padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("😀", fontSize = 26.sp, modifier = Modifier.width(44.dp))
        Column(Modifier.weight(1f)) {
            Text("אימוג׳ים", fontWeight = FontWeight.Bold)
            Muted("בחירה ושינוי לרשימות, קטגוריות ומוצרים", size = 13.sp)
        }
        Muted("›")
    }
    ToggleRow("צלילים", s.sounds) { v -> vm.updateSettings { it.copy(sounds = v) } }

    SectionTitle("מה יופיע במסך הבית")
    ToggleRow("כרטיס סיכום החסרים", s.showSummary) { v -> vm.updateSettings { it.copy(showSummary = v) } }
    ToggleRow("שורת חיפוש", s.showSearch) { v -> vm.updateSettings { it.copy(showSearch = v) } }
    ToggleRow("כפתורי ״רק מה שחסר״ ו״סיימתי קנייה״", s.showActions) { v -> vm.updateSettings { it.copy(showActions = v) } }

    SectionTitle("גיבוי, ייצוא וייבוא")
    Text("גיבוי אוטומטי ל-Google Drive", fontWeight = FontWeight.Bold)
    Muted(
        "האפליקציה נכללת בגיבוי של אנדרואיד, שנשמר ב-Google Drive של חשבון ה-Google שמחובר לטלפון. " +
            "כדי לוודא שהוא פעיל: הגדרות הטלפון ← Google ← גיבוי. אפשר גם לשמור קובץ גיבוי ישירות ל-Drive.",
        size = 13.sp,
    )
    ButtonRow {
        AppButton("שמירת גיבוי ל-Drive") {
            BackupManager(context).dataChanged()
            exportLauncher.launch(backupName())
        }
        AppButton("הגדרות הגיבוי בטלפון", BtnKind.Ghost) {
            runCatching { context.startActivity(Intent(Settings.ACTION_SETTINGS)) }
        }
    }
    Spacer(Modifier.height(8.dp))
    ButtonRow {
        AppButton("ייצוא JSON", BtnKind.Ghost) { exportLauncher.launch(backupName()) }
        AppButton("ייבוא JSON", BtnKind.Ghost) { importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }
        AppButton("ייבוא מטקסט", BtnKind.Ghost) { vm.sheet = Sheet.ImportText }
    }

    ButtonRow { AppButton("סיום") { vm.sheet = null } }
}

@Composable
private fun Swatch(label: String, color: Color, selected: Boolean, onClick: () -> Unit) {
    val c = LocalAppColors.current
    val shape = RoundedCornerShape(12.dp)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(width = 56.dp, height = 44.dp).clip(shape).background(color)
                .border(if (selected) 3.dp else 1.dp, if (selected) c.sun else c.line, shape)
                .clickable(role = Role.Button, onClickLabel = "צבע $label", onClick = onClick),
        )
        Muted(label, size = 12.sp)
    }
}
