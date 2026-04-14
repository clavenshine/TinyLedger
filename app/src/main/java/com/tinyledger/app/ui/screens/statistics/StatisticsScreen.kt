package com.tinyledger.app.ui.screens.statistics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tinyledger.app.domain.model.CategoryAmount
import com.tinyledger.app.ui.theme.ChartColors
import com.tinyledger.app.ui.viewmodel.StatisticsViewModel
import com.tinyledger.app.util.CurrencyUtils
import com.tinyledger.app.util.DateUtils
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.sqrt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDark = isSystemInDarkTheme()
    val chartColors = if (isDark) ChartColors.paletteDark else ChartColors.palette
    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // ── 顶部标题 ──
        item {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "统计",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }

        // ── 月份选择 + 收支概览卡片 ──
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Tab Row: Month tab (left) + Yearly tab (right) with large rounded corners
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color.White.copy(alpha = 0.2f))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            // Month Tab
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (!uiState.isYearlyMode) Color.White else Color.Transparent)
                                    .clickable { if (uiState.isYearlyMode) viewModel.exitYearlyMode() }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (!uiState.isYearlyMode) DateUtils.formatDisplayMonth(uiState.selectedYear, uiState.selectedMonth) else "月份",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = if (!uiState.isYearlyMode) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (!uiState.isYearlyMode) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.7f)
                                )
                            }
                            // Yearly Tab
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (uiState.isYearlyMode) Color.White else Color.Transparent)
                                    .clickable { if (!uiState.isYearlyMode) viewModel.enterYearlyMode() }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (uiState.isYearlyMode) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.7f)
                                    )
                                    Text(
                                        text = if (uiState.isYearlyMode) "${uiState.selectedYear}年" else "本年累计",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = if (uiState.isYearlyMode) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (uiState.isYearlyMode) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Month/Year navigation with arrows
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { viewModel.previousMonth() }) {
                                Icon(Icons.Default.ChevronLeft, "上一个月/年", tint = Color.White)
                            }
                            Text(
                                text = if (!uiState.isYearlyMode) DateUtils.formatDisplayMonth(uiState.selectedYear, uiState.selectedMonth) else "${uiState.selectedYear}年",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            IconButton(onClick = { viewModel.nextMonth() }) {
                                Icon(Icons.Default.ChevronRight, "下一个月/年", tint = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Summary row - floating card with shadow
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 2.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFF5F5F5) // Light gray consistent across all themes
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                SummaryColumn(
                                    label = "收入",
                                    value = CurrencyUtils.format(uiState.totalIncome, uiState.currencySymbol),
                                    valueColor = Color(0xFF2E7D32)
                                )
                                Box(modifier = Modifier.width(1.dp).height(40.dp).background(Color(0xFFE0E0E0)))
                                SummaryColumn(
                                    label = "支出",
                                    value = CurrencyUtils.format(uiState.totalExpense, uiState.currencySymbol),
                                    valueColor = Color(0xFFC62828)
                                )
                                Box(modifier = Modifier.width(1.dp).height(40.dp).background(Color(0xFFE0E0E0)))
                                SummaryColumn(
                                    label = "结余",
                                    value = CurrencyUtils.format(uiState.balance, uiState.currencySymbol),
                                    valueColor = if (uiState.balance >= 0) Color(0xFF1B5E20) else Color(0xFFB71C1C)
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── 饼图卡片 ──
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "支出分类",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (uiState.expenseByCategory.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "暂无数据",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "本月还没有支出记录",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                    } else {
                        PieChart(
                            data = uiState.expenseByCategory,
                            colors = chartColors,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                        )
                    }
                }
            }
        }

        // ── 分类详情列表 ──
        if (uiState.expenseByCategory.isNotEmpty()) {
            item {
                Text(
                    text = "分类详情",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 8.dp)
                )
            }

            itemsIndexed(uiState.expenseByCategory) { index, item ->
                // Feature 4: Parallax scrolling effect for category detail items
                ParallaxCategoryExpenseItem(
                    categoryAmount = item,
                    currencySymbol = uiState.currencySymbol,
                    color = chartColors[index % chartColors.size],
                    isLast = index == uiState.expenseByCategory.lastIndex,
                    index = index,
                    listState = listState
                )
            }
        }
    }
}

@Composable
private fun SummaryColumn(
    label: String,
    value: String,
    valueColor: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF757575)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
            color = valueColor
        )
    }
}

@Composable
fun PieChart(
    data: List<CategoryAmount>,
    colors: List<Color> = ChartColors.palette,
    modifier: Modifier = Modifier
) {
    // Feature 5: Track selected/hovered segment index
    var selectedIndex by remember { mutableIntStateOf(-1) }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        // Donut chart with tap interaction
        Box(
            modifier = Modifier.size(160.dp),
            contentAlignment = Alignment.Center
        ) {
            val density = LocalDensity.current

            Canvas(
                modifier = Modifier
                    .size(150.dp)
                    .pointerInput(data) {
                        detectTapGestures { tapOffset ->
                            // Convert tap to angle and determine which segment was tapped
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val dx = tapOffset.x - center.x
                            val dy = tapOffset.y - center.y
                            val distance = sqrt(dx.pow(2) + dy.pow(2))
                            val strokeWidth = with(density) { 32.dp.toPx() }
                            val radius = (size.width.coerceAtMost(size.height) - strokeWidth) / 2

                            // Check if tap is within the donut ring
                            if (distance >= radius - strokeWidth / 2 && distance <= radius + strokeWidth / 2) {
                                // Calculate angle in degrees (0 at top, clockwise)
                                var angle = Math.toDegrees(
                                    atan2(dy.toDouble(), dx.toDouble())
                                ).toFloat() + 90f
                                if (angle < 0) angle += 360f

                                // Find which segment this angle falls into
                                var cumAngle = 0f
                                var foundIndex = -1
                                for (i in data.indices) {
                                    val sweep = data[i].percentage * 3.6f
                                    if (angle >= cumAngle && angle < cumAngle + sweep) {
                                        foundIndex = i
                                        break
                                    }
                                    cumAngle += sweep
                                }
                                selectedIndex = if (foundIndex == selectedIndex) -1 else foundIndex
                            } else {
                                selectedIndex = -1
                            }
                        }
                    }
            ) {
                val strokeWidth = 32.dp.toPx()
                val selectedStrokeWidth = 40.dp.toPx() // Enlarged stroke for selected segment
                val radius = (size.minDimension - strokeWidth) / 2
                val center = Offset(size.width / 2, size.height / 2)
                val gap = 2f

                var startAngle = -90f
                data.forEachIndexed { index, item ->
                    val sweepAngle = (item.percentage * 3.6f) - gap
                    if (sweepAngle > 0) {
                        val isSelected = index == selectedIndex
                        val currentStroke = if (isSelected) selectedStrokeWidth else strokeWidth
                        val currentRadius = if (isSelected) {
                            (size.minDimension - selectedStrokeWidth) / 2
                        } else radius

                        drawArc(
                            color = colors[index % colors.size],
                            startAngle = startAngle + gap / 2,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            topLeft = Offset(center.x - currentRadius, center.y - currentRadius),
                            size = Size(currentRadius * 2, currentRadius * 2),
                            style = Stroke(width = currentStroke, cap = StrokeCap.Round)
                        )
                    }
                    startAngle += item.percentage * 3.6f
                }
            }

            // Center text - shows selected category or total
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (selectedIndex >= 0 && selectedIndex < data.size) {
                    val selected = data[selectedIndex]
                    Text(
                        text = selected.category.name,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = colors[selectedIndex % colors.size]
                    )
                    Text(
                        text = String.format("%.2f", selected.amount),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${String.format("%.1f", selected.percentage)}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "总支出",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val total = data.sumOf { it.amount }
                    Text(
                        text = String.format("%.2f", total),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Legend (top 5) - highlight selected
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            data.take(5).forEachIndexed { index, item ->
                val isSelected = index == selectedIndex
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .then(
                            if (isSelected) Modifier.background(
                                colors[index % colors.size].copy(alpha = 0.1f)
                            ) else Modifier
                        )
                        .clickable {
                            selectedIndex = if (selectedIndex == index) -1 else index
                        }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (isSelected) 12.dp else 10.dp)
                            .clip(CircleShape)
                            .background(colors[index % colors.size])
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item.category.name,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${String.format("%.0f", item.percentage)}%",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (data.size > 5) {
                Text(
                    text = "还有 ${data.size - 5} 个分类",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun CategoryExpenseItem(
    categoryAmount: CategoryAmount,
    currencySymbol: String,
    color: Color = ChartColors.palette[0],
    isLast: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color indicator
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(12.dp))

            // Category name
            Text(
                text = categoryAmount.category.name,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                modifier = Modifier.weight(1f)
            )

            // Percentage pill
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = color.copy(alpha = 0.12f)
            ) {
                Text(
                    text = "${String.format("%.1f", categoryAmount.percentage)}%",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = color,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Amount
            Text(
                text = CurrencyUtils.format(categoryAmount.amount, currencySymbol),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Feature 4: Category expense item with parallax scrolling effect
 * The card content shifts slightly relative to the card itself as you scroll,
 * creating a subtle depth/parallax illusion.
 */
@Composable
fun ParallaxCategoryExpenseItem(
    categoryAmount: CategoryAmount,
    currencySymbol: String,
    color: Color = ChartColors.palette[0],
    isLast: Boolean = false,
    index: Int = 0,
    listState: LazyListState
) {
    // Calculate parallax offset based on scroll position
    val parallaxOffset by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val itemInfo = layoutInfo.visibleItemsInfo.find { it.index == index + 3 } // offset by header items
            if (itemInfo != null) {
                val viewportCenter = layoutInfo.viewportStartOffset + layoutInfo.viewportSize.height / 2
                val itemCenter = itemInfo.offset + itemInfo.size / 2
                val distanceFromCenter = (itemCenter - viewportCenter).toFloat()
                // Subtle parallax: shift content by a fraction of distance from viewport center
                distanceFromCenter * 0.04f
            } else 0f
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    translationY = parallaxOffset
                }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color indicator
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(12.dp))

            // Category name
            Text(
                text = categoryAmount.category.name,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                modifier = Modifier.weight(1f)
            )

            // Percentage pill
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = color.copy(alpha = 0.12f)
            ) {
                Text(
                    text = "${String.format("%.1f", categoryAmount.percentage)}%",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = color,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Amount
            Text(
                text = CurrencyUtils.format(categoryAmount.amount, currencySymbol),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
