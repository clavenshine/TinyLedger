package com.tinyledger.app.ui.screens.accounts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.hilt.navigation.compose.hiltViewModel
import com.tinyledger.app.domain.model.Account
import com.tinyledger.app.domain.model.AccountAttribute
import com.tinyledger.app.domain.model.Transaction
import com.tinyledger.app.domain.model.TransactionType
import com.tinyledger.app.ui.components.getCategoryIcon
import com.tinyledger.app.ui.theme.IOSColors
import com.tinyledger.app.ui.viewmodel.AccountViewModel
import com.tinyledger.app.util.CurrencyUtils
import com.tinyledger.app.ui.components.NumericKeyboard
import com.tinyledger.app.ui.components.BankInfo
import com.tinyledger.app.ui.components.resolveBankLogo
import java.text.SimpleDateFormat
import java.util.*

// 本地账户图标映射
private fun getAccountIconLocal(iconName: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (iconName) {
        "account_balance" -> Icons.Default.AccountBalance
        "credit_card" -> Icons.Default.CreditCard
        "savings" -> Icons.Default.Savings
        "payments" -> Icons.Default.Payments
        "account_balance_wallet" -> Icons.Default.AccountBalanceWallet
        "person" -> Icons.Default.Person
        "business" -> Icons.Default.Business
        "home" -> Icons.Default.Home
        "car_rental" -> Icons.Default.CarRental
        else -> Icons.Default.AccountBalance
    }
}

/**
 * 根据账户名称判断是否为银行，并返回银行简称首字
 * 用于在图标位置显示银行标识文字
 */
private fun getBankInitial(accountName: String): String? {
    val name = accountName
    return when {
        // 工商银行
        name.contains("工商") -> "工"
        // 建设银行
        name.contains("建设") -> "建"
        // 农业银行
        name.contains("农业") -> "农"
        // 中国银行
        name.contains("中国") && name.contains("银行") -> "中"
        // 交通银行
        name.contains("交通") -> "交"
        // 招商银行
        name.contains("招商") -> "招"
        // 邮储银行
        name.contains("邮储") || name.contains("邮政") -> "邮"
        // 民生银行
        name.contains("民生") -> "民"
        // 兴业银行
        name.contains("兴业") -> "兴"
        // 中信银行
        name.contains("中信") -> "信"
        // 光大银行
        name.contains("光大") -> "光"
        // 平安银行
        name.contains("平安") -> "平"
        // 浦发银行
        name.contains("浦发") -> "浦"
        // 华夏银行
        name.contains("华夏") -> "华"
        // 广发银行
        name.contains("广发") -> "广"
        // 北京银行
        name.contains("北京") && name.contains("银行") -> "京"
        // 上海银行
        name.contains("上海") && name.contains("银行") -> "海"
        // 杭州银行
        name.contains("杭州") && name.contains("银行") -> "杭"
        // 通用银行匹配
        name.contains("银行") -> name.first().toString()
        else -> null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailScreen(
    accountId: Long,
    currencySymbol: String = "¥",
    onNavigateBack: () -> Unit = {},
    onNavigateToAddTransaction: (Long?, TransactionType, Long?) -> Unit = { _, _, _ -> },
    viewModel: AccountViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // 查找当前账户
    val account = uiState.accounts.find { it.id == accountId }
    val calculatedBalance = uiState.accountsWithBalance.find { it.first.id == accountId }?.second ?: 0.0
    
    // 操作菜单状态
    var showActionMenu by remember { mutableStateOf(false) }
    var showDisableConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showAdjustBalanceDialog by remember { mutableStateOf(false) }
    
    // 月份选择器状态
    var showMonthPicker by remember { mutableStateOf(false) }
    
    // 当前显示的月份（默认当前月）
    val calendar = Calendar.getInstance()
    val currentYear = calendar.get(Calendar.YEAR)
    val currentMonth = calendar.get(Calendar.MONTH) + 1
    val currentYearMonth = String.format("%04d-%02d", currentYear, currentMonth)
    var selectedYear by remember { mutableStateOf(currentYear) }
    var selectedMonth by remember { mutableStateOf(currentMonth) }
    var selectedYearMonth by remember { mutableStateOf(currentYearMonth) }
    
    // 获取该账户的交易记录
    LaunchedEffect(accountId) {
        viewModel.loadAccountTransactions(accountId)
    }
    
    // 直接从uiState获取该账户的所有交易记录
    val allAccountTransactions = uiState.accountTransactions[accountId] ?: emptyList()
    
    // 按所选月份过滤
    val transactions = allAccountTransactions.filter { tx ->
        val txCalendar = Calendar.getInstance()
        txCalendar.timeInMillis = tx.date
        val txYearMonth = String.format("%04d-%02d", 
            txCalendar.get(Calendar.YEAR), 
            txCalendar.get(Calendar.MONTH) + 1)
        txYearMonth == selectedYearMonth
    }
    
    // 计算流入流出（排除TRANSFER和LENDING类型）
    val incomeTotal = transactions.filter { 
        it.type != TransactionType.TRANSFER && it.type != TransactionType.LENDING && it.amount > 0
    }.sumOf { it.amount }
    val expenseTotal = transactions.filter { 
        it.type != TransactionType.TRANSFER && it.type != TransactionType.LENDING && it.amount < 0
    }.sumOf { kotlin.math.abs(it.amount) }
    
    // 格式化月份显示
    val monthDisplay = "${selectedYear}年${selectedMonth}月"
    
    // 当月收支明细（显示所有交易，按收入/支出分类）
    val incomeTransactions = transactions.filter { it.amount > 0 }
    val expenseTransactions = transactions.filter { it.amount < 0 }
    val filteredTransactions = incomeTransactions + expenseTransactions
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "账户详情",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showActionMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "更多操作")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            // 底部按钮
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        // 跳转到转账页面，默认选择当前账户为转出账户
                        onNavigateToAddTransaction(null, TransactionType.TRANSFER, accountId)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("转账", fontSize = 16.sp)
                }
                
                Button(
                    onClick = {
                        // 跳转到支出页面，默认选择当前账户
                        onNavigateToAddTransaction(accountId, TransactionType.EXPENSE, null)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text("记一笔", fontSize = 16.sp)
                }
            }
        }
    ) { paddingValues ->
        if (account == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("账户不存在", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 账户信息卡片
                item {
                    AccountInfoCard(
                        account = account,
                        calculatedBalance = calculatedBalance,
                        currencySymbol = currencySymbol,
                        onAdjustBalance = { showAdjustBalanceDialog = true }
                    )
                }
                
                // 月度统计卡片
                item {
                    MonthStatsCard(
                        monthDisplay = monthDisplay,
                        incomeTotal = incomeTotal,
                        expenseTotal = expenseTotal,
                        currencySymbol = currencySymbol,
                        onMonthClick = { showMonthPicker = true }
                    )
                }
                
                if (filteredTransactions.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                modifier = Modifier.size(80.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (allAccountTransactions.isEmpty()) "暂无交易记录" else "该月份暂无收支明细",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    // 当月收入明细标题
                    if (incomeTransactions.isNotEmpty()) {
                        item {
                            Text(
                                text = "本月收入明细",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                            )
                        }
                        
                        // 收入明细列表
                        items(
                            items = incomeTransactions,
                            key = { it.id }
                        ) { transaction ->
                            TransactionItem(
                                transaction = transaction,
                                currencySymbol = currencySymbol
                            )
                        }
                    }
                    
                    // 当月支出明细标题
                    if (expenseTransactions.isNotEmpty()) {
                        item {
                            Text(
                                text = "本月支出明细",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                            )
                        }
                        
                        // 支出明细列表
                        items(
                            items = expenseTransactions,
                            key = { it.id }
                        ) { transaction ->
                            TransactionItem(
                                transaction = transaction,
                                currencySymbol = currencySymbol
                            )
                        }
                    }
                }
            }
        }
    }
    
    // 编辑账户弹窗
    if (uiState.showEditDialog && uiState.selectedAccount != null) {
        com.tinyledger.app.ui.screens.accounts.EditAccountDialog(
            account = uiState.selectedAccount!!,
            onDismiss = { viewModel.hideEditDialog() },
            onConfirm = { name, type, icon, color, cardNumber, creditLimit, billDay, repaymentDay, initialBalance, initialBalanceDate ->
                viewModel.updateAccount(
                    uiState.selectedAccount!!, 
                    name, 
                    type, 
                    icon, 
                    color, 
                    cardNumber, 
                    creditLimit, 
                    billDay, 
                    repaymentDay,
                    initialBalance,
                    initialBalanceDate
                )
            },
            onDelete = { viewModel.deleteAccount(uiState.selectedAccount!!.id) },
            hasUnsettledDebt = uiState.selectedAccount!!.attribute == AccountAttribute.CREDIT &&
                uiState.selectedAccount!!.currentBalance != 0.0
        )
    }
    
    // 操作菜单 - 从底部弹出
    if (showActionMenu && account != null) {
        ActionBottomSheet(
            account = account,
            onDismiss = { showActionMenu = false },
            onEdit = {
                showActionMenu = false
                viewModel.showEditDialog(account)
            },
            onDisable = {
                showActionMenu = false
                showDisableConfirm = true
            },
            onDelete = {
                showActionMenu = false
                showDeleteConfirm = true
            }
        )
    }
    
    // 停用/启用确认弹窗
    if (showDisableConfirm && account != null) {
        val isCurrentlyDisabled = account.isDisabled
        StyledConfirmDialog(
            onDismissRequest = { showDisableConfirm = false },
            title = if (isCurrentlyDisabled) "启用账户" else "停用账户",
            message = if (isCurrentlyDisabled) "你确认启用本账户吗？" else "你确认停用本账户吗？账户一旦被停用，将无法被选择",
            confirmText = "确认",
            dismissText = "取消",
            onConfirm = {
                viewModel.toggleAccountDisabled(account.id, !account.isDisabled)
                showDisableConfirm = false
                onNavigateBack()
            },
            confirmColor = if (isCurrentlyDisabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
        )
    }
    
    // 删除确认弹窗
    if (showDeleteConfirm && account != null) {
        StyledConfirmDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = "删除账户",
            message = "你确认删除本账户吗？账户一旦被删除，数据将无法恢复",
            confirmText = "确认",
            dismissText = "取消",
            onConfirm = {
                viewModel.deleteAccount(account.id)
                showDeleteConfirm = false
                onNavigateBack()
            },
            confirmColor = MaterialTheme.colorScheme.error,
            countdownSeconds = 5  // 5秒倒计时
        )
    }
    
    // 月份选择器
    if (showMonthPicker) {
        com.tinyledger.app.ui.components.MonthYearPicker(
            currentYear = selectedYear,
            currentMonth = selectedMonth,
            onYearSelected = { year ->
                selectedYear = year
                selectedYearMonth = String.format("%04d-%02d", year, selectedMonth)
            },
            onMonthSelected = { year, month ->
                selectedYear = year
                selectedMonth = month
                selectedYearMonth = String.format("%04d-%02d", year, month)
            },
            onDismiss = { showMonthPicker = false }
        )
    }
    
    // 调整余额弹窗
    if (showAdjustBalanceDialog && account != null) {
        AdjustBalanceDialog(
            currentBalance = calculatedBalance,
            currencySymbol = currencySymbol,
            onDismiss = { showAdjustBalanceDialog = false },
            onConfirm = { newBalance ->
                viewModel.updateAccountBalance(accountId, newBalance)
                showAdjustBalanceDialog = false
            }
        )
    }
}

@Composable
private fun AccountInfoCard(
    account: Account,
    calculatedBalance: Double,
    currencySymbol: String,
    onAdjustBalance: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // 账户名称和类型
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = account.name,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = account.type.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    
                    if (!account.cardNumber.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "尾号${account.cardNumber.takeLast(4)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // 账户图标 - 银行Logo / 默认图标
                val bankInfo = resolveBankLogo(account.name)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (bankInfo != null) bankInfo.brandColor
                            else Color(android.graphics.Color.parseColor(account.color))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (bankInfo != null && bankInfo.shortName.isNotBlank()) {
                        // 显示银行简称（品牌色背景 + 白色文字）
                        Text(
                            text = bankInfo.shortName,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        // 非银行账户，使用默认 Material Icon
                        Icon(
                            imageVector = getAccountIconLocal(account.type.icon),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // 余额
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "余额",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$currencySymbol ${CurrencyUtils.formatAmount(calculatedBalance)}",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (calculatedBalance >= 0) IOSColors.SystemGreen else IOSColors.SystemRed
                    )
                }
                
                Button(
                    onClick = onAdjustBalance,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary
                    )
                ) {
                    Text("调整余额", fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun MonthStatsCard(
    monthDisplay: String,
    incomeTotal: Double,
    expenseTotal: Double,
    currencySymbol: String,
    onMonthClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // 月份标题
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable(onClick = onMonthClick)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = monthDisplay,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "流入",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$currencySymbol ${CurrencyUtils.formatAmount(incomeTotal)}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = IOSColors.SystemGreen
                            )
                        )
                    }
                    
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "流出",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$currencySymbol ${CurrencyUtils.formatAmount(expenseTotal)}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = IOSColors.SystemRed
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyTransactionsView() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.FolderOpen,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "当前时间还未有明细，快去记一笔吧～",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun TransactionItem(
    transaction: Transaction,
    currencySymbol: String
) {
    val dateFormat = remember { SimpleDateFormat("MM-dd", Locale.getDefault()) }
    val isExpense = when (transaction.type) {
        TransactionType.EXPENSE -> true
        TransactionType.INCOME -> false
        TransactionType.TRANSFER, TransactionType.LENDING -> transaction.amount < 0
    }
    val amountColor = if (isExpense) IOSColors.SystemRed else IOSColors.SystemGreen
    val amountPrefix = if (isExpense) "-" else "+"
    val displayAmount = kotlin.math.abs(transaction.amount)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 分类图标
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(amountColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getCategoryIcon(transaction.category.icon),
                    contentDescription = null,
                    tint = amountColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // 分类名称和备注
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.category.name,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium
                    )
                )
                if (!transaction.note.isNullOrBlank()) {
                    Text(
                        text = transaction.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // 金额
            Text(
                text = "$amountPrefix$currencySymbol ${CurrencyUtils.formatAmount(displayAmount)}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = amountColor
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActionBottomSheet(
    account: Account,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDisable: () -> Unit,
    onDelete: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .navigationBarsPadding()
        ) {
            // 拖拽指示器
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 标题
            Text(
                text = "账户操作",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // 编辑按钮
            ActionMenuItemModern(
                icon = Icons.Default.Edit,
                text = "编辑账户",
                description = "修改账户名称、类型等信息",
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                iconTint = MaterialTheme.colorScheme.primary,
                textColor = MaterialTheme.colorScheme.onSurface,
                onClick = onEdit
            )
            
            Spacer(modifier = Modifier.height(10.dp))
            
            // 停用/启用按钮
            ActionMenuItemModern(
                icon = if (account.isDisabled) Icons.Default.CheckCircle else Icons.Default.Block,
                text = if (account.isDisabled) "启用账户" else "停用账户",
                description = if (account.isDisabled) "启用后可正常使用" else "停用后将无法被选择",
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                iconTint = MaterialTheme.colorScheme.tertiary,
                textColor = MaterialTheme.colorScheme.onSurface,
                onClick = onDisable
            )
            
            Spacer(modifier = Modifier.height(10.dp))
            
            // 删除按钮
            ActionMenuItemModern(
                icon = Icons.Default.Delete,
                text = "删除账户",
                description = "删除后数据将无法恢复",
                containerColor = MaterialTheme.colorScheme.errorContainer,
                iconTint = MaterialTheme.colorScheme.error,
                textColor = MaterialTheme.colorScheme.error,
                onClick = onDelete
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 取消按钮
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Text(
                    text = "取消",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ActionMenuItemModern(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    description: String,
    containerColor: Color,
    iconTint: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = text,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            // 文字
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = textColor
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            // 箭头
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * 美化的确认对话框 - 适配深色/浅色模式，带弹性动画
 */
@Composable
private fun StyledConfirmDialog(
    onDismissRequest: () -> Unit,
    title: String,
    message: String,
    confirmText: String,
    dismissText: String,
    onConfirm: () -> Unit,
    confirmColor: Color,
    countdownSeconds: Int = 0  // 倒计时秒数，0表示不启用
) {
    // 弹性抖动动画
    val scale = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
    }
    
    // 倒计时逻辑
    var remainingSeconds by remember { mutableStateOf(countdownSeconds) }
    val isCountdownActive = countdownSeconds > 0
    
    LaunchedEffect(isCountdownActive) {
        if (isCountdownActive && remainingSeconds > 0) {
            while (remainingSeconds > 0) {
                kotlinx.coroutines.delay(1000)
                remainingSeconds--
            }
        }
    }
    
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            // 对话框容器 - 带描边和圆角
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .scale(scale.value)
                    .graphicsLayer {
                        shadowElevation = 16f
                    },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 16.dp
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // 图标
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        confirmColor.copy(alpha = 0.2f),
                                        confirmColor.copy(alpha = 0.05f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (title.contains("删除")) Icons.Default.DeleteForever
                            else if (title.contains("停用")) Icons.Default.Block
                            else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = confirmColor,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                    
                    // 标题
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    // 消息
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )
                    
                    // 按钮行 - 居中排列
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 取消按钮 - 精致立体感
                        OutlinedButton(
                            onClick = onDismissRequest,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                width = 2.dp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            )
                        ) {
                            Text(
                                text = dismissText,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        
                        // 确认按钮 - 精致立体感
                        Button(
                            onClick = onConfirm,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isCountdownActive && remainingSeconds > 0) 
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                else confirmColor
                            ),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 8.dp,
                                pressedElevation = 4.dp
                            ),
                            enabled = !isCountdownActive || remainingSeconds <= 0
                        ) {
                            Text(
                                text = if (isCountdownActive && remainingSeconds > 0) 
                                    "$confirmText ($remainingSeconds)"
                                else confirmText,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 调整余额弹窗 - 带数字键盘
 * 参照截图样式：输入框 + 取消/确认按钮 + 数字键盘
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdjustBalanceDialog(
    currentBalance: Double,
    currencySymbol: String = "\u00A5",
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var inputText by remember { mutableStateOf(currentBalance.toString()) }
    
    // 处理数字键盘输入
    fun handleKeyPress(key: String) {
        when {
            key == "." -> {
                if (!inputText.contains(".")) {
                    if (inputText.isEmpty() || inputText == "-") {
                        inputText = "0."
                    } else {
                        inputText += "."
                    }
                }
            }
            key == "+" -> {
                // 加号暂时忽略或可做累加
            }
            key == "-" -> {
                if (inputText.startsWith("-")) {
                    inputText = inputText.removePrefix("-")
                } else {
                    inputText = "-$inputText"
                }
            }
            key == "=" -> {
                // 等号等同于完成
            }
            key.length == 1 && key[0].isDigit() -> {
                if (inputText == "0" || inputText == "0.0") {
                    inputText = key
                } else {
                    // 限制小数点后两位
                    if (inputText.contains(".")) {
                        val parts = inputText.split(".")
                        if (parts.size == 2 && parts[1].length < 2) {
                            inputText += key
                        }
                    } else {
                        inputText += key
                    }
                }
            }
        }
    }
    
    fun handleBackspace() {
        if (inputText.length > 1) {
            inputText = inputText.dropLast(1)
        } else {
            inputText = "0"
        }
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        windowInsets = WindowInsets(0, 0, 0, 0)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // 标题
            Text(
                text = "将余额从 ${CurrencyUtils.formatAmount(currentBalance)} 调整为",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // 输入框显示
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Text(
                    text = "$currencySymbol ${inputText}",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 取消/确认按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 取消按钮
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "取消",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                // 确认按钮
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.onSurface)
                        .clickable {
                            val newBalance = inputText.toDoubleOrNull() ?: 0.0
                            onConfirm(newBalance)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "确认",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.surface
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 数字键盘
            NumericKeyboard(
                onKeyPress = { key -> handleKeyPress(key) },
                onBackspace = { handleBackspace() },
                onDone = {
                    val newBalance = inputText.toDoubleOrNull() ?: 0.0
                    onConfirm(newBalance)
                }
            )
        }
    }
}
