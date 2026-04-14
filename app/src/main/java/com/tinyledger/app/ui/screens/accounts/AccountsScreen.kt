package com.tinyledger.app.ui.screens.accounts

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import com.tinyledger.app.domain.model.Account
import com.tinyledger.app.domain.model.AccountAttribute
import com.tinyledger.app.domain.model.AccountType
import com.tinyledger.app.domain.model.getAccountTypesByAttribute
import com.tinyledger.app.domain.model.Transaction
import com.tinyledger.app.domain.model.TransactionType
import com.tinyledger.app.domain.model.accountColors
import com.tinyledger.app.ui.animation.cardEnterAnimation
import com.tinyledger.app.ui.theme.*
import com.tinyledger.app.ui.viewmodel.AccountUiState
import com.tinyledger.app.ui.viewmodel.AccountViewModel
import com.tinyledger.app.ui.viewmodel.MonthlyTransactions
import com.tinyledger.app.util.CurrencyUtils
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AccountsScreen(
    currencySymbol: String = "¥",
    onNavigateBack: () -> Unit = {},
    viewModel: AccountViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Tab state
    var selectedTab by remember { mutableStateOf(0) } // 0: 全部, 1: 现金, 2: 信用
    val tabs = listOf("全部账户", "现金账户", "信用账户")
    
    // 删除确认对话框状态
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var accountToDelete by remember { mutableStateOf<Account?>(null) }
    
    // 卡片进入动画计数器
    var animationKey by remember { mutableStateOf(0) }
    
    // 监听账户列表变化，重启动画
    LaunchedEffect(uiState.accountsWithBalance.size) {
        if (uiState.accountsWithBalance.isNotEmpty()) {
            animationKey++
        }
    }
    
    // 根据tab筛选账户
    val filteredAccountsWithBalance = when (selectedTab) {
        0 -> uiState.accountsWithBalance // 全部
        1 -> uiState.accountsWithBalance.filter { it.first.attribute == AccountAttribute.CASH } // 现金
        2 -> uiState.accountsWithBalance.filter { it.first.attribute == AccountAttribute.CREDIT } // 信用
        else -> uiState.accountsWithBalance
    }
    
    // 计算筛选后的总额
    val filteredTotalBalance = filteredAccountsWithBalance.sumOf { it.second }

    // 删除确认对话框
    if (showDeleteConfirmDialog && accountToDelete != null) {
        DeleteConfirmDialog(
            accountName = accountToDelete!!.name,
            onDismiss = {
                showDeleteConfirmDialog = false
                accountToDelete = null
            },
            onConfirm = {
                viewModel.deleteAccount(accountToDelete!!.id)
                showDeleteConfirmDialog = false
                accountToDelete = null
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "我的账户",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showAddDialog() },
                containerColor = MaterialTheme.colorScheme.primary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加账户")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 总资产卡片
            val totalCalculatedBalance = uiState.accountsWithBalance.sumOf { it.second }
            TotalBalanceCard(
                totalBalance = totalCalculatedBalance,
                currencySymbol = currencySymbol
            )
            
            // Tab 筛选器
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary,
                edgePadding = 16.dp,
                divider = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { 
                            Text(
                                title,
                                fontSize = 14.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            ) 
                        }
                    )
                }
            }
            
            // 显示当前筛选类型的总额
            if (selectedTab != 0) {
                Text(
                    text = "${tabs[selectedTab].replace("账户", "")}: ${currencySymbol} ${CurrencyUtils.formatAmount(filteredTotalBalance)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // 账户列表
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(
                    items = filteredAccountsWithBalance,
                    key = { _, item -> item.first.id }
                ) { index, (account, calculatedBalance) ->
                    Box(
                        modifier = Modifier
                            .animateItemPlacement()
                            .cardEnterAnimation(
                                index = index,
                                animationKey = animationKey,
                                animationDelay = 60,
                                animationDuration = 250,
                                initialOffsetX = -30f
                            )
                    ) {
                        SwipeableAccountCard(
                            account = account,
                            calculatedBalance = calculatedBalance,
                            currencySymbol = currencySymbol,
                            isExpanded = uiState.expandedAccountId == account.id,
                            monthlyTransactions = if (uiState.expandedAccountId == account.id) uiState.monthlyTransactions else emptyList(),
                            expandedMonths = uiState.expandedMonths,
                            onCardClick = { viewModel.toggleAccountExpanded(account.id) },
                            onMonthClick = { yearMonth -> viewModel.toggleMonthExpanded(account.id, yearMonth) },
                            onEdit = { viewModel.showEditDialog(account) },
                            onDelete = {
                                accountToDelete = account
                                showDeleteConfirmDialog = true
                            }
                        )
                    }
                }

                if (filteredAccountsWithBalance.isEmpty()) {
                    item {
                        if (uiState.accountsWithBalance.isEmpty()) {
                            EmptyAccountsView()
                        } else {
                            // 有账户但当前tab没有
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "该类型下暂无账户",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // 添加账户对话框
    if (uiState.showAddDialog) {
        AddAccountDialog(
            onDismiss = { viewModel.hideAddDialog() },
            onConfirm = { name, type, icon, balance, color, cardNumber, creditLimit, billDay, repaymentDay ->
                viewModel.addAccount(name, type, icon, balance, color, cardNumber, creditLimit, billDay, repaymentDay)
            }
        )
    }

    // 编辑账户对话框
    if (uiState.showEditDialog && uiState.selectedAccount != null) {
        EditAccountDialog(
            account = uiState.selectedAccount!!,
            onDismiss = { viewModel.hideEditDialog() },
            onConfirm = { name, type, icon, color, cardNumber, creditLimit, billDay, repaymentDay ->
                viewModel.updateAccount(uiState.selectedAccount!!, name, type, icon, color, cardNumber, creditLimit, billDay, repaymentDay)
            },
            onDelete = { viewModel.deleteAccount(uiState.selectedAccount!!.id) },
            hasUnsettledDebt = uiState.selectedAccount!!.attribute == AccountAttribute.CREDIT && 
                uiState.selectedAccount!!.currentBalance != 0.0
        )
    }
}

@Composable
private fun SwipeableAccountCard(
    account: Account,
    calculatedBalance: Double,
    currencySymbol: String,
    isExpanded: Boolean,
    monthlyTransactions: List<MonthlyTransactions>,
    expandedMonths: Set<String>,
    onCardClick: () -> Unit,
    onMonthClick: (String) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var swipeDirection by remember { mutableStateOf(0) } // 0=无滑动, -1=左滑(显示删除), 1=右滑(显示编辑)
    var cardWidth by remember { mutableIntStateOf(0) }
    
    // 计算阈值：屏幕宽度的10%
    val threshold10 = (cardWidth * 0.1f).roundToInt()
    // 滑动目标偏移：屏幕宽度的15%
    val targetOffset = (cardWidth * 0.15f).roundToInt().coerceAtLeast(1)

    // 动画过渡到目标位置
    val animatedOffset by animateFloatAsState(
        targetValue = when (swipeDirection) {
            -1 -> (-targetOffset).toFloat() // 左滑显示右侧删除按钮
            1 -> targetOffset.toFloat()      // 右滑显示左侧编辑按钮
            else -> offsetX
        },
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "swipe_offset"
    )

    // 当明细展开时，重置滑动状态
    LaunchedEffect(isExpanded) {
        if (isExpanded) {
            swipeDirection = 0
            offsetX = 0f
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { cardWidth = it.width }
    ) {
        // 左侧滑动背景（编辑按钮，向右滑时显示）
        if (swipeDirection == 1) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFFF9500)), // 橙色
                contentAlignment = Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .width((cardWidth * 0.15f).dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(topStart = 0.dp, bottomStart = 0.dp, topEnd = 12.dp, bottomEnd = 12.dp))
                        .clickable {
                            onEdit()
                            swipeDirection = 0
                            offsetX = 0f
                        },
                    contentAlignment = Alignment.CenterStart
                ) {
                    Column(
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(start = 12.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "编辑",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "编辑",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }

        // 右侧滑动背景（删除按钮，向左滑时显示）
        if (swipeDirection == -1) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(IOSColors.SystemRed), // 红色
                contentAlignment = Alignment.CenterEnd
            ) {
                Box(
                    modifier = Modifier
                        .width((cardWidth * 0.15f).dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp, topEnd = 0.dp, bottomEnd = 0.dp))
                        .clickable {
                            onDelete()
                            swipeDirection = 0
                            offsetX = 0f
                        },
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "删除",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "删除",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }

        // 主卡片（可滑动）
        Box(
            modifier = Modifier
                .fillMaxWidth() // 确保覆盖背景
                .offset { IntOffset(animatedOffset.roundToInt(), 0) }
                .pointerInput(isExpanded) {
                    // 如果明细已展开，禁用滑动
                    if (isExpanded) return@pointerInput
                    
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (swipeDirection == 0) {
                                // 根据滑动方向决定显示哪个操作按钮
                                if (offsetX < -threshold10) {
                                    // 向左滑，显示删除
                                    swipeDirection = -1
                                    offsetX = (-targetOffset).toFloat()
                                } else if (offsetX > threshold10) {
                                    // 向右滑，显示编辑
                                    swipeDirection = 1
                                    offsetX = targetOffset.toFloat()
                                } else {
                                    // 未超过阈值，重置
                                    offsetX = 0f
                                }
                            } else {
                                // 已有操作按钮显示，根据当前位置判断是否关闭
                                if (swipeDirection == -1 && offsetX >= -threshold10) {
                                    swipeDirection = 0
                                    offsetX = 0f
                                } else if (swipeDirection == 1 && offsetX <= threshold10) {
                                    swipeDirection = 0
                                    offsetX = 0f
                                }
                            }
                        },
                        onDragCancel = {
                            if (swipeDirection == 0) {
                                offsetX = 0f
                            }
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            if (swipeDirection != 0) {
                                // 已显示操作按钮时，可以拖动关闭
                                val newOffset = offsetX + dragAmount
                                if (swipeDirection == -1) {
                                    // 左滑显示删除，只能向右拖关闭
                                    if (newOffset >= -targetOffset) {
                                        offsetX = newOffset.coerceAtMost(0f)
                                        if (newOffset >= -threshold10) {
                                            swipeDirection = 0
                                            offsetX = 0f
                                        }
                                    }
                                } else {
                                    // 右滑显示编辑，只能向左拖关闭
                                    if (newOffset <= targetOffset) {
                                        offsetX = newOffset.coerceAtLeast(0f)
                                        if (newOffset <= threshold10) {
                                            swipeDirection = 0
                                            offsetX = 0f
                                        }
                                    }
                                }
                            } else {
                                // 正常滑动
                                val newOffset = offsetX + dragAmount
                                // 允许左右双向滑动
                                offsetX = newOffset.coerceIn((-targetOffset).toFloat(), targetOffset.toFloat())
                            }
                        }
                    )
                }
        ) {
            Column {
                AccountCard(
                    account = account,
                    calculatedBalance = calculatedBalance,
                    currencySymbol = currencySymbol,
                    isExpanded = isExpanded,
                    onClick = {
                        // 如果正在显示操作按钮，忽略点击并关闭
                        if (swipeDirection != 0) {
                            swipeDirection = 0
                            offsetX = 0f
                        } else {
                            onCardClick()
                        }
                    }
                )

                // 展开的交易明细
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    TransactionDetailsSection(
                        monthlyTransactions = monthlyTransactions,
                        expandedMonths = expandedMonths,
                        accountId = account.id,
                        currencySymbol = currencySymbol,
                        onMonthClick = onMonthClick,
                        onContainerClick = {
                            // 点击明细区域时，关闭滑动状态
                            if (swipeDirection != 0) {
                                swipeDirection = 0
                                offsetX = 0f
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TotalBalanceCard(
    totalBalance: Double,
    currencySymbol: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "总资产",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White.copy(alpha = 0.8f)
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$currencySymbol ${CurrencyUtils.formatAmount(totalBalance)}",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
        }
    }
}

@Composable
private fun AccountCard(
    account: Account,
    calculatedBalance: Double,
    currencySymbol: String,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "arrow_rotation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 账户图标
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(android.graphics.Color.parseColor(account.color))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getAccountIcon(account.type.icon),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // 账户信息
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = account.name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Medium
                            )
                        )
                        if (!account.cardNumber.isNullOrBlank()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "(${account.cardNumber.takeLast(4)})",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                    Text(
                        text = account.type.displayName,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                // 余额
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$currencySymbol ${CurrencyUtils.formatAmount(calculatedBalance)}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = if (calculatedBalance >= 0) IOSColors.SystemGreen else IOSColors.SystemRed
                        )
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 展开/折叠箭头
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "收起" else "展开",
                    modifier = Modifier.rotate(rotationAngle),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 滑动提示
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "← 向左滑动删除账户    向右滑动编辑账户 →",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                )
            }
        }
    }
}

@Composable
private fun TransactionDetailsSection(
    monthlyTransactions: List<MonthlyTransactions>,
    expandedMonths: Set<String>,
    accountId: Long,
    currencySymbol: String,
    onMonthClick: (String) -> Unit,
    onContainerClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clickable(onClick = onContainerClick) // 点击明细区域关闭滑动
    ) {
        Text(
            text = "收支明细",
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (monthlyTransactions.isEmpty()) {
            Text(
                text = "暂无交易记录",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                ),
                modifier = Modifier.padding(vertical = 16.dp)
            )
        } else {
            monthlyTransactions.forEach { monthly ->
                MonthSection(
                    monthly = monthly,
                    isExpanded = expandedMonths.contains("${accountId}_${monthly.yearMonth}"),
                    accountId = accountId,
                    currencySymbol = currencySymbol,
                    onClick = { onMonthClick(monthly.yearMonth) }
                )
            }
        }
    }
}

@Composable
private fun MonthSection(
    monthly: MonthlyTransactions,
    isExpanded: Boolean,
    accountId: Long,
    currencySymbol: String,
    onClick: () -> Unit
) {
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 90f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "arrow_rotation"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        // 月份标题行
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(rotationAngle),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = monthly.monthDisplay,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${monthly.transactions.size}笔",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }

        // 交易明细列表
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 28.dp, top = 4.dp)
            ) {
                monthly.transactions.forEach { transaction ->
                    TransactionItem(
                        transaction = transaction,
                        currencySymbol = currencySymbol
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionItem(
    transaction: Transaction,
    currencySymbol: String
) {
    val dateFormat = remember { SimpleDateFormat("MM-dd", Locale.getDefault()) }
    val isExpense = transaction.type == TransactionType.EXPENSE
    val amountColor = if (isExpense) IOSColors.SystemRed else IOSColors.SystemGreen
    val amountPrefix = if (isExpense) "-" else "+"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 日期
        Text(
            text = dateFormat.format(Date(transaction.date)),
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.width(44.dp)
        )

        // 分类图标
        Icon(
            imageVector = getCategoryIcon(transaction.category.icon),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = amountColor
        )

        Spacer(modifier = Modifier.width(8.dp))

        // 分类名称
        Text(
            text = transaction.category.name,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )

        // 金额
        Text(
            text = "$amountPrefix$currencySymbol ${CurrencyUtils.formatAmount(transaction.amount)}",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium,
                color = amountColor
            )
        )
    }
}

@Composable
private fun EmptyAccountsView() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.AccountBalanceWallet,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "暂无账户",
            style = MaterialTheme.typography.titleMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        Text(
            text = "点击右上角添加您的第一个账户",
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddAccountDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, AccountType, String, Double, String, String?, Double, Int, Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedAttribute by remember { mutableStateOf(AccountAttribute.CASH) }
    var selectedType by remember { mutableStateOf(AccountType.BANK) }
    var initialBalance by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(accountColors[0]) }
    var cardNumber by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    
    // Credit account fields
    var creditLimit by remember { mutableStateOf("") }
    var billDay by remember { mutableStateOf("1") }
    var repaymentDay by remember { mutableStateOf("10") }
    
    // Update selectedType when attribute changes
    LaunchedEffect(selectedAttribute) {
        val types = getAccountTypesByAttribute(selectedAttribute)
        selectedType = types.firstOrNull() ?: AccountType.BANK
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        windowInsets = WindowInsets(0, 0, 0, 0)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "添加账户",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            
            // 账户属性选择 - 胶囊按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AccountAttribute.entries.forEach { attr ->
                    FilterChip(
                        selected = selectedAttribute == attr,
                        onClick = { selectedAttribute = attr },
                        label = { Text(attr.displayName, fontSize = 14.sp) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("账户名称") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // 账户类型选择
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = selectedType.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("账户类型") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    getAccountTypesByAttribute(selectedAttribute).forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.displayName) },
                            onClick = {
                                selectedType = type
                                expanded = false
                            },
                            leadingIcon = {
                                Icon(getAccountIcon(type.icon), contentDescription = null)
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = initialBalance,
                onValueChange = { initialBalance = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("期初余额") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
            
            // 信用账户额外字段
            if (selectedAttribute == AccountAttribute.CREDIT) {
                OutlinedTextField(
                    value = creditLimit,
                    onValueChange = { creditLimit = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("信用额度") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = billDay,
                        onValueChange = { 
                            if (it.isEmpty() || it.toIntOrNull() in 1..31) billDay = it 
                        },
                        label = { Text("账单日") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    
                    OutlinedTextField(
                        value = repaymentDay,
                        onValueChange = { 
                            if (it.isEmpty() || it.toIntOrNull() in 1..31) repaymentDay = it 
                        },
                        label = { Text("还款日") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            } else {
                OutlinedTextField(
                    value = cardNumber,
                    onValueChange = { cardNumber = it.takeLast(4) },
                    label = { Text("卡号后4位(可选)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            // 颜色选择 - 两边对齐
            Text(
                "选择颜色",
                style = MaterialTheme.typography.labelMedium
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                accountColors.take(5).forEach { color ->
                    ColorOption(
                        color = color,
                        isSelected = selectedColor == color,
                        onClick = { selectedColor = color }
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                accountColors.drop(5).forEach { color ->
                    ColorOption(
                        color = color,
                        isSelected = selectedColor == color,
                        onClick = { selectedColor = color }
                    )
                }
            }

            // 按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(
                    onClick = {
                        if (name.isNotBlank()) {
                            onConfirm(
                                name,
                                selectedType,
                                selectedType.icon,
                                initialBalance.toDoubleOrNull() ?: 0.0,
                                selectedColor,
                                cardNumber.takeIf { it.isNotBlank() },
                                creditLimit.toDoubleOrNull() ?: 0.0,
                                billDay.toIntOrNull() ?: 1,
                                repaymentDay.toIntOrNull() ?: 10
                            )
                        }
                    },
                    enabled = name.isNotBlank()
                ) {
                    Text("添加")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditAccountDialog(
    account: Account,
    onDismiss: () -> Unit,
    onConfirm: (String, AccountType, String, String, String?, Double, Int, Int) -> Unit,
    onDelete: () -> Unit,
    hasUnsettledDebt: Boolean = false
) {
    var name by remember { mutableStateOf(account.name) }
    var selectedColor by remember { mutableStateOf(account.color) }
    var cardNumber by remember { mutableStateOf(account.cardNumber ?: "") }
    
    // Credit account fields
    var creditLimit by remember { mutableStateOf(account.creditLimit.toString().takeIf { account.creditLimit > 0 } ?: "") }
    var billDay by remember { mutableStateOf(account.billDay.toString().takeIf { account.billDay > 0 } ?: "1") }
    var repaymentDay by remember { mutableStateOf(account.repaymentDay.toString().takeIf { account.repaymentDay > 0 } ?: "10") }
    
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showDebtWarning by remember { mutableStateOf(false) }

    if (showDebtWarning) {
        AlertDialog(
            onDismissRequest = { showDebtWarning = false },
            title = { Text("无法删除") },
            text = { Text("该信用账户尚有未结清负债（当前余额: ¥${String.format("%.2f", account.currentBalance)}），请先还清负债后再删除。") },
            confirmButton = {
                TextButton(onClick = { showDebtWarning = false }) {
                    Text("知道了")
                }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除账户") },
            text = { Text("确定要删除「${account.name}」吗？此操作不可恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = IOSColors.SystemRed)
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        windowInsets = WindowInsets(0, 0, 0, 0)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "编辑账户",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("账户名称") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            // 显示账户类型（不可编辑）
            OutlinedTextField(
                value = account.type.displayName,
                onValueChange = {},
                readOnly = true,
                label = { Text("账户类型") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    Icon(getAccountIcon(account.type.icon), contentDescription = null)
                }
            )
            
            // 信用账户字段
            if (account.attribute == AccountAttribute.CREDIT) {
                OutlinedTextField(
                    value = creditLimit,
                    onValueChange = { creditLimit = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("信用额度") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = billDay,
                        onValueChange = { 
                            if (it.isEmpty() || it.toIntOrNull() in 1..31) billDay = it 
                        },
                        label = { Text("账单日") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    
                    OutlinedTextField(
                        value = repaymentDay,
                        onValueChange = { 
                            if (it.isEmpty() || it.toIntOrNull() in 1..31) repaymentDay = it 
                        },
                        label = { Text("还款日") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            } else {
                OutlinedTextField(
                    value = cardNumber,
                    onValueChange = { cardNumber = it.takeLast(4) },
                    label = { Text("卡号后4位(可选)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            Text("选择颜色", style = MaterialTheme.typography.labelMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                accountColors.take(5).forEach { color ->
                    ColorOption(
                        color = color,
                        isSelected = selectedColor == color,
                        onClick = { selectedColor = color }
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                accountColors.drop(5).forEach { color ->
                    ColorOption(
                        color = color,
                        isSelected = selectedColor == color,
                        onClick = { selectedColor = color }
                    )
                }
            }

            // 按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(
                    onClick = {
                        if (hasUnsettledDebt) {
                            showDebtWarning = true
                        } else {
                            showDeleteConfirm = true
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = IOSColors.SystemRed)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("删除账户")
                }
                Row {
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            if (name.isNotBlank()) {
                                onConfirm(
                                    name,
                                    account.type,
                                    account.type.icon,
                                    selectedColor,
                                    cardNumber.takeIf { it.isNotBlank() },
                                    creditLimit.toDoubleOrNull() ?: 0.0,
                                    billDay.toIntOrNull() ?: 1,
                                    repaymentDay.toIntOrNull() ?: 10
                                )
                            }
                        },
                        enabled = name.isNotBlank()
                    ) {
                        Text("保存")
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorOption(
    color: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colorValue = Color(android.graphics.Color.parseColor(color))
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(colorValue)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

private fun getAccountIcon(iconName: String): ImageVector {
    return when (iconName) {
        "account_balance" -> Icons.Default.AccountBalance
        "chat" -> Icons.Default.Chat
        "payment" -> Icons.Default.Payment
        "wallet" -> Icons.Default.Wallet
        else -> Icons.Default.AccountBalanceWallet
    }
}

private fun getCategoryIcon(iconName: String): ImageVector {
    return when (iconName) {
        "restaurant" -> Icons.Default.Restaurant
        "directions_bus" -> Icons.Default.DirectionsBus
        "shopping_bag" -> Icons.Default.ShoppingBag
        "local_movies" -> Icons.Default.LocalMovies
        "home" -> Icons.Default.Home
        "medical" -> Icons.Default.MedicalServices
        "education" -> Icons.Default.School
        "communication" -> Icons.Default.Phone
        "insurance" -> Icons.Default.Security
        "travel" -> Icons.Default.Flight
        "lend" -> Icons.Default.Money
        "investment_expense" -> Icons.Default.TrendingDown
        "other" -> Icons.Default.MoreHoriz
        "salary" -> Icons.Default.Work
        "bonus" -> Icons.Default.CardGiftcard
        "investment" -> Icons.Default.TrendingUp
        "financial" -> Icons.Default.AccountBalance
        "redpacket" -> Icons.Default.CardGiftcard
        "utilities" -> Icons.Default.ElectricalServices
        "credit_card_repay" -> Icons.Default.CreditCard
        "mortgage" -> Icons.Default.House
        "repay_loan" -> Icons.Default.Payments
        "alipay_repay" -> Icons.Default.Payment
        "douyin_repay" -> Icons.Default.Payment
        "jd_repay" -> Icons.Default.Payment
        "account_transfer" -> Icons.Default.SwapHoriz
        "dividend" -> Icons.Default.Paid
        "refund" -> Icons.Default.AssignmentReturn
        "deposit_back" -> Icons.Default.Savings
        "收回借款" -> Icons.Default.CallReceived
        "accommodation" -> Icons.Default.Hotel
        "charity" -> Icons.Default.VolunteerActivism
        "send_redpacket" -> Icons.Default.CardGiftcard
        "income_transfer" -> Icons.Default.SwapHoriz
        "reimbursement" -> Icons.Default.RequestPage
        else -> Icons.Default.Receipt
    }
}

// 删除确认对话框 - 带10秒倒计时，按钮居中
@Composable
private fun DeleteConfirmDialog(
    accountName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    var countdown by remember { mutableIntStateOf(10) }
    var isConfirmEnabled by remember { mutableStateOf(false) }
    
    // 启动倒计时
    LaunchedEffect(Unit) {
        while (countdown > 0) {
            kotlinx.coroutines.delay(1000)
            countdown--
        }
        isConfirmEnabled = true
    }
    
    AlertDialog(
        onDismissRequest = { },
        title = {
            Text(
                text = "删除账户",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "确定要删除「$accountName」吗？",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                // 倒计时显示
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            if (isConfirmEnabled) IOSColors.SystemRed 
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isConfirmEnabled) "✓" else "$countdown",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isConfirmEnabled) Color.White 
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = if (isConfirmEnabled) "可以确认删除" else "请等待 ${countdown} 秒",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        },
        confirmButton = {
            // 确认按钮和取消按钮居中放置
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "取消",
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(
                    onClick = onConfirm,
                    enabled = isConfirmEnabled,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.textButtonColors(
                        disabledContentColor = Color.Gray,
                        contentColor = IOSColors.SystemRed
                    )
                ) {
                    Text(
                        text = "确认删除",
                        fontWeight = FontWeight.Bold,
                        color = if (isConfirmEnabled) IOSColors.SystemRed else Color.Gray
                    )
                }
            }
        },
        dismissButton = {
            // 空置，因为按钮已移到confirmButton中居中
        }
    )
}
