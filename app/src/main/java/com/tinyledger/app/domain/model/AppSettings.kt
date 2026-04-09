package com.tinyledger.app.domain.model

enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}

data class AppSettings(
    val currencySymbol: String = "¥",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val colorTheme: ColorTheme = ColorTheme.IOS_BLUE
)
