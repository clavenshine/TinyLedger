package com.tinyledger.app.domain.model

/**
 * 预设颜色主题
 * 分组：原有iOS风格 / 专业商务型 / 清新生活型 / 极简高效型 / 年轻渐变型 / 系统主题型
 */
enum class ColorTheme(val displayName: String) {
    // ── 原有主题 ──────────────────────────────────────────────────
    IOS_BLUE("iOS蓝"),
    PURPLE("优雅紫"),
    GREEN("自然绿"),
    ORANGE("活力橙"),
    PINK("少女粉"),
    TEAL("清新青"),
    INDIGO("深邃靛"),
    BROWN("经典棕"),

    // ── 系统品牌主题 ───────────────────────────────────────────────
    IOS_THEME("iOS系统"),
    HUAWEI_THEME("华为系统"),
    XIAOMI_THEME("小米系统"),

    // ── F系列：专业商务型 ──────────────────────────────────────────
    F1_FINANCE("经典金融"),
    F2_WEALTH("资产增长"),
    F3_RATIONAL("冷静智慧"),
    F4_VINTAGE("复古账本"),
    F5_PREMIUM("高端理财"),
    F6_ALERT("警示超支"),
    F7_BUSINESS("清爽商务"),

    // ── L系列：清新生活型 ──────────────────────────────────────────
    L1_SAVINGS("经典存钱"),
    L2_VITALITY("充满活力"),
    L3_BLOSSOM("樱花粉黛"),
    L4_HEALING("治愈旅行"),
    L5_DREAM("梦幻清单"),
    L6_MINIMAL("冷淡极简"),
    L7_DAILY("珊瑚日常"),

    // ── M系列：极简高效型 ──────────────────────────────────────────
    M1_BLACKWHITE("纸质账本"),
    M2_SOFTBLACK("柔和黑白"),
    M3_CONTRAST("高对比度"),
    M4_REDBLACK("红黑冲击"),
    M5_CYANDARK("冷静克制"),
    // M6_AMBER 已删除
    M7_ACCOUNTING("传统会计"),

    // ── Y系列：年轻渐变型 ──────────────────────────────────────────
    // Y1_TECH 已删除
    Y2_GIRL("少女心"),
    Y3_DREAM_FUND("梦想基金"),
    Y4_MONEY("搞钱日记"),
    Y5_SPORT("活力运动"),
    // Y6_CYBER 已删除
    Y7_ECO("绿色生活"),
    Y8_SUMMER("清爽夏季"),

    // ── 深色模式专属主题 ───────────────────────────────────────────
    DARK_MIDNIGHT("午夜深蓝"),
    DARK_OCEAN("深海墨蓝")
}

/**
 * 应用颜色配置
 * primaryColor       主色（品牌色，用于标题栏、主按钮、强调元素）
 * primaryLightColor  辅助色（图表/点缀，需与主色有区分）
 * secondaryColor     次要色（图表第二层次、Tab选中等）
 * accentColor        强调色（支出/收入标记、图表第三色）
 * backgroundColor    界面基底色（浅色模式背景）
 * surfaceColor       卡片/表面色
 * textColor          主要文字色（确保与背景高对比度 ≥ 7:1）
 */
data class AppColorScheme(
    val theme: ColorTheme = ColorTheme.IOS_BLUE,
    val primaryColor: Long = 0xFF007AFF,
    val primaryLightColor: Long = 0xFF5AC8FA,
    val secondaryColor: Long = 0xFF5856D6,
    val accentColor: Long = 0xFFFF2D55,
    val backgroundColor: Long = 0xFFF2F2F7,
    val surfaceColor: Long = 0xFFFFFFFF,
    val textColor: Long = 0xFF212121
) {
    companion object {
        /**
         * 在深色模式下不适合使用的主题（背景本身就是深色，叠加系统暗色会难以阅读）
         */
        val darkModeUnsuitableThemes: Set<ColorTheme> = setOf(
            ColorTheme.M1_BLACKWHITE, // primaryColor=0xFF000000 纸质账本（黑色主色在暗色模式下对比差）
            ColorTheme.M4_REDBLACK,  // 红黑冲击（暗色模式下红色和黑色难以区分）
            // 所有浅色背景主题（白色/浅色背景在深色模式下会造成强烈对比）
            ColorTheme.IOS_BLUE, ColorTheme.PURPLE, ColorTheme.GREEN, ColorTheme.ORANGE,
            ColorTheme.PINK, ColorTheme.TEAL, ColorTheme.INDIGO, ColorTheme.BROWN,
            ColorTheme.IOS_THEME, ColorTheme.HUAWEI_THEME, ColorTheme.XIAOMI_THEME,
            ColorTheme.F1_FINANCE, ColorTheme.F2_WEALTH, ColorTheme.F3_RATIONAL,
            ColorTheme.F4_VINTAGE, ColorTheme.F5_PREMIUM, ColorTheme.F6_ALERT, ColorTheme.F7_BUSINESS,
            ColorTheme.L1_SAVINGS, ColorTheme.L2_VITALITY, ColorTheme.L3_BLOSSOM,
            ColorTheme.L4_HEALING, ColorTheme.L5_DREAM, ColorTheme.L6_MINIMAL, ColorTheme.L7_DAILY,
            ColorTheme.M2_SOFTBLACK, ColorTheme.M3_CONTRAST, ColorTheme.M5_CYANDARK, ColorTheme.M7_ACCOUNTING,
            ColorTheme.Y2_GIRL, ColorTheme.Y3_DREAM_FUND,
            ColorTheme.Y4_MONEY, ColorTheme.Y5_SPORT, ColorTheme.Y7_ECO, ColorTheme.Y8_SUMMER
        )
        fun fromTheme(theme: ColorTheme): AppColorScheme = when (theme) {

            // ══════════════════════════════════════════════════════════
            // 原有主题（默认 iOS 风格背景）
            // ══════════════════════════════════════════════════════════
            ColorTheme.IOS_BLUE -> AppColorScheme(
                theme = theme,
                primaryColor      = 0xFF007AFF,
                primaryLightColor = 0xFF5AC8FA,
                secondaryColor    = 0xFF5856D6,
                accentColor       = 0xFFFF2D55,
                backgroundColor   = 0xFFF2F2F7,
                surfaceColor      = 0xFFFFFFFF,
                textColor         = 0xFF000000
            )
            ColorTheme.PURPLE -> AppColorScheme(
                theme = theme,
                primaryColor      = 0xFFAF52DE,
                primaryLightColor = 0xFFBF5AF2,
                secondaryColor    = 0xFF5E5CE6,
                accentColor       = 0xFFFF375F,
                backgroundColor   = 0xFFF5F3FF,
                surfaceColor      = 0xFFFFFFFF,
                textColor         = 0xFF1C1B1F
            )
            ColorTheme.GREEN -> AppColorScheme(
                theme = theme,
                primaryColor      = 0xFF34C759,
                primaryLightColor = 0xFF30D158,
                secondaryColor    = 0xFF00C7BE,
                accentColor       = 0xFFFF9F0A,
                backgroundColor   = 0xFFF1F8E9,
                surfaceColor      = 0xFFFFFFFF,
                textColor         = 0xFF1B5E20
            )
            ColorTheme.ORANGE -> AppColorScheme(
                theme = theme,
                primaryColor      = 0xFFFF9500,
                primaryLightColor = 0xFFFFCC00,
                secondaryColor    = 0xFFFF3B30,
                accentColor       = 0xFF007AFF,
                backgroundColor   = 0xFFFFF8E1,
                surfaceColor      = 0xFFFFFFFF,
                textColor         = 0xFF212121
            )
            ColorTheme.PINK -> AppColorScheme(
                theme = theme,
                primaryColor      = 0xFFFF2D55,
                primaryLightColor = 0xFFFF6B8A,
                secondaryColor    = 0xFFFF6980,
                accentColor       = 0xFF5856D6,
                backgroundColor   = 0xFFFCE4EC,
                surfaceColor      = 0xFFFFFFFF,
                textColor         = 0xFF880E4F
            )
            ColorTheme.TEAL -> AppColorScheme(
                theme = theme,
                primaryColor      = 0xFF5AC8FA,
                primaryLightColor = 0xFF64D2FF,
                secondaryColor    = 0xFF00C7BE,
                accentColor       = 0xFFFF9F0A,
                backgroundColor   = 0xFFE0F7FA,
                surfaceColor      = 0xFFFFFFFF,
                textColor         = 0xFF004D40
            )
            ColorTheme.INDIGO -> AppColorScheme(
                theme = theme,
                primaryColor      = 0xFF5856D6,
                primaryLightColor = 0xFF7D7AFF,
                secondaryColor    = 0xFFFF2D55,
                accentColor       = 0xFFFF9500,
                backgroundColor   = 0xFFEDE7F6,
                surfaceColor      = 0xFFFFFFFF,
                textColor         = 0xFF1A237E
            )
            ColorTheme.BROWN -> AppColorScheme(
                theme = theme,
                primaryColor      = 0xFFA2845E,
                primaryLightColor = 0xFFAC8E68,
                secondaryColor    = 0xFF8B7355,
                accentColor       = 0xFFFF3B30,
                backgroundColor   = 0xFFEFEBE9,
                surfaceColor      = 0xFFFFFFFF,
                textColor         = 0xFF3E2723
            )

            // ══════════════════════════════════════════════════════════
            // 系统品牌主题
            // ══════════════════════════════════════════════════════════
            ColorTheme.IOS_THEME -> AppColorScheme(
                theme = theme,
                primaryColor      = 0xFF007AFF,
                primaryLightColor = 0xFF34C759,
                secondaryColor    = 0xFF5856D6,
                accentColor       = 0xFFFF2D55,
                backgroundColor   = 0xFFF2F2F7,
                surfaceColor      = 0xFFFFFFFF,
                textColor         = 0xFF000000
            )
            ColorTheme.HUAWEI_THEME -> AppColorScheme(
                theme = theme,
                primaryColor      = 0xFF1A6CE8,
                primaryLightColor = 0xFF4D97F0,
                secondaryColor    = 0xFFE96B00,
                accentColor       = 0xFF00B894,
                backgroundColor   = 0xFFF5F5F5,
                surfaceColor      = 0xFFFFFFFF,
                textColor         = 0xFF212121
            )
            ColorTheme.XIAOMI_THEME -> AppColorScheme(
                theme = theme,
                primaryColor      = 0xFFFF6900,
                primaryLightColor = 0xFFFFB347,
                secondaryColor    = 0xFF212121,
                accentColor       = 0xFF2196F3,
                backgroundColor   = 0xFFF5F5F5,
                surfaceColor      = 0xFFFFFFFF,
                textColor         = 0xFF212121
            )

            // ══════════════════════════════════════════════════════════
            // F系列：专业商务型 — 强调信任与数据可视化
            // ══════════════════════════════════════════════════════════
            ColorTheme.F1_FINANCE -> AppColorScheme(
                theme = theme,
                primaryColor      = 0xFF1565C0, // 深蓝
                primaryLightColor = 0xFF42A5F5, // 亮蓝
                secondaryColor    = 0xFF5C6BC0,
                accentColor       = 0xFFF59E0B,
                backgroundColor   = 0xFFFFFFFF, // 纯白
                surfaceColor      = 0xFFFFFFFF,
                textColor         = 0xFF263238  // 深灰
            )
            ColorTheme.F2_WEALTH -> AppColorScheme(
                theme = theme,
                primaryColor      = 0xFF2E7D32, // 森林绿
                primaryLightColor = 0xFF66BB6A, // 草绿
                secondaryColor    = 0xFF388E3C,
                accentColor       = 0xFFF59E0B,
                backgroundColor   = 0xFFF5F5F5, // 浅灰
                surfaceColor      = 0xFFFFFFFF,
                textColor         = 0xFF212121  // 黑
            )
            ColorTheme.F3_RATIONAL -> AppColorScheme(
                theme = theme,
                primaryColor      = 0xFF00796B, // 青色
                primaryLightColor = 0xFF26A69A, // 蓝绿
                secondaryColor    = 0xFF0097A7,
                accentColor       = 0xFFFF7043,
                backgroundColor   = 0xFFFAFAFA, // 米白
                surfaceColor      = 0xFFFFFFFF,
                textColor         = 0xFF37474F  // 蓝灰
            )
            ColorTheme.F4_VINTAGE -> AppColorScheme(
                theme = theme,
                primaryColor      = 0xFF5D4037, // 深棕
                primaryLightColor = 0xFF8D6E63, // 浅棕
                secondaryColor    = 0xFF6D4C41,
                accentColor       = 0xFFF59E0B,
                backgroundColor   = 0xFFEFEBE9, // 暖灰
                surfaceColor      = 0xFFFAF8F5,
                textColor         = 0xFF3E2723  // 深褐
            )
            ColorTheme.F5_PREMIUM -> AppColorScheme(
                theme = theme,
                primaryColor      = 0xFF4527A0, // 深紫
                primaryLightColor = 0xFF7E57C2, // 紫罗兰
                secondaryColor    = 0xFF311B92,
                accentColor       = 0xFFFFD54F,
                backgroundColor   = 0xFFFFFFFF, // 纯白
                surfaceColor      = 0xFFF8F5FF,
                textColor         = 0xFF1A237E  // 靛蓝
            )
            ColorTheme.F6_ALERT -> AppColorScheme(
                theme = theme,
                primaryColor      = 0xFFC62828, // 深红
                primaryLightColor = 0xFFE53935, // 鲜红
                secondaryColor    = 0xFFB71C1C,
                accentColor       = 0xFFF59E0B,
                backgroundColor   = 0xFFFFEBEE, // 淡红
                surfaceColor      = 0xFFFFFFFF,
                textColor         = 0xFF212121  // 黑
            )
            ColorTheme.F7_BUSINESS -> AppColorScheme(
                theme = theme,
                primaryColor      = 0xFF0277BD, // 天蓝
                primaryLightColor = 0xFF039BE5, // 蔚蓝
                secondaryColor    = 0xFF01579B,
                accentColor       = 0xFFFF8F00,
                backgroundColor   = 0xFFE1F5FE, // 极淡蓝
                surfaceColor      = 0xFFFFFFFF,
                textColor         = 0xFF01579B  // 深蓝
            )

            // ══════════════════════════════════════════════════════════
            // L系列：清新生活型 — 轻松存钱与愉悦记账
            // ══════════════════════════════════════════════════════════
            ColorTheme.L1_SAVINGS -> AppColorScheme(
                theme = theme,
                primaryColor      = 0xFF66BB6A, // 新芽绿
                primaryLightColor = 0xFFAED581, // 嫩绿
                secondaryColor    = 0xFF33691E,
                accentColor       = 0xFFF59E0B,
                backgroundColor   = 0xFFF1F8E9, // 淡绿白
                surfaceColor      = 0xFFFFFFFF,
                textColor         = 0xFF33691E  // 深绿
            )
            ColorTheme.L2_VITALITY -> AppColorScheme(
                theme = theme,
                primaryColor      = 0xFFFFA726, // 暖橙
                primaryLightColor = 0xFFFFCC80, // 杏色
                secondaryColor    = 0xFFE65100,
                accentColor       = 0xFF5C6BC0,
                backgroundColor   = 0xFFFFF3E0, // 淡橙白
                surfaceColor      = 0xFFFFFFFF,
                textColor         = 0xFFE65100  // 深橙
            )
            ColorTheme.L3_BLOSSOM -> AppColorScheme(
                theme = theme,
                primaryColor      = 0xFFEC407A, // 樱花粉
                primaryLightColor = 0xFFF06292, // 粉红
                secondaryColor    = 0xFF880E4F,
                accentColor       = 0xFF7E57C2,
                backgroundColor   = 0xFFFCE4EC, // 淡粉白
                surfaceColor      = 0xFFFFFFFF,
                textColor         = 0xFF880E4F  // 深粉
            )
            ColorTheme.L4_HEALING -> AppColorScheme(
                theme = theme,
                primaryColor      = 0xFF26A69A, // 蒂芙尼蓝
                primaryLightColor = 0xFF4DB6AC, // 浅青
                secondaryColor    = 0xFF004D40,
                accentColor       = 0xFFFF8F00,
                backgroundColor   = 0xFFE0F2F1, // 淡青白
                surfaceColor      = 0xFFFFFFFF,
                textColor         = 0xFF004D40  // 深青
            )
            ColorTheme.L5_DREAM -> AppColorScheme(
                theme = theme,
                primaryColor      = 0xFFAB47BC, // 香芋紫
                primaryLightColor = 0xFFBA68C8, // 淡紫
                secondaryColor    = 0xFF4A148C,
                accentColor       = 0xFF26A69A,
                backgroundColor   = 0xFFF3E5F5, // 淡紫白
                surfaceColor      = 0xFFFFFFFF,
                textColor         = 0xFF4A148C  // 深紫
            )
            ColorTheme.L6_MINIMAL -> AppColorScheme(
                theme = theme,
                primaryColor      = 0xFF78909C, // 蓝灰
                primaryLightColor = 0xFFB0BEC5, // 银灰
                secondaryColor    = 0xFF455A64,
                accentColor       = 0xFFFF7043,
                backgroundColor   = 0xFFECEFF1, // 冷灰
                surfaceColor      = 0xFFFFFFFF,
                textColor         = 0xFF455A64  // 深灰蓝
            )
            ColorTheme.L7_DAILY -> AppColorScheme(
                theme = theme,
                primaryColor      = 0xFFFF7043, // 珊瑚橘
                primaryLightColor = 0xFFFF8A65, // 浅橘
                secondaryColor    = 0xFFBF360C,
                accentColor       = 0xFF5C6BC0,
                backgroundColor   = 0xFFFFFFFF, // 纯白
                surfaceColor      = 0xFFFFFFFF,
                textColor         = 0xFFBF360C  // 深红橙
            )

            // ══════════════════════════════════════════════════════════
            // M系列：极简高效型 — 高对比度让数字一目了然
            // ══════════════════════════════════════════════════════════
            ColorTheme.M1_BLACKWHITE -> AppColorScheme(
                theme = theme,
                primaryColor      = 0xFF000000, // 纯黑
                primaryLightColor = 0xFF757575, // 中灰
                secondaryColor    = 0xFF212121,
                accentColor       = 0xFFD32F2F,
                backgroundColor   = 0xFFFFFFFF, // 纯白
                surfaceColor      = 0xFFFFFFFF,
                textColor         = 0xFF212121  // 深黑
            )
            ColorTheme.M2_SOFTBLACK -> AppColorScheme(
                theme = theme,
                primaryColor      = 0xFF212121, // 深灰
                primaryLightColor = 0xFF9E9E9E, // 灰色
                secondaryColor    = 0xFF424242,
                accentColor       = 0xFF1565C0,
                backgroundColor   = 0xFFFAFAFA, // 米白
                surfaceColor      = 0xFFFFFFFF,
                textColor         = 0xFF424242  // 次深灰
            )
            ColorTheme.M3_CONTRAST -> AppColorScheme(
                theme = theme,
                primaryColor      = 0xFF1A237E, // 靛蓝
                primaryLightColor = 0xFF3949AB, // 蓝紫
                secondaryColor    = 0xFF000000,
                accentColor       = 0xFFD32F2F,
                backgroundColor   = 0xFFFFFFFF, // 纯白
                surfaceColor      = 0xFFFFFFFF,
                textColor         = 0xFF000000  // 纯黑
            )
            ColorTheme.M4_REDBLACK -> AppColorScheme(
                theme = theme,
                primaryColor      = 0xFFD32F2F, // 正红
                primaryLightColor = 0xFFB71C1C, // 深红
                secondaryColor    = 0xFF212121,
                accentColor       = 0xFF1565C0,
                backgroundColor   = 0xFFFFFFFF, // 纯白
                surfaceColor      = 0xFFFFFFFF,
                textColor         = 0xFF212121  // 深灰
            )
            ColorTheme.M5_CYANDARK -> AppColorScheme(
                theme = theme,
                primaryColor      = 0xFF0097A7, // 深青
                primaryLightColor = 0xFF00BCD4, // 亮青
                secondaryColor    = 0xFF006064,
                accentColor       = 0xFFF59E0B,
                backgroundColor   = 0xFFFAFAFA, // 米白
                surfaceColor      = 0xFFFFFFFF,
                textColor         = 0xFF006064  // 深青黑
            )
            ColorTheme.M7_ACCOUNTING -> AppColorScheme(
                theme = theme,
                primaryColor      = 0xFF388E3C, // 正绿
                primaryLightColor = 0xFF4CAF50, // 亮绿
                secondaryColor    = 0xFF1B5E20,
                accentColor       = 0xFFD32F2F,
                backgroundColor   = 0xFFFFFFFF, // 纯白
                surfaceColor      = 0xFFFFFFFF,
                textColor         = 0xFF1B5E20  // 深绿
            )

            // ══════════════════════════════════════════════════════════
            // Y系列：年轻渐变型 — 趣味化，游戏化记账
            // ══════════════════════════════════════════════════════════
            ColorTheme.Y2_GIRL -> AppColorScheme(
                theme = theme,
                primaryColor      = 0xFFFF9A9E, // 粉嫩渐变
                primaryLightColor = 0xFFFECFEF,
                secondaryColor    = 0xFFFF6B6B,
                accentColor       = 0xFF553C3A,
                backgroundColor   = 0xFFFFF0F5, // 淡粉
                surfaceColor      = 0xFFFFFFFF,
                textColor         = 0xFF553C3A  // 深褐
            )
            ColorTheme.Y3_DREAM_FUND -> AppColorScheme(
                theme = theme,
                primaryColor      = 0xFF84FAB0, // 青蓝渐变
                primaryLightColor = 0xFF8FD3F4,
                secondaryColor    = 0xFF00B4DB,
                accentColor       = 0xFF006064,
                backgroundColor   = 0xFFE0F7FA, // 淡蓝
                surfaceColor      = 0xFFFFFFFF,
                textColor         = 0xFF006064  // 深青
            )
            ColorTheme.Y4_MONEY -> AppColorScheme(
                theme = theme,
                primaryColor      = 0xFFFFD200, // 金橙渐变
                primaryLightColor = 0xFFF7971E,
                secondaryColor    = 0xFFFF6F00,
                accentColor       = 0xFF3E2723,
                backgroundColor   = 0xFFFFFFFF, // 纯白
                surfaceColor      = 0xFFFFF8E1,
                textColor         = 0xFF3E2723  // 深褐
            )
            ColorTheme.Y5_SPORT -> AppColorScheme(
                theme = theme,
                primaryColor      = 0xFFFA709A, // 粉黄渐变
                primaryLightColor = 0xFFFEE140,
                secondaryColor    = 0xFFFF4B1F,
                accentColor       = 0xFF4A192C,
                backgroundColor   = 0xFFFFFBEB, // 暖白
                surfaceColor      = 0xFFFFFFFF,
                textColor         = 0xFF4A192C  // 深紫红
            )
            ColorTheme.Y7_ECO -> AppColorScheme(
                theme = theme,
                primaryColor      = 0xFFA8E063, // 草绿渐变
                primaryLightColor = 0xFF56AB2F,
                secondaryColor    = 0xFFC8E6C9,
                accentColor       = 0xFF1B5E20,
                backgroundColor   = 0xFFF1F8E9, // 淡绿
                surfaceColor      = 0xFFFFFFFF,
                textColor         = 0xFF1B5E20  // 深绿
            )
            ColorTheme.Y8_SUMMER -> AppColorScheme(
                theme = theme,
                primaryColor      = 0xFF4FACFE, // 天蓝渐变
                primaryLightColor = 0xFF00F2FE,
                secondaryColor    = 0xFF4FC3F7,
                accentColor       = 0xFF0277BD,
                backgroundColor   = 0xFFE1F5FE, // 淡蓝
                surfaceColor      = 0xFFFFFFFF,
                textColor         = 0xFF0277BD  // 深蓝
            )

            // ══════════════════════════════════════════════════════════
            // 深色模式专属主题 — 专为深色模式优化的高对比度配色
            // ══════════════════════════════════════════════════════════
            ColorTheme.DARK_MIDNIGHT -> AppColorScheme(
                theme = theme,
                primaryColor      = 0xFF64B5F6, // 中蓝（提亮适配深色背景）
                primaryLightColor = 0xFF90CAF9, // 浅蓝
                secondaryColor    = 0xFF81C784, // 中绿
                accentColor       = 0xFFFFB74D, // 暖橙
                backgroundColor   = 0xFF0D1117, // 极深蓝黑（GitHub Dark风格）
                surfaceColor      = 0xFF161B22, // 深蓝灰表面
                textColor         = 0xFFE6EDF3  // 浅灰白文字
            )
            ColorTheme.DARK_OCEAN -> AppColorScheme(
                theme = theme,
                primaryColor      = 0xFF4DD0E1, // 青蓝（海洋风格）
                primaryLightColor = 0xFF80DEEA, // 浅青
                secondaryColor    = 0xFFBA68C8, // 淡紫（点缀）
                accentColor       = 0xFFFF8A65, // 珊瑚橙
                backgroundColor   = 0xFF0A192F, // 深海蓝黑
                surfaceColor      = 0xFF112240, // 深海蓝表面
                textColor         = 0xFFCCD6F6  // 浅蓝白文字
            )
        }
    }
}
