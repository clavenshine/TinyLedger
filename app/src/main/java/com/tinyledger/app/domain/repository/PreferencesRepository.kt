package com.tinyledger.app.domain.repository

import com.tinyledger.app.domain.model.AppSettings
import com.tinyledger.app.domain.model.Category
import com.tinyledger.app.domain.model.ColorTheme
import com.tinyledger.app.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {
    fun getSettings(): Flow<AppSettings>
    suspend fun updateCurrencySymbol(symbol: String)
    suspend fun updateThemeMode(mode: ThemeMode)
    suspend fun updateColorTheme(theme: ColorTheme)
    suspend fun getCustomCategories(): List<Category>
    suspend fun saveCustomCategory(category: Category)
    suspend fun deleteCustomCategory(categoryId: String)
}
