package com.tinyledger.app.ui.screens.accounts

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.scale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.border
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
    initialTab: Int = 0, // 0: 全部, 1: 现金, 2: 信用
    onNavigateBack: () -> Unit = {},
    onNavigateToAccountDetail: (Long) -> Unit = {},
    onNavigateToRepay: (Account) -> Unit = {},
    onNavigateToAddAccount: () -> Unit = {},
    viewModel: AccountViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Tab state - use initialTab parameter
    var selectedTab by remember { mutableStateOf(initialTab) } // 0: 现金, 1: 信用, 2: 外部往来, 3: 停用账户
    val tabs = listOf("现金账户", "信用账户", "外部往来", "停用账户")
    
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
        0 -> uiState.accountsWithBalance.filter { it.first.attribute == AccountAttribute.CASH && !it.first.isDisabled } // 现金
        1 -> uiState.accountsWithBalance.filter { it.first.attribute == AccountAttribute.CREDIT_ACCOUNT && !it.first.isDisabled } // 信用
        2 -> uiState.accountsWithBalance.filter { it.first.attribute == AccountAttribute.CREDIT && !it.first.isDisabled } // 外部往来
        3 -> uiState.accountsWithBalance.filter { it.first.isDisabled } // 停用账户
        else -> uiState.accountsWithBalance
    }
    
    // 计算筛选后的总额
    val filteredTotalBalance = filteredAccountsWithBalance.sumOf { it.second }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "全部账户",
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
        bottomBar = {
            // 底部固定“新建账户”按钮
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable { onNavigateToAddAccount() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "新建账户",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Tab 筛选器 - 移到顶部，居中，3D立体效果
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEachIndexed { index, title ->
                    // 3D 浮起按钮效果
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clickable { selectedTab = index },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedTab == index) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = if (selectedTab == index) 8.dp else 4.dp
                        )
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedTab == index) 
                                    MaterialTheme.colorScheme.onPrimary 
                                else 
                                    MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    
                    if (index < tabs.lastIndex) {
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
            }
            
            // 总资产卡片 - 根据选中tab显示不同余额
            val displayBalance = when (selectedTab) {
                0 -> uiState.accountsWithBalance
                    .filter { it.first.attribute == AccountAttribute.CASH && !it.first.isDisabled }
                    .sumOf { it.second } // 现金账户
                1 -> uiState.accountsWithBalance
                    .filter { it.first.attribute == AccountAttribute.CREDIT_ACCOUNT && !it.first.isDisabled }
                    .sumOf { it.second } // 信用账户
                2 -> uiState.accountsWithBalance
                    .filter { it.first.attribute == AccountAttribute.CREDIT && !it.first.isDisabled }
                    .sumOf { it.second } // 外部往来账户
                3 -> 0.0 // 停用账户不显示总额
                else -> 0.0
            }
            
            TotalBalanceCard(
                totalBalance = displayBalance,
                currencySymbol = currencySymbol,
                showTitle = false, // 不显示“总资产”
                largeNumber = true // 放大数字
            )

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
                        AccountCardSimple(
                            account = account,
                            calculatedBalance = calculatedBalance,
                            currencySymbol = currencySymbol,
                            showDisabledBadge = selectedTab == 3, // 停用账户标签页显示"已停用"标识
                            onClick = {
                                // 点击账户卡片，跳转到账户详情页面
                                onNavigateToAccountDetail(account.id)
                            },
                            onRepay = {
                                onNavigateToRepay(account)
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

    // 编辑账户对话框
    if (uiState.showEditDialog && uiState.selectedAccount != null) {
        EditAccountDialog(
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
}

@Composable
private fun AccountCardSimple(
    account: Account,
    calculatedBalance: Double,
    currencySymbol: String,
    showDisabledBadge: Boolean = false,
    onClick: () -> Unit,
    onRepay: () -> Unit = {} // 信用还款回调
) {
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
        Box(modifier = Modifier.fillMaxSize()) {
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
                        
                        // 信用账户还款按钮
                        if (account.attribute == AccountAttribute.CREDIT && calculatedBalance < 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = {
                                    // 阻止事件冒泡，不触发卡片点击
                                    onRepay()
                                },
                                modifier = Modifier.height(24.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = IOSColors.SystemRed.copy(alpha = 0.1f),
                                    contentColor = IOSColors.SystemRed
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "还款",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
            
            // 已停用标识 - 右上角
            if (showDisabledBadge) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "已停用",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TotalBalanceCard(
    totalBalance: Double,
    currencySymbol: String,
    showTitle: Boolean = true,
    largeNumber: Boolean = false
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
            if (showTitle) {
                Text(
                    text = "总资产",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White.copy(alpha = 0.8f)
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
            } else {
                Spacer(modifier = Modifier.height(8.dp))
            }
            Text(
                text = "$currencySymbol ${CurrencyUtils.formatAmount(totalBalance)}",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = if (largeNumber) 36.sp else 28.sp
                )
            )
            Spacer(modifier = Modifier.height(if (showTitle) 8.dp else 16.dp))
        }
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun AddAccountDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, AccountType, String, Double, String, String?, Double, Int, Int, String) -> Unit
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
    
    // 期初余额日期
    var initialBalanceDate by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    
    // Update selectedType when attribute changes
    LaunchedEffect(selectedAttribute) {
        val types = getAccountTypesByAttribute(selectedAttribute)
        selectedType = types.firstOrNull() ?: AccountType.BANK
    }
    
    // 日期选择器
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                        initialBalanceDate = sdf.format(java.util.Date(millis))
                    }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
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
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()), // 添加滚动以避开键盘
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
                        label = { Text(attr.displayName, fontSize = 18.sp, fontWeight = FontWeight.Medium) },
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

            // 期初余额 + 期初余额日期（同一行）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 期初余额输入框
                OutlinedTextField(
                    value = initialBalance,
                    onValueChange = { input ->
                        val filtered = input.filter { char -> char.isDigit() || char == '.' || char == '-' }
                        initialBalance = if (selectedAttribute == AccountAttribute.CREDIT || selectedAttribute == AccountAttribute.CREDIT_ACCOUNT) {
                            // 信用账户和外部往来账户允许填负数
                            if (filtered.length > 1 && filtered[0] != '-')
                                filtered.filter { char -> char.isDigit() || char == '.' }
                            else
                                filtered
                        } else {
                            // 现金账户只允许正数
                            filtered.filter { char -> char.isDigit() || char == '.' }
                        }
                    },
                    label = { Text("期初余额") },
                    modifier = Modifier.weight(0.5f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                
                // 期初余额日期选择按钮
                OutlinedTextField(
                    value = initialBalanceDate,
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("期初日期") },
                    modifier = Modifier.weight(0.5f),
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = "选择日期")
                        }
                    },
                    enabled = true
                )
            }
            
            // 信用账户和外部往来账户期初余额提示
            if (selectedAttribute == AccountAttribute.CREDIT || selectedAttribute == AccountAttribute.CREDIT_ACCOUNT) {
                Text(
                    text = if (selectedAttribute == AccountAttribute.CREDIT_ACCOUNT) 
                        "期初余额可以为负数，表示欠款" 
                    else 
                        "期初余额用正数表示应收回债权，用负数表示应当归还的欠款",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )
            }
            
            // 信用账户额外字段（账单日、还款日、信用额度）
            if (selectedAttribute == AccountAttribute.CREDIT_ACCOUNT) {
                // 卡号后4位
                OutlinedTextField(
                    value = cardNumber,
                    onValueChange = { cardNumber = it.takeLast(4) },
                    label = { Text("卡号后4位(可选)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = creditLimit,
                    onValueChange = { creditLimit = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("信用额度", fontSize = 16.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                
                // 账单日和还款日 - 同一行显示
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 账单日 - 缩小宽度
                    Column(
                        modifier = Modifier.weight(0.45f),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "账单日",
                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 16.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        NumberPickerWithLabel(
                            label = "",
                            value = billDay.toIntOrNull() ?: 1,
                            range = 1..31,
                            onValueChange = { billDay = it.toString() },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    // 还款日 - 缩小宽度
                    Column(
                        modifier = Modifier.weight(0.45f),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "还款日",
                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 16.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        NumberPickerWithLabel(
                            label = "",
                            value = repaymentDay.toIntOrNull() ?: 10,
                            range = 1..31,
                            onValueChange = { repaymentDay = it.toString() },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            } else {
                // 现金账户：卡号后4位
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

            // 按钮 - 立体感设计，适配浅色/深色模式
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 取消按钮 - 灰色立体
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onDismiss)
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(12.dp),
                            spotColor = Color(0xFF6B7280).copy(alpha = 0.3f)
                        )
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFF9FAFB),  // 浅灰顶部
                                    Color(0xFFE5E7EB)   // 深灰底部
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            color = Color(0xFFD1D5DB).copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "取消",
                        color = Color(0xFF374151),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 14.dp)
                    )
                }

                // 添加按钮 - 蓝色立体
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
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
                                        repaymentDay.toIntOrNull() ?: 10,
                                        initialBalanceDate
                                    )
                                }
                            },
                            enabled = name.isNotBlank()
                        )
                        .shadow(
                            elevation = 10.dp,
                            shape = RoundedCornerShape(12.dp),
                            spotColor = Color(0xFF3B82F6).copy(alpha = 0.5f)
                        )
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            brush = Brush.verticalGradient(
                                colors = if (name.isNotBlank()) {
                                    listOf(
                                        Color(0xFF60A5FA),  // 亮蓝顶部
                                        Color(0xFF3B82F6)   // 深蓝底部
                                    )
                                } else {
                                    listOf(
                                        Color(0xFF9CA3AF),  // 灰色顶部
                                        Color(0xFF6B7280)   // 深灰底部
                                    )
                                }
                            )
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.4f),
                                    Color.White.copy(alpha = 0.1f)
                                )
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "添加",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 14.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAccountDialog(
    account: Account,
    onDismiss: () -> Unit,
    onConfirm: (String, AccountType, String, String, String?, Double, Int, Int, Double, String) -> Unit,
    onDelete: () -> Unit,
    hasUnsettledDebt: Boolean = false
) {
    var name by remember { mutableStateOf(account.name) }
    var selectedColor by remember { mutableStateOf(account.color) }
    var cardNumber by remember { mutableStateOf(account.cardNumber ?: "") }
    
    // 期初余额和日期
    var initialBalance by remember { mutableStateOf(account.initialBalance.toString()) }
    var initialBalanceDate by remember { mutableStateOf(account.initialBalanceDate) }
    var showDatePicker by remember { mutableStateOf(false) }
    
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
    
    // 日期选择器
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = if (initialBalanceDate.isNotBlank()) {
                java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).parse(initialBalanceDate)?.time
            } else {
                System.currentTimeMillis()
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                        initialBalanceDate = sdf.format(java.util.Date(millis))
                    }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
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
            
            // 期初余额 + 期初余额日期（同一行）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 期初余额输入框
                OutlinedTextField(
                    value = initialBalance,
                    onValueChange = { input ->
                        val filtered = input.filter { char -> char.isDigit() || char == '.' || char == '-' }
                        initialBalance = if (account.attribute == AccountAttribute.CREDIT || account.attribute == AccountAttribute.CREDIT_ACCOUNT) {
                            if (filtered.length > 1 && filtered[0] != '-')
                                filtered.filter { char -> char.isDigit() || char == '.' }
                            else
                                filtered
                        } else {
                            filtered.filter { char -> char.isDigit() || char == '.' }
                        }
                    },
                    label = { Text("期初余额") },
                    modifier = Modifier.weight(0.5f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                
                // 期初余额日期选择按钮
                OutlinedTextField(
                    value = initialBalanceDate,
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("期初日期") },
                    modifier = Modifier.weight(0.5f),
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = "选择日期")
                        }
                    },
                    enabled = true
                )
            }
            
            // 信用账户字段
            if (account.attribute == AccountAttribute.CREDIT) {
                OutlinedTextField(
                    value = creditLimit,
                    onValueChange = { creditLimit = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("信用额度", fontSize = 16.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                
                // 账单日和还款日 - 同一行显示
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 账单日 - 缩小宽度
                    Column(
                        modifier = Modifier.weight(0.45f),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "账单日",
                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 16.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        OutlinedTextField(
                            value = billDay,
                            onValueChange = { 
                                if (it.isEmpty() || it.toIntOrNull() in 1..31) billDay = it 
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                    
                    // 还款日 - 缩小宽度
                    Column(
                        modifier = Modifier.weight(0.45f),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "还款日",
                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 16.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        OutlinedTextField(
                            value = repaymentDay,
                            onValueChange = { 
                                if (it.isEmpty() || it.toIntOrNull() in 1..31) repaymentDay = it 
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
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

            // 按钮 - 美化版，有立体感和精致感
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 取消按钮 - 精致立体感
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(onClick = onDismiss)
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(14.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "取消",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // 保存按钮 - 精致立体感
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .then(
                            if (name.isNotBlank()) Modifier.clickable {
                                onConfirm(
                                    name,
                                    account.type,
                                    account.type.icon,
                                    selectedColor,
                                    cardNumber.takeIf { it.isNotBlank() },
                                    creditLimit.toDoubleOrNull() ?: 0.0,
                                    billDay.toIntOrNull() ?: 1,
                                    repaymentDay.toIntOrNull() ?: 10,
                                    initialBalance.toDoubleOrNull() ?: 0.0,
                                    initialBalanceDate
                                )
                            } else Modifier
                        )
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = if (name.isNotBlank()) {
                                    listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.95f),
                                        MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    listOf(
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                    )
                                }
                            )
                        )
                        .border(
                            width = 1.dp,
                            color = if (name.isNotBlank())
                                androidx.compose.ui.graphics.Color.White.copy(alpha = 0.2f)
                            else
                                androidx.compose.ui.graphics.Color.Transparent,
                            shape = RoundedCornerShape(14.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "保存",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (name.isNotBlank())
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
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

// 带标签的数字滚轮选择器
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NumberPickerWithLabel(
    label: String,
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = range.toList()
    val itemHeightDp = 30.dp
    val visibleItems = 3
    val density = LocalDensity.current
    val itemHeightPx = with(density) { itemHeightDp.toPx() }
    val listState = rememberLazyListState()
    val cardBgColor = Color(0xFFF5F5F5)

    // Initialize scroll position to current value
    LaunchedEffect(Unit) {
        val index = items.indexOf(value).coerceAtLeast(0)
        listState.scrollToItem(index)
    }

    // When value changes externally, scroll to it
    LaunchedEffect(value) {
        val index = items.indexOf(value).coerceAtLeast(0)
        if (listState.firstVisibleItemIndex != index || listState.firstVisibleItemScrollOffset != 0) {
            listState.animateScrollToItem(index)
        }
    }

    // When scroll settles, report the centered value
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val centerIndex = listState.firstVisibleItemIndex +
                (if (listState.firstVisibleItemScrollOffset > itemHeightPx / 2) 1 else 0)
            val newValue = items.getOrNull(centerIndex.coerceIn(items.indices))
            if (newValue != null && newValue != value) {
                onValueChange(newValue)
            }
        }
    }

    // Use snap fling behavior
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeightDp * visibleItems),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = cardBgColor
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 4.dp
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                LazyColumn(
                    state = listState,
                    flingBehavior = flingBehavior,
                    contentPadding = PaddingValues(vertical = itemHeightDp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(items) { index, item ->
                        val isCentered = remember {
                            derivedStateOf {
                                val firstVisible = listState.firstVisibleItemIndex
                                val offset = listState.firstVisibleItemScrollOffset
                                val centeredIndex = firstVisible + (if (offset > itemHeightPx / 2) 1 else 0)
                                index == centeredIndex
                            }
                        }
                        val centered = isCentered.value
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(itemHeightDp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$item 日",
                                fontSize = if (centered) 20.sp else 14.sp,
                                fontWeight = if (centered) FontWeight.Bold else FontWeight.Normal,
                                color = if (centered)
                                    MaterialTheme.colorScheme.primary
                                else
                                    Color.Gray.copy(alpha = 0.4f)
                            )
                        }
                    }
                }

                // Center highlight bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeightDp)
                        .align(Alignment.Center)
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                )

                // Top gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeightDp)
                        .align(Alignment.TopCenter)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    cardBgColor,
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Bottom gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeightDp)
                        .align(Alignment.BottomCenter)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    cardBgColor
                                )
                            )
                        )
                )
            }
        }
    }
}
