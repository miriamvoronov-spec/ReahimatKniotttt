package com.reshimarket.data

/** נתוני הפתיחה, לפי סעיף 16 במפרט ולפי הדמו */
object Seed {
    private fun c(hex: String): Int = (0xFF000000 or hex.removePrefix("#").toLong(16)).toInt()

    // שם, יש בבית?, כמות
    private data class S(val name: String, val have: Boolean = false, val qty: Int = 1)

    val data: BackupData = BackupData(
        lists = listOf(
            list("סופר", "🛒",
                cat("מוצרי חלב", "#2a7de1", S("חלב", false, 2), S("גבינה צהובה", true), S("קוטג'"), S("יוגורט", true), S("חמאה", true)),
                cat("ירקות ופירות", "#1f9d55", S("עגבניות"), S("מלפפונים", true), S("בננות"), S("תפוחים", true), S("בצל", true), S("אבוקדו")),
                cat("מאפים", "#d9822b", S("לחם"), S("פיתות", true), S("חלה", true)),
                cat("יבשים", "#a0522d", S("אורז", true), S("פסטה"), S("טחינה", true), S("קפה")),
                cat("ניקיון", "#0f9fb5", S("נוזל כלים", true), S("נייר טואלט"), S("אבקת כביסה", true)),
            ),
            list("פארם", "💊",
                cat("טיפוח", "#e0559a", S("שמפו"), S("משחת שיניים", true), S("סבון", true)),
            ),
        ),
    )

    private fun list(name: String, emoji: String, vararg cats: BackupCategory) = BackupList(name, emoji, cats.toList())

    private fun cat(name: String, color: String, vararg items: S) = BackupCategory(
        name = name,
        color = c(color),
        emoji = com.reshimarket.domain.Emojis.forCategory(name),
        items = items.map { BackupItem(it.name, com.reshimarket.domain.Emojis.forItem(it.name), it.have, false, it.qty) },
    )
}
