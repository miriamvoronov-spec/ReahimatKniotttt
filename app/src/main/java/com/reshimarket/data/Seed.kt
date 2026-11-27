package com.reshimarket.data

/** נתוני הפתיחה: הפתק מסעיף 16 במפרט. כל המוצרים מתחילים כחסרים */
object Seed {
    private fun c(hex: String): Int = (0xFF000000 or hex.removePrefix("#").toLong(16)).toInt()

    // שם, יש בבית?, כמות
    private data class S(val name: String, val have: Boolean = false, val qty: Int = 1)

    val data: BackupData = BackupData(
        lists = listOf(
            list("סופר", "🛒",
                cat("ירקות", "#1f9d55", S("מלפפונים"), S("עגבניות"), S("גמבות"), S("בצל לבן"), S("בצל סגול"), S("תפוחי אדמה"), S("בטטה"), S("פטריות"), S("גזר"), S("כוסברה"), S("פטרוזילה"), S("חציל")),
                cat("פירות", "#e63946", S("לימון")),
                cat("שתייה מתוקה", "#e85d04", S("קולה"), S("בריזר")),
                cat("שימורים", "#a0522d", S("טונה"), S("מלפפון במלח"), S("תירס"), S("אפונה"), S("אפונה+גזר")),
                cat("חלבי", "#2a7de1", S("חלב"), S("קוטג'"), S("מעדנים"), S("גבינה למריחה"), S("שמנת"), S("שמנת לבישול"), S("שמנת להקצפה"), S("פרמז'ן")),
                cat("ירקות קפואים", "#0f9fb5", S("כרובית"), S("ברוקולי"), S("שעועית"), S("אפונה"), S("גזר"), S("תירס")),
                cat("קפואים אחרים", "#5a7d9a", S("דגים"), S("שום כתוש")),
                cat("מעדניה", "#c08a00", S("בולגרית"), S("צהובה"), S("חריפה"), S("מעושנת"), S("פלפלונים ממולאים"), S("אצבעות שוקולד")),
                cat("קצביה", "#b5302a", S("חזה עוף"), S("בשר טחון"), S("עופות"), S("פרגיות"), S("כבד")),
                cat("דגים", "#1d6fa5", S("סלמון")),
                cat("ניירות", "#8a6d3b", S("נייר טואלט"), S("נייר סופג")),
                cat("חומרי ניקוי", "#0f9fb5", S("חומר לאסלה"), S("ספריי ריח לשירותים"), S("חומר לחלונות"), S("ספריי אקונומיקה"), S("שקיות זבל"), S("שקיות אוכל")),
                cat("מוצרי היגיינה", "#e0559a", S("תחתוניות"), S("טמפונים"), S("דאורדורנט"), S("שמפו"), S("מסכה לשיער"), S("בושם")),
                cat("ביצים", "#d9822b", S("ביצים")),
                cat("רטבים", "#7b4bd6", S("טריאקי"), S("צ'ילי מתוק"), S("סויה")),
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
