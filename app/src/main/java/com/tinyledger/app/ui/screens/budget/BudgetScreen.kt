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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
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
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
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
                        color = MaterialTheme.colorScheme.surface
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
                        color = MaterialTheme.colorScheme.surface
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
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
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
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                Box(modifier = Modifier.width(1.dp).height(36.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)))
                StatItem("超额支出", "${uiState.currencySymbol} ${String.format("%.2f", uiState.overBudget)}")
                Box(modifier = Modifier.width(1.dp).height(36.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)))
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .scale(scale.value)
                    .graphicsLayer {
                        shadowElevation = 16f
                    },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // 标题
                    Text(
                        text = if (isEditing) "修改预算" else "设置预算",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (isEditing) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = IOSColors.SystemOrange.copy(alpha = 0.1f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = IOSColors.SystemOrange,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "注意：修改后将计入修改次数",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = IOSColors.SystemOrange
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Total budget display
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                        ),
                        border = BorderStroke(
                            2.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "总预算",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }
                            Text(
                                "${currencySymbol} ${String.format("%.2f", totalAmount)}",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        "支出分类预算",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Category list
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        expenseCategories.forEach { category ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = category.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    modifier = Modifier.width(80.dp),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                OutlinedTextField(
                                    value = amountMap[category.id] ?: "",
                                    onValueChange = { input ->
                                        if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                            amountMap[category.id] = input
                                        }
                                    },
                                    modifier = Modifier.weight(1f).height(52.dp),
                                    prefix = { Text(currencySymbol, fontSize = 14.sp) },
                                    placeholder = { Text("0.00", fontSize = 14.sp) },
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 按钮区域
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 取消按钮
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            border = BorderStroke(
                                2.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            )
                        ) {
                            Text(
                                "取消",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        
                        // 确定按钮
                        Button(
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
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 8.dp,
                                pressedElevation = 4.dp
                            ),
                            enabled = totalAmount > 0
                        ) {
                            Text(
                                "确定",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
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
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // 标题
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    // 图标
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                    
                    // 设置内容
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 总预算使用提醒开关
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.NotificationsActive,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        "总预算使用提醒",
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                                    )
                                }
                                Switch(
                                    checked = enabled,
                                    onCheckedChange = { enabled = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                                        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                )
                            }
                        }
                        
                        // 提醒比例输入
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Percent,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        "提醒比例",
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
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
                                            textAlign = TextAlign.End,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "%",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
                        }
                    }
                    
                    // 按钮区域
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 取消按钮
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            border = BorderStroke(
                                2.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            )
                        ) {
                            Text(
                                "取消",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        
                        // 确定按钮
                        Button(
                            onClick = {
                                val pct = percentText.toIntOrNull()?.coerceIn(1, 100) ?: 80
                                onConfirm(enabled, pct)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 8.dp,
                                pressedElevation = 4.dp
                            )
                        ) {
                            Text(
                                "确定",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
