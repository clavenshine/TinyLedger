package com.tinyledger.app.ui.screens.statistics

import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tinyledger.app.domain.model.CategoryAmount
import com.tinyledger.app.domain.model.DailyRecord
import com.tinyledger.app.domain.model.MonthlyRecord
import com.tinyledger.app.ui.theme.ChartColors
import com.tinyledger.app.ui.theme.IOSColors
import com.tinyledger.app.ui.theme.IOSColorsDark
import com.tinyledger.app.ui.viewmodel.DateMode
import com.tinyledger.app.ui.viewmodel.StatsTab
import com.tinyledger.app.ui.viewmodel.StatisticsUiState
import com.tinyledger.app.ui.viewmodel.StatisticsViewModel
import com.tinyledger.app.util.CurrencyUtils
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

// ──────────────────────────── 深紫灰配色常量 ────────────────────────────
private val DeepPurple = Color(0xFF6B5B95)        // 主色深紫灰色
private val DeepPurpleDark = Color(0xFF8A7BB5)     // 深色模式提亮
private val LightGrayBar = Color(0xFFF5F4F9)       // 浅色模式背景柱默认
private val LightGrayBarSelected = Color(0xFFE8E6F0) // 浅色模式背景柱选中
private val DarkGrayBar = Color(0xFF28282C)        // 深色模式背景柱默认
private val DarkGrayBarSelected = Color(0xFF3A3A40)// 深色模式背景柱选中
private val DividerColorLight = Color(0xFFEBEBEF)   // 浅色分割线
private val DividerColorDark = Color(0xFF38383A)    // 深色分割线
private val TextPrimary = Color(0xFF1C1C1E)         // 主文字黑
private val TextSecondary = Color(0xFF8E8E93)       // 次要文字灰
private val DotColor = Color(0xFFC7C7CC)            // 基线空心圆点颜色

/** 判断当前是否为深色模式 — 基于 MaterialTheme 而非 isSystemInDarkTheme()，
 *  这样 App 手动切换深色模式时也能正确响应。
 *  使用 remember 缓存结果，避免每次重组重复计算 */
@Composable
private fun isInDarkTheme(): Boolean {
    val bg = MaterialTheme.colorScheme.background
    return remember(bg) {
        val luminance = 0.2126f * (bg.red * bg.red) + 0.7152f * (bg.green * bg.green) + 0.0722f * (bg.blue * bg.blue)
        luminance < 0.25f
    }
}

// ──────────────────────────── 主界面 ────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun StatisticsScreen(
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }

    // Pager 状态：3 个页面 — 支出/收入/结余
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 3 })
    val tabToPage = mapOf(StatsTab.EXPENSE to 0, StatsTab.INCOME to 1, StatsTab.BALANCE to 2)
    val pageToTab = mapOf(0 to StatsTab.EXPENSE, 1 to StatsTab.INCOME, 2 to StatsTab.BALANCE)

    // 同步 pager → viewModel
    LaunchedEffect(pagerState.currentPage) {
        val tab = pageToTab[pagerState.currentPage] ?: StatsTab.EXPENSE
        if (uiState.activeTab != tab) viewModel.setActiveTab(tab)
    }
    // 同步 viewModel → pager
    LaunchedEffect(uiState.activeTab) {
        val page = tabToPage[uiState.activeTab] ?: 0
        if (pagerState.currentPage != page) {
            pagerState.animateScrollToPage(page)
        }
    }

    Surface(color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 固定区域：Tab + 日期控件行
            TabWithDateSelector(uiState, viewModel, onShowPicker = { showDatePicker = it })

            // 可滚动区域：其余所有内容
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize().weight(1f),
                verticalAlignment = Alignment.Top
            ) { page ->
                val tab = pageToTab[page] ?: StatsTab.EXPENSE

                LazyColumn(
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    // 概览卡片（随内容滚动）
                    item { OverviewCards(uiState) }

                    item { TrendChartCard(uiState = uiState) }

                    if (tab != StatsTab.BALANCE) {
                        val categories = if (tab == StatsTab.EXPENSE)
                            uiState.expenseByCategory else uiState.incomeByCategory
                        val totalAmount = if (tab == StatsTab.EXPENSE)
                            uiState.totalExpense else uiState.totalIncome
                        if (categories.isNotEmpty()) {
                            item { Spacer(modifier = Modifier.height(8.dp)) }
                            item { CategoryRankingSection(categories, totalAmount, uiState.currencySymbol) }
                        }
                    }

                    // 结余Tab
                    if (tab == StatsTab.BALANCE) {
                        if (uiState.dateMode == DateMode.MONTHLY && uiState.dailyRecords.isNotEmpty()) {
                            item { Spacer(modifier = Modifier.height(8.dp)) }
                            item {
                                DailySummaryTable(
                                    dailyRecords = uiState.dailyRecords,
                                    currencySymbol = uiState.currencySymbol,
                                    totalIncome = uiState.totalIncome,
                                    totalExpense = uiState.totalExpense,
                                    balance = uiState.balance
                                )
                            }
                        }
                        // 年度模式：每月概况
                        if (uiState.dateMode == DateMode.YEARLY && uiState.monthlyRecords.isNotEmpty()) {
                            item { Spacer(modifier = Modifier.height(8.dp)) }
                            item {
                                MonthlySummaryTable(
                                    monthlyRecords = uiState.monthlyRecords,
                                    currencySymbol = uiState.currencySymbol,
                                    totalIncome = uiState.totalIncome,
                                    totalExpense = uiState.totalExpense,
                                    balance = uiState.balance,
                                    selectedYear = uiState.selectedYear
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        BottomDatePickerSheet(uiState, viewModel, onDismiss = { showDatePicker = false })
    }
}

// ──────────────────────────── Tab + 日期选择器 ────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TabWithDateSelector(
    uiState: StatisticsUiState,
    viewModel: StatisticsViewModel,
    onShowPicker: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 4.dp,
            modifier = Modifier.padding(end = 12.dp)
        ) {
            Row(
                modifier = Modifier.padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val isDark = isInDarkTheme()
                StatsTab.entries.forEach { tab ->
                    val isSelected = uiState.activeTab == tab
                    val bgColor = if (isSelected) when (tab) {
                        StatsTab.EXPENSE -> Color(0xFFFF3B30)
                        StatsTab.INCOME -> Color(0xFF34C759)
                        StatsTab.BALANCE -> if (isDark) IOSColorsDark.Primary else IOSColors.Primary
                    } else Color.Transparent
                    val textColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = bgColor,
                        modifier = Modifier
                            .height(36.dp)
                            .clickable { viewModel.setActiveTab(tab) }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = when (tab) { StatsTab.EXPENSE -> "支出"; StatsTab.INCOME -> "收入"; StatsTab.BALANCE -> "结余" },
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                color = textColor,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp)
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 4.dp, border = null,
            modifier = Modifier.clickable { onShowPicker(true) }) {
            Row(modifier = Modifier.height(48.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(text = if (uiState.dateMode == DateMode.MONTHLY) "${uiState.selectedYear}年${String.format("%02d", uiState.selectedMonth)}月"
                       else "${uiState.selectedYear}年",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium, fontSize = 14.sp), color = MaterialTheme.colorScheme.onSurface)
                Icon(Icons.Default.KeyboardArrowDown, "", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ──────────────────────────── 底部弹出日期选择器（两Tab栏：按月/按年，选择自动确认） ────────────────────────────

/** 日期弹窗的 Tab 类型 */
enum class DatePickerTab { MONTHLY, YEARLY }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BottomDatePickerSheet(uiState: StatisticsUiState, viewModel: StatisticsViewModel, onDismiss: () -> Unit) {
    val isDark = isInDarkTheme()
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1

    var localTab by remember { mutableStateOf(
        when (uiState.dateMode) {
            DateMode.MONTHLY -> DatePickerTab.MONTHLY
            DateMode.YEARLY -> DatePickerTab.YEARLY
        }
    )}
    var localYear by remember { mutableStateOf(uiState.selectedYear) }
    var localMonth by remember { mutableStateOf(uiState.selectedMonth) }
    // 年份区间起始年（每页显示12年，4×3网格，仅按年查看Tab使用）
    var yearPageStart by remember { mutableStateOf((localYear / 12) * 12) }

    val accentColor = if (isDark) Color(0xFFAF52DE) else Color(0xFF5856D6)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 32.dp)
        ) {
            // ── 两标签 Tab 栏：按月查看 / 按年查看 ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                listOf(
                    DatePickerTab.MONTHLY to "按月查看",
                    DatePickerTab.YEARLY to "按年查看"
                ).forEach { (tab, label) ->
                    val isSelected = localTab == tab
                    Column(
                        modifier = Modifier
                            .clickable { localTab = tab }
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            label,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            ),
                            color = if (isSelected) {
                                if (isDark) Color.White else Color(0xFF1C1C1E)
                            } else {
                                if (isDark) Color(0xFF8E8E93) else Color(0xFFAEAEB2)
                            }
                        )
                        Spacer(Modifier.height(4.dp))
                        // 选中态底部下划线
                        Box(
                            modifier = Modifier
                                .width(24.dp)
                                .height(2.5.dp)
                                .background(
                                    color = if (isSelected) accentColor else Color.Transparent,
                                    shape = RoundedCornerShape(1.25.dp)
                                )
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── 按月查看：年份导航 + 月份网格（选择月份自动确认并关闭） ──
            if (localTab == DatePickerTab.MONTHLY) {
                // 年份导航
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { localYear -= 1 }) {
                        Icon(Icons.Default.ChevronLeft, "",
                            tint = if (isDark) Color(0xFFAEAEB2) else Color(0xFF8E8E93))
                    }
                    Text(
                        "${localYear}年",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = if (isDark) Color.White else Color(0xFF1C1C1E),
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    IconButton(onClick = {
                        if (localYear < currentYear) localYear += 1
                    }) {
                        Icon(Icons.Default.ChevronRight, "",
                            tint = if (isDark) Color(0xFFAEAEB2) else Color(0xFF8E8E93))
                    }
                }

                Spacer(Modifier.height(12.dp))

                // 月份网格（4×3），点击月份自动确认
                (1..12).chunked(4).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        row.forEach { m ->
                            val isSelected = m == localMonth && localYear == uiState.selectedYear
                            val isFutureMonth = localYear == currentYear && m > currentMonth

                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) accentColor else Color.Transparent)
                                    .then(
                                        if (!isFutureMonth) Modifier.clickable {
                                            // 选中月份自动确认并关闭
                                            viewModel.setDateMode(DateMode.MONTHLY)
                                            viewModel.setYearMonth(localYear, m)
                                            onDismiss()
                                        } else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "${m}月",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 14.sp
                                    ),
                                    color = when {
                                        isSelected -> Color.White
                                        isFutureMonth -> if (isDark) Color(0xFF48484A) else Color(0xFFD1D1D6)
                                        else -> if (isDark) Color(0xFFCCCCCC) else Color(0xFF1C1C1E)
                                    }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            // ── 按年查看：年份标题区 + 年份网格（选择年份自动确认并关闭） ──
            if (localTab == DatePickerTab.YEARLY) {
                // 年份标题区：左箭头 + "年份" + 右箭头
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { yearPageStart -= 12 },
                        enabled = yearPageStart > 2000
                    ) {
                        Icon(
                            Icons.Default.ChevronLeft, "",
                            tint = if (isDark) Color(0xFFAEAEB2) else Color(0xFF8E8E93),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        "年份",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        ),
                        color = if (isDark) Color.White else Color(0xFF1C1C1E),
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    IconButton(
                        onClick = { yearPageStart += 12 },
                        enabled = yearPageStart + 11 < currentYear + 1
                    ) {
                        Icon(
                            Icons.Default.ChevronRight, "",
                            tint = if (isDark) Color(0xFFAEAEB2) else Color(0xFF8E8E93),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // 年份选择网格（4列×3行 = 12个年份），点击年份自动确认
                val years = (yearPageStart..yearPageStart + 11).toList()
                years.chunked(4).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        row.forEach { year ->
                            val isSelected = year == localYear
                            val isFuture = year > currentYear

                            Box(
                                modifier = Modifier
                                    .size(64.dp, 48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) accentColor else Color.Transparent
                                    )
                                    .then(
                                        if (!isFuture) Modifier.clickable {
                                            // 选中年份自动确认并关闭
                                            viewModel.setDateMode(DateMode.YEARLY)
                                            viewModel.setYearMonth(year, 1)
                                            onDismiss()
                                        } else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "$year",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 14.sp
                                    ),
                                    color = when {
                                        isSelected -> Color.White
                                        isFuture -> if (isDark) Color(0xFF48484A) else Color(0xFFD1D1D6)
                                        else -> if (isDark) Color(0xFFCCCCCC) else Color(0xFF1C1C1E)
                                    }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

// ──────────────────────────── 概览卡片 ────────────────────────────
@Composable
private fun OverviewCards(uiState: StatisticsUiState) {
    val surfaceCard = MaterialTheme.colorScheme.surface
    val textColor = MaterialTheme.colorScheme.onSurface
    val secondaryText = MaterialTheme.colorScheme.onSurfaceVariant
    val isMonthly = uiState.dateMode == DateMode.MONTHLY

    // 第一行：主数据卡片
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        when (uiState.activeTab) {
            StatsTab.EXPENSE -> {
                OverviewMiniCard("实支金额", CurrencyUtils.format(uiState.totalExpense, uiState.currencySymbol), surfaceCard, textColor, secondaryText, Modifier.weight(1f))
                val avgLabel = if (isMonthly) "日均支出" else "月均支出"
                val elapsedMonths = if (!isMonthly && uiState.selectedYear == Calendar.getInstance().get(Calendar.YEAR))
                    Calendar.getInstance().get(Calendar.MONTH) + 1 else 12.coerceAtLeast(Calendar.getInstance().get(Calendar.MONTH) + 1)
                val avgValue = if (isMonthly) uiState.dailyAverageExpense else uiState.totalExpense / elapsedMonths
                OverviewMiniCard(avgLabel, CurrencyUtils.format(avgValue, uiState.currencySymbol), surfaceCard, textColor, secondaryText, Modifier.weight(1f))
            }
            StatsTab.INCOME -> {
                OverviewMiniCard("收入金额", CurrencyUtils.format(uiState.totalIncome, uiState.currencySymbol), surfaceCard, textColor, secondaryText, Modifier.weight(1f))
                val avgLabel = if (isMonthly) "日均收入" else "月均收入"
                val elapsedMonths = if (!isMonthly && uiState.selectedYear == Calendar.getInstance().get(Calendar.YEAR))
                    Calendar.getInstance().get(Calendar.MONTH) + 1 else 12.coerceAtLeast(Calendar.getInstance().get(Calendar.MONTH))
                val avgValue = if (isMonthly) uiState.dailyAverageIncome else uiState.totalIncome / elapsedMonths
                OverviewMiniCard(avgLabel, CurrencyUtils.format(avgValue, uiState.currencySymbol), surfaceCard, textColor, secondaryText, Modifier.weight(1f))
            }
            StatsTab.BALANCE -> {
                OverviewMiniCard("结余金额", CurrencyUtils.format(uiState.balance, uiState.currencySymbol), surfaceCard, textColor, secondaryText, Modifier.weight(1f))
                val avgLabel = if (isMonthly) "日均结余" else "月均结余"
                val avgValue: Double
                if (isMonthly) {
                    // 月度模式：日均结余 = 当月总结余 / 当月累计已过天数
                    val cal = Calendar.getInstance()
                    val currentDay = if (uiState.selectedYear == cal.get(Calendar.YEAR) &&
                        uiState.selectedMonth == cal.get(Calendar.MONTH) + 1) {
                        cal.get(Calendar.DAY_OF_MONTH)
                    } else {
                        cal.set(uiState.selectedYear, uiState.selectedMonth - 1, 1)
                        cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                    }
                    avgValue = uiState.balance / currentDay.coerceAtLeast(1)
                } else {
                    // 年度模式：月均结余 = 当年总结余 / 当年累计已过月份
                    val cal = Calendar.getInstance()
                    val elapsedMonths = if (uiState.selectedYear == cal.get(Calendar.YEAR)) {
                        cal.get(Calendar.MONTH) + 1
                    } else 12
                    avgValue = uiState.balance / elapsedMonths.coerceAtLeast(1)
                }
                OverviewMiniCard(avgLabel, CurrencyUtils.format(avgValue, uiState.currencySymbol), surfaceCard, textColor, secondaryText, Modifier.weight(1f))
            }
        }
    }

    // 第二行：仅支出Tab显示预算卡片，其他Tab不显示空白卡片
    if (uiState.activeTab == StatsTab.EXPENSE) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val budgetLabel = if (isMonthly) "本月预算" else "本年预算"
            OverviewMiniCard(budgetLabel, CurrencyUtils.format(uiState.budgetAmount, uiState.currencySymbol), surfaceCard, textColor, secondaryText, Modifier.weight(1f))
            OverviewMiniCard("剩余预算", CurrencyUtils.format(uiState.remainingBudget, uiState.currencySymbol), surfaceCard, textColor, secondaryText, Modifier.weight(1f))
        }
    }
}

@Composable
private fun OverviewMiniCard(label: String, value: String, bg: Color, textColor: Color, secondaryText: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = bg), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            if (label.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(secondaryText.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                        Text(label.firstOrNull()?.toString() ?: "", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold), color = secondaryText)
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(label, style = MaterialTheme.typography.labelSmall, color = secondaryText)
                }
            }
            Spacer(Modifier.height(4.dp))
            if (value.isNotEmpty()) Text(value, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp), color = textColor)
        }
    }
}

// ──────────────────────────── 趋势折线图（全新设计） ────────────────────────────
@Composable
private fun TrendChartCard(uiState: StatisticsUiState) {
    val isDark = isInDarkTheme()
    val lineColor = if (isDark) DeepPurpleDark else DeepPurple
    val barDefault = if (isDark) DarkGrayBar else LightGrayBar
    val barSelected = if (isDark) DarkGrayBarSelected else LightGrayBarSelected
    val dividerColor = if (isDark) DividerColorDark else DividerColorLight
    val isMonthly = uiState.dateMode == DateMode.MONTHLY
    // 联动选中索引
    var chartSelectedIndex by remember { mutableIntStateOf(-1) }

    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {

        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            // 标题区域：竖条标识 + 加粗标题
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(4.dp, 18.dp).background(lineColor, RoundedCornerShape(2.dp)))
                Spacer(Modifier.width(8.dp))
                val title = when (uiState.activeTab) { StatsTab.EXPENSE -> "支出趋势"; StatsTab.INCOME -> "收入趋势"; StatsTab.BALANCE -> "结余趋势" }
                Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, fontSize = 17.sp), color = MaterialTheme.colorScheme.onSurface)
            }

            Spacer(Modifier.height(10.dp))

            // 分割线
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(dividerColor))

            Spacer(Modifier.height(10.dp))

            // 日期行 + 累计金额（联动选中索引）
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                val dateText = if (isMonthly) "${uiState.selectedYear}.${String.format("%02d", uiState.selectedMonth)}"
                               else "${uiState.selectedYear}"
                Text(dateText, style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                    color = if (isDark) Color(0xFFAAAAAE) else TextSecondary)

                // 根据选中索引计算到该位置的累计值
                val totalLabel = when (uiState.activeTab) { StatsTab.EXPENSE -> "累计支出"; StatsTab.INCOME -> "累计收入"; StatsTab.BALANCE -> "结余" }
                val displayVal = if (isMonthly && chartSelectedIndex >= 0) {
                    // 月度模式：累计到选中日期
                    val dailyRecords = uiState.dailyRecords
                    if (chartSelectedIndex < dailyRecords.size) {
                        when (uiState.activeTab) {
                            StatsTab.EXPENSE -> dailyRecords.subList(0, chartSelectedIndex + 1).sumOf { it.expense }
                            StatsTab.INCOME -> dailyRecords.subList(0, chartSelectedIndex + 1).sumOf { it.income }
                            StatsTab.BALANCE -> dailyRecords[chartSelectedIndex].balance
                        }
                    } else {
                        when (uiState.activeTab) { StatsTab.EXPENSE -> uiState.totalExpense; StatsTab.INCOME -> uiState.totalIncome; StatsTab.BALANCE -> uiState.balance }
                    }
                } else if (!isMonthly && chartSelectedIndex >= 0) {
                    // 年度模式：累计到选中月份
                    val monthlyRecords = uiState.monthlyRecords
                    if (chartSelectedIndex < monthlyRecords.size) {
                        when (uiState.activeTab) {
                            StatsTab.EXPENSE -> monthlyRecords.subList(0, chartSelectedIndex + 1).sumOf { it.expense }
                            StatsTab.INCOME -> monthlyRecords.subList(0, chartSelectedIndex + 1).sumOf { it.income }
                            StatsTab.BALANCE -> monthlyRecords[chartSelectedIndex].balance
                        }
                    } else {
                        when (uiState.activeTab) { StatsTab.EXPENSE -> uiState.totalExpense; StatsTab.INCOME -> uiState.totalIncome; StatsTab.BALANCE -> uiState.balance }
                    }
                } else {
                    when (uiState.activeTab) { StatsTab.EXPENSE -> uiState.totalExpense; StatsTab.INCOME -> uiState.totalIncome; StatsTab.BALANCE -> uiState.balance }
                }
                Text(buildAnnotatedString {
                    append("$totalLabel: ")
                    withStyle(style = SpanStyle(color = lineColor, fontWeight = FontWeight.Bold)) { append(CurrencyUtils.format(displayVal, uiState.currencySymbol)) }
                }, style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp), color = if (isDark) Color(0xFFAAAAAE) else TextSecondary)
            }

            Spacer(Modifier.height(14.dp))

            // 图表主体（含交互）
            if (isMonthly) {
                val records = uiState.dailyRecords
                if (records.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Text("暂无数据", style = MaterialTheme.typography.bodyMedium, color = if (isDark) Color(0xFF555558) else TextSecondary)
                    }
                } else {
                    InteractiveTrendChart(
                        records = records.map { TrendPoint(it.day.toString(), it.expense, it.income, it.balance) },
                        activeTab = uiState.activeTab,
                        lineColor = lineColor,
                        isDark = isDark,
                        isMonthly = true,
                        barDefault = barDefault,
                        barSelected = barSelected,
                        xAxisLabels = listOf("01","05","10","15","20","25","30").takeIf { records.lastOrNull()?.day != 30 } ?: listOf("01","05","10","15","20","25","31"),
                        chartHeight = 200.dp,
                        onSelectedIndexChanged = { chartSelectedIndex = it }
                    )
                }
            } else {
                val records = uiState.monthlyRecords
                if (records.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Text("暂无数据", style = MaterialTheme.typography.bodyMedium, color = if (isDark) Color(0xFF555558) else TextSecondary)
                    }
                } else {
                    InteractiveTrendChart(
                        records = records.map { TrendPoint(String.format("%02d", it.month), it.expense, it.income, it.balance) },
                        activeTab = uiState.activeTab,
                        lineColor = lineColor,
                        isDark = isDark,
                        isMonthly = false,
                        barDefault = barDefault,
                        barSelected = barSelected,
                        xAxisLabels = listOf("01","02","03","04","05","06","07","08","09","10","11","12"),
                        chartHeight = 200.dp,
                        onSelectedIndexChanged = { chartSelectedIndex = it }
                    )
                }
            }
        }
    }
}

/** 单个数据点 */
private data class TrendPoint(val label: String, val expense: Double, val income: Double, val balance: Double)

/**
 * 交互式趋势图核心组件：
 * - 背景柱列（每列对应一个日期/月份）
 * - 深紫色折线 + 面积填充
 * - 滑动/点击选中交互 + 动画
 * - X轴标签 + 底部基线圆点
 */
@Composable
private fun InteractiveTrendChart(
    records: List<TrendPoint>,
    activeTab: StatsTab,
    lineColor: Color,
    isDark: Boolean,
    isMonthly: Boolean,
    barDefault: Color,
    barSelected: Color,
    xAxisLabels: List<String>,
    chartHeight: Dp = 200.dp,
    onSelectedIndexChanged: ((Int) -> Unit)? = null
) {
    var selectedIndex by remember { mutableIntStateOf(records.lastIndex.coerceAtLeast(0)) }
    // 选中索引变化时通知外部
    LaunchedEffect(selectedIndex) { onSelectedIndexChanged?.invoke(selectedIndex) }
    val density = LocalDensity.current
    val animatedBarAlpha by animateFloatAsState(
        targetValue = 1f, animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing), label = "barAlpha"
    )

    // 数据值提取
    val values = records.map { p -> when (activeTab) { StatsTab.EXPENSE -> p.expense; StatsTab.INCOME -> p.income; StatsTab.BALANCE -> p.balance } }
    val maxV = (values.maxOrNull() ?: 0.0).coerceAtLeast(1.0)

    // 选中项的值用于联动显示
    val selectedValue = values.getOrNull(selectedIndex) ?: 0.0
    val selectedLabel = records.getOrNull(selectedIndex)?.label ?: ""

    Column {
        // Canvas 图表区
        val chartHt = chartHeight
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHt)
                .pointerInput(records) {
                    detectTapGestures(
                        onTap = { offset ->
                            val colWf = this.size.width.toFloat() / records.size.toFloat()
                            val idx = (offset.x / colWf).toInt().coerceIn(0, records.lastIndex)
                            selectedIndex = idx
                        }
                    )
                }
                .pointerInput(records) {
                    detectDragGestures(
                        onDrag = { change, _ ->
                            change.consume()
                            val colWf = this.size.width.toFloat() / records.size.toFloat()
                            val idx = (change.position.x / colWf).toInt().coerceIn(0, records.lastIndex)
                            selectedIndex = idx
                        }
                    )
                }
        ) {
            val w = size.width
            val h = size.height
            val paddingTop = 8.dp.toPx()
            val paddingBottom = 24.dp.toPx()
            val chartH = h - paddingBottom

            if (records.isEmpty()) return@Canvas

            val colW = w / records.size.toFloat()

            // 绘制每列背景柱
            records.forEachIndexed { i, _ ->
                val barX = i * colW
                val barColor = if (i == selectedIndex) barSelected else barDefault
                val alpha = if (i == selectedIndex) 1f else 0.35f
                drawRoundRect(
                    color = barColor, topLeft = Offset(barX + 1.dp.toPx(), 0f),
                    size = Size(colW - 2.dp.toPx(), chartH), cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()),
                    alpha = alpha * animatedBarAlpha
                )
            }

            // 计算所有点的 Y 坐标
            val points = records.mapIndexed { i, rec ->
                val x = i.toFloat() * colW + colW / 2f
                val v = when (activeTab) { StatsTab.EXPENSE -> rec.expense; StatsTab.INCOME -> rec.income; StatsTab.BALANCE -> rec.balance }
                val normalized = (v / maxV).toFloat().coerceIn(0f, 1f)
                val y = paddingTop + chartH * (1f - normalized)
                Offset(x, y.coerceIn(paddingTop, chartH))
            }

            // 始终绘制完整折线 + 填充区域（不受选中索引限制）
            if (points.size >= 2) {
                val fillPath = Path().apply {
                    moveTo(points[0].x, chartH)
                    for (j in points.indices) lineTo(points[j].x, points[j].y)
                    lineTo(points[points.lastIndex].x, chartH)
                    close()
                }
                drawPath(fillPath, brush = Brush.verticalGradient(
                    colors = listOf(lineColor.copy(alpha = 0.25f), lineColor.copy(alpha = 0.04f)),
                    startY = points.minOf { it.y }, endY = chartH
                ))
                for (j in 0 until points.lastIndex) {
                    drawLine(lineColor, points[j], points[j + 1], strokeWidth = 1.8.dp.toPx(), cap = StrokeCap.Round)
                }
            } else if (points.isNotEmpty()) {
                drawCircle(center = points[0], radius = colW.coerceAtMost(chartH) * 0.15f, brush = Brush.radialGradient(
                    colors = listOf(lineColor.copy(alpha = 0.2f), lineColor.copy(alpha = 0.02f)), center = points[0]
                ))
            }

            // 所有小数据点标记（非选中）
            for (j in points.indices) {
                val v = values.getOrNull(j) ?: 0.0
                if (v > 0.001 || activeTab == StatsTab.BALANCE) {
                    if (j != selectedIndex) {
                        drawCircle(Color.White, radius = 2.2.dp.toPx(), center = points[j])
                        drawCircle(lineColor, radius = 1.5.dp.toPx(), center = points[j])
                    }
                }
            }

            // 选中点的大圆点（深色实心）
            if (selectedIndex < points.size) {
                val sp = points[selectedIndex]
                drawCircle(Color.White, radius = 5.5.dp.toPx(), center = sp)
                drawCircle(lineColor, radius = 4.dp.toPx(), center = sp)
                drawCircle(Color.White, radius = 2.dp.toPx(), center = sp)
            }
        }

        // X轴标签 + 基线圆点
        val displayLabels = if (xAxisLabels.size == records.size) xAxisLabels
                             else generateXAxisLabels(records, isMonthly)

        Row(modifier = Modifier.fillMaxWidth().padding(start = 0.dp, end = 0.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            records.forEachIndexed { i, _ ->
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.size(5.dp).padding(top = 2.dp)) {
                        Canvas(modifier = Modifier.matchParentSize()) {
                            drawCircle(color = if (i == selectedIndex) lineColor else DotColor, center = Offset(size.width / 2f, size.height / 2f), radius = 2.5f, style = Stroke(width = 1.dp.toPx()))
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(displayLabels.getOrNull(i) ?: "",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = if (i == selectedIndex) lineColor else (if (isDark) Color(0xFF66666A) else TextSecondary.copy(alpha = 0.6f)),
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/** 生成X轴标签列表 */
private fun generateXAxisLabels(records: List<TrendPoint>, isMonthly: Boolean): List<String> {
    return records.indices.map { i ->
        if (isMonthly) {
            // 月度模式：显示关键日期 01,05,10,15,20,25,最后一天
            val day = records[i].label.toIntOrNull() ?: (i + 1)
            when (day) {
                1, 5, 10, 15, 20, 25 -> String.format("%02d", day)
                in listOf(records.lastOrNull()?.label?.toIntOrNull()) -> records[i].label
                else -> ""
            }
        } else {
            // 年度模式：每月都显示
            records[i].label
        }
    }
}

// ──────────────────────────── 分类排行（完整重写：标题+Tab+引导线环形图+排行列表+收起） ────────────────────────────

/** 大类/小类 Tab 状态 */
enum class CategoryTab { MAIN, SUB }

@Composable
private fun CategoryRankingSection(
    categories: List<CategoryAmount>,
    totalAmount: Double,
    currencySymbol: String,
    onCollapse: () -> Unit = {}
) {
    val isDark = isInDarkTheme()
    val chartColors = if (isDark) ChartColors.paletteDark else ChartColors.palette
    var selectedTab by remember { mutableStateOf(CategoryTab.MAIN) }
    var donutSelectedIndex by remember { mutableIntStateOf(-1) }

    // 按金额降序排列（用于列表）
    val sortedCategories = remember(categories) { categories.sortedByDescending { it.amount } }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1C1C1E) else Color(0xFFF6F6F8)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 12.dp)) {

            // ─── 标题行：左侧竖条 + 标题 + 右侧 Tab 切换 ───
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 深紫色竖条标识
                    Box(
                        modifier = Modifier.width(4.dp).height(18.dp).background(
                            color = if (isDark) Color(0xFFAF52DE) else Color(0xFF5856D6),
                            shape = RoundedCornerShape(2.dp)
                        )
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("分类排行", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, fontSize = 17.sp),
                        color = MaterialTheme.colorScheme.onSurface)
                }

                // 大类/小类 胶囊 Tab 切换
                Row(
                    modifier = Modifier.clip(RoundedCornerShape(20.dp))
                        .background(if (isDark) Color(0xFF2C2C2E) else Color(0xFFF2F2F7)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TabChip(label = "大类", selected = selectedTab == CategoryTab.MAIN, onClick = { selectedTab = CategoryTab.MAIN }, isDark = isDark)
                    TabChip(label = "小类", selected = selectedTab == CategoryTab.SUB, onClick = { selectedTab = CategoryTab.SUB }, isDark = isDark)
                }
            }

            Spacer(Modifier.height(20.dp))

            // ─── 带引导线标注的环形图 ───
            DonutChartWithLabels(
                data = sortedCategories,
                colors = chartColors,
                totalAmount = totalAmount,
                currencySymbol = currencySymbol,
                selectedIndex = donutSelectedIndex,
                onSelectedChange = { idx -> donutSelectedIndex = idx },
                modifier = Modifier.size(210.dp).align(Alignment.CenterHorizontally),
                isDark = isDark
            )

            Spacer(Modifier.height(22.dp))

            // ─── 排行列表（降序，每项：图标+名称+进度条+金额） ───
            sortedCategories.forEachIndexed { index, item ->
                RankingListItem(item = item, color = chartColors[index % chartColors.size],
                    currencySymbol = currencySymbol, isDark = isDark)
                Spacer(Modifier.height(12.dp))
            }

            // ─── 底部收起按钮 ───
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).clickable { onCollapse() },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("收起", style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp),
                    color = if (isDark) Color(0xFF8E8E93) else Color(0xFFAEAEB2))
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = null,
                    tint = if (isDark) Color(0xFF8E8E93) else Color(0xFFAEAEB2), modifier = Modifier.size(18.dp))
            }
        }
    }
}

/** 胶囊形 Tab Chip */
@Composable
private fun TabChip(label: String, selected: Boolean, onClick: () -> Unit, isDark: Boolean) {
    val accentColor = if (isDark) Color(0xFFAF52DE) else Color(0xFF5856D6)
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal),
        color = if (selected) Color.White else (if (isDark) Color(0xFF98989D) else Color(0xFF8E8E93)),
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) accentColor else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    )
}

/** 空心 Donut 环形图 — 带引导虚线、端点圆点、外侧百分比标注、点击放大凸起 */
@Composable
private fun DonutChartWithLabels(
    data: List<CategoryAmount>,
    colors: List<Color>,
    totalAmount: Double,
    currencySymbol: String,
    selectedIndex: Int,
    onSelectedChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    isDark: Boolean
) {
    val animatedIdx by animateIntAsState(targetValue = selectedIndex, animationSpec = tween(durationMillis = 250), label = "donutSel")
    // 需求3：点击放大5%（1.05）+ 弹性动画凸起效果
    val scaleAnimatable = remember { Animatable(1.0f) }
    LaunchedEffect(selectedIndex) {
        if (selectedIndex >= 0) {
            scaleAnimatable.snapTo(1.0f)
            scaleAnimatable.animateTo(
                targetValue = 1.05f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
            )
        } else {
            scaleAnimatable.animateTo(
                targetValue = 1.0f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
            )
        }
    }
    val effectiveScale = scaleAnimatable.value

    // 计算每个扇区的角度范围和引导线端点位置（预计算 cos/sin 避免重复三角函数）
    data class SliceInfo(val startAngle: Float, val sweepAngle: Float, val midAngleRad: Float, val cosMid: Float, val sinMid: Float)

    val slicesInfo = remember(data) {
        val result = mutableListOf<SliceInfo>()
        var cumulative = -90f
        val gapDeg = 0f
        data.forEach { item ->
            val sweep = item.percentage * 3.6f - gapDeg
            if (sweep > 0.01f) {
                val midDeg = cumulative + gapDeg / 2f + sweep / 2f
                val midRad = java.lang.Math.toRadians(midDeg.toDouble()).toFloat()
                result.add(SliceInfo(cumulative + gapDeg / 2f, sweep, midRad, kotlin.math.cos(midRad), kotlin.math.sin(midRad)))
            }
            cumulative += item.percentage * 3.6f
        }
        result
    }

    // 需求2背景色：使用与Card一致的浅灰/深色
    val surfaceColor = if (isDark) Color(0xFF1C1C1E) else Color(0xFFF6F6F8)

    // 预计算引导线位置（基于已知尺寸，不依赖 DrawScope）
    data class LabelLayout(val idx: Int, val pct: Double, val color: Color,
                           val anchorOffDp: Pair<Float, Float>,    // 锚点相对中心的dp偏移
                           val elbowOffDp: Pair<Float, Float>,     // 肘点相对中心的dp偏移
                           val endOffDp: Pair<Float, Float>,       // 水平段终点相对中心的dp偏移
                           val isLeft: Boolean)

    val chartBoxSize = 210f  // 与 Modifier.size(210.dp) 一致
    val strokeWidthDp = 64f
    val outerEdgeDp = chartBoxSize / 2f  // = 105dp
    val anchorDp = outerEdgeDp + 2f      // = 107dp（锚点在圆弧外边缘）
    val elbowLenDp = 18f                  // 斜线段长度
    val horizLenDp = 35f                  // 水平段长度（加长，延伸到空白区域）

    val labelLayouts = remember(data, colors) {
        // 收集需要标注的分类，按占比降序（最大优先）
        val items = slicesInfo.mapIndexed { idx, info -> Triple(idx, info, data.getOrElse(idx) { null }) }
            .filter { (_, _, item) -> item != null && item!!.percentage > 3.0 }
            .sortedByDescending { (_, _, item) -> item!!.percentage }

        // 计算每条引导线的原始位置
        data class Cand(val idx: Int, val pct: Double, val color: Color,
                        val anchorOff: Pair<Float, Float>,  // 锚点相对中心
                        val midAngle: Float,                 // 弧中点角度（弧度）
                        val isLeft: Boolean,
                        var elbowOff: Pair<Float, Float>,    // 肘点相对中心
                        var endOff: Pair<Float, Float>)      // 终点相对中心

        val cands = items.mapNotNull { (i, info, item) ->
            if (i >= colors.size || item == null) return@mapNotNull null
            val c = colors[i % colors.size]
            val left = info.cosMid < 0
            // 锚点
            val aOff = Pair(anchorDp * info.cosMid, anchorDp * info.sinMid)
            // 原始肘点（沿径向外延）
            val eOff = Pair((anchorDp + elbowLenDp) * info.cosMid, (anchorDp + elbowLenDp) * info.sinMid)
            // 终点
            val hOff = Pair(eOff.first + if (left) -horizLenDp else horizLenDp, eOff.second)
            Cand(i, item.percentage.toDouble(), c, aOff, info.midAngleRad, left, eOff, hOff)
        }

        // 自动避让算法：左右分组，每组内按原始Y排序后均匀分布
        val minGap = 18f  // 标签间最小间距dp

        fun redistributeGroup(group: List<Cand>): List<Cand> {
            if (group.isEmpty()) return group
            val sorted = group.sortedBy { it.elbowOff.second }.toMutableList()
            // 先向下推开
            for (j in 1 until sorted.size) {
                if (sorted[j].elbowOff.second - sorted[j - 1].elbowOff.second < minGap) {
                    sorted[j].elbowOff = sorted[j].elbowOff.copy(
                        second = sorted[j - 1].elbowOff.second + minGap
                    )
                }
            }
            // 再向上收回（避免超出下边界）
            for (j in sorted.size - 2 downTo 0) {
                if (sorted[j + 1].elbowOff.second - sorted[j].elbowOff.second < minGap) {
                    sorted[j].elbowOff = sorted[j].elbowOff.copy(
                        second = sorted[j + 1].elbowOff.second - minGap
                    )
                }
            }
            // 更新endOff（水平段终点Y与肘点一致）
            sorted.forEach { c ->
                c.endOff = c.endOff.copy(second = c.elbowOff.second)
            }
            return sorted
        }

        val leftGroup = redistributeGroup(cands.filter { it.isLeft })
        val rightGroup = redistributeGroup(cands.filter { !it.isLeft })

        (leftGroup + rightGroup).map { c ->
            LabelLayout(c.idx, c.pct, c.color, c.anchorOff, c.elbowOff, c.endOff, c.isLeft)
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {

        // Canvas 绘制环形图 + 引导虚线
        Canvas(modifier = Modifier.matchParentSize()
            .pointerInput(data) {
                detectTapGestures { offset ->
                    val cx = size.width / 2f; val cy = size.height / 2f
                    val dx = offset.x - cx; val dy = offset.y - cy
                    val dist = sqrt(dx * dx + dy * dy)
                    val sw = 52.dp.toPx(); val outerR = (size.width.coerceAtMost(size.height) - sw) / 2f
                    val innerR = outerR - sw
                    if (dist in innerR..outerR + 10.dp.toPx()) {
                        var angle = java.lang.Math.toDegrees(kotlin.math.atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f
                        if (angle < 0) angle += 360f
                        var cum = 0f; var hit = -1
                        data.indices.forEach { i ->
                            val swp = data[i].percentage * 3.6f
                            if (angle >= cum && angle < cum + swp) hit = i
                            cum += swp
                        }
                        onSelectedChange(if (hit == selectedIndex) -1 else hit)
                    }
                }
            }
        ) {
            val strokeWidth = 64.dp.toPx()
            val minDim = size.width.coerceAtMost(size.height)
            val baseRadius = (minDim - strokeWidth) / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            val gap = 0f

            // 底环背景已去除

            // 扇区绘制：分两遍，先绘制未选中扇区，再绘制选中扇区（确保浮于上层）
            var runningStart = -90f
            
            // 第一遍：绘制所有未选中扇区
            data.forEachIndexed { i, item ->
                val sweep = (item.percentage * 3.6f) - gap
                if (sweep > 0.01f && i != animatedIdx) {
                    val curR = baseRadius
                    val curSW = strokeWidth
                    val arcColor = colors[i % colors.size]

                    // 色块弧
                    drawArc(color = arcColor, startAngle = runningStart + gap / 2, sweepAngle = sweep, useCenter = false,
                        topLeft = Offset(center.x - curR, center.y - curR), size = Size(curR * 2f, curR * 2f),
                        style = Stroke(width = curSW, cap = StrokeCap.Butt))

                    // 立体圆润效果：外弧高光 + 内弧暗影
                    val outerR = curR + curSW / 2f
                    val innerEdgeR = (curR - curSW / 2f).coerceAtLeast(0f)
                    val sAngle = runningStart + gap / 2f
                    val eAngle = sAngle + sweep
                    // 外弧高光
                    val hlAlpha = 0.35f
                    val hlWidth = 2.5.dp.toPx()
                    drawArc(color = Color.White.copy(alpha = hlAlpha), startAngle = sAngle, sweepAngle = sweep, useCenter = false,
                        topLeft = Offset(center.x - outerR, center.y - outerR), size = Size(outerR * 2f, outerR * 2f),
                        style = Stroke(width = hlWidth, cap = StrokeCap.Butt))
                    // 内弧暗影
                    val shAlpha = 0.15f
                    val shWidth = 2.dp.toPx()
                    drawArc(color = Color.Black.copy(alpha = shAlpha), startAngle = sAngle, sweepAngle = sweep, useCenter = false,
                        topLeft = Offset(center.x - innerEdgeR, center.y - innerEdgeR), size = Size(innerEdgeR * 2f, innerEdgeR * 2f),
                        style = Stroke(width = shWidth, cap = StrokeCap.Butt))
                }
                runningStart += item.percentage * 3.6f
            }
            
            // 第二遍：绘制选中扇区（确保浮于其他色块之上）
            runningStart = -90f
            data.forEachIndexed { i, item ->
                val sweep = (item.percentage * 3.6f) - gap
                if (sweep > 0.01f && i == animatedIdx) {
                    val sel = true
                    val scale = effectiveScale
                    val curR = baseRadius * scale
                    val curSW = strokeWidth * 1.12f
                    val arcColor = colors[i % colors.size]

                    // 色块弧
                    drawArc(color = arcColor, startAngle = runningStart + gap / 2, sweepAngle = sweep, useCenter = false,
                        topLeft = Offset(center.x - curR, center.y - curR), size = Size(curR * 2f, curR * 2f),
                        style = Stroke(width = curSW, cap = StrokeCap.Butt))

                    // 立体圆润效果：外弧高光 + 内弧暗影
                    val outerR = curR + curSW / 2f
                    val innerEdgeR = (curR - curSW / 2f).coerceAtLeast(0f)
                    val sAngle = runningStart + gap / 2f
                    val eAngle = sAngle + sweep
                    // 外弧高光（更亮）
                    val hlAlpha = 0.55f
                    val hlWidth = 4.dp.toPx()
                    drawArc(color = Color.White.copy(alpha = hlAlpha), startAngle = sAngle, sweepAngle = sweep, useCenter = false,
                        topLeft = Offset(center.x - outerR, center.y - outerR), size = Size(outerR * 2f, outerR * 2f),
                        style = Stroke(width = hlWidth, cap = StrokeCap.Butt))
                    // 内弧暗影（更深）
                    val shAlpha = 0.25f
                    val shWidth = 3.5.dp.toPx()
                    drawArc(color = Color.Black.copy(alpha = shAlpha), startAngle = sAngle, sweepAngle = sweep, useCenter = false,
                        topLeft = Offset(center.x - innerEdgeR, center.y - innerEdgeR), size = Size(innerEdgeR * 2f, innerEdgeR * 2f),
                        style = Stroke(width = shWidth, cap = StrokeCap.Butt))

                    // 选中态白色描边 — 用Path描绘选中色块完整轮廓（外弧+两条径向边+内弧）
                    val outlineOuterR = curR + curSW / 2f
                    val outlineInnerR = (curR - curSW / 2f).coerceAtLeast(0f)
                    val outlineSAngle = runningStart + gap / 2f
                    val outlineEAngle = outlineSAngle + sweep

                    val outline = Path().apply {
                        val sRad = java.lang.Math.toRadians(outlineSAngle.toDouble())
                        val eRad = java.lang.Math.toRadians(outlineEAngle.toDouble())
                        // 从外弧起点开始
                        moveTo(
                            center.x + outlineOuterR * kotlin.math.cos(sRad).toFloat(),
                            center.y + outlineOuterR * kotlin.math.sin(sRad).toFloat()
                        )
                        // 外弧
                        arcTo(
                            rect = androidx.compose.ui.geometry.Rect(
                                center.x - outlineOuterR, center.y - outlineOuterR,
                                center.x + outlineOuterR, center.y + outlineOuterR
                            ),
                            startAngleDegrees = outlineSAngle, sweepAngleDegrees = sweep, forceMoveTo = false
                        )
                        // 径向边到内弧终点
                        lineTo(
                            center.x + outlineInnerR * kotlin.math.cos(eRad).toFloat(),
                            center.y + outlineInnerR * kotlin.math.sin(eRad).toFloat()
                        )
                        // 内弧（反向）
                        arcTo(
                            rect = androidx.compose.ui.geometry.Rect(
                                center.x - outlineInnerR, center.y - outlineInnerR,
                                center.x + outlineInnerR, center.y + outlineInnerR
                            ),
                            startAngleDegrees = outlineEAngle, sweepAngleDegrees = -sweep, forceMoveTo = false
                        )
                        close()
                    }
                    drawPath(outline, color = Color.White, style = Stroke(width = 3.75.dp.toPx(), join = StrokeJoin.Miter))
                }
                runningStart += item.percentage * 3.6f
            }

            // 内圆背景：灰白色覆盖圆环镂空区域
            val ringInnerR = baseRadius - strokeWidth / 2f
            drawCircle(color = surfaceColor, radius = ringInnerR, center = center)

            // ────────────────────────────────────────────────
            // 引导线系统：同色锚点 + 斜线→水平折线
            // ────────────────────────────────────────────────
            val lineW = 1.5.dp.toPx()
            val dotR = 3.5.dp.toPx()
            val dpToPx = size.width / chartBoxSize  // dp→px转换因子

            val validLabels = labelLayouts.filter { it.idx < slicesInfo.size }
            for (ll in validLabels) {
                val info = slicesInfo[ll.idx]
                val c = ll.color
                // 引导线颜色：浅色模式黑色，深色模式白色
                val lc = if (isDark) Color.White else Color.Black

                // 锚点（圆弧外边缘）
                val ax = center.x + ll.anchorOffDp.first * dpToPx
                val ay = center.y + ll.anchorOffDp.second * dpToPx
                // 肘点
                val ex = center.x + ll.elbowOffDp.first * dpToPx
                val ey = center.y + ll.elbowOffDp.second * dpToPx
                // 水平段终点
                val hx = center.x + ll.endOffDp.first * dpToPx
                val hy = center.y + ll.endOffDp.second * dpToPx

                // 锚点实心圆
                drawCircle(color = c, radius = dotR, center = Offset(ax, ay))
                // 第一段：斜向实线
                drawLine(color = lc, start = Offset(ax, ay), end = Offset(ex, ey), strokeWidth = lineW)
                // 第二段：水平实线
                drawLine(color = lc, start = Offset(ex, ey), end = Offset(hx, hy), strokeWidth = lineW)
                // 末端小圆点
                drawCircle(color = c, radius = dotR * 0.7f, center = Offset(hx, hy))
            }
        }

        // ────────────────────────────────────────────────
        // 外侧标注文字（同色系，固定在水平线末端外侧）
        // ────────────────────────────────────────────────
        val labelFontSize = 11.sp
        // 标签文字颜色：浅色模式黑色，深色模式白色
        val labelTextColor = if (isDark) Color.White else Color.Black
        for (ll in labelLayouts) {
            if (ll.idx >= data.size) continue
            val item = data[ll.idx]
            val pctStr = "${item.category.name} ${"%.1f".format(item.percentage)}%"
            // 文字在水平段终点外侧
            val tx = ll.endOffDp.first.dp + if (ll.isLeft) (-4).dp else 4.dp
            val ty = ll.endOffDp.second.dp - 8.dp
            Box(modifier = Modifier.offset(x = tx, y = ty)) {
                Text(
                    text = pctStr,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = labelFontSize, fontWeight = FontWeight.Bold),
                    color = labelTextColor
                )
            }
        }

        // ────────────────────────────────────────────────
        // 中心文字 — 字体大小完全自适应内圆直径
        // ────────────────────────────────────────────────
        // 内圆文字区域（加大到80dp）
        val innerDiamDp = 80f
        val safeWidthDp = 72f
        val nameFontSize = 13.sp
        val amountFontSize = 14.sp

        Box(
            modifier = Modifier
                .size(innerDiamDp.dp)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = ((innerDiamDp - safeWidthDp) / 2f).dp)
            ) {
                if (animatedIdx >= 0 && animatedIdx < data.size) {
                    val s = data[animatedIdx]
                    Text(s.category.name,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = nameFontSize, fontWeight = FontWeight.Black),
                        color = colors[animatedIdx % colors.size], maxLines = 1, softWrap = false)
                    Text("$currencySymbol${"%.2f".format(s.amount)}",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = amountFontSize, fontWeight = FontWeight.Black),
                        color = if (isDark) Color(0xFFFFFFFF) else Color(0xFF1C1C1E), maxLines = 1, softWrap = false)
                } else {
                    val label = if (data.any { it.category.type.name == "EXPENSE" }) "支出" else "收入"
                    Text(label,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = nameFontSize, fontWeight = FontWeight.Black),
                        color = if (isDark) Color(0xFF8E8E93) else Color(0xFFAEAEB2), maxLines = 1, softWrap = false)
                    Text("$currencySymbol${"%.2f".format(totalAmount)}",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = amountFontSize, fontWeight = FontWeight.Black),
                        color = if (isDark) Color(0xFFFFFFFF) else Color(0xFF1C1C1E), maxLines = 1, softWrap = false)
                }
            }
        }
    }
}

/** 单个排行条目：左侧图标 + 名称 + 百分比 + 进度条 + 右侧金额 */
@Composable
private fun RankingListItem(item: CategoryAmount, color: Color, currencySymbol: String, isDark: Boolean) {
    val pctText = "%.2f%%".format(item.percentage)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧圆形浅底 + 分类图标
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape)
                .background(color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = getIconFromName(item.category.icon),
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        // 中间：名称 + 百分比 + 进度条
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(item.category.name, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    color = if (isDark) Color(0xFFFFFFFF) else Color(0xFF1C1C1E), maxLines = 1)
                Spacer(Modifier.width(8.dp))
                Text(pctText, style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = if (isDark) Color(0xFF8E8E93) else Color(0xFFAEAEB2))
            }
            Spacer(Modifier.height(5.dp))
            // 进度条
            Box(modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(3.5.dp))
                .background(if (isDark) Color(0xFF2C2C2E) else Color(0xFFF2F2F7))) {
                Box(modifier = Modifier.fillMaxHeight().fillMaxWidth((item.percentage / 100f).coerceIn(0f, 1f))
                    .background(color.copy(alpha = 0.75f), RoundedCornerShape(3.5.dp)))
            }
        }

        Spacer(Modifier.width(12.dp))

        // 右侧金额
        Text(CurrencyUtils.format(item.amount, currencySymbol),
            style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
            color = if (isDark) Color(0xFFFFFFFF) else Color(0xFF1C1C1E))
    }
}

/** 图标名到 ImageVector 的静态映射表，避免每次重组都执行 when 分支 */
private val iconMap: Map<String, ImageVector> by lazy {
    mapOf(
        "restaurant" to Icons.Default.Restaurant,
        "ic_restaurant" to Icons.Default.Restaurant,
        "directions_bus" to Icons.Default.DirectionsBus,
        "ic_directions_bus" to Icons.Default.DirectionsBus,
        "shopping_bag" to Icons.Default.ShoppingBag,
        "ic_shopping_bag" to Icons.Default.ShoppingBag,
        "local_movies" to Icons.Default.LocalMovies,
        "ic_local_movies" to Icons.Default.LocalMovies,
        "home" to Icons.Default.Home,
        "ic_home" to Icons.Default.Home,
        "medical" to Icons.Default.LocalHospital,
        "ic_medical" to Icons.Default.LocalHospital,
        "education" to Icons.Default.School,
        "ic_education" to Icons.Default.School,
        "communication" to Icons.Default.Phone,
        "ic_communication" to Icons.Default.Phone,
        "insurance" to Icons.Default.Security,
        "ic_insurance" to Icons.Default.Security,
        "travel" to Icons.Default.Flight,
        "ic_travel" to Icons.Default.Flight,
        "lend" to Icons.Default.Send,
        "ic_lend" to Icons.Default.Send,
        "investment_expense" to Icons.Default.TrendingDown,
        "ic_investment_expense" to Icons.Default.TrendingDown,
        "other" to Icons.Default.MoreVert,
        "ic_other" to Icons.Default.MoreVert,
        "salary" to Icons.Default.Payments,
        "ic_salary" to Icons.Default.Payments,
        "bonus" to Icons.Default.CardGiftcard,
        "ic_bonus" to Icons.Default.CardGiftcard,
        "investment" to Icons.Default.TrendingUp,
        "ic_investment" to Icons.Default.TrendingUp,
        "financial" to Icons.Default.AttachMoney,
        "ic_financial" to Icons.Default.AttachMoney,
        "redpacket" to Icons.Default.Mail,
        "ic_redpacket" to Icons.Default.Mail,
        "family_living" to Icons.Default.HomeWork,
        "ic_family_living" to Icons.Default.HomeWork,
        "children" to Icons.Default.Person,
        "ic_children" to Icons.Default.Person,
        "elderly_care" to Icons.Default.Accessibility,
        "ic_elderly_care" to Icons.Default.Accessibility
    )
}

/** 根据分类图标名获取 ImageVector */
private fun getIconFromName(iconName: String): ImageVector {
    return iconMap[iconName] ?: Icons.Default.MoreVert
}

/** 表格颜色缓存数据类，避免重组时重复计算颜色 */
private data class TableColors(
    val headerBg: Color,
    val summaryBg: Color,
    val dividerColor: Color,
    val rowBg: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textWhite: Color
)

// ──────────────────────────── 每日概况表（结余 Tab）────────────────────────────
@Composable
private fun DailySummaryTable(dailyRecords: List<DailyRecord>, currencySymbol: String, totalIncome: Double, totalExpense: Double, balance: Double) {
    val calNow = Calendar.getInstance()
    val currentDayOfMonth = calNow.get(Calendar.DAY_OF_MONTH)
    val isDark = isInDarkTheme()
    val colors = remember(isDark) {
        TableColors(
            headerBg = if (isDark) Color(0xFF3A3A3C) else Color(0xFF5856D6),
            summaryBg = if (isDark) Color(0xFF2C2C2E) else Color(0xFFF2F2F7),
            dividerColor = if (isDark) Color(0xFF38383A) else Color(0xFFE5E5EA),
            rowBg = if (isDark) Color(0xFF1C1C1E) else Color.White,
            textPrimary = if (isDark) Color(0xFFCCCCCC) else Color(0xFF1C1C1E),
            textSecondary = if (isDark) Color(0xFF66666A) else Color(0xFFAEAEB2),
            textWhite = if (isDark) Color.White else Color(0xFF1C1C1E)
        )
    }
    val cellPadding = Modifier.padding(vertical = 10.dp, horizontal = 6.dp)

    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Column(modifier = Modifier.padding(18.dp)) {
            // 标题行：竖条 + 加粗大号标题
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.width(4.dp).height(18.dp).background(
                    color = if (isDark) Color(0xFFAF52DE) else Color(0xFF5856D6),
                    shape = RoundedCornerShape(2.dp)
                ))
                Spacer(Modifier.width(8.dp))
                Text("每日概况", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, fontSize = 17.sp),
                    color = MaterialTheme.colorScheme.onSurface)
            }

            Spacer(Modifier.height(10.dp))
            // 分割线
            Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(colors.dividerColor))
            Spacer(Modifier.height(10.dp))

            // 表头行（与每月概况风格一致：深色背景+白色加粗字体）
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)).background(colors.headerBg),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("日期", "支出", "收入", "结余").forEachIndexed { i, title ->
                    Text(title, modifier = cellPadding.weight(if (i == 0) 0.7f else 1f),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                        color = Color.White, textAlign = TextAlign.Center)
                }
            }

            // 数据行
            dailyRecords.forEachIndexed { index, rec ->
                val hasActivity = rec.income > 0 || rec.expense > 0 || rec.balance != 0.0
                val showRow = hasActivity || rec.day <= currentDayOfMonth
                if (showRow) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth().background(colors.rowBg),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(rec.dateStr, modifier = cellPadding.weight(0.7f),
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                                color = colors.textPrimary,
                                textAlign = TextAlign.Center)
                            Text(if (rec.expense > 0) "- ${String.format("%.2f", rec.expense)}" else "\u2014",
                                modifier = cellPadding.weight(1f),
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                                color = if (rec.expense > 0) Color(0xFFFF3B30) else colors.textSecondary,
                                textAlign = TextAlign.Center)
                            Text(if (rec.income > 0) String.format("%.2f", rec.income) else "\u2014",
                                modifier = cellPadding.weight(1f),
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                                color = if (rec.income > 0) Color(0xFF34C759) else colors.textSecondary,
                                textAlign = TextAlign.Center)
                            Text(if (hasActivity) String.format("%.2f", rec.balance) else "\u2014",
                                modifier = cellPadding.weight(1f),
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, fontWeight = if (hasActivity) FontWeight.Medium else FontWeight.Normal),
                                color = if (hasActivity) colors.textWhite else colors.textSecondary,
                                textAlign = TextAlign.Center)
                        }
                        // 行分割线
                        Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(colors.dividerColor))
                    }
                }
            }

            // 汇总行（与每月概况一致：圆角底+浅色背景）
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)).background(colors.summaryBg),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("汇总", modifier = cellPadding.weight(0.7f),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                    color = colors.textWhite, textAlign = TextAlign.Center)
                Text(if (totalExpense > 0) "- ${String.format("%.2f", totalExpense)}" else "\u2014",
                    modifier = cellPadding.weight(1f),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                    color = Color(0xFFFF3B30), textAlign = TextAlign.Center)
                Text(if (totalIncome > 0) String.format("%.2f", totalIncome) else "\u2014",
                    modifier = cellPadding.weight(1f),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                    color = Color(0xFF34C759), textAlign = TextAlign.Center)
                Text(String.format("%.2f", balance), modifier = cellPadding.weight(1f),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                    color = colors.textWhite, textAlign = TextAlign.Center)
            }
        }
    }
}

// ──────────────────────────── 每月概况表（结余 Tab + 年度模式） ────────────────────────────
@Composable
private fun MonthlySummaryTable(
    monthlyRecords: List<MonthlyRecord>,
    currencySymbol: String,
    totalIncome: Double,
    totalExpense: Double,
    balance: Double,
    selectedYear: Int
) {
    val isDark = isInDarkTheme()
    val calNow = Calendar.getInstance()
    val currentMonth = if (selectedYear == calNow.get(Calendar.YEAR)) calNow.get(Calendar.MONTH) + 1 else 12

    val colors = remember(isDark) {
        TableColors(
            headerBg = if (isDark) Color(0xFF3A3A3C) else Color(0xFF5856D6),
            summaryBg = if (isDark) Color(0xFF2C2C2E) else Color(0xFFF2F2F7),
            dividerColor = if (isDark) Color(0xFF38383A) else Color(0xFFE5E5EA),
            rowBg = if (isDark) Color(0xFF1C1C1E) else Color.White,
            textPrimary = if (isDark) Color(0xFFCCCCCC) else Color(0xFF1C1C1E),
            textSecondary = if (isDark) Color(0xFF66666A) else Color(0xFFAEAEB2),
            textWhite = if (isDark) Color.White else Color(0xFF1C1C1E)
        )
    }
    val cellPadding = Modifier.padding(vertical = 10.dp, horizontal = 6.dp)

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // 标题行：竖条 + 加粗大号标题
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.width(4.dp).height(18.dp).background(
                    color = if (isDark) Color(0xFFAF52DE) else Color(0xFF5856D6),
                    shape = RoundedCornerShape(2.dp)
                ))
                Spacer(Modifier.width(8.dp))
                Text("每月概况", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, fontSize = 17.sp),
                    color = MaterialTheme.colorScheme.onSurface)
            }

            Spacer(Modifier.height(10.dp))
            // 分割线
            Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(colors.dividerColor))
            Spacer(Modifier.height(10.dp))

            // 表头行
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)).background(colors.headerBg),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("月份", "支出", "收入", "结余").forEachIndexed { i, title ->
                    Text(title, modifier = cellPadding.weight(if (i == 0) 0.7f else 1f),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                        color = Color.White, textAlign = TextAlign.Center)
                }
            }

            // 12 个月数据行
            (1..12).forEach { m ->
                val record = monthlyRecords.find { it.month == m }
                val hasData = record != null && (record.expense > 0 || record.income > 0)

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().background(colors.rowBg),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 月份列
                        Text("${m}月", modifier = cellPadding.weight(0.7f),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                            color = colors.textPrimary,
                            textAlign = TextAlign.Center)
                        // 支出列
                        Text(
                            if (hasData && record != null) "- ${String.format("%.2f", record.expense)}" else "\u2014",
                            modifier = cellPadding.weight(1f),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                            color = colors.textPrimary,
                            textAlign = TextAlign.Center
                        )
                        // 收入列
                        Text(
                            if (hasData && record != null) String.format("%.2f", record.income) else "\u2014",
                            modifier = cellPadding.weight(1f),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                            color = colors.textPrimary,
                            textAlign = TextAlign.Center
                        )
                        // 结余列
                        Text(
                            if (hasData && record != null) String.format("%.2f", record.balance) else "\u2014",
                            modifier = cellPadding.weight(1f),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                            color = colors.textPrimary,
                            textAlign = TextAlign.Center
                        )
                    }
                    // 行分割线
                    Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(colors.dividerColor))
                }
            }

            // 汇总行
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)).background(colors.summaryBg),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("汇总", modifier = cellPadding.weight(0.7f),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                    color = colors.textWhite, textAlign = TextAlign.Center)
                Text("- ${String.format("%.2f", totalExpense)}", modifier = cellPadding.weight(1f),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                    color = colors.textWhite, textAlign = TextAlign.Center)
                Text(String.format("%.2f", totalIncome), modifier = cellPadding.weight(1f),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                    color = colors.textWhite, textAlign = TextAlign.Center)
                Text(String.format("%.2f", balance), modifier = cellPadding.weight(1f),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                    color = colors.textWhite, textAlign = TextAlign.Center)
            }
        }
    }
}
