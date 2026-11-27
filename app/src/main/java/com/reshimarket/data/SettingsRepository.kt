package com.reshimarket.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode { AUTO, DAY, NIGHT }

data class AppSettings(
    val mode: ThemeMode = ThemeMode.AUTO,
    val fontSize: Int = 16,
    val shelves: Boolean = true,
    /** צבעי הגרדיאנט. null = ברירת מחדל */
    val gradientTop: Int? = null,
    val gradientBottom: Int? = null,
    val sounds: Boolean = true,
    /** צבע הכתב. null = ברירת מחדל */
    val textColor: Int? = null,
    val showSummary: Boolean = true,
    val showSearch: Boolean = true,
    val showActions: Boolean = true,
    val currentListId: Long = 0,
)

class SettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val state = MutableStateFlow(read())
    val settings: StateFlow<AppSettings> = state.asStateFlow()

    val current: AppSettings get() = state.value

    fun update(change: (AppSettings) -> AppSettings) {
        val n = change(state.value)
        state.value = n
        write(n)
    }

    private fun read(): AppSettings = AppSettings(
        mode = runCatching { ThemeMode.valueOf(prefs.getString("mode", "AUTO")!!) }.getOrDefault(ThemeMode.AUTO),
        fontSize = prefs.getInt("fontSize", 16),
        shelves = prefs.getBoolean("shelves", true),
        gradientTop = if (prefs.getBoolean("gradient", false)) prefs.getInt("g1", 0) else null,
        gradientBottom = if (prefs.getBoolean("gradient", false)) prefs.getInt("g2", 0) else null,
        sounds = prefs.getBoolean("sounds", true),
        textColor = if (prefs.contains("textColor")) prefs.getInt("textColor", 0) else null,
        showSummary = prefs.getBoolean("showSummary", true),
        showSearch = prefs.getBoolean("showSearch", true),
        showActions = prefs.getBoolean("showActions", true),
        currentListId = prefs.getLong("currentListId", 0),
    )

    private fun write(s: AppSettings) {
        prefs.edit()
            .putString("mode", s.mode.name)
            .putInt("fontSize", s.fontSize)
            .putBoolean("shelves", s.shelves)
            .putBoolean("gradient", s.gradientTop != null && s.gradientBottom != null)
            .putInt("g1", s.gradientTop ?: 0)
            .putInt("g2", s.gradientBottom ?: 0)
            .putBoolean("sounds", s.sounds)
            .apply { if (s.textColor != null) putInt("textColor", s.textColor) else remove("textColor") }
            .putBoolean("showSummary", s.showSummary)
            .putBoolean("showSearch", s.showSearch)
            .putBoolean("showActions", s.showActions)
            .putLong("currentListId", s.currentListId)
            .apply()
    }
}
