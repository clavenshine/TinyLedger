package com.tinyledger.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.tinyledger.app.domain.model.Category
import com.tinyledger.app.domain.model.TransactionType
import com.tinyledger.app.ui.theme.IOSColors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CategorySelector(
    categories: List<Category>,
    selectedCategory: Category?,
    onCategorySelected: (Category) -> Unit,
    onAddCategory: ((String) -> Unit)? = null,
    onDeleteCategory: ((Category) -> Unit)? = null,
    onRenameCategory: ((Category, String) -> Unit)? = null,
    showAddButton: Boolean = true,
    transactionType: TransactionType = TransactionType.EXPENSE,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<Category?>(null) }
    var showEditDialog by remember { mutableStateOf<Category?>(null) }
    var showActionMenu by remember { mutableStateOf<Category?>(null) }

    Column(modifier = modifier) {
        // Non-lazy grid: display all items without scrolling
        // This avoids the crash caused by nesting LazyVerticalGrid inside verticalScroll
        val itemsPerRow = 4
        categories.chunked(itemsPerRow).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { category ->
                    Box(modifier = Modifier.weight(1f)) {
                        CategoryItem(
                            category = category,
                            isSelected = category == selectedCategory,
                            onClick = { onCategorySelected(category) },
                            onLongClick = {
                                if (onDeleteCategory != null || onRenameCategory != null) {
                                    showActionMenu = category
                                }
                            }
                        )
                    }
                }
                // Fill empty slots in the last row
                repeat(itemsPerRow - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Add category button row
        if (showAddButton && onAddCategory != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    AddCategoryButton(
                        onClick = { showAddDialog = true }
                    )
                }
                repeat(itemsPerRow - 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }

    // Animated action menu (Edit / Delete)
    showActionMenu?.let { category ->
        CategoryActionMenu(
            category = category,
            onEdit = {
                showActionMenu = null
                showEditDialog = category
            },
            onDelete = {
                showActionMenu = null
                showDeleteDialog = category
            },
            onDismiss = { showActionMenu = null }
        )
    }

    // Delete confirmation dialog with 10-second countdown
    showDeleteDialog?.let { categoryToDelete ->
        DeleteCategoryDialog(
            categoryName = categoryToDelete.name,
            onConfirm = {
                onDeleteCategory?.invoke(categoryToDelete)
                showDeleteDialog = null
            },
            onDismiss = { showDeleteDialog = null }
        )
    }

    if (showAddDialog && onAddCategory != null) {
        AddCategoryDialog(
            transactionType = transactionType,
            existingCategories = categories,
            onDismiss = { showAddDialog = false },
            onConfirm = { name ->
                onAddCategory(name)
                showAddDialog = false
            }
        )
    }

    // Edit category dialog
    showEditDialog?.let { category ->
        EditCategoryDialog(
            category = category,
            existingCategories = categories,
            onDismiss = { showEditDialog = null },
            onConfirm = { newName ->
                onRenameCategory?.invoke(category, newName)
                showEditDialog = null
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CategoryItem(
    category: Category,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    val animatedBorderWidth by animateDpAsState(
        targetValue = if (isSelected) 2.dp else 1.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "border_width"
    )
    
    Column(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                else MaterialTheme.colorScheme.surface
            )
            .border(
                width = animatedBorderWidth,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = getIconFromName(category.icon),
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = category.name,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            color = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}

@Composable
private fun AddCategoryButton(
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "add_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "add_scale"
    )
    
    Column(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .background(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "新增分类",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "新增",
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddCategoryDialog(
    transactionType: TransactionType,
    existingCategories: List<Category>,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var categoryName by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf(categoryIcons.first()) }
    var duplicateError by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    
    val isExpense = transactionType == TransactionType.EXPENSE
    
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                "新增${if (isExpense) "支出" else "收入"}分类",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = categoryName,
                    onValueChange = { 
                        categoryName = it
                        duplicateError = existingCategories.any { c -> c.name == it.trim() }
                    },
                    label = { Text("分类名称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (categoryName.isNotBlank() && !duplicateError) {
                                onConfirm(categoryName)
                            }
                        }
                    ),
                    leadingIcon = {
                        Icon(
                            imageVector = selectedIcon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    isError = duplicateError,
                    supportingText = if (duplicateError) {
                        { Text("该分类名称已存在，请重新输入分类名称", color = MaterialTheme.colorScheme.error) }
                    } else null
                )
                
                Text(
                    "选择图标",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // 图标选择网格
                Box(modifier = Modifier.height(150.dp)) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(6),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(categoryIcons) { icon ->
                            IconOption(
                                icon = icon,
                                isSelected = selectedIcon == icon,
                                onClick = { selectedIcon = icon }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (categoryName.isNotBlank() && !duplicateError) {
                        onConfirm(categoryName)
                    }
                },
                enabled = categoryName.isNotBlank() && !duplicateError
            ) {
                Text("添加")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun DeleteCategoryDialog(
    categoryName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var countdown by remember { mutableIntStateOf(10) }
    val canConfirm = countdown <= 0
    
    LaunchedEffect(Unit) {
        while (countdown > 0) {
            kotlinx.coroutines.delay(1000)
            countdown--
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        title = { Text("删除分类", fontWeight = FontWeight.Bold) },
        text = {
            Text("你确认要删除此分类类型吗？")
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = canConfirm
            ) {
                Text(
                    if (canConfirm) "确认删除" else "确认删除 (${countdown}s)",
                    color = if (canConfirm) MaterialTheme.colorScheme.error
                           else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun CategoryActionMenu(
    category: Category,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    // Semi-transparent overlay
    Dialog(onDismissRequest = onDismiss) {
        // Animated menu with shake effect
        val infiniteTransition = rememberInfiniteTransition(label = "shake")
        val shakeOffset by infiniteTransition.animateFloat(
            initialValue = -1f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(50),
                repeatMode = RepeatMode.Reverse
            ),
            label = "shake"
        )
        val scale by animateFloatAsState(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessHigh
            ),
            label = "scale"
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .scale(scale)
                .offset(x = with(LocalDensity.current) { shakeOffset.toDp() * 0.3f }),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                HorizontalDivider()
                // Edit option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEdit() }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "编辑",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                HorizontalDivider()
                // Delete option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDelete() }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "删除",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditCategoryDialog(
    category: Category,
    existingCategories: List<Category>,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var categoryName by remember { mutableStateOf(category.name) }
    var charLimitError by remember { mutableStateOf(false) }
    var duplicateError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text("编辑分类", style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column {
                OutlinedTextField(
                    value = categoryName,
                    onValueChange = {
                        categoryName = it
                        charLimitError = it.trim().length > 4
                        duplicateError = existingCategories.any { c -> c.name == it.trim() && c.id != category.id }
                    },
                    label = { Text("分类名称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = charLimitError || duplicateError,
                    supportingText = when {
                        charLimitError -> {{ Text("分类名称不得超过4个字", color = MaterialTheme.colorScheme.error) }}
                        duplicateError -> {{ Text("该分类名称已存在，请重新输入", color = MaterialTheme.colorScheme.error) }}
                        else -> null
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (categoryName.isNotBlank() && !charLimitError && !duplicateError) {
                        onConfirm(categoryName.trim())
                    }
                },
                enabled = categoryName.isNotBlank() && !charLimitError && !duplicateError
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun IconOption(
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

// 常用图标列表
private val categoryIcons = listOf(
    Icons.Default.Restaurant,       // 餐饮
    Icons.Default.DirectionsBus,    // 交通
    Icons.Default.ShoppingBag,      // 购物
    Icons.Default.LocalMovies,      // 娱乐
    Icons.Default.Home,             // 居住
    Icons.Default.LocalHospital,    // 医疗
    Icons.Default.School,           // 教育
    Icons.Default.Phone,            // 通讯
    Icons.Default.Savings,          // 储蓄
    Icons.Default.FitnessCenter,    // 运动
    Icons.Default.Pets,             // 宠物
    Icons.Default.ChildCare,       // 孩子
    Icons.Default.Work,             // 工作
    Icons.Default.Payments,        // 工资
    Icons.Default.CardGiftcard,     // 礼物
    Icons.Default.AttachMoney,     // 投资
    Icons.Default.Money,            // 红包
    Icons.Default.MoreHoriz        // 其他
)

private fun getIconFromName(iconName: String): ImageVector {
    return when (iconName) {
        "restaurant" -> Icons.Default.Restaurant
        "directions_bus" -> Icons.Default.DirectionsBus
        "shopping_bag" -> Icons.Default.ShoppingBag
        "local_movies" -> Icons.Default.LocalMovies
        "home" -> Icons.Default.Home
        "medical" -> Icons.Default.LocalHospital
        "education" -> Icons.Default.School
        "communication" -> Icons.Default.Phone
        "insurance" -> Icons.Default.Security
        "travel" -> Icons.Default.Flight
        "lend" -> Icons.Default.Send
        "investment_expense" -> Icons.Default.TrendingDown
        "other" -> Icons.Default.MoreHoriz
        "salary" -> Icons.Default.Payments
        "bonus" -> Icons.Default.CardGiftcard
        "investment" -> Icons.Default.TrendingUp
        "financial" -> Icons.Default.AccountBalance
        "redpacket" -> Icons.Default.Mail
        "recover_loan" -> Icons.Default.CallReceived
        "family_living" -> Icons.Default.FamilyRestroom
        "ic_other" -> Icons.Default.MoreHoriz
        "ic_restaurant" -> Icons.Default.Restaurant
        "ic_directions_bus" -> Icons.Default.DirectionsBus
        "ic_shopping_bag" -> Icons.Default.ShoppingBag
        "ic_local_movies" -> Icons.Default.LocalMovies
        "ic_home" -> Icons.Default.Home
        "ic_medical" -> Icons.Default.LocalHospital
        "ic_education" -> Icons.Default.School
        "ic_communication" -> Icons.Default.Phone
        "ic_insurance" -> Icons.Default.Security
        "ic_travel" -> Icons.Default.Flight
        "ic_lend" -> Icons.Default.Send
        "ic_investment_expense" -> Icons.Default.TrendingDown
        "ic_salary" -> Icons.Default.Payments
        "ic_bonus" -> Icons.Default.CardGiftcard
        "ic_financial" -> Icons.Default.AccountBalance
        "ic_redpacket" -> Icons.Default.Mail
        "ic_recover_loan" -> Icons.Default.CallReceived
        else -> Icons.Default.MoreHoriz
    }
}
