package com.reshimarket.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ShopDao {
    @Query("SELECT * FROM lists ORDER BY position, id")
    fun observeLists(): Flow<List<ShoppingList>>

    @Query("SELECT * FROM lists ORDER BY position, id")
    suspend fun lists(): List<ShoppingList>

    @Query("SELECT * FROM lists WHERE id = :id")
    suspend fun list(id: Long): ShoppingList?

    @Transaction
    @Query("SELECT * FROM categories WHERE listId = :listId ORDER BY position, id")
    fun observeCategories(listId: Long): Flow<List<CategoryWithItems>>

    @Transaction
    @Query("SELECT * FROM categories WHERE listId = :listId ORDER BY position, id")
    suspend fun categories(listId: Long): List<CategoryWithItems>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun category(id: Long): Category?

    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun item(id: Long): Item?

    @Insert suspend fun insertList(list: ShoppingList): Long
    @Insert suspend fun insertCategory(category: Category): Long
    @Insert suspend fun insertItem(item: Item): Long

    @Update suspend fun updateList(list: ShoppingList)
    @Update suspend fun updateCategory(category: Category)
    @Update suspend fun updateCategories(categories: List<Category>)
    @Update suspend fun updateItem(item: Item)
    @Update suspend fun updateItems(items: List<Item>)

    @Query("DELETE FROM lists WHERE id = :id") suspend fun deleteList(id: Long)
    @Query("DELETE FROM categories WHERE id = :id") suspend fun deleteCategory(id: Long)
    @Query("DELETE FROM items WHERE id = :id") suspend fun deleteItem(id: Long)
    @Query("DELETE FROM lists") suspend fun deleteAllLists()

    @Query("UPDATE categories SET expanded = :expanded WHERE id = :id")
    suspend fun setExpanded(id: Long, expanded: Boolean)

    @Query("UPDATE items SET have = :have, inCart = :inCart WHERE id = :id")
    suspend fun setState(id: Long, have: Boolean, inCart: Boolean)

    @Query("UPDATE lists SET emoji = :emoji WHERE id = :id") suspend fun setListEmoji(id: Long, emoji: String)
    @Query("UPDATE categories SET emoji = :emoji WHERE id = :id") suspend fun setCategoryEmoji(id: Long, emoji: String)
    @Query("UPDATE items SET emoji = :emoji WHERE id = :id") suspend fun setItemEmoji(id: Long, emoji: String)

    @Query("SELECT COUNT(*) FROM lists") suspend fun listCount(): Int
    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM lists") suspend fun nextListPosition(): Int
    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM categories WHERE listId = :listId") suspend fun nextCategoryPosition(listId: Long): Int
    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM items WHERE categoryId = :categoryId") suspend fun nextItemPosition(categoryId: Long): Int

    @Query("SELECT COUNT(*) FROM items i JOIN categories c ON i.categoryId = c.id WHERE c.listId = :listId AND i.have = 0")
    suspend fun missingCount(listId: Long): Int

    @Query("SELECT COUNT(*) FROM items i JOIN categories c ON i.categoryId = c.id WHERE c.listId = :listId AND i.have = 0")
    fun observeMissingCount(listId: Long): Flow<Int>
}
