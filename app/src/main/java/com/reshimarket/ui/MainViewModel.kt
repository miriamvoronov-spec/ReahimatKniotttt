package com.reshimarket.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.reshimarket.ReshimarketApp
import com.reshimarket.data.AppSettings
import com.reshimarket.data.BackupJson
import com.reshimarket.data.Category
import com.reshimarket.data.CategoryWithItems
import com.reshimarket.data.EmojiTarget
import com.reshimarket.data.Item
import com.reshimarket.data.ShoppingList
import com.reshimarket.domain.Emojis
import com.reshimarket.domain.TextTools
import com.reshimarket.widget.MissingWidget
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Date

// ---------- מצב המסכים הצפים ----------

class ItemDraft(name: String, emoji: String, qty: Int, categoryId: Long) {
    var name by mutableStateOf(name)
    var emoji by mutableStateOf(emoji)
    var qty by mutableIntStateOf(qty)
    var categoryId by mutableStateOf(categoryId)
    var manualEmoji by mutableStateOf(false)
}

class CategoryDraft(name: String, color: Int, emoji: String) {
    var name by mutableStateOf(name)
    var color by mutableIntStateOf(color)
    var emoji by mutableStateOf(emoji)
}

class ListDraft(name: String, emoji: String) {
    var name by mutableStateOf(name)
    var emoji by mutableStateOf(emoji)
}

sealed interface Sheet {
    data class EditItem(val itemId: Long, val draft: ItemDraft) : Sheet
    data class EditCategory(val categoryId: Long?, val draft: CategoryDraft) : Sheet
    data class EditList(val listId: Long?, val draft: ListDraft) : Sheet
    data class PickEmoji(val title: String, val current: String, val onPick: (String) -> Unit, val back: Sheet?) : Sheet
    data class EmojiManager(val target: EmojiTarget) : Sheet
    data class Confirm(val message: String, val onYes: () -> Unit) : Sheet
    data object Settings : Sheet
    data object Cart : Sheet
    data object Share : Sheet
    data object ImportText : Sheet
}

data class ToastData(val message: String, val actionLabel: String? = null, val action: (() -> Unit)? = null, val id: Long = System.nanoTime())

data class Receipt(val listName: String, val time: Date, val items: List<Item>)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val container = app as ReshimarketApp
    private val repo = container.repository
    private val settingsRepo = container.settings

    val settings: StateFlow<AppSettings> = settingsRepo.settings

    val lists: StateFlow<List<ShoppingList>> = repo.observeLists()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val currentList: StateFlow<ShoppingList?> = combine(lists, settings) { ls, s ->
        ls.firstOrNull { it.id == s.currentListId } ?: ls.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val categories: StateFlow<List<CategoryWithItems>> = currentList.map { it?.id }.distinctUntilChanged()
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else repo.observeCategories(id) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    var loaded by mutableStateOf(false)
        private set

    var query by mutableStateOf("")
    var onlyMissing by mutableStateOf(false)
    var sheet by mutableStateOf<Sheet?>(null)
    var toast by mutableStateOf<ToastData?>(null)
        private set
    var shareAll by mutableStateOf(false)

    /** מוצר שנוסף עכשיו, לאנימציית הכניסה */
    var newItemId by mutableStateOf<Long?>(null)
    /** מוצר שכבר קיים, להבהוב צהוב */
    var flash by mutableStateOf<Pair<Long, Long>?>(null)
        private set
    /** בקשה לגלול אל מוצר */
    var scrollTo by mutableStateOf<Pair<Long, Long>?>(null)
        private set

    var receipt by mutableStateOf<Receipt?>(null)
        private set
    /** עולה בכל "סיימתי קנייה" ומפעיל את רצף האנימציות */
    var finishTick by mutableIntStateOf(0)
        private set

    init {
        viewModelScope.launch {
            repo.seedIfEmpty()
            loaded = true
        }
        viewModelScope.launch {
            settings.collect { Sounds.enabled = it.sounds }
        }
        // עדכון הווידג'ט כשמספר החסרים משתנה
        viewModelScope.launch {
            currentList.map { it?.id }.distinctUntilChanged()
                .flatMapLatest { id -> if (id == null) flowOf(-1L) else repo.observeMissingCount(id).map { id * 100000L + it } }
                .distinctUntilChanged().debounce(400)
                .collect { MissingWidget.refresh(getApplication<Application>()) }
        }
    }

    fun showToast(message: String, actionLabel: String? = null, action: (() -> Unit)? = null) {
        toast = ToastData(message, actionLabel, action)
    }

    fun dismissToast(id: Long) {
        if (toast?.id == id) toast = null
    }

    fun updateSettings(change: (AppSettings) -> AppSettings) = settingsRepo.update(change)

    private fun allItems() = categories.value.flatMap { it.items }

    // ---------- רשימות ----------

    fun selectList(id: Long) {
        settingsRepo.update { it.copy(currentListId = id) }
        query = ""
    }

    fun openNewList() { sheet = Sheet.EditList(null, ListDraft("", "")) }

    fun openEditList() {
        val l = currentList.value ?: return
        sheet = Sheet.EditList(l.id, ListDraft(l.name, l.emoji))
    }

    fun saveList(s: Sheet.EditList) {
        val name = Emojis.norm(s.draft.name)
        if (name.isEmpty()) return
        val emoji = s.draft.emoji.ifEmpty { Emojis.forList(name) }
        viewModelScope.launch {
            if (s.listId == null) {
                val id = repo.addList(name, emoji)
                selectList(id)
            } else {
                lists.value.firstOrNull { it.id == s.listId }?.let { repo.updateList(it.copy(name = name, emoji = emoji)) }
            }
        }
        sheet = null
    }

    fun deleteList(id: Long) {
        if (lists.value.size < 2) {
            showToast("צריכה להישאר לפחות רשימה אחת")
            return
        }
        val l = lists.value.firstOrNull { it.id == id } ?: return
        sheet = Sheet.Confirm("למחוק את הרשימה ״${l.name}״?") {
            viewModelScope.launch {
                repo.deleteList(id)
                lists.value.firstOrNull { it.id != id }?.let { selectList(it.id) }
            }
        }
    }

    // ---------- קטגוריות ----------

    fun openNewCategory() { sheet = Sheet.EditCategory(null, CategoryDraft("", 0xFFE85D04.toInt(), "")) }

    fun openEditCategory(c: Category) { sheet = Sheet.EditCategory(c.id, CategoryDraft(c.name, c.color, c.emoji)) }

    fun saveCategory(s: Sheet.EditCategory) {
        val name = Emojis.norm(s.draft.name)
        if (name.isEmpty()) return
        val emoji = s.draft.emoji.ifEmpty { Emojis.forCategory(name) }
        val listId = currentList.value?.id ?: return
        viewModelScope.launch {
            if (s.categoryId == null) repo.addCategory(listId, name, s.draft.color, emoji)
            else categories.value.firstOrNull { it.category.id == s.categoryId }?.let {
                repo.updateCategory(it.category.copy(name = name, color = s.draft.color, emoji = emoji))
            }
        }
        sheet = null
    }

    fun deleteCategory(id: Long) {
        val c = categories.value.firstOrNull { it.category.id == id } ?: return
        sheet = Sheet.Confirm("למחוק את ״${c.category.name}״ ואת ${c.items.size} המוצרים שבה?") {
            viewModelScope.launch { repo.deleteCategory(id) }
        }
    }

    fun toggleExpanded(c: Category) = viewModelScope.launch { repo.setExpanded(c.id, !c.expanded) }

    fun moveCategoryBy(id: Long, delta: Int) {
        val ids = categories.value.map { it.category.id }.toMutableList()
        val i = ids.indexOf(id)
        val j = i + delta
        if (i < 0 || j < 0 || j >= ids.size) return
        ids[i] = ids[j].also { ids[j] = ids[i] }
        viewModelScope.launch { repo.reorderCategories(ids) }
    }

    fun dropCategory(id: Long, targetId: Long, after: Boolean) {
        if (id == targetId) return
        val ids = categories.value.map { it.category.id }.toMutableList()
        ids.remove(id)
        val t = ids.indexOf(targetId)
        if (t < 0) return
        ids.add(if (after) t + 1 else t, id)
        viewModelScope.launch { repo.reorderCategories(ids) }
    }

    // ---------- מוצרים ----------

    fun addItem(categoryId: Long, raw: String): Boolean {
        val name = Emojis.norm(raw)
        if (name.isEmpty()) return false
        for (c in categories.value) {
            val dup = c.items.firstOrNull { Emojis.norm(it.name) == name }
            if (dup != null) {
                showToast("״$name״ כבר ברשימה, ב${c.category.name}")
                viewModelScope.launch {
                    if (!c.category.expanded) repo.setExpanded(c.category.id, true)
                    flash = dup.id to System.nanoTime()
                    scrollTo = dup.id to System.nanoTime()
                }
                return true
            }
        }
        val emoji = Emojis.forItem(name)
        viewModelScope.launch {
            val id = repo.addItem(categoryId, name, emoji)
            newItemId = id
            if (emoji == Emojis.UNKNOWN_ITEM) {
                showToast("לא זיהיתי אימוג׳י ל״$name״", "לבחירה") {
                    sheet = Sheet.PickEmoji("מוצר ״$name״", emoji, { e ->
                        viewModelScope.launch { repo.setEmoji(EmojiTarget.ITEM, id, e) }
                    }, null)
                }
            }
        }
        return true
    }

    fun setHave(item: Item, have: Boolean) = viewModelScope.launch { repo.setState(item.id, have, have) }

    /** לחיצה על מוצר בתוך העגלה */
    fun toggleInCart(item: Item) {
        if (item.inCart) {
            Sounds.back()
            showToast("${item.emoji} ${item.name} חזר לרשימה כחסר")
            setHave(item, false)
        } else {
            Sounds.pop()
            setHave(item, true)
        }
    }

    fun openItem(item: Item) {
        sheet = Sheet.EditItem(item.id, ItemDraft(item.name, item.emoji, item.qty, item.categoryId))
    }

    fun saveItem(s: Sheet.EditItem) {
        val item = allItems().firstOrNull { it.id == s.itemId } ?: run { sheet = null; return }
        val name = Emojis.norm(s.draft.name).ifEmpty { item.name }
        val emoji = if (name != item.name && !s.draft.manualEmoji) Emojis.forItem(name) else s.draft.emoji
        viewModelScope.launch {
            if (s.draft.categoryId != item.categoryId) {
                val target = categories.value.firstOrNull { it.category.id == s.draft.categoryId }
                val order = (target?.sortedItems?.map { it.id } ?: emptyList()) + item.id
                repo.updateItem(item.copy(name = name, emoji = emoji, qty = s.draft.qty))
                repo.placeItem(item.id, s.draft.categoryId, order)
            } else {
                repo.updateItem(item.copy(name = name, emoji = emoji, qty = s.draft.qty))
            }
        }
        sheet = null
    }

    fun deleteItem(itemId: Long) {
        val item = allItems().firstOrNull { it.id == itemId } ?: return
        sheet = null
        viewModelScope.launch {
            repo.deleteItem(item.id)
            showToast("״${item.name}״ נמחק", "ביטול") { viewModelScope.launch { repo.restoreItem(item) } }
        }
    }

    /** גרירה: מניחים מוצר לפני/אחרי מוצר אחר, או בסוף קטגוריה */
    fun dropItem(itemId: Long, targetCategoryId: Long, targetItemId: Long?, after: Boolean) {
        val item = allItems().firstOrNull { it.id == itemId } ?: return
        val target = categories.value.firstOrNull { it.category.id == targetCategoryId } ?: return
        val ids = target.sortedItems.map { it.id }.filter { it != itemId }.toMutableList()
        val t = targetItemId?.let { ids.indexOf(it) } ?: -1
        if (t < 0) ids.add(itemId) else ids.add(if (after) t + 1 else t, itemId)
        viewModelScope.launch {
            repo.placeItem(itemId, targetCategoryId, ids)
            if (!target.category.expanded) repo.setExpanded(targetCategoryId, true)
        }
        if (targetCategoryId != item.categoryId) showToast("${item.emoji} עבר ל${target.category.name}")
    }

    // ---------- סיימתי קנייה ----------

    fun finish() {
        val list = currentList.value ?: return
        val pending = allItems().filter { !it.have || it.inCart }
        if (pending.isEmpty()) {
            showToast("אין מה לסיים, הכל כבר בבית")
            return
        }
        viewModelScope.launch {
            val got = repo.finishShopping(list.id)
            receipt = Receipt(list.name, Date(), got)
            finishTick++
        }
    }

    fun closeReceipt() { receipt = null }

    // ---------- אימוג'ים ----------

    fun pickEmojiFor(target: EmojiTarget, id: Long, title: String, current: String) {
        sheet = Sheet.PickEmoji(title, current, { e ->
            viewModelScope.launch { repo.setEmoji(target, id, e) }
        }, Sheet.EmojiManager(target))
    }

    // ---------- גיבוי, ייצוא וייבוא ----------

    suspend fun exportJson(): String = BackupJson.encode(repo.exportAll())

    fun importJson(text: String) {
        val data = runCatching { BackupJson.decode(text) }.getOrNull()
        if (data == null || data.lists.isEmpty()) {
            showToast("הקובץ לא נראה כמו גיבוי של רשימרקט")
            return
        }
        val n = data.lists.sumOf { l -> l.categories.sumOf { it.items.size } }
        sheet = Sheet.Confirm("לייבא ${data.lists.size} רשימות ו-$n מוצרים? הנתונים הנוכחיים יוחלפו.") {
            viewModelScope.launch {
                runCatching { repo.replaceAll(data) }
                    .onSuccess {
                        repo.lists().firstOrNull()?.let { selectList(it.id) }
                        showToast("הגיבוי שוחזר")
                    }
                    .onFailure { showToast("לא הצלחתי לייבא את הקובץ") }
            }
        }
    }

    fun importText(text: String) {
        val listId = currentList.value?.id ?: return
        val parsed = TextTools.parse(text)
        if (parsed.isEmpty()) {
            showToast("לא מצאתי מוצרים בטקסט")
            return
        }
        viewModelScope.launch {
            val n = repo.importParsed(listId, parsed)
            showToast(if (n == 0) "כל המוצרים כבר ברשימה" else "נוספו $n מוצרים")
        }
        sheet = null
    }

    fun shareText(): String = TextTools.shareText(currentList.value?.name ?: "", categories.value, shareAll)
}
