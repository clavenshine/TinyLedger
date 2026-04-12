package com.tinyledger.app.ui.screens.bills

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tinyledger.app.domain.model.Transaction
import com.tinyledger.app.domain.model.TransactionType
import com.tinyledger.app.ui.components.TransactionCard
import com.tinyledger.app.ui.theme.IOSColors
import com.tinyledger.app.ui.viewmodel.BillsUiState
import com.tinyledger.app.ui.viewmodel.BillsViewModel
import com.tinyledger.app.ui.viewmodel.BillsViewMode
import com.tinyledger.app.ui.viewmodel.FilterType
import kotlinx.coroutines.launch
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillsScreen(
    onEditTransaction: (Long) -> Unit,
    onNavigateToSearch: () -> Unit = {},
    viewModel: BillsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf<Long?>(null) }
    var showYearMonthPicker by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        // Top bar with title and view mode toggle
        item {
            TopAppBar(
                title = {
                    Text(
                        text = "账单",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateToSearch) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "搜索"
                        )
                    }
                },
                actions = {
                    // Year/Month picker button
                    TextButton(onClick = { showYearMonthPicker = true }) {
                        Text(
                            text = "${uiState.selectedYear}年${uiState.selectedMonth}月",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF5F5F5)
                )
            )
        }

        // View mode toggle (流水/日历)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    shadowElevation = 3.dp
                ) {
                    Row(modifier = Modifier.padding(4.dp)) {
                        val modes = listOf(
                            "流水" to BillsViewMode.LIST,
                            "日历" to BillsViewMode.CALENDAR
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
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                    ),
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
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
                        text = "向左滑动明细记录删除，向右滑动明细记录编辑",
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
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }

    // Year/month picker dialog
    if (showYearMonthPicker) {
        YearMonthPickerDialog(
            currentYear = uiState.selectedYear,
            currentMonth = uiState.selectedMonth,
            onConfirm = { year, month ->
                viewModel.changeMonth(year, month)
                showYearMonthPicker = false
            },
            onDismiss = { showYearMonthPicker = false }
        )
    }

    // Delete confirmation
    showDeleteDialog?.let { transactionId ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("确认删除") },
            text = { Text("确定要删除这条记录吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTransaction(transactionId)
                        showDeleteDialog = null
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("取消")
                }
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
                    text = "${currencySymbol} ${String.format("%.2f", balance)}",
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
                            text = "${currencySymbol} ${String.format("%.2f", expense)}",
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
                            text = "${currencySymbol} ${String.format("%.2f", income)}",
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
            // Left side — Edit button (visible when swiped right)
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

            Spacer(modifier = Modifier.weight(1f))

            // Right side — Delete button (visible when swiped left)
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
                                    // Swiped left enough to show delete
                                    current < -actionWidthPx / 2 -> offsetX.animateTo(-actionWidthPx, tween(200))
                                    // Swiped right enough to show edit
                                    current > actionWidthPx / 2 -> offsetX.animateTo(actionWidthPx, tween(200))
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
                        onEdit()
                    }
                }
            )
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
        colors = CardDefaults.cardColors(containerColor = Color.White),
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
                            val hasExpense = dayTransactions.any { it.type == TransactionType.EXPENSE }
                            val hasIncome = dayTransactions.any { it.type == TransactionType.INCOME }

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
                                val dayExpense = dayTransactions.filter { it.type == TransactionType.EXPENSE }
                                    .sumOf { it.amount }
                                val dayIncome = dayTransactions.filter { it.type == TransactionType.INCOME }
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
