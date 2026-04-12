package com.tinyledger.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.tinyledger.app.domain.model.Account
import com.tinyledger.app.domain.model.AccountType
import com.tinyledger.app.domain.model.TransactionType
import com.tinyledger.app.ui.components.CategorySelector
import com.tinyledger.app.ui.viewmodel.AddTransactionViewModel
import com.tinyledger.app.ui.viewmodel.LendingSubType
import com.tinyledger.app.util.DateUtils
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAccounts: () -> Unit = {},
    transactionId: Long? = null,
    viewModel: AddTransactionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }
    var showAccountSelector by remember { mutableStateOf(false) }
    var showFromAccountSelector by remember { mutableStateOf(false) }
    var showToAccountSelector by remember { mutableStateOf(false) }
    var showNoAccountDialog by remember { mutableStateOf(false) }

    LaunchedEffect(transactionId) {
        if (transactionId != null && transactionId > 0) {
            viewModel.loadTransaction(transactionId)
        }
    }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.isEditing) "编辑记录" else "添加记录",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Transaction Type Selector - 4 types
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "类型",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = uiState.transactionType == TransactionType.EXPENSE,
                            onClick = { viewModel.setTransactionType(TransactionType.EXPENSE) },
                            label = { Text("支出") },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                                selectedLabelColor = MaterialTheme.colorScheme.error
                            )
                        )
                        FilterChip(
                            selected = uiState.transactionType == TransactionType.INCOME,
                            onClick = { viewModel.setTransactionType(TransactionType.INCOME) },
                            label = { Text("收入") },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        FilterChip(
                            selected = uiState.transactionType == TransactionType.TRANSFER,
                            onClick = { viewModel.setTransactionType(TransactionType.TRANSFER) },
                            label = { Text("转账") },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFFF9800).copy(alpha = 0.2f),
                                selectedLabelColor = Color(0xFFFF9800)
                            )
                        )
                        FilterChip(
                            selected = uiState.transactionType == TransactionType.LENDING,
                            onClick = { viewModel.setTransactionType(TransactionType.LENDING) },
                            label = { Text("借贷") },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF9C27B0).copy(alpha = 0.2f),
                                selectedLabelColor = Color(0xFF9C27B0)
                            )
                        )
                    }
                }
            }

            // Amount Input
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "金额",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = uiState.amount,
                        onValueChange = { viewModel.setAmount(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("0.00") },
                        prefix = { Text(uiState.currencySymbol) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                }
            }

            // Lending SubType Selector (only for LENDING)
            if (uiState.transactionType == TransactionType.LENDING) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            LendingTypeButton(
                                icon = Icons.Default.SouthWest,
                                label = "借入",
                                isSelected = uiState.lendingSubType == LendingSubType.BORROW_IN,
                                selectedColor = Color(0xFF9C27B0),
                                onClick = { viewModel.setLendingSubType(LendingSubType.BORROW_IN) }
                            )
                            LendingTypeButton(
                                icon = Icons.Default.NorthEast,
                                label = "借出",
                                isSelected = uiState.lendingSubType == LendingSubType.BORROW_OUT,
                                selectedColor = Color(0xFF9C27B0),
                                onClick = { viewModel.setLendingSubType(LendingSubType.BORROW_OUT) }
                            )
                            LendingTypeButton(
                                icon = Icons.Default.CreditCardOff,
                                label = "还款",
                                isSelected = uiState.lendingSubType == LendingSubType.REPAY,
                                selectedColor = Color(0xFF9C27B0),
                                onClick = { viewModel.setLendingSubType(LendingSubType.REPAY) }
                            )
                            LendingTypeButton(
                                icon = Icons.Default.CreditScore,
                                label = "收款",
                                isSelected = uiState.lendingSubType == LendingSubType.COLLECT,
                                selectedColor = Color(0xFF9C27B0),
                                onClick = { viewModel.setLendingSubType(LendingSubType.COLLECT) }
                            )
                        }
                    }
                }
            }

            // Category Selector (only for EXPENSE/INCOME)
            if (uiState.transactionType == TransactionType.EXPENSE || uiState.transactionType == TransactionType.INCOME) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "分类",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        CategorySelector(
                            categories = uiState.categories,
                            selectedCategory = uiState.selectedCategory,
                            onCategorySelected = { viewModel.selectCategory(it) },
                            onAddCategory = { name -> viewModel.addCategory(name) },
                            showAddButton = true,
                            transactionType = uiState.transactionType,
                            modifier = Modifier.height(200.dp)
                        )
                    }
                }

                // Account Selector (for EXPENSE/INCOME)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "账户", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        AccountSelectorRow(
                            account = uiState.selectedAccount,
                            placeholder = "选择账户（必选）",
                            onClick = {
                                viewModel.refreshAccounts()
                                if (uiState.accounts.isEmpty()) showNoAccountDialog = true else showAccountSelector = true
                            }
                        )
                    }
                }
            }

            // From/To Account Selector for TRANSFER
            if (uiState.transactionType == TransactionType.TRANSFER) {
                // From Account (转出账户)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "转出账户", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        AccountSelectorRow(
                            account = uiState.selectedFromAccount,
                            placeholder = "请选择转出账户",
                            onClick = {
                                viewModel.refreshAccounts()
                                if (uiState.accounts.isEmpty()) showNoAccountDialog = true else showFromAccountSelector = true
                            }
                        )
                    }
                }
                // Down arrow
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.South, contentDescription = null, tint = Color(0xFF9C27B0), modifier = Modifier.size(28.dp))
                }
                // To Account (转入账户)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "转入账户", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        AccountSelectorRow(
                            account = uiState.selectedToAccount,
                            placeholder = "请选择转入账户",
                            onClick = {
                                viewModel.refreshAccounts()
                                if (uiState.accounts.isEmpty()) showNoAccountDialog = true else showToAccountSelector = true
                            }
                        )
                    }
                }
            }

            // From/To Account Selector for LENDING
            if (uiState.transactionType == TransactionType.LENDING) {
                val fromLabel = when (uiState.lendingSubType) {
                    LendingSubType.BORROW_IN -> "负债账户"
                    LendingSubType.BORROW_OUT -> "出账账户"
                    LendingSubType.REPAY -> "出账账户"
                    LendingSubType.COLLECT -> "债权账户"
                }
                val toLabel = when (uiState.lendingSubType) {
                    LendingSubType.BORROW_IN -> "入账账户"
                    LendingSubType.BORROW_OUT -> "债权账户"
                    LendingSubType.REPAY -> "负债账户"
                    LendingSubType.COLLECT -> "入账账户"
                }
                // From Account
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = fromLabel, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        AccountSelectorRow(
                            account = uiState.selectedFromAccount,
                            placeholder = "请选择${fromLabel}",
                            onClick = {
                                viewModel.refreshAccounts()
                                if (uiState.accounts.isEmpty()) showNoAccountDialog = true else showFromAccountSelector = true
                            }
                        )
                    }
                }
                // Down arrow
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.South, contentDescription = null, tint = Color(0xFF9C27B0), modifier = Modifier.size(28.dp))
                }
                // To Account
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = toLabel, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        AccountSelectorRow(
                            account = uiState.selectedToAccount,
                            placeholder = "请选择${toLabel}",
                            onClick = {
                                viewModel.refreshAccounts()
                                if (uiState.accounts.isEmpty()) showNoAccountDialog = true else showToAccountSelector = true
                            }
                        )
                    }
                }
            }

            // Date Selector
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "日期",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            .format(Date(uiState.date)),
                        onValueChange = {},
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = "选择日期"
                                )
                            }
                        }
                    )
                }
            }

            // Note Input
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "备注",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = uiState.note,
                        onValueChange = { viewModel.setNote(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("添加备注...") },
                        maxLines = 3
                    )
                }
            }

            // Error Message
            uiState.errorMessage?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Save Button
            Button(
                onClick = {
                    viewModel.saveTransaction()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !uiState.isSaving,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        text = "保存",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.date
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            viewModel.setDate(it)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("取消")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Account Selector Dialog
    if (showAccountSelector) {
        AlertDialog(
            onDismissRequest = { showAccountSelector = false },
            title = { Text("选择账户") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 账户列表
                    uiState.accounts.forEach { account ->
                        ListItem(
                            headlineContent = { Text(account.name) },
                            supportingContent = { 
                                Text("余额: ¥${String.format("%.2f", account.currentBalance)}") 
                            },
                            leadingContent = {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(parseColor(account.color)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = getAccountIcon(account.type),
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            },
                            trailingContent = {
                                if (uiState.selectedAccount?.id == account.id) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "已选择",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            modifier = Modifier.clickable {
                                viewModel.selectAccount(account)
                                showAccountSelector = false
                            }
                        )
                    }
                    
                    if (uiState.accounts.isEmpty()) {
                        Text(
                            text = "暂无账户，请在\"账户\"管理中添加",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAccountSelector = false }) {
                    Text("关闭")
                }
            }
        )
    }

    // From Account Selector Dialog
    if (showFromAccountSelector) {
        val dialogTitle = when (uiState.transactionType) {
            TransactionType.TRANSFER -> "选择转出账户"
            TransactionType.LENDING -> when (uiState.lendingSubType) {
                LendingSubType.BORROW_IN -> "选择负债账户"
                LendingSubType.BORROW_OUT -> "选择出账账户"
                LendingSubType.REPAY -> "选择出账账户"
                LendingSubType.COLLECT -> "选择债权账户"
            }
            else -> "选择账户"
        }
        AlertDialog(
            onDismissRequest = { showFromAccountSelector = false },
            title = { Text(dialogTitle) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.accounts.forEach { account ->
                        ListItem(
                            headlineContent = { Text(account.name) },
                            supportingContent = {
                                Text("余额: ¥${String.format("%.2f", account.currentBalance)}")
                            },
                            leadingContent = {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(parseColor(account.color)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = getAccountIcon(account.type),
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            },
                            trailingContent = {
                                if (uiState.selectedFromAccount?.id == account.id) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "已选择",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            modifier = Modifier.clickable {
                                viewModel.selectFromAccount(account)
                                showFromAccountSelector = false
                            }
                        )
                    }
                    if (uiState.accounts.isEmpty()) {
                        Text(
                            text = "暂无账户，请在\"账户\"管理中添加",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFromAccountSelector = false }) {
                    Text("关闭")
                }
            }
        )
    }

    // To Account Selector Dialog
    if (showToAccountSelector) {
        val dialogTitle = when (uiState.transactionType) {
            TransactionType.TRANSFER -> "选择转入账户"
            TransactionType.LENDING -> when (uiState.lendingSubType) {
                LendingSubType.BORROW_IN -> "选择入账账户"
                LendingSubType.BORROW_OUT -> "选择债权账户"
                LendingSubType.REPAY -> "选择负债账户"
                LendingSubType.COLLECT -> "选择入账账户"
            }
            else -> "选择账户"
        }
        AlertDialog(
            onDismissRequest = { showToAccountSelector = false },
            title = { Text(dialogTitle) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.accounts.forEach { account ->
                        ListItem(
                            headlineContent = { Text(account.name) },
                            supportingContent = {
                                Text("余额: ¥${String.format("%.2f", account.currentBalance)}")
                            },
                            leadingContent = {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(parseColor(account.color)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = getAccountIcon(account.type),
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            },
                            trailingContent = {
                                if (uiState.selectedToAccount?.id == account.id) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "已选择",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            modifier = Modifier.clickable {
                                viewModel.selectToAccount(account)
                                showToAccountSelector = false
                            }
                        )
                    }
                    if (uiState.accounts.isEmpty()) {
                        Text(
                            text = "暂无账户，请在\"账户\"管理中添加",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showToAccountSelector = false }) {
                    Text("关闭")
                }
            }
        )
    }

    // 未添加账户提示对话框 - 精美卡片样式
    if (showNoAccountDialog) {
        Dialog(onDismissRequest = { showNoAccountDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // 警告图标
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(
                                MaterialTheme.colorScheme.errorContainer
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }

                    // 标题
                    Text(
                        text = "账户未建立",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    // 副标题
                    Text(
                        text = "请先建立账户",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        fontSize = 16.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 确认按钮
                    Button(
                        onClick = {
                            showNoAccountDialog = false
                            onNavigateToAccounts()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = "去建立账户",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // 取消按钮
                    TextButton(
                        onClick = { showNoAccountDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "返回",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// 账户类型图标映射
private fun getAccountIcon(type: AccountType): ImageVector {
    return when (type) {
        AccountType.BANK -> Icons.Default.AccountBalance
        AccountType.WECHAT -> Icons.Default.Chat
        AccountType.ALIPAY -> Icons.Default.Payment
        AccountType.CASH -> Icons.Default.Wallet
        AccountType.OTHER -> Icons.Default.AccountBalanceWallet
    }
}

// 解析颜色字符串
private fun parseColor(colorString: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(colorString))
    } catch (e: Exception) {
        Color(0xFF10B981) // 默认绿色
    }
}

@Composable
private fun AccountSelectorRow(
    account: Account?,
    placeholder: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (account != null) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(parseColor(account.color)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getAccountIcon(account.type),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(text = account.name, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = "\u4F59\u989D: \u00A5${String.format("%.2f", account.currentBalance)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "选择",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LendingTypeButton(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(if (isSelected) selectedColor else Color(0xFFE0E0E0)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else Color(0xFF757575),
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            ),
            color = if (isSelected) selectedColor else Color(0xFF757575)
        )
    }
}
