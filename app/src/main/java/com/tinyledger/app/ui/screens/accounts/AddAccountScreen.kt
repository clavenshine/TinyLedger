package com.tinyledger.app.ui.screens.accounts

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tinyledger.app.domain.model.AccountAttribute
import com.tinyledger.app.domain.model.AccountType
import com.tinyledger.app.domain.model.accountColors
import com.tinyledger.app.ui.viewmodel.AccountViewModel
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.graphics.vector.ImageVector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: AccountViewModel = hiltViewModel()
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
    
    // 备注
    var note by remember { mutableStateOf("") }
    
    // Update selectedType when attribute changes
    LaunchedEffect(selectedAttribute) {
        val types = getAccountTypesByAttribute(selectedAttribute)
        selectedType = types.firstOrNull() ?: AccountType.BANK
    }
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "新建账户",
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // 账户属性选择 - 胶囊按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AccountAttribute.entries.forEach { attr ->
                    FilterChip(
                        selected = selectedAttribute == attr,
                        onClick = { selectedAttribute = attr },
                        label = { Text(attr.displayName, fontSize = 14.sp, fontWeight = FontWeight.Medium) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("账户名称") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

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
                                Icon(getAccountIconLocal(type.icon), contentDescription = null)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

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
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
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
                    placeholder = { Text("YYYY-MM-DD", fontSize = 12.sp) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 卡号后4位（现金账户和信用账户显示）
            if (selectedAttribute == AccountAttribute.CASH || selectedAttribute == AccountAttribute.CREDIT_ACCOUNT) {
                OutlinedTextField(
                    value = cardNumber,
                    onValueChange = { 
                        // 只允许输入数字，最多4位
                        cardNumber = it.filter { char -> char.isDigit() }.take(4)
                    },
                    label = { Text("卡号后4位（如有）") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 信用额度（仅信用账户显示）
            if (selectedAttribute == AccountAttribute.CREDIT_ACCOUNT) {
                OutlinedTextField(
                    value = creditLimit,
                    onValueChange = { input ->
                        creditLimit = input.filter { char -> char.isDigit() || char == '.' }
                    },
                    label = { Text("信用额度") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                Spacer(modifier = Modifier.height(8.dp))

                // 账单日和还款日（同一行）
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = billDay,
                        onValueChange = { input ->
                            val day = input.filter { char -> char.isDigit() }.toIntOrNull()?.coerceIn(1, 31) ?: 1
                            billDay = day.toString()
                        },
                        label = { Text("账单日") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = repaymentDay,
                        onValueChange = { input ->
                            val day = input.filter { char -> char.isDigit() }.toIntOrNull()?.coerceIn(1, 31) ?: 10
                            repaymentDay = day.toString()
                        },
                        label = { Text("还款日") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 备注（所有账户类型都显示）
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("备注") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 保存按钮
            Button(
                onClick = {
                    val balance = initialBalance.toDoubleOrNull() ?: 0.0
                    val limit = creditLimit.toDoubleOrNull() ?: 0.0
                    val bill = billDay.toIntOrNull() ?: 1
                    val repay = repaymentDay.toIntOrNull() ?: 10
                    
                    viewModel.addAccount(
                        name = name,
                        type = selectedType,
                        icon = selectedType.icon,
                        initialBalance = balance,
                        color = selectedColor,
                        cardNumber = cardNumber.ifBlank { null },
                        creditLimit = limit,
                        billDay = bill,
                        repaymentDay = repay,
                        isEnabled = true,
                        initialBalanceDate = initialBalanceDate,
                        purpose = note
                    )
                    onNavigateBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = name.isNotBlank()
            ) {
                Text("保存", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
    
    // 日期选择器
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = System.currentTimeMillis()
        )
        AlertDialog(
            onDismissRequest = { showDatePicker = false },
            title = { Text("选择日期") },
            text = {
                DatePicker(state = datePickerState)
            },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        initialBalanceDate = sdf.format(Date(millis))
                    }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            }
        )
    }
}

// 辅助函数
private fun getAccountIconLocal(iconName: String): ImageVector {
    return when (iconName) {
        "account_balance" -> Icons.Default.AccountBalance
        "payment" -> Icons.Default.Payment
        "wallet" -> Icons.Default.Wallet
        "credit_card" -> Icons.Default.CreditCard
        "lend" -> Icons.Default.Money
        else -> Icons.Default.AccountBalance
    }
}

private fun getAccountTypesByAttribute(attribute: AccountAttribute): List<AccountType> {
    return when (attribute) {
        AccountAttribute.CASH -> listOf(
            AccountType.BANK,
            AccountType.CASH,
            AccountType.WECHAT,
            AccountType.YUEBAO,
            AccountType.OTHER
        )
        AccountAttribute.CREDIT_ACCOUNT -> listOf(
            AccountType.CREDIT_CARD,
            AccountType.CONSUMPTION_PLATFORM
        )
        AccountAttribute.CREDIT -> listOf(
            AccountType.PERSONAL_TRANSACTION,
            AccountType.LOAN_LIABILITY
        )
    }
}
