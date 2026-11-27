package com.reshimarket.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "lists")
data class ShoppingList(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val emoji: String,
    val position: Int = 0,
)

@Entity(
    tableName = "categories",
    foreignKeys = [ForeignKey(
        entity = ShoppingList::class,
        parentColumns = ["id"],
        childColumns = ["listId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("listId")],
)
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val listId: Long,
    val name: String,
    /** צבע ARGB של שם הקטגוריה */
    val color: Int,
    val emoji: String,
    val expanded: Boolean = true,
    val position: Int = 0,
)

@Entity(
    tableName = "items",
    foreignKeys = [ForeignKey(
        entity = Category::class,
        parentColumns = ["id"],
        childColumns = ["categoryId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("categoryId")],
)
data class Item(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long,
    val name: String,
    val emoji: String,
    /** true = ירוק (קניתי / יש בבית), false = אדום (חסר) */
    val have: Boolean = false,
    /** נקנה בקנייה הנוכחית ונמצא בעגלה */
    val inCart: Boolean = false,
    val qty: Int = 1,
    val position: Int = 0,
)

data class CategoryWithItems(
    @Embedded val category: Category,
    @Relation(parentColumn = "id", entityColumn = "categoryId")
    val items: List<Item>,
) {
    val sortedItems: List<Item> get() = items.sortedWith(compareBy({ it.position }, { it.id }))
}
