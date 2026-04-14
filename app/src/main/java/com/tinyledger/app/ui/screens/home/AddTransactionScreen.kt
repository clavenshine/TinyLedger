package com.tinyledger.app.ui.screens.home

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.BorderStroke
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
    initialCreditAccountId: Long? = null,
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

    // Trigger credit repayment mode when navigating from credit account
    LaunchedEffect(initialCreditAccountId) {
        if (initialCreditAccountId != null && initialCreditAccountId > 0) {
            viewModel.setCreditRepayModeById(initialCreditAccountId)
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
                        text = if (uiState.isEditing) "编辑记账" else "添加记账",
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = uiState.transactionType == TransactionType.EXPENSE,
                        onClick = { viewModel.setTransactionType(TransactionType.EXPENSE) },
                        label = { Text("支出", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                            selectedLabelColor = MaterialTheme.colorScheme.error
                        )
                    )
                    FilterChip(
                        selected = uiState.transactionType == TransactionType.INCOME,
                        onClick = { viewModel.setTransactionType(TransactionType.INCOME) },
                        label = { Text("收入", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    FilterChip(
                        selected = uiState.transactionType == TransactionType.TRANSFER,
                        onClick = { viewModel.setTransactionType(TransactionType.TRANSFER) },
                        label = { Text("转账", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFF9800).copy(alpha = 0.2f),
                            selectedLabelColor = Color(0xFFFF9800)
                        )
                    )
                    FilterChip(
                        selected = uiState.transactionType == TransactionType.LENDING,
                        onClick = { viewModel.setTransactionType(TransactionType.LENDING) },
                        label = { Text("借贷", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF9C27B0).copy(alpha = 0.2f),
                            selectedLabelColor = Color(0xFF9C27B0)
                        )
                    )
                }
            }

            // Amount Input with dynamic zoom effect
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
                    OutlinedTextField(
                        value = uiState.amount,
                        onValueChange = { viewModel.setAmount(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("0.00", fontSize = 28.sp, fontWeight = FontWeight.Bold) },
                        prefix = { Text(uiState.currencySymbol, fontSize = 28.sp, fontWeight = FontWeight.Bold) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp
                        )
                    )
                }
            }

            // Account & Date Selector on same row (for EXPENSE/INCOME) - moved right after Amount
            if (uiState.transactionType == TransactionType.EXPENSE || uiState.transactionType == TransactionType.INCOME) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Account Selector
                    Card(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.refreshAccounts()
                                    if (uiState.accounts.isEmpty()) showNoAccountDialog = true else showAccountSelector = true
                                }
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AccountSelectorRow(
                                account = uiState.selectedAccount,
                                placeholder = "选择账户"
                            )
                        }
                    }
                    // Date Selector
                    Card(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { showDatePicker = true }
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = "选择日期",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                        .format(Date(uiState.date)),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Date Selector (for TRANSFER/LENDING - full width)
            if (uiState.transactionType == TransactionType.TRANSFER || uiState.transactionType == TransactionType.LENDING) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePicker = true }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "选择日期",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                    .format(Date(uiState.date)),
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp)
                            )
                        }
                    }
                }
            }

            // Lending SubType Selector (only for LENDING) - redesigned to match category style
            if (uiState.transactionType == TransactionType.LENDING) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
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

            // Category Selector (only for EXPENSE/INCOME)
            if (uiState.transactionType == TransactionType.EXPENSE || uiState.transactionType == TransactionType.INCOME) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        CategorySelector(
                            categories = uiState.categories,
                            selectedCategory = uiState.selectedCategory,
                            onCategorySelected = { viewModel.selectCategory(it) },
                            onAddCategory = { name -> viewModel.addCategory(name) },
                            onDeleteCategory = { category -> viewModel.deleteCategory(category) },
                            onRenameCategory = { category, newName -> viewModel.renameCategory(category, newName) },
                            showAddButton = true,
                            transactionType = uiState.transactionType
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

    // Date Picker Bottom Sheet
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.date
        )
        // Auto-close when date is selected
        LaunchedEffect(datePickerState.selectedDateMillis) {
            if (showDatePicker && datePickerState.selectedDateMillis != null && datePickerState.selectedDateMillis != uiState.date) {
                viewModel.setDate(datePickerState.selectedDateMillis!!)
                showDatePicker = false
            }
        }
        ModalBottomSheet(
            onDismissRequest = { showDatePicker = false },
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            windowInsets = WindowInsets(0, 0, 0, 0)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp)
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }

    // Account Selector Bottom Sheet
    if (showAccountSelector) {
        ModalBottomSheet(
            onDismissRequest = { showAccountSelector = false },
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            windowInsets = WindowInsets(0, 0, 0, 0)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = "选择账户",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                uiState.accounts.forEachIndexed { index, account ->
                    AccountItem(
                        account = account,
                        isSelected = uiState.selectedAccount?.id == account.id,
                        isLast = index == uiState.accounts.lastIndex,
                        onClick = {
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
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    // From Account Selector Bottom Sheet
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
        ModalBottomSheet(
            onDismissRequest = { showFromAccountSelector = false },
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            windowInsets = WindowInsets(0, 0, 0, 0)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = dialogTitle,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                uiState.accounts.forEachIndexed { index, account ->
                    AccountItem(
                        account = account,
                        isSelected = uiState.selectedFromAccount?.id == account.id,
                        isLast = index == uiState.accounts.lastIndex,
                        onClick = {
                            viewModel.selectFromAccount(account)
                            showFromAccountSelector = false
                        }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    // To Account Selector Bottom Sheet
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
        ModalBottomSheet(
            onDismissRequest = { showToAccountSelector = false },
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            windowInsets = WindowInsets(0, 0, 0, 0)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = dialogTitle,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                uiState.accounts.forEachIndexed { index, account ->
                    AccountItem(
                        account = account,
                        isSelected = uiState.selectedToAccount?.id == account.id,
                        isLast = index == uiState.accounts.lastIndex,
                        onClick = {
                            viewModel.selectToAccount(account)
                            showToAccountSelector = false
                        }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
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
        AccountType.CASH -> Icons.Default.Wallet
        AccountType.YUEBAO -> Icons.Default.AccountBalanceWallet
        AccountType.OTHER -> Icons.Default.HelpOutline
        // Credit account types
        AccountType.CREDIT_CARD -> Icons.Default.CreditCard
        AccountType.HUA_BEI -> Icons.Default.Payment
        AccountType.JIE_BEI -> Icons.Default.Payments
        AccountType.JD_BAITIAO -> Icons.Default.ShoppingBag
        AccountType.MEITUAN_YUEFU -> Icons.Default.Restaurant
        AccountType.DOUYIN_YUEFU -> Icons.Default.VideoLibrary
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
private fun AccountItem(
    account: Account,
    isSelected: Boolean,
    onClick: () -> Unit,
    isLast: Boolean = false
) {
    Column {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clickable { onClick() },
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
            ),
            border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(parseColor(account.color)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getAccountIcon(account.type),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = account.name,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        if (!account.cardNumber.isNullOrBlank()) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "(${account.cardNumber.takeLast(4)})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        text = "\u00A5${String.format("%.2f", account.currentBalance)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "已选择",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        if (!isLast) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 28.dp),
                color = Color.Gray.copy(alpha = 0.2f),
                thickness = 0.5.dp
            )
        }
    }
}

@Composable
private fun AccountSelectorRow(
    account: Account?,
    placeholder: String,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
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
    val animatedBorderWidth by animateDpAsState(
        targetValue = if (isSelected) 2.dp else 1.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "lending_border_width"
    )

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .background(
                if (isSelected) selectedColor.copy(alpha = 0.1f)
                else MaterialTheme.colorScheme.surface
            )
            .border(
                width = animatedBorderWidth,
                color = if (isSelected) selectedColor
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) selectedColor.copy(alpha = 0.2f)
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) selectedColor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            color = if (isSelected) selectedColor else MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}
