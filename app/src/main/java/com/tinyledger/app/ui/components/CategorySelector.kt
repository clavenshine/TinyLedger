package com.tinyledger.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
    showAddButton: Boolean = true,
    transactionType: TransactionType = TransactionType.EXPENSE,
    vibrationEnabled: Boolean = false,
    onNavigateToCategoryManage: ((TransactionType) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var expandedParentId by remember { mutableStateOf<String?>(null) }

    // 分离一级分类和二级分类
    val topLevelCategories = categories.filter { it.parentId == null }
    val subCategoryMap = topLevelCategories.associateWith { parent ->
        categories.filter { it.parentId == parent.id }
    }

    // 二级分类删除回调
    var subCategoryToDelete by remember { mutableStateOf<Category?>(null) }
    // 二级分类编辑回调 - 需要父分类名称
    var subCategoryToEdit by remember { mutableStateOf<Category?>(null) }
    var parentCategoryForEdit by remember { mutableStateOf<Category?>(null) }

    Column(modifier = modifier) {
        // Non-lazy grid: display all items without scrolling
        // This avoids the crash caused by nesting LazyVerticalGrid inside verticalScroll
        val itemsPerRow = 5  // 每行5个图标
        // 将"分类管理"按钮作为最后一个元素加入列表
        val allItems = if (showAddButton && onAddCategory != null) {
            topLevelCategories + listOf(Category("category_manage", "分类管理", "settings", TransactionType.EXPENSE))
        } else {
            topLevelCategories
        }
        allItems.chunked(itemsPerRow).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { category ->
                    Box(modifier = Modifier.weight(1f)) {
                        if (category.id == "category_manage") {
                            CategoryManageButton(
                                onClick = {
                                    if (onNavigateToCategoryManage != null) {
                                        onNavigateToCategoryManage(transactionType)
                                    } else {
                                        showAddDialog = true
                                    }
                                }
                            )
                        } else {
                            CategoryItem(
                                category = category,
                                isSelected = category == selectedCategory || (selectedCategory?.parentId == category.id),
                                onClick = {
                                    val subs = subCategoryMap[category] ?: emptyList()
                                    if (subs.isNotEmpty()) {
                                        // 有子分类：切换展开/折叠；如果已展开且再次点击，选中一级分类
                                        if (expandedParentId == category.id) {
                                            expandedParentId = null
                                            onCategorySelected(category)
                                        } else {
                                            expandedParentId = category.id
                                        }
                                    } else {
                                        // 无子分类：直接选中
                                        onCategorySelected(category)
                                    }
                                },
                                vibrationEnabled = vibrationEnabled,
                                hasSubCategories = (subCategoryMap[category]?.isNotEmpty() == true)
                            )
                        }
                    }
                }
                repeat(itemsPerRow - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // 展开的二级分类列表（纵向排列）
            val expandedCategory = rowItems.find { it.id != "category_manage" && it.id == expandedParentId }
            if (expandedCategory != null) {
                val subs = subCategoryMap[expandedCategory] ?: emptyList()
                if (subs.isNotEmpty()) {
                    SubCategoryList(
                        parentCategory = expandedCategory,
                        subCategories = subs,
                        selectedCategory = selectedCategory,
                        onSubCategorySelected = { sub ->
                            onCategorySelected(sub)
                        },
                        onSubCategoryEdit = { sub ->
                            subCategoryToEdit = sub
                            parentCategoryForEdit = expandedCategory
                        },
                        onSubCategoryDelete = { sub ->
                            subCategoryToDelete = sub
                        }
                    )
                }
            }
        }
    }

    // 二级分类删除确认对话框
    subCategoryToDelete?.let { sub ->
        DeleteSubCategoryDialog(
            categoryName = sub.name,
            onConfirm = {
                onAddCategory?.let { addCb ->
                    // 通过 ViewModel 删除二级分类
                    Category.removeCustomCategory(sub)
                }
                subCategoryToDelete = null
            },
            onDismiss = { subCategoryToDelete = null }
        )
    }

    // 二级分类编辑对话框
    subCategoryToEdit?.let { sub ->
        parentCategoryForEdit?.let { parent ->
            EditSubCategoryDialog(
                subCategory = sub,
                parentCategoryName = parent.name,
                onConfirm = { newName ->
                    // 重命名二级分类
                    Category.renameCustomCategory(sub, newName)
                    subCategoryToEdit = null
                    parentCategoryForEdit = null
                },
                onDismiss = {
                    subCategoryToEdit = null
                    parentCategoryForEdit = null
                }
            )
        }
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
}

@Composable
private fun CategoryItem(
    category: Category,
    isSelected: Boolean,
    onClick: () -> Unit,
    vibrationEnabled: Boolean = false,
    hasSubCategories: Boolean = false
) {
    val haptic = LocalHapticFeedback.current
    
    Column(
        modifier = Modifier
            .aspectRatio(0.85f)
            .clip(RoundedCornerShape(12.dp))
            .clickable {
                if (vibrationEnabled) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                onClick()
            }
            .background(Color.Transparent)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
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
                imageVector = getCategoryIcon(category.icon),
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            // 有子分类时在图标右下角显示三点小图标
            if (hasSubCategories) {
                Icon(
                    imageVector = Icons.Default.MoreHoriz,
                    contentDescription = "有二级分类",
                    tint = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (-2).dp, y = (-2).dp)
                        .size(14.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        )
                        .padding(1.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = category.name,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp
            ),
            textAlign = TextAlign.Center,
            color = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            minLines = 1
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
                .size(44.dp)
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
                modifier = Modifier.size(26.dp)
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

/**
 * “分类管理”按钮 - 放在分类网格最后一个位置
 * 样式与CategoryItem一致，使用设置图标
 */
@Composable
private fun CategoryManageButton(
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .aspectRatio(0.85f)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .background(Color.Transparent)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "分类管理",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "分类管理",
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp
            ),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
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

/**
 * 二级分类纵向列表（添加记账页面专用）
 * 每行：二级分类图标 + 一级分类名称-二级分类名称 + 单选框
 * 浅色虚线分隔，无编辑/删除图标
 */
@Composable
private fun SubCategoryList(
    parentCategory: Category,
    subCategories: List<Category>,
    selectedCategory: Category?,
    onSubCategorySelected: (Category) -> Unit,
    onSubCategoryEdit: (Category) -> Unit,
    onSubCategoryDelete: (Category) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            subCategories.forEachIndexed { index, subCategory ->
                if (index > 0) {
                    // 浅色虚线分隔
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        thickness = 0.5.dp
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (subCategory == selectedCategory)
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                            else Color.Transparent
                        )
                        .clickable { onSubCategorySelected(subCategory) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 左侧：二级分类图标 + 名称
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        // 二级分类图标
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(
                                    if (subCategory == selectedCategory)
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = getCategoryIcon(subCategory.icon),
                                contentDescription = null,
                                tint = if (subCategory == selectedCategory)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        // 一级分类名称-二级分类名称
                        Text(
                            text = "${parentCategory.name}-${subCategory.name}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (subCategory == selectedCategory)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    // 右侧：单选框
                    RadioButton(
                        selected = subCategory == selectedCategory,
                        onClick = { onSubCategorySelected(subCategory) },
                        modifier = Modifier.size(24.dp),
                        colors = RadioButtonDefaults.colors(
                            selectedColor = MaterialTheme.colorScheme.primary,
                            unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    )
                }
            }
        }
    }
}

/**
 * 二级分类删除确认对话框（使用美化版 DeleteConfirmationDialog 样式）
 */
@Composable
private fun DeleteSubCategoryDialog(
    categoryName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
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
                    .graphicsLayer { shadowElevation = 16f },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                                        MaterialTheme.colorScheme.error.copy(alpha = 0.05f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                    Text(
                        text = "删除「$categoryName」？",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "删除后将无法恢复，请谨慎操作",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                        ) {
                            Text("取消", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        }
                        Button(
                            onClick = onConfirm,
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("确认删除", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 二级分类编辑对话框
 */
@Composable
private fun EditSubCategoryDialog(
    subCategory: Category,
    parentCategoryName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newName by remember { mutableStateOf(subCategory.name) }
    var duplicateError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        title = { Text("编辑二级分类", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column {
                Text(
                    text = "父分类：$parentCategoryName",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                OutlinedTextField(
                    value = newName,
                    onValueChange = {
                        newName = it
                        duplicateError = false
                    },
                    label = { Text("分类名称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = duplicateError,
                    supportingText = if (duplicateError) {
                        { Text("该分类名称已存在", color = MaterialTheme.colorScheme.error) }
                    } else null
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (newName.isNotBlank() && newName != subCategory.name) {
                        onConfirm(newName.trim())
                    }
                },
                enabled = newName.isNotBlank() && newName != subCategory.name
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
        // 支出分类
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
        "investment_expense" -> Icons.Default.TrendingDown
        "utilities" -> Icons.Default.ElectricalServices
        "accommodation" -> Icons.Default.Hotel
        "charity" -> Icons.Default.VolunteerActivism
        "send_redpacket" -> Icons.Default.CardGiftcard
        "family_living" -> Icons.Default.FamilyRestroom
        "children" -> Icons.Default.ChildCare
        "elderly_care" -> Icons.Default.Elderly
        "other" -> Icons.Default.MoreHoriz
        // 收入分类
        "salary" -> Icons.Default.Work
        "bonus" -> Icons.Default.Star
        "investment" -> Icons.Default.TrendingUp
        "financial" -> Icons.Default.AccountBalance
        "dividend" -> Icons.Default.Paid
        "refund" -> Icons.Default.AssignmentReturn
        "deposit_back" -> Icons.Default.AccountBalanceWallet
        "redpacket" -> Icons.Default.Redeem
        "reimbursement" -> Icons.Default.RequestPage
        // 借贷分类
        "lend" -> Icons.Default.Money
        "account_transfer" -> Icons.Default.SwapHoriz
        "credit_card_repay" -> Icons.Default.CreditCard
        // 旧版ic_前缀兼容
        "ic_other" -> Icons.Default.MoreHoriz
        "ic_restaurant" -> Icons.Default.Restaurant
        "ic_directions_bus" -> Icons.Default.DirectionsBus
        "ic_shopping_bag" -> Icons.Default.ShoppingBag
        "ic_local_movies" -> Icons.Default.LocalMovies
        "ic_home" -> Icons.Default.Home
        "ic_medical" -> Icons.Default.MedicalServices
        "ic_education" -> Icons.Default.School
        "ic_communication" -> Icons.Default.Phone
        "ic_insurance" -> Icons.Default.Security
        "ic_travel" -> Icons.Default.Flight
        "ic_lend" -> Icons.Default.Money
        "ic_investment_expense" -> Icons.Default.TrendingDown
        "ic_salary" -> Icons.Default.Work
        "ic_bonus" -> Icons.Default.Star
        "ic_financial" -> Icons.Default.AccountBalance
        "ic_redpacket" -> Icons.Default.Redeem
        "ic_family_living" -> Icons.Default.FamilyRestroom
        "ic_children" -> Icons.Default.ChildCare
        "ic_elderly_care" -> Icons.Default.Elderly
        else -> Icons.Default.MoreHoriz
    }
}
