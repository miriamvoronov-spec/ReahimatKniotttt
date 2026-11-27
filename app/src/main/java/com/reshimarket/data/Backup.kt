package com.reshimarket.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class BackupData(val app: String = "reshimarket", val version: Int = 1, val lists: List<BackupList>)

@Serializable
data class BackupList(val name: String, val emoji: String, val categories: List<BackupCategory>)

@Serializable
data class BackupCategory(
    val name: String,
    val color: Int,
    val emoji: String,
    val expanded: Boolean = true,
    val items: List<BackupItem>,
)

@Serializable
data class BackupItem(
    val name: String,
    val emoji: String,
    val have: Boolean = false,
    val inCart: Boolean = false,
    val qty: Int = 1,
)

object BackupJson {
    val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }
    fun encode(d: BackupData): String = json.encodeToString(BackupData.serializer(), d)
    fun decode(s: String): BackupData = json.decodeFromString(BackupData.serializer(), s)
}
