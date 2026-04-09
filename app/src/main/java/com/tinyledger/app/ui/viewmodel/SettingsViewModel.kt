package com.tinyledger.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tinyledger.app.domain.model.AppColorScheme
import com.tinyledger.app.domain.model.AppSettings
import com.tinyledger.app.domain.model.ColorTheme
import com.tinyledger.app.domain.model.ThemeMode
import com.tinyledger.app.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val colorScheme: AppColorScheme = AppColorScheme.fromTheme(ColorTheme.IOS_BLUE),
    val isLoading: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState(isLoading = true))
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            preferencesRepository.getSettings().collect { settings ->
                _uiState.value = SettingsUiState(
                    settings = settings,
                    colorScheme = AppColorScheme.fromTheme(settings.colorTheme),
                    isLoading = false
                )
            }
        }
    }

    fun updateCurrencySymbol(symbol: String) {
        viewModelScope.launch {
            preferencesRepository.updateCurrencySymbol(symbol)
        }
    }

    fun updateThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferencesRepository.updateThemeMode(mode)
        }
    }

    fun updateColorTheme(theme: ColorTheme) {
        viewModelScope.launch {
            preferencesRepository.updateColorTheme(theme)
        }
    }
}
