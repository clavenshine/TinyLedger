package com.tinyledger.app.ui.theme

import android.app.Activity
import android.content.res.Configuration
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.tinyledger.app.domain.model.AppColorScheme
import com.tinyledger.app.domain.model.ColorTheme
import com.tinyledger.app.domain.model.ThemeMode

// 全局颜色方案状态
private val LocalAppColorScheme = staticCompositionLocalOf { AppColorScheme.fromTheme(ColorTheme.IOS_BLUE) }

/**
 * 将颜色在 HSL 空间中提亮，确保深色模式下主色在 #121212 背景上有足够对比度。
 * 目标：lightness 至少到 [minLightness]，最高不超过 0.85。
 */
private fun Color.brightenForDark(minLightness: Float = 0.55f): Color {
    val r = this.red; val g = this.green; val b = this.blue
    val max = maxOf(r, g, b); val min = minOf(r, g, b)
    var h: Float; val s: Float; var l = (max + min) / 2f

    if (max == min) {
        h = 0f; s = 0f
    } else {
        val d = max - min
        s = if (l > 0.5f) d / (2f - max - min) else d / (max + min)
        h = when {
            max == r -> (g - b) / d + (if (g < b) 6f else 0f)
            max == g -> (b - r) / d + 2f
            else -> (r - g) / d + 4f
        }
        h /= 6f
    }

    // 将亮度提升到目标值
    if (l < minLightness) l = minLightness
    if (l > 0.85f) l = 0.85f

    // HSL → RGB
    fun hue2rgb(p: Float, q: Float, t: Float): Float {
        var tt = t
        if (tt < 0f) tt += 1f
        if (tt > 1f) tt -= 1f
        return when {
            tt < 1f / 6f -> p + (q - p) * 6f * tt
            tt < 1f / 2f -> q
            tt < 2f / 3f -> p + (q - p) * (2f / 3f - tt) * 6f
            else -> p
        }
    }

    val rr: Float; val gg: Float; val bb: Float
    if (s == 0f) {
        rr = l; gg = l; bb = l
    } else {
        val q = if (l < 0.5f) l * (1f + s) else l + s - l * s
        val p = 2f * l - q
        rr = hue2rgb(p, q, h + 1f / 3f)
        gg = hue2rgb(p, q, h)
        bb = hue2rgb(p, q, h - 1f / 3f)
    }
    return Color(rr, gg, bb, this.alpha)
}

// Light Color Scheme — 使用每个主题自带的 backgroundColor / surfaceColor / textColor
private fun lightColorScheme(
    appColorScheme: AppColorScheme = AppColorScheme.fromTheme(ColorTheme.IOS_BLUE)
): ColorScheme {
    val bg = Color(appColorScheme.backgroundColor)
    val surface = Color(appColorScheme.surfaceColor)
    val text = Color(appColorScheme.textColor)
    // 次要文字色：在主文字色基础上降低不透明度，但保持 ≥ 4.5:1 对比度
    val textSecondary = text.copy(alpha = 0.6f)

    return lightColorScheme(
        primary = Color(appColorScheme.primaryColor),
        onPrimary = Color.White,
        primaryContainer = Color(appColorScheme.primaryLightColor).copy(alpha = 0.15f),
        onPrimaryContainer = Color(appColorScheme.primaryColor),
        secondary = Color(appColorScheme.secondaryColor),
        onSecondary = Color.White,
        secondaryContainer = Color(appColorScheme.secondaryColor).copy(alpha = 0.15f),
        onSecondaryContainer = Color(appColorScheme.secondaryColor),
        tertiary = Color(appColorScheme.accentColor),
        onTertiary = Color.White,
        tertiaryContainer = Color(appColorScheme.accentColor).copy(alpha = 0.15f),
        onTertiaryContainer = Color(appColorScheme.accentColor),
        background = bg,
        onBackground = text,
        surface = surface,
        onSurface = text,
        surfaceVariant = bg,
        onSurfaceVariant = textSecondary,
        outline = text.copy(alpha = 0.2f),
        outlineVariant = text.copy(alpha = 0.1f)
    )
}

// Dark Color Scheme — 主色自动提亮，保证在深色背景上的可读性
// 对于深色专属主题（DARK_MIDNIGHT、DARK_OCEAN），使用其自带的深色背景
// 对于浅色主题，使用 Material Design 标准深色背景
private fun darkColorScheme(
    appColorScheme: AppColorScheme = AppColorScheme.fromTheme(ColorTheme.IOS_BLUE)
): ColorScheme {
    val isDarkTheme = appColorScheme.theme == ColorTheme.DARK_MIDNIGHT || appColorScheme.theme == ColorTheme.DARK_OCEAN
    val bg = if (isDarkTheme) Color(appColorScheme.backgroundColor) else Color(0xFF121212)
    val surface = if (isDarkTheme) Color(appColorScheme.surfaceColor) else Color(0xFF1E1E1E)
    val surfaceVariant = if (isDarkTheme) {
        // 深色专属主题：surfaceVariant 比背景稍亮
        Color(appColorScheme.surfaceColor).copy(alpha = 0.85f).compositeOver(Color(0xFF2C2C2E))
    } else Color(0xFF2C2C2E)

    return darkColorScheme(
        primary = Color(appColorScheme.primaryColor).brightenForDark(0.55f),
        onPrimary = Color.White,
        primaryContainer = Color(appColorScheme.primaryLightColor).brightenForDark(0.4f).copy(alpha = 0.18f),
        onPrimaryContainer = Color(appColorScheme.primaryLightColor).brightenForDark(0.65f),
        secondary = Color(appColorScheme.secondaryColor).brightenForDark(0.5f),
        onSecondary = Color.White,
        secondaryContainer = Color(appColorScheme.secondaryColor).brightenForDark(0.35f).copy(alpha = 0.18f),
        onSecondaryContainer = Color(appColorScheme.secondaryColor).brightenForDark(0.6f),
        tertiary = Color(appColorScheme.accentColor).brightenForDark(0.5f),
        onTertiary = Color.White,
        tertiaryContainer = Color(appColorScheme.accentColor).brightenForDark(0.35f).copy(alpha = 0.18f),
        onTertiaryContainer = Color(appColorScheme.accentColor).brightenForDark(0.6f),
        background = bg,
        onBackground = if (isDarkTheme) Color(appColorScheme.textColor) else Color(0xFFE0E0E0),
        surface = surface,
        onSurface = if (isDarkTheme) Color(appColorScheme.textColor) else Color(0xFFE0E0E0),
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = if (isDarkTheme) Color(appColorScheme.textColor).copy(alpha = 0.7f) else Color(0xFF9E9E9E),
        outline = if (isDarkTheme) Color(appColorScheme.textColor).copy(alpha = 0.15f) else Color(0xFF38383A),
        outlineVariant = if (isDarkTheme) Color(appColorScheme.textColor).copy(alpha = 0.1f) else Color(0xFF3A3A3C)
    )
}

@Composable
fun TinyLedgerTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    appColorScheme: AppColorScheme = AppColorScheme.fromTheme(ColorTheme.IOS_BLUE),
    content: @Composable () -> Unit
) {
    // 使用 LocalConfiguration 直接检测系统深色模式
    val configuration = LocalConfiguration.current
    val systemIsDark = (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    
    // 主题生效逻辑：
    // 1. 系统深色模式：强制使用深色模式，只有午夜深蓝/深海墨蓝主题生效，浅色主题回退为午夜深蓝
    // 2. 系统浅色模式：先判断 app 的"深色模式"设置，再应用用户选择的主题（所有主题均可选）
    val isDarkTheme = appColorScheme.theme == ColorTheme.DARK_MIDNIGHT || appColorScheme.theme == ColorTheme.DARK_OCEAN

    val (effectiveColorScheme, darkTheme) = if (systemIsDark) {
        // 系统深色模式：始终深色，只有深色专属主题生效
        Pair(
            if (isDarkTheme) appColorScheme else AppColorScheme.fromTheme(ColorTheme.DARK_MIDNIGHT),
            true
        )
    } else {
        // 系统浅色模式：遵循 app 的"深色模式"设置
        val dark = when (themeMode) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.SYSTEM -> false // 系统浅色，跟随系统结果为浅色
        }
        Pair(appColorScheme, dark)
    }

    val colorScheme = if (darkTheme) {
        darkColorScheme(effectiveColorScheme)
    } else {
        lightColorScheme(effectiveColorScheme)
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(
        LocalAppColorScheme provides effectiveColorScheme
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

// 获取当前App颜色方案
val LocalAppColorSchemeValue: AppColorScheme
    @Composable
    @ReadOnlyComposable
    get() = LocalAppColorScheme.current
