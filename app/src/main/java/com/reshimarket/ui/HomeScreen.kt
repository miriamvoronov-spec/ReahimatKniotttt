package com.reshimarket.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.Settings
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reshimarket.R
import com.reshimarket.data.Category
import com.reshimarket.data.CategoryWithItems
import com.reshimarket.data.Item
import com.reshimarket.domain.Emojis
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ---------- שורות הרשימה (רשימה שטוחה אחת, כדי שאפשר יהיה לגרור מוצר בין קטגוריות) ----------

private sealed interface Entry {
    val key: String
    val categoryId: Long

    data class Header(val cat: CategoryWithItems, val missing: Int, val open: Boolean, val index: Int, val count: Int) : Entry {
        override val key get() = "c${cat.category.id}"
        override val categoryId get() = cat.category.id
    }

    data class ItemRow(val item: Item) : Entry {
        override val key get() = "i${item.id}"
        override val categoryId get() = item.categoryId
    }

    data class Empty(override val categoryId: Long, val text: String) : Entry {
        override val key get() = "e$categoryId"
    }

    data class Add(val cat: Category) : Entry {
        override val key get() = "a${cat.id}"
        override val categoryId get() = cat.id
    }
}

private fun buildEntries(cats: List<CategoryWithItems>, query: String, only: Boolean): List<Entry> {
    val q = Emojis.norm(query)
    val out = mutableListOf<Entry>()
    cats.forEachIndexed { ci, c ->
        val its = c.sortedItems
            .filter { (!only || !it.have) && (q.isEmpty() || Emojis.norm(it.name).contains(q)) }
            .sortedBy { it.have } // האדומים (חסרים) קודם, ושאר הסדר נשמר
        if (q.isNotEmpty() && its.isEmpty()) return@forEachIndexed
        val open = c.category.expanded || q.isNotEmpty()
        out += Entry.Header(c, c.items.count { !it.have }, open, ci, cats.size)
        if (open) {
            if (its.isEmpty()) out += Entry.Empty(c.category.id, if (only) "אין כאן חסרים" else "עדיין אין מוצרים")
            its.forEach { out += Entry.ItemRow(it) }
            out += Entry.Add(c.category)
        }
    }
    return out
}

// ---------- גרירה ----------

private class DragController(val list: LazyListState, val vm: MainViewModel) {
    var key by mutableStateOf<String?>(null)
    var offset by mutableFloatStateOf(0f)
    var targetKey by mutableStateOf<String?>(null)
    var targetAfter by mutableStateOf(false)
    var entries: Map<String, Entry> = emptyMap()
    private var edge = 0f

    fun start(k: String, edgePx: Float) {
        key = k
        offset = 0f
        targetKey = null
        edge = edgePx
    }

    fun drag(dy: Float) {
        val k = key ?: return
        offset += dy
        val info = list.layoutInfo
        val me = info.visibleItemsInfo.firstOrNull { it.key == k } ?: return
        val center = me.offset + offset + me.size / 2f
        // גלילה אוטומטית ליד הקצוות
        val scroll = when {
            center < info.viewportStartOffset + edge -> -12f
            center > info.viewportEndOffset - edge -> 12f
            else -> 0f
        }
        if (scroll != 0f) offset += list.dispatchRawDelta(scroll)
        val hit = info.visibleItemsInfo.firstOrNull {
            it.key != k && entries.containsKey(it.key) && center >= it.offset && center < it.offset + it.size
        }
        targetKey = hit?.key as? String
        targetAfter = hit != null && center > hit.offset + hit.size / 2f
    }

    fun end() {
        val k = key
        val t = targetKey?.let { entries[it] }
        val after = targetAfter
        reset()
        val dragged = k?.let { entries[it] } ?: return
        if (t == null) return
        when (dragged) {
            is Entry.Header -> {
                val target = t.categoryId
                if (target != dragged.categoryId) vm.dropCategory(dragged.categoryId, target, after = t !is Entry.Header || after)
            }
            is Entry.ItemRow -> when (t) {
                is Entry.ItemRow -> vm.dropItem(dragged.item.id, t.item.categoryId, t.item.id, after)
                is Entry.Header -> {
                    val first = t.cat.sortedItems.firstOrNull { it.id != dragged.item.id }
                    vm.dropItem(dragged.item.id, t.categoryId, first?.id, false)
                }
                else -> vm.dropItem(dragged.item.id, t.categoryId, null, true)
            }
            else -> Unit
        }
    }

    fun reset() {
        key = null
        offset = 0f
        targetKey = null
    }
}

private fun Modifier.dragHandle(drag: DragController, key: String): Modifier = pointerInput(key) {
    detectDragGestures(
        onDragStart = { drag.start(key, 90.dp.toPx()) },
        onDragEnd = { drag.end() },
        onDragCancel = { drag.reset() },
        onDrag = { change, amount ->
            change.consume()
            drag.drag(amount.y)
        },
    )
}

private fun Modifier.dragged(drag: DragController, key: String): Modifier =
    if (drag.key == key) zIndex(10f).graphicsLayer {
        translationY = drag.offset
        shadowElevation = 14.dp.toPx()
        alpha = 0.95f
    } else this

private fun Modifier.dropIndicator(drag: DragController, key: String, color: Color): Modifier =
    if (drag.targetKey == key) drawWithContent {
        drawContent()
        val h = 4.dp.toPx()
        drawRect(color, Offset(0f, if (drag.targetAfter) size.height - h else 0f), Size(size.width, h))
    } else this

// ---------- המסך הראשי ----------

@Composable
fun App(vm: MainViewModel) {
    val c = LocalAppColors.current
    val context = LocalContext.current
    val settings by vm.settings.collectAsStateWithLifecycle()
    val lists by vm.lists.collectAsStateWithLifecycle()
    val current by vm.currentList.collectAsStateWithLifecycle()
    val cats by vm.categories.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val fx = remember { FxState(scope) }
    val reduceMotion = remember {
        runCatching { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }.getOrDefault(false)
    }

    var micCategory by remember { mutableLongStateOf(0L) }
    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        val text = res.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (!text.isNullOrBlank()) vm.addItem(micCategory, text)
    }
    fun startMic(cat: Category) {
        micCategory = cat.id
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "he-IL")
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "he-IL")
            .putExtra(RecognizerIntent.EXTRA_PROMPT, "מה להוסיף ל${cat.name}?")
            .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        try {
            micLauncher.launch(intent)
        } catch (_: ActivityNotFoundException) {
            vm.showToast("אין במכשיר זיהוי דיבור. אפשר להתקין את אפליקציית Google")
        }
    }

    fun onItemClick(item: Item) {
        if (!item.have) {
            val from = fx.emojiPos[item.id]
            vm.setHave(item, true)
            if (from != null && !reduceMotion) {
                fx.flights += Flight(item.emoji, from, fx.cartCenter, { fx.bump() })
                Sounds.pop(0.75)
            } else {
                fx.bump()
                Sounds.pop()
            }
        } else {
            vm.setHave(item, false)
            Sounds.back(if (reduceMotion) 0.0 else 0.6)
            if (reduceMotion) return
            fx.hidden += item.id
            scope.launch {
                delay(120) // מחכים שהשורה תתמקם מחדש ברשימה
                val to = fx.emojiPos[item.id]
                if (to == null) {
                    fx.hidden.remove(item.id)
                } else {
                    fx.flights += Flight(item.emoji, fx.cartCenter, to, { fx.hidden.remove(item.id) })
                }
            }
        }
    }

    val missingAll = cats.sumOf { cc -> cc.items.count { !it.have } }
    val inCart = cats.sumOf { cc -> cc.items.count { it.inCart } }

    Box(
        Modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(c.bg)
                val g1 = settings.gradientTop
                val g2 = settings.gradientBottom
                if (g1 != null && g2 != null) drawGradient(Color(g1), Color(g2))
                if (settings.shelves) drawShelves(c)
            },
    ) {
        Column(Modifier.fillMaxSize().imePadding()) {
            TopBar(onShare = { vm.sheet = Sheet.Share }, onSettings = { vm.sheet = Sheet.Settings })
            HomeList(
                vm = vm,
                fx = fx,
                lists = lists,
                currentId = current?.id,
                cats = cats,
                missingAll = missingAll,
                showSummary = settings.showSummary,
                showSearch = settings.showSearch,
                showActions = settings.showActions,
                onItemClick = ::onItemClick,
                onMic = ::startMic,
            )
        }

        CartButton(fx, inCart) { vm.sheet = Sheet.Cart }
        FlightsLayer(fx)
        ToastHost(vm)
        FinishLayer(vm, fx, reduceMotion)
        SheetHost(vm, cats, lists, onBump = { fx.bump() })
    }
}

@Composable
private fun TopBar(onShare: () -> Unit, onSettings: () -> Unit) {
    val c = LocalAppColors.current
    BoxWithConstraints(Modifier.fillMaxWidth().background(c.bg.copy(alpha = 0.86f)).statusBarsPadding()) {
        val narrow = maxWidth < 400.dp
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painterResource(R.drawable.ic_logo),
                contentDescription = "הלוגו של רשימרקט: סל קש מלא מוצרים",
                modifier = Modifier.size(if (narrow) 50.dp else 62.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "רשימרקט",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = (if (narrow) 28 else 32).fsp(),
                    color = c.brand,
                    letterSpacing = (-0.02).em,
                    lineHeight = (if (narrow) 30 else 34).fsp(),
                    maxLines = 1,
                )
                Text("הסופר שלך בכיס", color = c.mut, fontSize = 14.fsp(), maxLines = 1)
            }
            IconBox("שיתוף", onShare) { LineIcon(Icons.share, c.ink) }
            Spacer(Modifier.width(6.dp))
            IconBox("הגדרות", onSettings) { LineIcon(Icons.settings, c.ink) }
        }
    }
}

@Composable
private fun HomeList(
    vm: MainViewModel,
    fx: FxState,
    lists: List<com.reshimarket.data.ShoppingList>,
    currentId: Long?,
    cats: List<CategoryWithItems>,
    missingAll: Int,
    showSummary: Boolean,
    showSearch: Boolean,
    showActions: Boolean,
    onItemClick: (Item) -> Unit,
    onMic: (Category) -> Unit,
) {
    val c = LocalAppColors.current
    val listState = rememberLazyListState()
    val drag = remember(listState) { DragController(listState, vm) }
    val entries = remember(cats, vm.query, vm.onlyMissing) { buildEntries(cats, vm.query, vm.onlyMissing) }
    drag.entries = remember(entries) { entries.associateBy { it.key } }
    val prefix = 1 + (if (showSummary) 1 else 0) + (if (showSearch) 1 else 0) + (if (showActions) 1 else 0)

    // גלילה אל מוצר שכבר קיים
    LaunchedEffect(vm.scrollTo) {
        val target = vm.scrollTo ?: return@LaunchedEffect
        delay(200)
        val idx = buildEntries(vm.categories.value, vm.query, vm.onlyMissing).indexOfFirst { it.key == "i${target.first}" }
        if (idx >= 0) listState.animateScrollToItem(prefix + idx, -300)
    }

    val cardBg = c.card.copy(alpha = 0.96f)
    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
        item(key = "tabs") {
            ListTabs(lists, currentId, onSelect = vm::selectList, onAdd = vm::openNewList, onMenu = vm::openEditList)
        }
        if (showSummary) item(key = "summary") { SummaryCard(cats, missingAll) }
        if (showSearch) item(key = "search") { SearchField(vm.query) { vm.query = it } }
        if (showActions) item(key = "actions") {
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Chip("רק מה שחסר", selected = vm.onlyMissing, selectedBg = c.red) { vm.onlyMissing = !vm.onlyMissing }
                Spacer(Modifier.weight(1f))
                Chip("סיימתי קנייה", selected = true, selectedBg = c.grn) { vm.finish() }
            }
        }
        if (!showActions) item(key = "gap") { Spacer(Modifier.height(10.dp)) }

        items(entries, key = { it.key }) { e ->
            val base = Modifier
                .dragged(drag, e.key)
                .animateItem(fadeInSpec = null, fadeOutSpec = null)
                .padding(horizontal = 16.dp)
            when (e) {
                is Entry.Header -> {
                    val shape = if (e.open) RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp) else RoundedCornerShape(22.dp)
                    CategoryHeader(
                        e = e,
                        modifier = base
                            .padding(bottom = if (e.open) 0.dp else 12.dp)
                            .clip(shape)
                            .background(cardBg)
                            .dropIndicator(drag, e.key, c.sun),
                        grip = Modifier.dragHandle(drag, e.key),
                        onFold = { vm.toggleExpanded(e.cat.category) },
                        onUp = { vm.moveCategoryBy(e.categoryId, -1) },
                        onDown = { vm.moveCategoryBy(e.categoryId, 1) },
                        onMenu = { vm.openEditCategory(e.cat.category) },
                    )
                }
                is Entry.ItemRow -> {
                    val flashKey = vm.flash?.takeIf { it.first == e.item.id }?.second
                    ItemRowView(
                        item = e.item,
                        isNew = vm.newItemId == e.item.id,
                        onNewDone = { if (vm.newItemId == e.item.id) vm.newItemId = null },
                        flashKey = flashKey,
                        hidden = e.item.id in fx.hidden,
                        modifier = base.background(cardBg).padding(horizontal = 8.dp).dropIndicator(drag, e.key, c.sun),
                        grip = Modifier.dragHandle(drag, e.key),
                        onPos = { fx.emojiPos[e.item.id] = it },
                        onGone = { fx.emojiPos.remove(e.item.id) },
                        onClick = { onItemClick(e.item) },
                        onMenu = { vm.openItem(e.item) },
                    )
                }
                is Entry.Empty -> Box(base.background(cardBg).fillMaxWidth().dropIndicator(drag, e.key, c.sun).padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 12.dp)) {
                    Muted(e.text)
                }
                is Entry.Add -> AddRow(
                    cat = e.cat,
                    modifier = base
                        .padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(bottomStart = 22.dp, bottomEnd = 22.dp))
                        .background(cardBg)
                        .dropIndicator(drag, e.key, c.sun),
                    onAdd = { vm.addItem(e.cat.id, it) },
                    onMic = { onMic(e.cat) },
                )
            }
        }

        if (entries.isEmpty() && vm.query.isNotBlank()) item(key = "nores") {
            Muted("לא נמצא מוצר בשם הזה", Modifier.fillMaxWidth().padding(16.dp))
        }

        item(key = "addcat") {
            Box(
                Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(c.card.copy(alpha = 0.6f))
                    .dashedBorder(c.ink.copy(alpha = 0.25f), 2.dp, 18.dp)
                    .clickable(role = Role.Button) { vm.openNewCategory() },
                contentAlignment = Alignment.Center,
            ) { Text("＋ קטגוריה חדשה", fontWeight = FontWeight.Bold) }
        }
        item(key = "bottom") { Spacer(Modifier.navigationBarsPadding().height(140.dp)) }
    }
}

@Composable
private fun ListTabs(
    lists: List<com.reshimarket.data.ShoppingList>,
    currentId: Long?,
    onSelect: (Long) -> Unit,
    onAdd: () -> Unit,
    onMenu: () -> Unit,
) {
    LazyRow(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(lists, key = { it.id }) { l ->
            Chip("${l.emoji} ${l.name}", selected = l.id == currentId, height = 40.dp) { onSelect(l.id) }
        }
        item(key = "add") { Chip("＋ רשימה", height = 40.dp, onClick = onAdd) }
        item(key = "menu") { Chip("⋯", height = 40.dp, onClick = onMenu) }
    }
}

@Composable
private fun SummaryCard(cats: List<CategoryWithItems>, missing: Int) {
    val c = LocalAppColors.current
    val mc = cats.count { cc -> cc.items.any { !it.have } }
    val ems = cats.flatMap { cc -> cc.sortedItems.filter { !it.have } }.joinToString("") { it.emoji }
    val shape = RoundedCornerShape(22.dp)
    Row(
        Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 12.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(c.card)
            .border(1.dp, c.line, shape)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(missing.toString(), fontSize = 44.sp, fontWeight = FontWeight.ExtraBold, color = if (missing > 0) c.red else c.grn, lineHeight = 44.sp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                when {
                    missing == 0 -> "הכל בבית"
                    mc == 1 -> "חסרים בקטגוריה אחת"
                    else -> "חסרים ב-$mc קטגוריות"
                },
                fontWeight = FontWeight.Bold,
            )
            if (ems.isNotEmpty()) Text(ems, fontSize = 20.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, letterSpacing = 2.sp)
        }
    }
}

@Composable
private fun SearchField(value: String, onChange: (String) -> Unit) {
    val c = LocalAppColors.current
    val shape = RoundedCornerShape(14.dp)
    BasicTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        textStyle = TextStyle(color = c.ink, fontFamily = Rubik, fontSize = 16.sp),
        cursorBrush = SolidColor(c.ink),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
        decorationBox = { inner ->
            Row(
                Modifier.fillMaxWidth().heightIn(min = 46.dp).clip(shape).background(c.card).border(1.dp, c.line, shape).padding(start = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) Text("חיפוש מוצר בכל הקטגוריות", color = c.mut, fontSize = 16.sp, maxLines = 1)
                    inner()
                }
                if (value.isNotEmpty()) {
                    Box(Modifier.size(44.dp).clickable(role = Role.Button, onClickLabel = "ניקוי החיפוש") { onChange("") }, contentAlignment = Alignment.Center) {
                        Text("✕", color = c.mut)
                    }
                }
            }
        },
    )
}

@Composable
private fun Grip(modifier: Modifier) {
    Box(modifier.size(width = 32.dp, height = 44.dp), contentAlignment = Alignment.Center) {
        Text("⋮⋮", color = LocalAppColors.current.mut, fontSize = 18.sp, letterSpacing = (-3).sp)
    }
}

@Composable
private fun SmallButton(text: String, label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(width = 40.dp, height = 44.dp).clip(RoundedCornerShape(10.dp)).clickable(role = Role.Button, onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = LocalAppColors.current.mut, fontSize = 15.sp) }
}

@Composable
private fun CategoryHeader(
    e: Entry.Header,
    modifier: Modifier,
    grip: Modifier,
    onFold: () -> Unit,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onMenu: () -> Unit,
) {
    val c = LocalAppColors.current
    val cat = e.cat.category
    val chev by animateFloatAsState(if (e.open) 0f else 90f, tween(200), label = "chev")
    Row(modifier.fillMaxWidth().padding(start = 2.dp, end = 6.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Grip(grip)
        Row(
            Modifier.weight(1f).heightIn(min = 44.dp).clip(RoundedCornerShape(12.dp)).clickable(role = Role.Button, onClickLabel = if (e.open) "כיווץ" else "פתיחה", onClick = onFold),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(cat.emoji, fontSize = 18.sp)
            Spacer(Modifier.width(8.dp))
            Text(
                cat.name,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                color = lerp(c.ink, Color(cat.color), 0.82f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (e.missing > 0) {
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier.heightIn(min = 26.dp).widthIn(min = 26.dp).clip(RoundedCornerShape(999.dp)).background(c.red).padding(horizontal = 7.dp),
                    contentAlignment = Alignment.Center,
                ) { Text(e.missing.toString(), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
            }
            Spacer(Modifier.weight(0.01f))
            Text("▾", color = c.mut, modifier = Modifier.padding(horizontal = 6.dp).rotate(chev))
        }
        SmallButton("▲", "למעלה", onUp)
        SmallButton("▼", "למטה", onDown)
        SmallButton("⋯", "עריכה", onMenu)
    }
}

@Composable
private fun ItemRowView(
    item: Item,
    isNew: Boolean,
    onNewDone: () -> Unit,
    flashKey: Long?,
    hidden: Boolean,
    modifier: Modifier,
    grip: Modifier,
    onPos: (Offset) -> Unit,
    onGone: () -> Unit,
    onClick: () -> Unit,
    onMenu: () -> Unit,
) {
    val c = LocalAppColors.current
    val bg by animateColorAsState(if (item.have) c.grnBg else c.redBg, tween(250), label = "rowbg")
    val flip = remember { Animatable(0f) }
    LaunchedEffect(isNew) {
        if (isNew) {
            flip.snapTo(-95f)
            flip.animateTo(0f, spring(dampingRatio = 0.45f, stiffness = 260f))
            onNewDone()
        }
    }
    val flash = remember { Animatable(0f) }
    LaunchedEffect(flashKey) {
        if (flashKey != null) repeat(2) {
            flash.animateTo(1f, tween(450))
            flash.animateTo(0f, tween(450))
        }
    }
    DisposableEffect(item.id) { onDispose { onGone() } }

    Box(modifier.fillMaxWidth().padding(bottom = 6.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    rotationX = flip.value
                    translationY = if (flip.value < 0f) flip.value / 95f * 18.dp.toPx() else 0f
                    alpha = (1f + flip.value / 95f * 1.6f).coerceIn(0f, 1f)
                    cameraDistance = 6f * density
                }
                .heightIn(min = 54.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(lerp(bg, c.sun.copy(alpha = 0.55f).compositeOverSafe(bg), flash.value))
                .clickable(role = Role.Button, onClickLabel = if (item.have) "סימון כחסר" else "סימון קניתי", onClick = onClick)
                .padding(start = 2.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Grip(grip)
            Box(
                Modifier
                    .width(44.dp)
                    .onGloballyPositioned { onPos(it.boundsInRoot().center) }
                    .graphicsLayer { alpha = if (hidden) 0f else 1f },
                contentAlignment = Alignment.Center,
            ) { Text(item.emoji, fontSize = 27.sp, textAlign = TextAlign.Center) }
            Spacer(Modifier.width(6.dp))
            Text(
                buildString {
                    append(item.name)
                    if (item.qty > 1) append("  ×${item.qty}")
                },
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                color = if (item.have) c.ink.copy(alpha = 0.65f) else c.ink,
                textDecoration = if (item.have) TextDecoration.LineThrough else null,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(6.dp))
            Pill(if (item.have) "✓ קניתי" else "חסר", if (item.have) c.grn else c.red)
            SmallButton("⋯", "עריכה", onMenu)
        }
    }
}

private fun Color.compositeOverSafe(bg: Color): Color =
    Color(
        red = red * alpha + bg.red * (1 - alpha),
        green = green * alpha + bg.green * (1 - alpha),
        blue = blue * alpha + bg.blue * (1 - alpha),
        alpha = 1f,
    )

@Composable
private fun AddRow(cat: Category, modifier: Modifier, onAdd: (String) -> Boolean, onMic: () -> Unit) {
    val c = LocalAppColors.current
    val catColor = Color(cat.color)
    var text by rememberSaveable(cat.id) { mutableStateOf("") }
    var focused by remember { mutableStateOf(false) }
    val suggestions = remember(text) {
        val n = Emojis.norm(text)
        if (n.isEmpty()) emptyList()
        else Emojis.SUGGESTIONS.filter { it.contains(n) && it != n }.sortedBy { !it.startsWith(n) }.take(8)
    }
    fun submit(value: String) {
        if (onAdd(value)) text = ""
    }
    Column(modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                textStyle = TextStyle(color = c.ink, fontFamily = Rubik, fontSize = 16.sp),
                cursorBrush = SolidColor(c.ink),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit(text) }),
                modifier = Modifier.weight(1f).onFocusChanged { focused = it.isFocused },
                decorationBox = { inner ->
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 46.dp)
                            .dashedBorder(lerp(c.line, catColor, 0.55f), 1.dp, 14.dp)
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (text.isEmpty()) Text("הוספת מוצר ל${cat.name}", color = c.mut, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        inner()
                    }
                },
            )
            Spacer(Modifier.width(6.dp))
            val btnBg = lerp(c.card, catColor, 0.18f)
            Box(
                Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(btnBg).clickable(role = Role.Button, onClickLabel = "הוספה בקול", onClick = onMic),
                contentAlignment = Alignment.Center,
            ) { LineIcon(Icons.mic, c.ink, size = 20.dp) }
            Spacer(Modifier.width(6.dp))
            Box(
                Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(btnBg).clickable(role = Role.Button, onClickLabel = "הוספה") { submit(text) },
                contentAlignment = Alignment.Center,
            ) { Text("＋", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
        }
        AnimatedVisibility(visible = focused && suggestions.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                for (s in suggestions) {
                    Chip("${Emojis.forItem(s)} $s", height = 40.dp) { submit(s) }
                }
            }
        }
    }
}

// ---------- הודעות קופצות ----------

@Composable
private fun androidx.compose.foundation.layout.BoxScope.ToastHost(vm: MainViewModel) {
    val c = LocalAppColors.current
    val t = vm.toast
    var last by remember { mutableStateOf(t) }
    if (t != null) last = t
    LaunchedEffect(t?.id) {
        if (t != null) {
            delay(4500)
            vm.dismissToast(t.id)
        }
    }
    AnimatedVisibility(
        visible = t != null,
        enter = fadeIn(tween(250)) + slideInVertically(tween(250)) { it / 2 },
        exit = fadeOut(tween(250)) + slideOutVertically(tween(250)) { it / 2 },
        modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().imePadding().padding(bottom = 110.dp, start = 16.dp, end = 16.dp),
    ) {
        val shown = last ?: return@AnimatedVisibility
        Row(
            Modifier.clip(RoundedCornerShape(14.dp)).background(c.ink).padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp).heightIn(min = 36.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(shown.message, color = c.bg, fontSize = 15.sp, modifier = Modifier.weight(1f, fill = false).padding(vertical = 4.dp))
            val label = shown.actionLabel
            val action = shown.action
            if (label != null && action != null) {
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(10.dp)).clickable(role = Role.Button) {
                        vm.dismissToast(shown.id)
                        action()
                    }.padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center,
                ) { Text(label, color = c.sun, fontWeight = FontWeight.ExtraBold) }
            } else Spacer(Modifier.width(8.dp))
        }
    }
}

// ---------- "סיימתי קנייה" ----------

@Composable
private fun androidx.compose.foundation.layout.BoxScope.FinishLayer(vm: MainViewModel, fx: FxState, reduceMotion: Boolean) {
    var confettiKey by remember { mutableIntStateOf(0) }
    var doorsShut by remember { mutableStateOf(false) }
    var receiptShown by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val width = constraints.maxWidth.toFloat()
        LaunchedEffect(vm.finishTick) {
            if (vm.finishTick == 0) return@LaunchedEffect
            Sounds.fanfare()
            if (reduceMotion) {
                doorsShut = true
                receiptShown = true
                return@LaunchedEffect
            }
            confettiKey++
            launch { fx.driveAway(width, density.density) }
            delay(900)
            doorsShut = true
            delay(700)
            receiptShown = true
        }
        Confetti(confettiKey)
        GlassDoors(doorsShut)
        ReceiptView(vm.receipt, receiptShown) {
            receiptShown = false
            doorsShut = false
            scope.launch {
                delay(600)
                fx.cartDrive.snapTo(0f)
                fx.cartRot.snapTo(0f)
                fx.bump()
                delay(600)
                vm.closeReceipt()
            }
        }
    }
}
