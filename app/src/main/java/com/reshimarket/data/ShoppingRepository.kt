package com.reshimarket.data

import com.reshimarket.domain.TextTools
import kotlinx.coroutines.flow.Flow

/**
 * שכבת הנתונים של האפליקציה. כרגע המימוש מקומי (Room).
 * בעתיד אפשר להוסיף מימוש שמסנכרן עם Firebase בלי לשנות את המסכים.
 */
interface ShoppingRepository {
    fun observeLists(): Flow<List<ShoppingList>>
    fun observeCategories(listId: Long): Flow<List<CategoryWithItems>>
    fun observeMissingCount(listId: Long): Flow<Int>

    suspend fun seedIfEmpty()
    suspend fun lists(): List<ShoppingList>
    suspend fun missingCount(listId: Long): Int

    suspend fun addList(name: String, emoji: String): Long
    suspend fun updateList(list: ShoppingList)
    suspend fun deleteList(id: Long)

    suspend fun addCategory(listId: Long, name: String, color: Int, emoji: String): Long
    suspend fun updateCategory(category: Category)
    suspend fun deleteCategory(id: Long)
    suspend fun setExpanded(id: Long, expanded: Boolean)
    suspend fun reorderCategories(orderedIds: List<Long>)

    suspend fun addItem(categoryId: Long, name: String, emoji: String): Long
    suspend fun updateItem(item: Item)
    suspend fun deleteItem(id: Long)
    suspend fun restoreItem(item: Item)
    suspend fun setState(id: Long, have: Boolean, inCart: Boolean)
    /** מעביר מוצר לקטגוריה (אפשר אותה קטגוריה) וקובע את הסדר החדש שלה */
    suspend fun placeItem(itemId: Long, categoryId: Long, orderedIds: List<Long>)
    /** "סיימתי קנייה": מסמן הכל כנקנה ומחזיר את מה שנקנה עכשיו */
    suspend fun finishShopping(listId: Long): List<Item>

    suspend fun setEmoji(kind: EmojiTarget, id: Long, emoji: String)

    suspend fun exportAll(): BackupData
    suspend fun replaceAll(data: BackupData)
    /** מייבא טקסט לרשימה ומחזיר כמה מוצרים נוספו */
    suspend fun importParsed(listId: Long, parsed: List<TextTools.ParsedCategory>): Int
}

enum class EmojiTarget { LIST, CATEGORY, ITEM }
