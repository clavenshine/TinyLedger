package com.tinyledger.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tinyledger.app.domain.model.Category
import com.tinyledger.app.domain.model.TransactionType
import com.tinyledger.app.ui.theme.IOSColors

@Composable
fun CategorySelector(
    categories: List<Category>,
    selectedCategory: Category?,
    onCategorySelected: (Category) -> Unit,
    onAddCategory: ((String) -> Unit)? = null,
    showAddButton: Boolean = true,
    transactionType: TransactionType = TransactionType.EXPENSE,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }

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
                            onClick = { onCategorySelected(category) }
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

    if (showAddDialog && onAddCategory != null) {
        AddCategoryDialog(
            transactionType = transactionType,
            onDismiss = { showAddDialog = false },
            onConfirm = { name ->
                onAddCategory(name)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun CategoryItem(
    category: Category,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val animatedBorderWidth by animateDpAsState(
        targetValue = if (isSelected) 2.dp else 1.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "border_width"
    )
    
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
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
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
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
                modifier = Modifier.size(24.dp)
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
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
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
                modifier = Modifier.size(24.dp)
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
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var categoryName by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf(categoryIcons.first()) }
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
                    onValueChange = { categoryName = it },
                    label = { Text("分类名称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (categoryName.isNotBlank()) {
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
                    }
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
                    if (categoryName.isNotBlank()) {
                        onConfirm(categoryName)
                    }
                },
                enabled = categoryName.isNotBlank()
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
