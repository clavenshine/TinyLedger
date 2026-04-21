package com.tinyledger.app.ui.screens.photoalbum

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.ui.text.style.TextAlign
import com.tinyledger.app.domain.model.Transaction
import com.tinyledger.app.domain.model.TransactionType
import com.tinyledger.app.ui.theme.IOSColors
import com.tinyledger.app.ui.viewmodel.DateFilter
import com.tinyledger.app.ui.viewmodel.DateFilterMode
import com.tinyledger.app.ui.viewmodel.PhotoAlbumViewModel
import com.tinyledger.app.util.CurrencyUtils
import com.tinyledger.app.util.DateUtils
import androidx.hilt.navigation.compose.hiltViewModel
import java.io.File
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoAlbumScreen(
    onNavigateBack: () -> Unit,
    onViewTransactionDetail: (Long) -> Unit,
    viewModel: PhotoAlbumViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }
    var showTypeFilter by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "账单相册",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 筛选器行
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 年月选择器
                FilterChip(
                    selected = true,
                    onClick = { showDatePicker = true },
                    label = { Text(uiState.dateFilter.displayText()) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier.height(36.dp)
                )

                // 分类筛选器
                FilterChip(
                    selected = uiState.selectedTypes.isNotEmpty(),
                    onClick = { showTypeFilter = true },
                    label = {
                        Text(
                            if (uiState.selectedTypes.isEmpty()) "全部分类"
                            else uiState.selectedTypes.joinToString(", ") {
                                when (it) {
                                    TransactionType.EXPENSE -> "支出"
                                    TransactionType.INCOME -> "收入"
                                    TransactionType.TRANSFER -> "转账"
                                    TransactionType.LENDING -> "借贷"
                                }
                            }
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.FilterList,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier.height(36.dp)
                )
            }

            // 内容区域
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.groupedByMonth.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无带图片的账单",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    uiState.groupedByMonth.forEach { (monthLabel, transactions) ->
                        // 月份标题
                        item(span = { GridItemSpan(currentLineSpan = 2) }) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .height(16.dp)
                                        .background(IOSColors.SystemIndigo, RoundedCornerShape(2.dp))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = monthLabel,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // 图片卡片
                        items(transactions) { transaction ->
                            PhotoCard(
                                transaction = transaction,
                                onClick = { onViewTransactionDetail(transaction.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    // 年月选择器底部弹窗
    if (showDatePicker) {
        DateFilterBottomSheet(
            currentDateFilter = uiState.dateFilter,
            onFilterSelected = { filter ->
                viewModel.setDateFilter(filter)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }

    // 分类筛选底部弹窗
    if (showTypeFilter) {
        TypeFilterBottomSheet(
            selectedTypes = uiState.selectedTypes,
            onTypesSelected = { types ->
                viewModel.setTypes(types)
                showTypeFilter = false
            },
            onDismiss = { showTypeFilter = false }
        )
    }
}

@Composable
private fun PhotoCard(
    transaction: Transaction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        // 图片
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            // 解析多图片路径，只显示第一张
            val firstImagePath = if (!transaction.imagePath.isNullOrBlank()) {
                transaction.imagePath.split("||").firstOrNull { it.isNotBlank() }
            } else null
            if (firstImagePath != null) {
                AsyncImage(
                    model = File(firstImagePath),
                    contentDescription = "账单图片",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 金额
        val isExpense = transaction.type == TransactionType.EXPENSE
        val amountColor = if (isExpense) IOSColors.SystemRed else IOSColors.SystemGreen
        val prefix = if (isExpense) "- " else "+ "

        Text(
            text = "$prefix${CurrencyUtils.format(kotlin.math.abs(transaction.amount), "¥")}",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = amountColor
        )

        // 日期
        Text(
            text = DateUtils.formatDisplayDate(transaction.date),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

/**
 * 底部弹出的年月选择器
 * 顶部显示年份左右箭头，下方显示12个月份网格
 * 点击月份 → 按月筛选; 点击顶部年份 → 按年筛选
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateFilterBottomSheet(
    currentDateFilter: DateFilter,
    onFilterSelected: (DateFilter) -> Unit,
    onDismiss: () -> Unit
) {
    val currentCalendar = Calendar.getInstance()
    val currentYear = currentCalendar.get(Calendar.YEAR)
    val currentMonth = currentCalendar.get(Calendar.MONTH) + 1

    // 年份选择状态（临时，只在弹窗内使用）
    var pickerYear by remember { mutableIntStateOf(currentDateFilter.year) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp)
        ) {
            // 标题
            Text(
                text = "选择时间",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 年份选择行：左右箭头 + 年份文字（点击年份文字 = 按年筛选）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 上一年
                IconButton(
                    onClick = { pickerYear-- },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowLeft,
                        contentDescription = "上一年",
                        modifier = Modifier.size(28.dp)
                    )
                }

                // 年份文字 - 点击按年筛选
                Text(
                    text = "${pickerYear}年",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (currentDateFilter.mode == DateFilterMode.YEAR && currentDateFilter.year == pickerYear)
                        MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .clickable {
                            onFilterSelected(DateFilter(DateFilterMode.YEAR, pickerYear))
                        }
                        .padding(horizontal = 24.dp, vertical = 4.dp)
                )

                // 下一年
                IconButton(
                    onClick = { pickerYear++ },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowRight,
                        contentDescription = "下一年",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 12个月份网格 (4列 x 3行)
            val months = (1..12).toList()
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                months.chunked(4).forEach { rowMonths ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowMonths.forEach { month ->
                            val isCurrentMonth = (pickerYear == currentYear && month == currentMonth)
                            val isSelectedMonth = (currentDateFilter.mode == DateFilterMode.MONTH
                                    && currentDateFilter.year == pickerYear
                                    && currentDateFilter.month == month)

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        when {
                                            isSelectedMonth -> MaterialTheme.colorScheme.primary
                                            isCurrentMonth -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        }
                                    )
                                    .clickable {
                                        onFilterSelected(DateFilter(DateFilterMode.MONTH, pickerYear, month))
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${month}月",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = if (isSelectedMonth) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = when {
                                        isSelectedMonth -> Color.White
                                        isCurrentMonth -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }
                        }
                        // 补齐最后一行不足4个的占位
                        repeat(4 - rowMonths.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 提示文字
            Text(
                text = "点击月份按月筛选，点击年份按年筛选",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TypeFilterBottomSheet(
    selectedTypes: Set<TransactionType>,
    onTypesSelected: (Set<TransactionType>) -> Unit,
    onDismiss: () -> Unit
) {
    var innerSelectedTypes by remember { mutableStateOf(selectedTypes.toSet()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp)
        ) {
            Text(
                text = "选择分类",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            val types = listOf(
                TransactionType.INCOME to "收入",
                TransactionType.EXPENSE to "支出",
                TransactionType.TRANSFER to "转账",
                TransactionType.LENDING to "借贷"
            )

            types.forEach { (type, label) ->
                val isChecked = type in innerSelectedTypes
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(
                            width = 1.dp,
                            color = if (isChecked) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .then(
                            if (isChecked) Modifier.background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
                            ) else Modifier
                        )
                        .clickable {
                            innerSelectedTypes = if (isChecked)
                                innerSelectedTypes - type
                            else
                                innerSelectedTypes + type
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = if (isChecked) FontWeight.SemiBold else FontWeight.Normal
                        ),
                        color = if (isChecked) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .border(
                                width = 2.dp,
                                color = if (isChecked) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .then(
                                if (isChecked) Modifier.background(
                                    MaterialTheme.colorScheme.primary
                                ) else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isChecked) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { onTypesSelected(innerSelectedTypes) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Text(
                    text = "确定",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
