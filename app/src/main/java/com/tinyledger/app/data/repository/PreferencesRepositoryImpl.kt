package com.tinyledger.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.tinyledger.app.domain.model.AppSettings
import com.tinyledger.app.domain.model.ColorTheme
import com.tinyledger.app.domain.model.ThemeMode
import com.tinyledger.app.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class PreferencesRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : PreferencesRepository {

    companion object {
        private val CURRENCY_SYMBOL_KEY = stringPreferencesKey("currency_symbol")
        private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        private val COLOR_THEME_KEY = stringPreferencesKey("color_theme")
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
}
