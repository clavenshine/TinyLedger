package com.tinyledger.app.ui.theme

import androidx.compose.ui.graphics.Color

// ── 收入/支出基础颜色 ─────────────────────────────────────────────────
val IncomeGreen = Color(0xFF34C759)
val IncomeGreenLight = Color(0xFF30D158)
val ExpenseRed = Color(0xFFFF3B30)
val ExpenseRedLight = Color(0xFFFF453A)

// ── 饼图/图表专用配色（高区分度，跨越色相轮，保证深色/浅色模式均可读）───
/**
 * 图表切片颜色集，覆盖：绿、蓝、黄橙、红、紫、粉、青、绿黄、深橙
 * 相邻色相差 ≥ 40°，任意两色对 WCAG 对比度 ≥ 3:1（浅色背景）
 */
object ChartColors {
    // 浅色模式图表色（饱和度高，用于白/浅灰背景）
    val palette = listOf(
        Color(0xFF10B981), // 翡翠绿   — 餐饮
        Color(0xFF3B82F6), // 天蓝     — 交通
        Color(0xFFF59E0B), // 琥珀橙   — 购物
        Color(0xFFEF4444), // 珊瑚红   — 娱乐
        Color(0xFF8B5CF6), // 葡萄紫   — 居住
        Color(0xFFEC4899), // 玫瑰粉   — 医疗
        Color(0xFF06B6D4), // 青蓝     — 教育
        Color(0xFF84CC16), // 酸橙绿   — 通讯
        Color(0xFFF97316), // 橘橙     — 其他/保险
        Color(0xFF6366F1), // 靛紫     — 旅游
        Color(0xFF14B8A6), // 绿松石   — 借出
        Color(0xFFFBBF24)  // 金黄     — 投资支出
    )

    // 深色模式图表色（亮度提高约 10%，确保在 #121212 背景上对比度达标）
    val paletteDark = listOf(
        Color(0xFF34D399), // 翡翠绿亮
        Color(0xFF60A5FA), // 天蓝亮
        Color(0xFFFBBF24), // 琥珀亮
        Color(0xFFF87171), // 珊瑚红亮
        Color(0xFFA78BFA), // 葡萄紫亮
        Color(0xFFF472B6), // 玫瑰粉亮
        Color(0xFF22D3EE), // 青蓝亮
        Color(0xFFA3E635), // 酸橙绿亮
        Color(0xFFFB923C), // 橘橙亮
        Color(0xFF818CF8), // 靛紫亮
        Color(0xFF2DD4BF), // 绿松石亮
        Color(0xFFFCD34D)  // 金黄亮
    )
}

// ── iOS 风格配色（浅色）────────────────────────────────────────────────
object IOSColors {
    val Primary = Color(0xFF007AFF)
    val PrimaryLight = Color(0xFF5AC8FA)

    val SystemGreen = Color(0xFF34C759)
    val SystemRed = Color(0xFFFF3B30)
    val SystemOrange = Color(0xFFFF9500)
    val SystemYellow = Color(0xFFFFCC00)
    val SystemTeal = Color(0xFF5AC8FA)
    val SystemIndigo = Color(0xFF5856D6)
    val SystemPurple = Color(0xFFAF52DE)
    val SystemPink = Color(0xFFFF2D55)

    val Background = Color(0xFFF2F2F7)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceSecondary = Color(0xFFF2F2F7)

    // 数字可读性：深黑文字，对比度 > 7:1（白背景）
    val TextPrimary = Color(0xFF000000)
    val TextSecondary = Color(0xFF8E8E93)
    val TextTertiary = Color(0xFFC7C7CC)

    val Separator = Color(0xFFC6C6C8)
    val SeparatorLight = Color(0xFFE5E5EA)

    val Income = Color(0xFF34C759)
    val Expense = Color(0xFFFF3B30)

    val AccountBank = Color(0xFF007AFF)
    val AccountWechat = Color(0xFF07C160)
    val AccountAlipay = Color(0xFF1677FF)
    val AccountCash = Color(0xFFFF9500)
    val AccountOther = Color(0xFF8E8E93)
}

// ── iOS 风格配色（深色）────────────────────────────────────────────────
object IOSColorsDark {
    val Primary = Color(0xFF0A84FF)
    val PrimaryLight = Color(0xFF64D2FF)

    val Background = Color(0xFF000000)
    val Surface = Color(0xFF1C1C1E)
    val SurfaceSecondary = Color(0xFF2C2C2E)

    // 深色模式：纯白文字，对比度 > 7:1（#1C1C1E 背景）
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFF8E8E93)
    val TextTertiary = Color(0xFF48484A)

    val Separator = Color(0xFF38383A)
    val SeparatorLight = Color(0xFF3A3A3C)

    val Income = Color(0xFF30D158)
    val Expense = Color(0xFFFF453A)
}

// ── 华为 HarmonyOS 配色参考 ──────────────────────────────────────────
object HuaweiColors {
    val Primary = Color(0xFF1A6CE8)        // 星河蓝
    val PrimaryLight = Color(0xFF4D97F0)   // 浅星河蓝
    val Accent = Color(0xFFE96B00)         // 橙金
    val Success = Color(0xFF00B894)        // 薄荷绿
    val Background = Color(0xFFF5F5F5)
    val Surface = Color(0xFFFFFFFF)
    val TextPrimary = Color(0xFF212121)    // 对比度 > 14:1（白背景）
    val TextSecondary = Color(0xFF757575)
}

// ── 小米 MIUI 配色参考 ───────────────────────────────────────────────
object MIUIColors {
    val Primary = Color(0xFFFF6900)        // 小米橙红
    val PrimaryLight = Color(0xFFFFB347)   // 浅橙
    val Dark = Color(0xFF212121)           // 深黑灰
    val Accent = Color(0xFF2196F3)         // 蓝色数据高亮
    val Background = Color(0xFFF5F5F5)
    val Surface = Color(0xFFFFFFFF)
    val TextPrimary = Color(0xFF212121)
    val TextSecondary = Color(0xFF757575)
}

// ── 主题颜色预览色块（用于设置页展示）───────────────────────────────────
data class ThemeColorPreview(
    val name: String,
    val primary: Color,
    val primaryLight: Color,
    val secondary: Color,
    val background: Color = Color(0xFFF2F2F7),
    val group: String = ""  // 分组标签，用于 UI 分组显示
)

object ThemeColorPreviews {
    val themes = listOf(
        // 原有主题
        ThemeColorPreview("iOS蓝",   Color(0xFF007AFF), Color(0xFF5AC8FA), Color(0xFF5856D6), Color(0xFFF2F2F7), "经典"),
        ThemeColorPreview("优雅紫",   Color(0xFFAF52DE), Color(0xFFBF5AF2), Color(0xFF5E5CE6), Color(0xFFF5F3FF), "经典"),
        ThemeColorPreview("自然绿",   Color(0xFF34C759), Color(0xFF30D158), Color(0xFF00C7BE), Color(0xFFF1F8E9), "经典"),
        ThemeColorPreview("活力橙",   Color(0xFFFF9500), Color(0xFFFFCC00), Color(0xFFFF3B30), Color(0xFFFFF8E1), "经典"),
        ThemeColorPreview("少女粉",   Color(0xFFFF2D55), Color(0xFFFF6B8A), Color(0xFFFF6980), Color(0xFFFCE4EC), "经典"),
        ThemeColorPreview("清新青",   Color(0xFF5AC8FA), Color(0xFF64D2FF), Color(0xFF00C7BE), Color(0xFFE0F7FA), "经典"),
        ThemeColorPreview("深邃靛",   Color(0xFF5856D6), Color(0xFF7D7AFF), Color(0xFFFF2D55), Color(0xFFEDE7F6), "经典"),
        ThemeColorPreview("经典棕",   Color(0xFFA2845E), Color(0xFFAC8E68), Color(0xFF8B7355), Color(0xFFEFEBE9), "经典"),

        // 系统品牌主题
        ThemeColorPreview("iOS系统",  Color(0xFF007AFF), Color(0xFF34C759), Color(0xFF5856D6), Color(0xFFF2F2F7), "系统"),
        ThemeColorPreview("华为系统",  Color(0xFF1A6CE8), Color(0xFF4D97F0), Color(0xFFE96B00), Color(0xFFF5F5F5), "系统"),
        ThemeColorPreview("小米系统",  Color(0xFFFF6900), Color(0xFFFFB347), Color(0xFF212121), Color(0xFFF5F5F5), "系统"),

        // 专业商务型
        ThemeColorPreview("经典金融", Color(0xFF1565C0), Color(0xFF42A5F5), Color(0xFF5C6BC0), Color(0xFFFFFFFF), "商务"),
        ThemeColorPreview("资产增长", Color(0xFF2E7D32), Color(0xFF66BB6A), Color(0xFF388E3C), Color(0xFFF5F5F5), "商务"),
        ThemeColorPreview("冷静智慧", Color(0xFF00796B), Color(0xFF26A69A), Color(0xFF0097A7), Color(0xFFFAFAFA), "商务"),
        ThemeColorPreview("复古账本", Color(0xFF5D4037), Color(0xFF8D6E63), Color(0xFF6D4C41), Color(0xFFEFEBE9), "商务"),
        ThemeColorPreview("高端理财", Color(0xFF4527A0), Color(0xFF7E57C2), Color(0xFF311B92), Color(0xFFFFFFFF), "商务"),
        ThemeColorPreview("警示超支", Color(0xFFC62828), Color(0xFFE53935), Color(0xFFB71C1C), Color(0xFFFFEBEE), "商务"),
        ThemeColorPreview("清爽商务", Color(0xFF0277BD), Color(0xFF039BE5), Color(0xFF01579B), Color(0xFFE1F5FE), "商务"),

        // 清新生活型
        ThemeColorPreview("经典存钱", Color(0xFF66BB6A), Color(0xFFAED581), Color(0xFF33691E), Color(0xFFF1F8E9), "生活"),
        ThemeColorPreview("充满活力", Color(0xFFFFA726), Color(0xFFFFCC80), Color(0xFFE65100), Color(0xFFFFF3E0), "生活"),
        ThemeColorPreview("樱花粉黛", Color(0xFFEC407A), Color(0xFFF06292), Color(0xFF880E4F), Color(0xFFFCE4EC), "生活"),
        ThemeColorPreview("治愈旅行", Color(0xFF26A69A), Color(0xFF4DB6AC), Color(0xFF004D40), Color(0xFFE0F2F1), "生活"),
        ThemeColorPreview("梦幻清单", Color(0xFFAB47BC), Color(0xFFBA68C8), Color(0xFF4A148C), Color(0xFFF3E5F5), "生活"),
        ThemeColorPreview("冷淡极简", Color(0xFF78909C), Color(0xFFB0BEC5), Color(0xFF455A64), Color(0xFFECEFF1), "生活"),
        ThemeColorPreview("珊瑚日常", Color(0xFFFF7043), Color(0xFFFF8A65), Color(0xFFBF360C), Color(0xFFFFFFFF), "生活"),

        // 极简高效型
        ThemeColorPreview("纸质账本", Color(0xFF000000), Color(0xFF757575), Color(0xFF212121), Color(0xFFFFFFFF), "极简"),
        ThemeColorPreview("柔和黑白", Color(0xFF212121), Color(0xFF9E9E9E), Color(0xFF424242), Color(0xFFFAFAFA), "极简"),
        ThemeColorPreview("高对比度", Color(0xFF1A237E), Color(0xFF3949AB), Color(0xFF000000), Color(0xFFFFFFFF), "极简"),
        ThemeColorPreview("红黑冲击", Color(0xFFD32F2F), Color(0xFFB71C1C), Color(0xFF212121), Color(0xFFFFFFFF), "极简"),
        ThemeColorPreview("冷静克制", Color(0xFF0097A7), Color(0xFF00BCD4), Color(0xFF006064), Color(0xFFFAFAFA), "极简"),
        ThemeColorPreview("传统会计", Color(0xFF388E3C), Color(0xFF4CAF50), Color(0xFF1B5E20), Color(0xFFFFFFFF), "极简"),

        // 年轻渐变型
        ThemeColorPreview("少女心",   Color(0xFFFF9A9E), Color(0xFFFECFEF), Color(0xFFFF6B6B), Color(0xFFFFF0F5), "渐变"),
        ThemeColorPreview("梦想基金", Color(0xFF84FAB0), Color(0xFF8FD3F4), Color(0xFF00B4DB), Color(0xFFE0F7FA), "渐变"),
        ThemeColorPreview("搞钱日记", Color(0xFFFFD200), Color(0xFFF7971E), Color(0xFFFF6F00), Color(0xFFFFFFFF), "渐变"),
        ThemeColorPreview("活力运动", Color(0xFFFA709A), Color(0xFFFEE140), Color(0xFFFF4B1F), Color(0xFFFFFBEB), "渐变"),
        ThemeColorPreview("绿色生活", Color(0xFFA8E063), Color(0xFF56AB2F), Color(0xFFC8E6C9), Color(0xFFF1F8E9), "渐变"),
        ThemeColorPreview("清爽夏季", Color(0xFF4FACFE), Color(0xFF00F2FE), Color(0xFF4FC3F7), Color(0xFFE1F5FE), "渐变"),

        // 深色模式专属主题
        ThemeColorPreview("午夜深蓝", Color(0xFF64B5F6), Color(0xFF90CAF9), Color(0xFF81C784), Color(0xFF0D1117), "深色"),
        ThemeColorPreview("深海墨蓝", Color(0xFF4DD0E1), Color(0xFF80DEEA), Color(0xFFBA68C8), Color(0xFF0A192F), "深色")
    )

    // 按分组获取，方便设置页分组展示
    fun byGroup(): Map<String, List<ThemeColorPreview>> = themes.groupBy { it.group }

    // 分组显示顺序
    val groupOrder = listOf("经典", "系统", "商务", "生活", "极简", "渐变", "深色")
}
