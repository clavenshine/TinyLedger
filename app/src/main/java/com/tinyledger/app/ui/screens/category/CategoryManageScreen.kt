package com.tinyledger.app.ui.screens.category

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tinyledger.app.domain.model.Category
import com.tinyledger.app.domain.model.TransactionType
import com.tinyledger.app.ui.components.getCategoryIcon
import com.tinyledger.app.ui.theme.IOSColors
import com.tinyledger.app.ui.viewmodel.AddTransactionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManageScreen(
    transactionType: TransactionType = TransactionType.EXPENSE,
    onNavigateBack: () -> Unit = {},
    onNavigateToAddCategory: (TransactionType) -> Unit = {},
    viewModel: AddTransactionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var currentType by remember { mutableStateOf(transactionType) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<Category?>(null) }
    var showEditDialog by remember { mutableStateOf<Category?>(null) }
    var showActionMenu by remember { mutableStateOf<Category?>(null) }

    val categories = if (currentType == TransactionType.EXPENSE) {
        Category.getCategoriesByType(TransactionType.EXPENSE)
    } else {
        Category.getCategoriesByType(TransactionType.INCOME)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "分类管理",
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
        },
        bottomBar = {
            // 新建一级分类按钮
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
                        .background(MaterialTheme.colorScheme.onSurface)
                        .clickable { onNavigateToAddCategory(currentType) },  // 跳转到新建分类页面
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "新建一级分类",
                        color = MaterialTheme.colorScheme.surface,
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
            // 支出/收入切换
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 支出按钮
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            if (currentType == TransactionType.EXPENSE)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                        .clickable { currentType = TransactionType.EXPENSE },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "支出",
                        color = if (currentType == TransactionType.EXPENSE)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                // 收入按钮
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            if (currentType == TransactionType.INCOME)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                        .clickable { currentType = TransactionType.INCOME },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "收入",
                        color = if (currentType == TransactionType.INCOME)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 分类网格
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(items = categories, key = { it.id }) { category ->
                    CategoryManageItem(
                        category = category,
                        onLongClick = { showActionMenu = category }
                    )
                }
            }
        }
    }

    // 操作菜单
    showActionMenu?.let { category ->
        CategoryActionMenuSheet(
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

    // 删除确认
    showDeleteDialog?.let { categoryToDelete ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("删除分类") },
            text = { Text("确定要删除「${categoryToDelete.name}」吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCategory(categoryToDelete)
                        showDeleteDialog = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = IOSColors.SystemRed)
                ) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) { Text("取消") }
            }
        )
    }

    // 新建分类对话框
    if (showAddDialog) {
        AddCategoryManageDialog(
            transactionType = currentType,
            existingCategories = categories,
            onDismiss = { showAddDialog = false },
            onConfirm = { name ->
                viewModel.addCategory(name)
                showAddDialog = false
            }
        )
    }

    // 编辑分类对话框
    showEditDialog?.let { category ->
        EditCategoryManageDialog(
            category = category,
            existingCategories = categories,
            onDismiss = { showEditDialog = null },
            onConfirm = { newName ->
                viewModel.renameCategory(category, newName)
                showEditDialog = null
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CategoryManageItem(
    category: Category,
    onLongClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(
                onClick = {},
                onLongClick = onLongClick
            )
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = getCategoryIcon(category.icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = category.name,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryActionMenuSheet(
    category: Category,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = category.name,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable(onClick = onEdit)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Text("编辑分类", color = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .clickable(onClick = onDelete)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.width(12.dp))
                Text("删除分类", color = MaterialTheme.colorScheme.error)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AddCategoryManageDialog(
    transactionType: TransactionType,
    existingCategories: List<Category>,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    val typeName = if (transactionType == TransactionType.EXPENSE) "支出" else "收入"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建${typeName}分类") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("分类名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name) },
                enabled = name.isNotBlank()
            ) { Text("确认") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun EditCategoryManageDialog(
    category: Category,
    existingCategories: List<Category>,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(category.name) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑分类") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("分类名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name) },
                enabled = name.isNotBlank()
            ) { Text("确认") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
