package com.tinyledger.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.tinyledger.app.domain.model.AppSettings
import com.tinyledger.app.domain.model.Category
import com.tinyledger.app.domain.model.ColorTheme
import com.tinyledger.app.domain.model.ThemeMode
import com.tinyledger.app.domain.model.TransactionType
import com.tinyledger.app.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class PreferencesRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : PreferencesRepository {

    companion object {
        private val CURRENCY_SYMBOL_KEY = stringPreferencesKey("currency_symbol")
        private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        private val COLOR_THEME_KEY = stringPreferencesKey("color_theme")
        private val CUSTOM_CATEGORIES_KEY = stringPreferencesKey("custom_categories")
    }

    override fun getSettings(): Flow<AppSettings> {
        return dataStore.data.map { preferences ->
            AppSettings(
                currencySymbol = preferences[CURRENCY_SYMBOL_KEY] ?: "¥",
                themeMode = preferences[THEME_MODE_KEY]?.let { ThemeMode.valueOf(it) } ?: ThemeMode.SYSTEM,
                colorTheme = preferences[COLOR_THEME_KEY]?.let {
                    try { ColorTheme.valueOf(it) } catch (_: IllegalArgumentException) { null }
                } ?: ColorTheme.IOS_BLUE
            )
        }
    }

    override suspend fun updateCurrencySymbol(symbol: String) {
        dataStore.edit { preferences ->
            preferences[CURRENCY_SYMBOL_KEY] = symbol
        }
    }

    override suspend fun updateThemeMode(mode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[THEME_MODE_KEY] = mode.name
        }
    }

    override suspend fun updateColorTheme(theme: ColorTheme) {
        dataStore.edit { preferences ->
            preferences[COLOR_THEME_KEY] = theme.name
        }
    }

    override suspend fun getCustomCategories(): List<Category> {
        val preferences = dataStore.data.first()
        val json = preferences[CUSTOM_CATEGORIES_KEY] ?: return emptyList()
        return parseCustomCategories(json)
    }

    override suspend fun saveCustomCategory(category: Category) {
        dataStore.edit { preferences ->
            val existing = preferences[CUSTOM_CATEGORIES_KEY] ?: ""
            val categories = parseCustomCategories(existing).toMutableList()
            categories.add(category)
            preferences[CUSTOM_CATEGORIES_KEY] = serializeCustomCategories(categories)
        }
    }

    override suspend fun deleteCustomCategory(categoryId: String) {
        dataStore.edit { preferences ->
            val existing = preferences[CUSTOM_CATEGORIES_KEY] ?: ""
            val categories = parseCustomCategories(existing).toMutableList()
            categories.removeAll { it.id == categoryId }
            preferences[CUSTOM_CATEGORIES_KEY] = serializeCustomCategories(categories)
        }
    }

    /**
     * 简单的自定义分类序列化格式: id|name|icon|typeValue;id|name|icon|typeValue;...
     */
    private fun serializeCustomCategories(categories: List<Category>): String {
        return categories.joinToString(";") { "${it.id}|${it.name}|${it.icon}|${it.type.value}" }
    }

    private fun parseCustomCategories(data: String): List<Category> {
        if (data.isBlank()) return emptyList()
        return data.split(";").mapNotNull { entry ->
            val parts = entry.split("|")
            if (parts.size >= 4) {
                Category(
                    id = parts[0],
                    name = parts[1],
                    icon = parts[2],
                    type = TransactionType.fromInt(parts[3].toIntOrNull() ?: 0),
                    isDefault = false
                )
            } else null
        }
    }
}
