package com.reshimarket.domain

import com.reshimarket.data.CategoryWithItems

/** טקסט לשיתוף ולייבוא רשימה */
object TextTools {

    fun shareText(listName: String, categories: List<CategoryWithItems>, all: Boolean): String {
        val sb = StringBuilder("🛒 רשימרקט · $listName\n")
        for (c in categories) {
            val items = c.sortedItems.filter { all || !it.have }
            if (items.isEmpty()) continue
            sb.append("\n${c.category.emoji} ${c.category.name}:\n")
            for (x in items) {
                val mark = if (all) (if (x.have) "🟢 " else "🔴 ") else "• "
                val q = if (x.qty > 1) " ×${x.qty}" else ""
                sb.append("$mark${x.emoji} ${x.name}$q\n")
            }
        }
        return sb.toString().trimEnd() + "\n"
    }

    data class ParsedItem(val name: String, val emoji: String?, val qty: Int, val have: Boolean)
    data class ParsedCategory(val name: String, val emoji: String?, val items: MutableList<ParsedItem> = mutableListOf())

    private val bullet = Regex("^[\\s•\\-*▪◦·‣–—]+|^\\d+[.)]\\s*")
    private val qtyRe = Regex("\\s*(?:[×xX*]\\s*(\\d+)|(\\d+)\\s*[×xX*])\\s*$")
    private val letter = Regex("[\\p{L}\\p{N}]")

    /** מפריד אימוג'י בתחילת שורה (אם יש) מהשם */
    private fun splitEmoji(s: String): Pair<String?, String> {
        val t = s.trim()
        val sp = t.indexOf(' ')
        if (sp > 0) {
            val first = t.substring(0, sp)
            if (!letter.containsMatchIn(first)) return first to t.substring(sp + 1).trim()
        }
        return null to t
    }

    /**
     * שורה שמסתיימת בנקודתיים היא קטגוריה, וכל שורה אחרת היא מוצר.
     * מבין גם את פורמט השיתוף של האפליקציה (🔴/🟢, תבליטים, ×2).
     */
    fun parse(text: String): List<ParsedCategory> {
        val out = mutableListOf<ParsedCategory>()
        var cur: ParsedCategory? = null
        for (raw in text.lines()) {
            var line = raw.trim()
            if (line.isEmpty()) continue
            if (line.contains("רשימרקט")) continue
            if (line.endsWith(":") || line.endsWith("：")) {
                val (e, n) = splitEmoji(line.dropLast(1))
                val name = Emojis.norm(n)
                if (name.isEmpty()) continue
                cur = out.firstOrNull { it.name == name } ?: ParsedCategory(name, e).also { out.add(it) }
                continue
            }
            var have = false
            when {
                line.startsWith("🟢") -> { have = true; line = line.removePrefix("🟢") }
                line.startsWith("🔴") -> line = line.removePrefix("🔴")
                line.startsWith("✅") || line.startsWith("✔") -> { have = true; line = line.substring(1) }
                line.startsWith("☐") || line.startsWith("⬜") -> line = line.substring(1)
            }
            line = line.replace(bullet, "").trim()
            var qty = 1
            qtyRe.find(line)?.let { m ->
                qty = (m.groupValues[1].ifEmpty { m.groupValues[2] }).toIntOrNull()?.coerceIn(1, 999) ?: 1
                line = line.substring(0, m.range.first)
            }
            val (e, n) = splitEmoji(line)
            val name = Emojis.norm(n)
            if (name.isEmpty() || !letter.containsMatchIn(name)) continue
            val c = cur ?: ParsedCategory("כללי", null).also { out.add(it); cur = it }
            if (c.items.none { it.name == name }) c.items.add(ParsedItem(name, e, qty, have))
        }
        return out
    }
}
