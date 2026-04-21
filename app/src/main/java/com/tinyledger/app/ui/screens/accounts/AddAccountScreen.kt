package com.tinyledger.app.ui.screens.accounts

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.border

@OptIn(ExperimentalMaterial3Api::class, kotlin.ExperimentalStdlibApi::class)
@Composable
fun AddAccountScreen(
    initialAccountType: Int = 0, // 0: 现金账户, 1: 信用账户, 2: 外部往来
    onNavigateBack: () -> Unit = {},
    viewModel: AccountViewModel = hiltViewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var name by remember { mutableStateOf("") }
    // 根据传入的initialAccountType设置默认选中的账户类别
    val initialAttribute = when (initialAccountType) {
        0 -> AccountAttribute.CASH // 现金账户
        1 -> AccountAttribute.CREDIT_ACCOUNT // 信用账户
        2 -> AccountAttribute.CREDIT // 外部往来
        else -> AccountAttribute.CASH
    }
    var selectedAttribute by remember { mutableStateOf(initialAttribute) }
    var selectedType by remember { mutableStateOf(AccountType.BANK) }
    var initialBalance by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(accountColors[0]) }
    var cardNumber by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    
    // Credit account fields
    var creditLimit by remember { mutableStateOf("") }
    var billDay by remember { mutableStateOf("1") }
    var repaymentDay by remember { mutableStateOf("8") }
    
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
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .padding(top = 24.dp)
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

                // 账单日和还款日（同一行，使用数字滚动选择器）
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 账单日
                    NumberPickerField(
                        label = "账单日",
                        value = billDay.toIntOrNull() ?: 1,
                        onValueChange = { billDay = it.toString() },
                        modifier = Modifier.weight(1f),
                        range = 1..31
                    )
                    
                    // 还款日
                    NumberPickerField(
                        label = "还款日",
                        value = repaymentDay.toIntOrNull() ?: 8,
                        onValueChange = { repaymentDay = it.toString() },
                        modifier = Modifier.weight(1f),
                        range = 1..31
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
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 选择颜色
            Text(
                text = "选择颜色",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(accountColors.size) { index ->
                    val colorStr = accountColors[index]
                    val color = Color(android.graphics.Color.parseColor(colorStr))
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(color = color)
                            .border(
                                width = if (colorStr == selectedColor) 3.dp else 0.dp,
                                color = if (colorStr == selectedColor) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable {
                                selectedColor = colorStr
                            }
                    )
                }
            }

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
                    
                    // 如果声音设置已开启，播放“咻”提示音
                    if (com.tinyledger.app.data.notification.TransactionNotificationService.isSoundEnabled(context)) {
                        com.tinyledger.app.data.notification.TransactionNotificationHelper.playWaterDropSound()
                    }
                    
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
    
    // 日期选择器 - 自定义日历样式
    if (showDatePicker) {
        val currentDate = remember { 
            val cal = Calendar.getInstance()
            Triple(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
        }
        
        // 解析已保存的日期
        val savedDate = remember(initialBalanceDate) {
            if (initialBalanceDate.isNotBlank()) {
                try {
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val date = sdf.parse(initialBalanceDate)
                    val cal = Calendar.getInstance()
                    cal.time = date
                    Triple(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
                } catch (e: Exception) {
                    null
                }
            } else {
                null
            }
        }
        
        var selectedYear by remember { mutableStateOf(savedDate?.first ?: currentDate.first) }
        var selectedMonth by remember { mutableStateOf(savedDate?.second ?: currentDate.second) }
        var selectedDay by remember { mutableStateOf(savedDate?.third ?: currentDate.third) }
        
        ModalBottomSheet(
            onDismissRequest = { showDatePicker = false },
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            windowInsets = WindowInsets(0, 0, 0, 0),
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = null
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // 顶部导航栏：年份、月份、左右箭头
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // 左箭头
                    IconButton(onClick = {
                        if (selectedMonth == 1) {
                            selectedMonth = 12
                            selectedYear--
                        } else {
                            selectedMonth--
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowLeft,
                            contentDescription = "上个月",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    // 年份和月份
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${selectedYear}年",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        ) {
                            Text(
                                text = "${selectedMonth}月",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }
                    
                    // 右箭头
                    IconButton(onClick = {
                        if (selectedMonth == 12) {
                            selectedMonth = 1
                            selectedYear++
                        } else {
                            selectedMonth++
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = "下个月",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 星期标题
                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf("一", "二", "三", "四", "五", "六", "日").forEach { day ->
                        Text(
                            text = day,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (day == "六" || day == "日") 
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 日历网格
                val cal = Calendar.getInstance()
                cal.set(selectedYear, selectedMonth - 1, 1)
                val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1 // 0=Sunday, 转换为一=0
                val adjustedFirstDay = if (firstDayOfWeek == 0) 6 else firstDayOfWeek - 1 // 调整为周一开始
                val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                
                val totalCells = adjustedFirstDay + daysInMonth
                val rows = (totalCells + 6) / 7
                
                for (row in 0 until rows) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        for (col in 0..6) {
                            val cellIndex = row * 7 + col
                            val day = cellIndex - adjustedFirstDay + 1
                            
                            if (day in 1..daysInMonth) {
                                val isSelected = selectedDay == day
                                val isToday = currentDate.first == selectedYear && 
                                             currentDate.second == selectedMonth && 
                                             currentDate.third == day
                                
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .padding(2.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else Color.Transparent
                                        )
                                        .let { modifier ->
                                            if (isToday && !isSelected) {
                                                modifier.border(
                                                    width = 2.dp,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                            } else {
                                                modifier
                                            }
                                        }
                                        .clickable {
                                            selectedDay = day
                                            // 自动确认选择
                                            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                            val selectedCal = Calendar.getInstance()
                                            selectedCal.set(selectedYear, selectedMonth - 1, day)
                                            initialBalanceDate = sdf.format(selectedCal.time)
                                            showDatePicker = false
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$day",
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = when {
                                            isSelected -> MaterialTheme.colorScheme.onPrimary
                                            isToday -> MaterialTheme.colorScheme.primary
                                            else -> MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
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

/**
 * 数字滚动选择器字段 - 与OutlinedTextField高度一致
 */
@Composable
private fun NumberPickerField(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    range: IntRange
) {
    Column(modifier = modifier) {
        // 标签
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        
        // 选择器容器 - 与OutlinedTextField高度一致
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp), // 与OutlinedTextField默认高度一致
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 减少按钮
                IconButton(
                    onClick = {
                        if (value > range.first) {
                            onValueChange(value - 1)
                        }
                    },
                    enabled = value > range.first,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "减少",
                        tint = if (value > range.first) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }
                
                // 显示当前值
                Text(
                    text = value.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                
                // 增加按钮
                IconButton(
                    onClick = {
                        if (value < range.last) {
                            onValueChange(value + 1)
                        }
                    },
                    enabled = value < range.last,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "增加",
                        tint = if (value < range.last) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}

/**
 * 日期选择器对话框 - 用于选择账单日/还款日（1-31）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayPickerDialog(
    title: String,
    currentDay: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var selectedDay by remember { mutableStateOf(currentDay) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            // 使用 LazyGrid 显示 1-31 的日期选项
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier.height(200.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(31) { index ->
                    val day = index + 1
                    FilterChip(
                        selected = selectedDay == day,
                        onClick = { selectedDay = day },
                        label = {
                            Text(
                                text = day.toString(),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedDay) },
                enabled = selectedDay in 1..31
            ) {
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
