package com.tinyledger.app.ui.screens.home

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tinyledger.app.data.notification.TransactionNotificationService
import com.tinyledger.app.domain.model.TransactionType
import com.tinyledger.app.ui.components.DeleteConfirmationDialog
import com.tinyledger.app.ui.components.TransactionCard
import com.tinyledger.app.ui.theme.IOSColors
import com.tinyledger.app.ui.viewmodel.HomeViewModel
import com.tinyledger.app.util.CurrencyUtils
import com.tinyledger.app.util.DateUtils
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAddTransaction: () -> Unit = {},
    onEditTransaction: (Long) -> Unit = {},
    onViewTransactionDetail: (Long) -> Unit = {},
    onNavigateToAccounts: (Int) -> Unit = {}, // 0: 现金, 1: 信用, 2: 外部往来
    onNavigateToAutoAccounting: () -> Unit = {},
    onNavigateToCreditAccounts: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // 检查自动记账是否开启
    val isAutoAccountingEnabled = remember {
        TransactionNotificationService.hasPermission(context) &&
                TransactionNotificationService.isEnabled(context)
    }

    val (year, month) = remember {
        val cal = Calendar.getInstance()
        Pair(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
    }

    // 删除确认对话框
    var showDeleteDialog by remember { mutableStateOf(false) }
    var transactionToDelete by remember { mutableStateOf<Long?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        // ── 顶部标题栏 ──
        item {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "我的账本",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Icon(
                            Icons.Default.SwapVert,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp).padding(start = 4.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },

                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }

        // ── 本月支出卡片 ──
        item {
            MonthSummaryCard(
                year = year,
                month = month,
                monthlyExpense = uiState.monthlyExpense,
                monthlyIncome = uiState.monthlyIncome,
                dailyAvgExpense = uiState.dailyAvgExpense,
                currencySymbol = uiState.currencySymbol
            )
        }

        // ── 今日账单 ──
        item {
            TodayBillsCard(
                todayIncome = uiState.todayIncome,
                todayExpense = uiState.todayExpense,
                todayTransactions = uiState.todayTransactions,
                currencySymbol = uiState.currencySymbol,
                onViewTransactionDetail = onViewTransactionDetail,
                onEditTransaction = onEditTransaction
            )
        }

        // ── 待确认账单 ──
        item {
            PendingTransactionsCard(
                pendingTransactions = uiState.pendingTransactions,
                currencySymbol = uiState.currencySymbol,
                onConfirm = { transaction ->
                    onEditTransaction(transaction.id)
                },
                onDelete = { transactionId ->
                    viewModel.deletePendingTransaction(transactionId)
                }
            )
        }

        // ── 添加资产账户提示卡片（仅在没有账户时显示） ──
        if (!uiState.hasAccounts) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clickable { onNavigateToAccounts(0) }, // 导航到全部账户
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.AccountBalance,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "添加账户",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "首次安装使用本软件时，需添加账户，添加账户后本卡片不再提示，后续要添加账户，请到'我的'-->'账户管理'模块中添加",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // ── 账户卡片区域 ──
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 第一行：外部往来 + 信用账户
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 外部往来
                    Card(
                        modifier = Modifier.weight(1f).fillMaxHeight().clickable { onNavigateToAccounts(2) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(IOSColors.SystemRed)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "外部往来",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                                )
                            }
                            val creditAccounts = uiState.accountsWithBalance.filter { it.first.attribute == com.tinyledger.app.domain.model.AccountAttribute.CREDIT }
                            val totalCreditBalance = creditAccounts.sumOf { it.second }
                            if (totalCreditBalance != 0.0) {
                                Text(
                                    text = "${uiState.currencySymbol} ${CurrencyUtils.formatAmount(totalCreditBalance)}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (totalCreditBalance < 0) Color(0xFFC62828) else Color(0xFF2E7D32),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            } else {
                                Text(
                                    text = "无外部往来",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = Color(0xFF2E7D32),
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                            }
                        }
                    }

                    // 信用账户
                    Card(
                        modifier = Modifier.weight(1f).fillMaxHeight().clickable { onNavigateToCreditAccounts() },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF6366F1))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "信用账户",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                                )
                            }
                            val creditAccountAccounts = uiState.accountsWithBalance.filter { it.first.attribute == com.tinyledger.app.domain.model.AccountAttribute.CREDIT_ACCOUNT }
                            val creditAccountTotal = creditAccountAccounts.sumOf { it.second }
                            val availableBalance = creditAccountAccounts.sumOf { it.first.creditLimit } + creditAccountTotal
                            if (creditAccountAccounts.isNotEmpty()) {
                                Text(
                                    text = "可用 ${uiState.currencySymbol} ${CurrencyUtils.formatAmount(availableBalance)}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            } else {
                                Text(
                                    text = "无信用账户",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                            }
                        }
                    }
                }

                // 第二行：现金账户 + 自动记账
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 现金账户
                    Card(
                        modifier = Modifier.weight(1f).fillMaxHeight().clickable { onNavigateToAccounts(0) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(IOSColors.SystemGreen)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "现金账户",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                                )
                            }
                            val cashAccounts = uiState.accountsWithBalance.filter { it.first.attribute == com.tinyledger.app.domain.model.AccountAttribute.CASH }
                            val cashTotal = cashAccounts.sumOf { it.second }
                            Text(
                                text = "${uiState.currencySymbol} ${CurrencyUtils.formatAmount(cashTotal)}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    // 自动记账
                    Card(
                        modifier = Modifier.weight(1f).fillMaxHeight().clickable { onNavigateToAutoAccounting() },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (isAutoAccountingEnabled) IOSColors.SystemGreen else IOSColors.SystemOrange)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "自动记账",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                                )
                            }
                            if (isAutoAccountingEnabled) {
                                Text(
                                    text = "已开启",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            } else {
                                Text(
                                    text = "未开启，点击开启>",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = IOSColors.SystemOrange,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }


    }

    // 删除确认对话框
    if (showDeleteDialog && transactionToDelete != null) {
        DeleteConfirmationDialog(
            title = "删除账单记录？",
            onDismiss = { showDeleteDialog = false; transactionToDelete = null },
            onConfirm = {
                // 如果声音设置已开启，播放“弹簧”提示音
                if (com.tinyledger.app.data.notification.TransactionNotificationService.isSoundEnabled(context)) {
                    com.tinyledger.app.data.notification.TransactionNotificationHelper.playSpringSound()
                }
                transactionToDelete?.let { viewModel.deleteTransaction(it) }
                showDeleteDialog = false
                transactionToDelete = null
            }
        )
    }
}

@Composable
private fun MonthSummaryCard(
    year: Int,
    month: Int,
    monthlyExpense: Double,
    monthlyIncome: Double,
    dailyAvgExpense: Double,
    currencySymbol: String
) {
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
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.55f)
                        ),
                        start = Offset.Zero,
                        end = Offset.Infinite
                    )
                )
                .padding(24.dp)
        ) {
            // Date chip at top-right corner
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White.copy(alpha = 0.18f),
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Text(
                    text = "${month}月1日-${month}月${getDaysInMonth(year, month)}日",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                )
            }

            Column {
                Text(
                    text = "本月支出",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "${currencySymbol} ${CurrencyUtils.formatAmount(monthlyExpense)}",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        letterSpacing = (-0.5).sp
                    ),
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 收入与日均支出 - floating card with shadow
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 18.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "本月收入",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF757575)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${currencySymbol} ${CurrencyUtils.formatAmount(monthlyIncome)}",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp),
                                color = Color(0xFF2E7D32)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(44.dp)
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "日均支出",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF757575)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${currencySymbol} ${CurrencyUtils.formatAmount(dailyAvgExpense)}",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp),
                                color = Color(0xFFC62828)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TodayBillsCard(
    todayIncome: Double,
    todayExpense: Double,
    todayTransactions: List<com.tinyledger.app.domain.model.Transaction>,
    currencySymbol: String,
    onViewTransactionDetail: (Long) -> Unit,
    onEditTransaction: (Long) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 今日账单标题
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "今日账单",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "收入 ${String.format("%.2f", todayIncome)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = IOSColors.SystemGreen
                    )
                    Text(
                        text = "支出 ${String.format("%.2f", todayExpense)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (todayTransactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "今天还没有账单哦",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // 今日交易列表
                todayTransactions.forEach { transaction ->
                    TransactionCard(
                        transaction = transaction,
                        currencySymbol = currencySymbol,
                        onClick = { onViewTransactionDetail(transaction.id) },
                        flat = true
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

private fun getDaysInMonth(year: Int, month: Int): Int {
    val cal = Calendar.getInstance()
    cal.set(year, month - 1, 1)
    return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
}

@Composable
private fun PendingTransactionsCard(
    pendingTransactions: List<com.tinyledger.app.domain.model.Transaction>,
    currencySymbol: String,
    onConfirm: (com.tinyledger.app.domain.model.Transaction) -> Unit,
    onDelete: (Long) -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf<Long?>(null) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 标题
            Text(
                text = "待确认账单",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (pendingTransactions.isEmpty()) {
                // 无待确认账单
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂时无待确认交易账单",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = Color(0xFFBDBDBD)
                    )
                }
            } else {
                // 待确认账单列表
                pendingTransactions.forEach { transaction ->
                    val isIncome = transaction.type == com.tinyledger.app.domain.model.TransactionType.INCOME
                    val amountColor = if (isIncome) IOSColors.SystemGreen else IOSColors.SystemRed
                    val amountPrefix = if (isIncome) "+" else "-"

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 左侧：交易信息
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = transaction.category.name,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                            )
                            if (!transaction.note.isNullOrBlank()) {
                                Text(
                                    text = transaction.note.take(40),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                            Text(
                                text = DateUtils.formatDisplayDate(transaction.date),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // 中间：金额
                        Text(
                            text = "$amountPrefix$currencySymbol ${String.format("%.2f", kotlin.math.abs(transaction.amount))}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = amountColor,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        // 右侧：操作按钮
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // 待确认按钮
                            Button(
                                onClick = { onConfirm(transaction) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text(
                                    text = "待确认",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 11.sp
                                )
                            }

                            // 删除按钮
                            Button(
                                onClick = { showDeleteDialog = transaction.id },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text(
                                    text = "删除",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    // 分隔线（最后一项不显示）
                    if (transaction != pendingTransactions.last()) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                        )
                    }
                }
            }
        }
    }

    // 删除确认对话框
    showDeleteDialog?.let { transactionId ->
        DeleteConfirmationDialog(
            title = "删除待确认记录？",
            onDismiss = { showDeleteDialog = null },
            onConfirm = {
                onDelete(transactionId)
                showDeleteDialog = null
            }
        )
    }
}
