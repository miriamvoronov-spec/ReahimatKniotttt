package com.reshimarket

import android.app.Application
import com.reshimarket.data.AppDatabase
import com.reshimarket.data.LocalShoppingRepository
import com.reshimarket.data.SettingsRepository
import com.reshimarket.data.ShoppingRepository

/** מחזיק את התלויות של האפליקציה. כדי להוסיף סנכרון בענן בעתיד מחליפים כאן את ה-Repository. */
class ReshimarketApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.get(this) }
    val repository: ShoppingRepository by lazy { LocalShoppingRepository(database) }
    val settings: SettingsRepository by lazy { SettingsRepository(this) }
}
