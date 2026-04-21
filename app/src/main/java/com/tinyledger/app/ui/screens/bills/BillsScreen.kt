package com.tinyledger.app.ui.screens.bills

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.tinyledger.app.domain.model.Transaction
import com.tinyledger.app.domain.model.TransactionType
import com.tinyledger.app.ui.components.DeleteConfirmationDialog
import com.tinyledger.app.ui.components.TransactionCard
import com.tinyledger.app.ui.theme.IOSColors
import com.tinyledger.app.ui.viewmodel.BillsUiState
import com.tinyledger.app.ui.viewmodel.BillsViewModel
import com.tinyledger.app.ui.viewmodel.BillsViewMode
import com.tinyledger.app.util.CurrencyUtils
import com.tinyledger.app.util.DateUtils
import com.tinyledger.app.util.SoundFeedbackManager
import com.tinyledger.app.ui.viewmodel.FilterType
import com.tinyledger.app.ui.viewmodel.DateFilter
import com.tinyledger.app.ui.viewmodel.DateFilterMode
import com.tinyledger.app.ui.viewmodel.PhotoAlbumViewModel
import coil.compose.AsyncImage
import java.io.File
import kotlinx.coroutines.launch
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillsScreen(
    onEditTransaction: (Long) -> Unit,
    onViewTransactionDetail: (Long) -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    viewModel: BillsViewModel = hiltViewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf<Long?>(null) }

    if (uiState.viewMode == BillsViewMode.ALBUM) {
        AlbumView(
            onViewTransactionDetail = onViewTransactionDetail
        )
    } else {
    var showYearMonthPicker by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        // View mode toggle (流水/日历/相册) + 日期控件
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 视图切换
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 3.dp
                ) {
                    Row(modifier = Modifier.padding(4.dp)) {
                        val modes = listOf(
                            "流水" to BillsViewMode.LIST,
                            "日历" to BillsViewMode.CALENDAR,
                            "相册" to BillsViewMode.ALBUM
                        )
                        modes.forEach { (label, mode) ->
                            val isSelected = uiState.viewMode == mode
                            Surface(
                                modifier = Modifier.clickable { viewModel.setViewMode(mode) },
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                            ) {
                                Text(
                                    text = label,
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                    ),
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.weight(1f))

                // 日期控件（靠右对齐，与左侧切换按钮风格一致的 Surface 容器）
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 3.dp,
                    modifier = Modifier.clickable { showYearMonthPicker = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "${uiState.selectedYear}年${uiState.selectedMonth}月",
                            modifier = Modifier.padding(vertical = 8.dp),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Monthly summary card
        item {
            MonthSummaryCard(
                year = uiState.selectedYear,
                month = uiState.selectedMonth,
                balance = uiState.monthlyBalance,
                expense = uiState.monthlyExpense,
                income = uiState.monthlyIncome,
                currencySymbol = uiState.currencySymbol,
                onPreviousMonth = { viewModel.previousMonth() },
                onNextMonth = { viewModel.nextMonth() }
            )
        }

        if (uiState.viewMode == BillsViewMode.CALENDAR) {
            // Calendar view
            item {
                CalendarView(
                    year = uiState.selectedYear,
                    month = uiState.selectedMonth,
                    selectedDay = uiState.selectedDay,
                    dailyTransactionMap = uiState.dailyTransactionMap,
                    onDayClick = { day -> viewModel.selectDay(day) }
                )
            }

            // Selected day transactions
            if (uiState.selectedDay != null) {
                item {
                    Text(
                        text = "${uiState.selectedMonth}月${uiState.selectedDay}日",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                if (uiState.selectedDayTransactions.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "当天没有账单",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    itemsIndexed(
                        items = uiState.selectedDayTransactions,
                        key = { _, tx -> tx.id }
                    ) { _, transaction ->
                        SwipeableTransactionCard(
                            transaction = transaction,
                            currencySymbol = uiState.currencySymbol,
                            onEdit = { onEditTransaction(transaction.id) },
                            onDelete = { showDeleteDialog = transaction.id },
                            onViewDetail = { onViewTransactionDetail(transaction.id) },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        } else {
            // List view - filter chips
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = uiState.filterType == FilterType.ALL,
                        onClick = { viewModel.setFilterType(FilterType.ALL) },
                        label = { Text("全部") }
                    )
                    FilterChip(
                        selected = uiState.filterType == FilterType.EXPENSE,
                        onClick = { viewModel.setFilterType(FilterType.EXPENSE) },
                        label = { Text("支出") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                            selectedLabelColor = MaterialTheme.colorScheme.error
                        )
                    )
                    FilterChip(
                        selected = uiState.filterType == FilterType.INCOME,
                        onClick = { viewModel.setFilterType(FilterType.INCOME) },
                        label = { Text("收入") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            // Transaction list
            if (uiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (uiState.filteredTransactions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "暂无记录",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // Swipe hint text
                item {
                    Text(
                        text = "向右滑动明细记录删除，向左滑动明细记录编辑",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        textAlign = TextAlign.Center
                    )
                }

                itemsIndexed(
                    items = uiState.filteredTransactions,
                    key = { _, transaction -> transaction.id }
                ) { _, transaction ->
                    SwipeableTransactionCard(
                        transaction = transaction,
                        currencySymbol = uiState.currencySymbol,
                        onEdit = { onEditTransaction(transaction.id) },
                        onDelete = { showDeleteDialog = transaction.id },
                        onViewDetail = { onViewTransactionDetail(transaction.id) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }

    // Year/month picker bottom sheet
    if (showYearMonthPicker) {
        AlbumDateFilterBottomSheet(
            currentDateFilter = DateFilter(
                mode = DateFilterMode.MONTH,
                year = uiState.selectedYear,
                month = uiState.selectedMonth
            ),
            onFilterSelected = { filter ->
                viewModel.changeMonth(filter.year, filter.month)
                showYearMonthPicker = false
            },
            onDismiss = { showYearMonthPicker = false }
        )
    }
    }

    // Delete confirmation
    showDeleteDialog?.let { transactionId ->
        DeleteConfirmationDialog(
            title = "删除账单记录？",
            onDismiss = { showDeleteDialog = null },
            onConfirm = {
                // 播放删除成功音效和震动
                SoundFeedbackManager.onDeleted(context)
                viewModel.deleteTransaction(transactionId)
                showDeleteDialog = null
            }
        )
    }
}

@Composable
private fun MonthSummaryCard(
    year: Int,
    month: Int,
    balance: Double,
    expense: Double,
    income: Double,
    currencySymbol: String,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                // Month navigation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onPreviousMonth,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.ChevronLeft,
                            contentDescription = "上个月",
                            tint = Color.White.copy(alpha = 0.8f)
                        )
                    }
                    Text(
                        text = "${year}年${month}月",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    IconButton(
                        onClick = onNextMonth,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = "下个月",
                            tint = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Balance
                Text(
                    text = "结余",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Text(
                    text = "${currencySymbol} ${CurrencyUtils.formatAmount(balance)}",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Income and Expense
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "支出",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "${currencySymbol} ${CurrencyUtils.formatAmount(expense)}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "收入",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "${currencySymbol} ${CurrencyUtils.formatAmount(income)}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SwipeableTransactionCard(
    transaction: Transaction,
    currencySymbol: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onViewDetail: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val actionWidth = 80.dp
    val actionWidthPx = with(androidx.compose.ui.platform.LocalDensity.current) { actionWidth.toPx() }

    Box(
        modifier = modifier.clip(RoundedCornerShape(12.dp))
    ) {
        // Background action buttons
        Row(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(12.dp))
        ) {
            // Left side — Delete button (visible when swiped right)
            Box(
                modifier = Modifier
                    .width(actionWidth)
                    .fillMaxHeight()
                    .background(Color(0xFFFF3B30))
                    .clickable {
                        coroutineScope.launch {
                            offsetX.animateTo(0f, tween(200))
                        }
                        onDelete()
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Delete, contentDescription = "删除", tint = Color.White)
                    Text("删除", color = Color.White, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Right side — Edit button (visible when swiped left)
            Box(
                modifier = Modifier
                    .width(actionWidth)
                    .fillMaxHeight()
                    .background(Color(0xFFFF9500))
                    .clickable {
                        coroutineScope.launch {
                            offsetX.animateTo(0f, tween(200))
                        }
                        onEdit()
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Edit, contentDescription = "编辑", tint = Color.White)
                    Text("编辑", color = Color.White, fontSize = 12.sp)
                }
            }
        }

        // Foreground card — swipeable
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            coroutineScope.launch {
                                val current = offsetX.value
                                when {
                                    // Swiped right enough to show delete
                                    current > actionWidthPx / 2 -> offsetX.animateTo(actionWidthPx, tween(200))
                                    // Swiped left enough to show edit
                                    current < -actionWidthPx / 2 -> offsetX.animateTo(-actionWidthPx, tween(200))
                                    // Snap back
                                    else -> offsetX.animateTo(0f, tween(200))
                                }
                            }
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            coroutineScope.launch {
                                val newOffset = (offsetX.value + dragAmount)
                                    .coerceIn(-actionWidthPx, actionWidthPx)
                                offsetX.snapTo(newOffset)
                            }
                        }
                    )
                }
        ) {
            TransactionCard(
                transaction = transaction,
                currencySymbol = currencySymbol,
                onClick = {
                    if (offsetX.value != 0f) {
                        coroutineScope.launch { offsetX.animateTo(0f, tween(200)) }
                    } else {
                        // 点击跳转到详情页
                        onViewDetail()
                    }
                }
            )
        }
    }
}

// ==================== Album View ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlbumView(
    onViewTransactionDetail: (Long) -> Unit
) {
    val billsViewModel: BillsViewModel = hiltViewModel()
    val albumViewModel: PhotoAlbumViewModel = hiltViewModel()
    val billsUiState by billsViewModel.uiState.collectAsState()
    val albumUiState by albumViewModel.uiState.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }
    var showTypeFilter by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // View mode toggle (流水/日历/相册) + 日期控件
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 3.dp
            ) {
                Row(modifier = Modifier.padding(4.dp)) {
                    val modes = listOf(
                        "流水" to BillsViewMode.LIST,
                        "日历" to BillsViewMode.CALENDAR,
                        "相册" to BillsViewMode.ALBUM
                    )
                    modes.forEach { (label, mode) ->
                        val isSelected = billsUiState.viewMode == mode
                        Surface(
                            modifier = Modifier.clickable { billsViewModel.setViewMode(mode) },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                        ) {
                            Text(
                                text = label,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                ),
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // 日期控件（靠右对齐，与左侧切换按钮风格一致的 Surface 容器）
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 3.dp,
                modifier = Modifier.clickable { showDatePicker = true }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        albumUiState.dateFilter.displayText(),
                        modifier = Modifier.padding(vertical = 8.dp),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Album filter chips（全部分类）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            FilterChip(
                selected = albumUiState.selectedTypes.isNotEmpty(),
                onClick = { showTypeFilter = true },
                label = {
                    Text(
                        if (albumUiState.selectedTypes.isEmpty()) "全部分类"
                        else albumUiState.selectedTypes.joinToString(", ") {
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

        // Album content
        if (albumUiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (albumUiState.groupedByMonth.isEmpty()) {
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
                albumUiState.groupedByMonth.forEach { (monthLabel, transactions) ->
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

                    items(transactions) { transaction ->
                        AlbumPhotoCard(
                            transaction = transaction,
                            onClick = { onViewTransactionDetail(transaction.id) }
                        )
                    }
                }
            }
        }
    }

    // Date picker bottom sheet
    if (showDatePicker) {
        AlbumDateFilterBottomSheet(
            currentDateFilter = albumUiState.dateFilter,
            onFilterSelected = { filter ->
                albumViewModel.setDateFilter(filter)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }

    // Type filter bottom sheet
    if (showTypeFilter) {
        AlbumTypeFilterBottomSheet(
            selectedTypes = albumUiState.selectedTypes,
            onTypesSelected = { types ->
                albumViewModel.setTypes(types)
                showTypeFilter = false
            },
            onDismiss = { showTypeFilter = false }
        )
    }
}

@Composable
private fun AlbumPhotoCard(
    transaction: Transaction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
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

        val isExpense = transaction.type == TransactionType.EXPENSE
        val amountColor = if (isExpense) IOSColors.SystemRed else IOSColors.SystemGreen
        val prefix = if (isExpense) "- " else "+ "

        Text(
            text = "$prefix${CurrencyUtils.format(kotlin.math.abs(transaction.amount), "\u00a5")}",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = amountColor
        )

        Text(
            text = DateUtils.formatDisplayDate(transaction.date),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlbumDateFilterBottomSheet(
    currentDateFilter: DateFilter,
    onFilterSelected: (DateFilter) -> Unit,
    onDismiss: () -> Unit
) {
    val currentCalendar = Calendar.getInstance()
    val currentYear = currentCalendar.get(Calendar.YEAR)
    val currentMonth = currentCalendar.get(Calendar.MONTH) + 1

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

            // Year navigation row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { pickerYear-- }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "上一年", modifier = Modifier.size(28.dp))
                }

                Text(
                    text = "${pickerYear}年",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (currentDateFilter.mode == com.tinyledger.app.ui.viewmodel.DateFilterMode.YEAR && currentDateFilter.year == pickerYear)
                        MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .clickable {
                            onFilterSelected(DateFilter(com.tinyledger.app.ui.viewmodel.DateFilterMode.YEAR, pickerYear))
                        }
                        .padding(horizontal = 24.dp, vertical = 4.dp)
                )

                IconButton(onClick = { pickerYear++ }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = "下一年", modifier = Modifier.size(28.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Month grid (4x3)
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
                            val isSelectedMonth = (currentDateFilter.mode == com.tinyledger.app.ui.viewmodel.DateFilterMode.MONTH
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
                                        onFilterSelected(DateFilter(com.tinyledger.app.ui.viewmodel.DateFilterMode.MONTH, pickerYear, month))
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
                        repeat(4 - rowMonths.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

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
private fun AlbumTypeFilterBottomSheet(
    selectedTypes: Set<TransactionType>,
    onTypesSelected: (Set<TransactionType>) -> Unit,
    onDismiss: () -> Unit
) {
    var innerSelectedTypes by remember { mutableStateOf(selectedTypes.toSet()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        scrimColor = Color(0x99000000),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp)
        ) {
            // Title
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

            // Options — compact padding to fit screen
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
                    // Checkbox style
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

            // Disabled item: 单个分类
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "单个分类",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(4.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.35f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Confirm button
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

@Composable
private fun CalendarView(
    year: Int,
    month: Int,
    selectedDay: Int?,
    dailyTransactionMap: Map<Int, List<Transaction>>,
    onDayClick: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(elevation = 6.dp, shape = RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Weekday headers
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("日", "一", "二", "三", "四", "五", "六").forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Calendar grid
            val cal = Calendar.getInstance()
            cal.set(year, month - 1, 1)
            val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1 // 0=Sunday
            val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            val today = Calendar.getInstance()
            val isCurrentMonth = today.get(Calendar.YEAR) == year &&
                    (today.get(Calendar.MONTH) + 1) == month
            val todayDay = today.get(Calendar.DAY_OF_MONTH)

            val totalCells = firstDayOfWeek + daysInMonth
            val rows = (totalCells + 6) / 7

            for (row in 0 until rows) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (col in 0..6) {
                        val cellIndex = row * 7 + col
                        val day = cellIndex - firstDayOfWeek + 1

                        if (day in 1..daysInMonth) {
                            val isSelected = selectedDay == day
                            val isToday = isCurrentMonth && day == todayDay
                            val hasTransactions = dailyTransactionMap.containsKey(day)
                            val dayTransactions = dailyTransactionMap[day] ?: emptyList()
                            val hasExpense = dayTransactions.any { it.amount < 0 }
                            val hasIncome = dayTransactions.any { it.amount > 0 }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        when {
                                            isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                            else -> Color.Transparent
                                        }
                                    )
                                    .let {
                                        if (isSelected) {
                                            it.border(
                                                width = 2.dp,
                                                color = MaterialTheme.colorScheme.primary,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                        } else {
                                            it
                                        }
                                    }
                                    .clickable { onDayClick(day) },
                                contentAlignment = Alignment.Center
                            ) {
                                val dayExpense = dayTransactions.filter { it.amount < 0 }
                                    .sumOf { kotlin.math.abs(it.amount) }
                                val dayIncome = dayTransactions.filter { it.amount > 0 }
                                    .sumOf { it.amount }
                                val netAmount = dayIncome - dayExpense

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "$day",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = when {
                                            isSelected -> MaterialTheme.colorScheme.primary
                                            isToday -> MaterialTheme.colorScheme.primary
                                            else -> MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                    if (hasTransactions) {
                                        Text(
                                            text = String.format("%.2f", kotlin.math.abs(netAmount)),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = when {
                                                netAmount > 0 -> IOSColors.SystemGreen
                                                netAmount < 0 -> IOSColors.SystemRed
                                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                            fontSize = MaterialTheme.typography.labelSmall.fontSize * 0.9f
                                        )
                                    }
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun YearMonthPickerDialog(
    currentYear: Int,
    currentMonth: Int,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedYear by remember { mutableStateOf(currentYear) }
    var selectedMonth by remember { mutableStateOf(currentMonth) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = "选择年月",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column {
                // Year selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { selectedYear-- }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = null)
                    }
                    Text(
                        text = "${selectedYear}年",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(onClick = { selectedYear++ }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Month grid (4x3)
                for (row in 0..2) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (col in 0..3) {
                            val m = row * 4 + col + 1
                            val isSelected = selectedMonth == m
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedMonth = m },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else Color.Transparent
                            ) {
                                Text(
                                    text = "${m}月",
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (isSelected) Color.White
                                    else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                    if (row < 2) Spacer(modifier = Modifier.height(4.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedYear, selectedMonth) }) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
