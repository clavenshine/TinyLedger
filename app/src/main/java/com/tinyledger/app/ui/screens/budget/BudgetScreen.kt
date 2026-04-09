package com.tinyledger.app.ui.screens.budget

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tinyledger.app.domain.model.Category
import com.tinyledger.app.domain.model.TransactionType
import com.tinyledger.app.ui.theme.IOSColors
import com.tinyledger.app.ui.viewmodel.BudgetUiState
import com.tinyledger.app.ui.viewmodel.BudgetViewModel
import com.tinyledger.app.ui.viewmodel.BudgetViewMode
import com.tinyledger.app.ui.viewmodel.CategoryBudgetItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: BudgetViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showBudgetDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("预算管理", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "返回", modifier = Modifier.size(28.dp))
                    }
                },
                actions = {
                    TextButton(onClick = { showSettingsDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("预算设置", color = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFFF5F5F5)
                )
            )
        },
        containerColor = Color(0xFFF5F5F5)
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Period selector
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("¥", color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold, fontSize = 14.sp,
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("默认账本", style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { viewModel.previousPeriod() }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.ChevronLeft, contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary)
                            }
                            Text(
                                text = if (uiState.viewMode == BudgetViewMode.MONTHLY)
                                    "${uiState.selectedYear}年${uiState.selectedMonth}月"
                                else "${uiState.selectedYear}年",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                            )
                            IconButton(onClick = { viewModel.nextPeriod() }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.ChevronRight, contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }

            // View mode toggle
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8E8E8))
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                        listOf("月预算" to BudgetViewMode.MONTHLY, "年预算" to BudgetViewMode.YEARLY).forEach { (label, mode) ->
                            val isSelected = uiState.viewMode == mode
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.setViewMode(mode) },
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                            ) {
                                Text(
                                    text = label,
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Budget summary card
            item {
                BudgetSummaryCard(uiState = uiState, onEditBudget = { showBudgetDialog = true })
            }

            // Category budget list
            if (uiState.budgetExists && uiState.categoryBudgets.isNotEmpty()) {
                item {
                    Text(
                        "分类预算",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                itemsIndexed(uiState.categoryBudgets) { _, item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(item.categoryName, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "${uiState.currencySymbol} ${String.format("%.2f", item.amount)}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Modify hint
            if (uiState.budgetExists && uiState.modifyHint.isNotBlank()) {
                item {
                    Text(
                        text = "*${uiState.modifyHint}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }

            // Set budget prompt when no budget
            if (!uiState.budgetExists) {
                item {
                    Text(
                        text = "*当前总预算为${uiState.currencySymbol}0.00，无法进行设置操作哦~",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                    )
                }
            }

            // Add/Set budget button
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { showBudgetDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    enabled = !uiState.budgetExists || uiState.canModify
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (uiState.budgetExists) "修改预算" else "设置预算")
                }
            }
        }
    }

    // Budget amount dialog with category selection
    if (showBudgetDialog) {
        BudgetCategoryDialog(
            existingBudgets = uiState.categoryBudgets,
            currencySymbol = uiState.currencySymbol,
            isEditing = uiState.budgetExists,
            onConfirm = { categoryBudgets ->
                viewModel.saveBudget(categoryBudgets)
                showBudgetDialog = false
            },
            onDismiss = { showBudgetDialog = false }
        )
    }

    // Budget settings dialog
    if (showSettingsDialog) {
        BudgetSettingsDialog(
            title = if (uiState.viewMode == BudgetViewMode.MONTHLY) "月预算设置" else "年预算设置",
            reminderEnabled = uiState.reminderEnabled,
            reminderPercentage = uiState.reminderPercentage,
            onConfirm = { enabled, percentage ->
                viewModel.updateBudgetSettings(enabled, percentage)
                showSettingsDialog = false
            },
            onDismiss = { showSettingsDialog = false }
        )
    }
}

@Composable
private fun BudgetSummaryCard(
    uiState: BudgetUiState,
    onEditBudget: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular progress
                Box(
                    modifier = Modifier.size(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { (uiState.usagePercent / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier.size(80.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        strokeWidth = 6.dp,
                        strokeCap = StrokeCap.Round
                    )
                    Text(
                        text = if (uiState.budgetExists) "${uiState.usagePercent}%" else "--%",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.width(20.dp))

                // Budget info
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("剩余预算", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "${uiState.currencySymbol} ${String.format("%.2f", uiState.remaining)}",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Column(horizontalAlignment = Alignment.End) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("总预算", style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (uiState.canModify) {
                                    IconButton(onClick = onEditBudget, modifier = Modifier.size(20.dp)) {
                                        Icon(Icons.Default.Edit, contentDescription = "编辑",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                            modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                            Text(
                                text = "${uiState.currencySymbol} ${String.format("%.2f", uiState.totalBudget)}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFFF0F0F0))
            Spacer(modifier = Modifier.height(12.dp))

            // Bottom stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("已支出", "${uiState.currencySymbol} ${String.format("%.2f", uiState.spent)}")
                Box(modifier = Modifier.width(1.dp).height(36.dp).background(Color(0xFFE8E8E8)))
                StatItem("超额支出", "${uiState.currencySymbol} ${String.format("%.2f", uiState.overBudget)}")
                Box(modifier = Modifier.width(1.dp).height(36.dp).background(Color(0xFFE8E8E8)))
                StatItem("日均消费", "${uiState.currencySymbol} ${String.format("%.2f", uiState.dailyAvg)}")
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
    }
}

/**
 * 预算分类设置弹窗：列出所有支出分类并允许填写每个分类的预算金额
 */
@Composable
private fun BudgetCategoryDialog(
    existingBudgets: List<CategoryBudgetItem>,
    currencySymbol: String,
    isEditing: Boolean,
    onConfirm: (List<CategoryBudgetItem>) -> Unit,
    onDismiss: () -> Unit
) {
    val expenseCategories = Category.getCategoriesByType(TransactionType.EXPENSE)

    // 初始化每个分类的金额
    val amountMap = remember {
        mutableStateMapOf<String, String>().apply {
            expenseCategories.forEach { cat ->
                val existing = existingBudgets.find { it.categoryId == cat.id }
                this[cat.id] = if (existing != null && existing.amount > 0) {
                    String.format("%.2f", existing.amount)
                } else {
                    ""
                }
            }
        }
    }

    // 计算总金额
    val totalAmount = amountMap.values.sumOf { it.toDoubleOrNull() ?: 0.0 }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = if (isEditing) "修改预算" else "设置预算",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
            ) {
                if (isEditing) {
                    Text(
                        text = "注意：修改后将计入修改次数",
                        style = MaterialTheme.typography.bodySmall,
                        color = IOSColors.SystemOrange,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // Total budget display
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("总预算", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                        Text(
                            "${currencySymbol} ${String.format("%.2f", totalAmount)}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    "支出分类预算",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Category list
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    expenseCategories.forEach { category ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = category.name,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.width(80.dp)
                            )
                            OutlinedTextField(
                                value = amountMap[category.id] ?: "",
                                onValueChange = { input ->
                                    if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                        amountMap[category.id] = input
                                    }
                                },
                                modifier = Modifier.weight(1f).height(52.dp),
                                prefix = { Text(currencySymbol, fontSize = 13.sp) },
                                placeholder = { Text("0.00", fontSize = 13.sp) },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val results = expenseCategories.map { cat ->
                        CategoryBudgetItem(
                            categoryId = cat.id,
                            categoryName = cat.name,
                            amount = amountMap[cat.id]?.toDoubleOrNull() ?: 0.0
                        )
                    }.filter { it.amount > 0 }
                    if (results.isNotEmpty()) {
                        onConfirm(results)
                    }
                },
                enabled = totalAmount > 0
            ) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun BudgetSettingsDialog(
    title: String,
    reminderEnabled: Boolean,
    reminderPercentage: Int,
    onConfirm: (Boolean, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var enabled by remember { mutableStateOf(reminderEnabled) }
    var percentText by remember { mutableStateOf(reminderPercentage.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("总预算使用提醒", style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = enabled, onCheckedChange = { enabled = it })
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("提醒比例", style = MaterialTheme.typography.bodyLarge)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = percentText,
                            onValueChange = { input ->
                                if (input.isEmpty() || (input.all { it.isDigit() } && input.length <= 3)) {
                                    percentText = input
                                }
                            },
                            modifier = Modifier.width(80.dp),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.End
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("%", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val pct = percentText.toIntOrNull()?.coerceIn(1, 100) ?: 80
                onConfirm(enabled, pct)
            }) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
