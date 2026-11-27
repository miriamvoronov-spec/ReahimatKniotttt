package com.reshimarket.data

import androidx.room.withTransaction
import com.reshimarket.domain.Emojis
import com.reshimarket.domain.TextTools
import kotlinx.coroutines.flow.Flow

class LocalShoppingRepository(private val db: AppDatabase) : ShoppingRepository {
    private val dao = db.dao()

    override fun observeLists(): Flow<List<ShoppingList>> = dao.observeLists()
    override fun observeCategories(listId: Long): Flow<List<CategoryWithItems>> = dao.observeCategories(listId)
    override fun observeMissingCount(listId: Long): Flow<Int> = dao.observeMissingCount(listId)

    override suspend fun seedIfEmpty() {
        db.withTransaction { if (dao.listCount() == 0) insertBackup(Seed.data) }
    }

    override suspend fun lists(): List<ShoppingList> = dao.lists()
    override suspend fun missingCount(listId: Long): Int = dao.missingCount(listId)

    override suspend fun addList(name: String, emoji: String): Long =
        dao.insertList(ShoppingList(name = name, emoji = emoji, position = dao.nextListPosition()))

    override suspend fun updateList(list: ShoppingList) = dao.updateList(list)
    override suspend fun deleteList(id: Long) = dao.deleteList(id)

    override suspend fun addCategory(listId: Long, name: String, color: Int, emoji: String): Long =
        dao.insertCategory(Category(listId = listId, name = name, color = color, emoji = emoji, position = dao.nextCategoryPosition(listId)))

    override suspend fun updateCategory(category: Category) = dao.updateCategory(category)
    override suspend fun deleteCategory(id: Long) = dao.deleteCategory(id)
    override suspend fun setExpanded(id: Long, expanded: Boolean) = dao.setExpanded(id, expanded)

    override suspend fun reorderCategories(orderedIds: List<Long>) = db.withTransaction {
        val updated = orderedIds.mapIndexedNotNull { i, id -> dao.category(id)?.copy(position = i) }
        dao.updateCategories(updated)
    }

    override suspend fun addItem(categoryId: Long, name: String, emoji: String): Long =
        dao.insertItem(Item(categoryId = categoryId, name = name, emoji = emoji, position = dao.nextItemPosition(categoryId)))

    override suspend fun updateItem(item: Item) = dao.updateItem(item)
    override suspend fun deleteItem(id: Long) = dao.deleteItem(id)

    override suspend fun restoreItem(item: Item) {
        if (dao.category(item.categoryId) != null) dao.insertItem(item)
    }

    override suspend fun setState(id: Long, have: Boolean, inCart: Boolean) = dao.setState(id, have, inCart)

    override suspend fun placeItem(itemId: Long, categoryId: Long, orderedIds: List<Long>) = db.withTransaction {
        val moved = dao.item(itemId) ?: return@withTransaction
        dao.updateItem(moved.copy(categoryId = categoryId))
        val updated = orderedIds.mapIndexedNotNull { i, id -> dao.item(id)?.copy(position = i) }
        dao.updateItems(updated)
    }

    override suspend fun finishShopping(listId: Long): List<Item> = db.withTransaction {
        val all = dao.categories(listId).flatMap { it.sortedItems }
        val got = all.filter { !it.have || it.inCart }
        dao.updateItems(all.filter { !it.have || it.inCart }.map { it.copy(have = true, inCart = false) })
        got
    }

    override suspend fun setEmoji(kind: EmojiTarget, id: Long, emoji: String) = when (kind) {
        EmojiTarget.LIST -> dao.setListEmoji(id, emoji)
        EmojiTarget.CATEGORY -> dao.setCategoryEmoji(id, emoji)
        EmojiTarget.ITEM -> dao.setItemEmoji(id, emoji)
    }

    override suspend fun exportAll(): BackupData = BackupData(
        lists = dao.lists().map { l ->
            BackupList(l.name, l.emoji, dao.categories(l.id).map { c ->
                BackupCategory(c.category.name, c.category.color, c.category.emoji, c.category.expanded,
                    c.sortedItems.map { BackupItem(it.name, it.emoji, it.have, it.inCart, it.qty) })
            })
        },
    )

    override suspend fun replaceAll(data: BackupData) = db.withTransaction {
        require(data.lists.isNotEmpty()) { "אין רשימות בקובץ" }
        dao.deleteAllLists()
        insertBackup(data)
    }

    private suspend fun insertBackup(data: BackupData) {
        data.lists.forEachIndexed { li, l ->
            val listId = dao.insertList(ShoppingList(name = l.name, emoji = l.emoji, position = li))
            l.categories.forEachIndexed { ci, c ->
                val catId = dao.insertCategory(Category(listId = listId, name = c.name, color = c.color, emoji = c.emoji, expanded = c.expanded, position = ci))
                c.items.forEachIndexed { ii, it ->
                    dao.insertItem(Item(categoryId = catId, name = it.name, emoji = it.emoji, have = it.have, inCart = it.inCart, qty = it.qty.coerceAtLeast(1), position = ii))
                }
            }
        }
    }

    override suspend fun importParsed(listId: Long, parsed: List<TextTools.ParsedCategory>): Int = db.withTransaction {
        val existing = dao.categories(listId)
        val existingNames = existing.flatMap { it.items }.map { Emojis.norm(it.name) }.toMutableSet()
        var added = 0
        val palette = listOf(0xFF2A7DE1, 0xFF1F9D55, 0xFFD9822B, 0xFFA0522D, 0xFF0F9FB5, 0xFFE0559A, 0xFF7B4BD6, 0xFFE85D04)
        var colorIdx = existing.size
        for (pc in parsed) {
            val newItems = pc.items.filter { it.name !in existingNames }
            if (newItems.isEmpty()) continue
            val catId = existing.firstOrNull { Emojis.norm(it.category.name) == pc.name }?.category?.id
                ?: dao.insertCategory(Category(
                    listId = listId, name = pc.name, color = palette[colorIdx++ % palette.size].toInt(),
                    emoji = pc.emoji ?: Emojis.forCategory(pc.name), position = dao.nextCategoryPosition(listId),
                ))
            for (pi in newItems) {
                dao.insertItem(Item(
                    categoryId = catId, name = pi.name, emoji = pi.emoji ?: Emojis.forItem(pi.name),
                    have = pi.have, qty = pi.qty, position = dao.nextItemPosition(catId),
                ))
                existingNames.add(pi.name)
                added++
            }
        }
        added
    }
}
