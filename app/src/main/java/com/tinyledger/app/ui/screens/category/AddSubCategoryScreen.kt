package com.tinyledger.app.ui.screens.category

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tinyledger.app.domain.model.Category
import com.tinyledger.app.domain.model.TransactionType
import com.tinyledger.app.ui.components.BeautifiedAlertDialog
import com.tinyledger.app.ui.components.getCategoryIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSubCategoryScreen(
    parentCategory: Category,
    transactionType: TransactionType,
    onNavigateBack: () -> Unit = {},
    onSubCategoryCreated: (Category) -> Unit = {},
    onSaveSubCategory: ((Category) -> Unit)? = null,
    onAutoMatchTransactions: ((Category) -> Int)? = null // 智能匹配回调，返回匹配数量
) {
    var subCategoryName by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf<String?>(null) }
    var selectedGroupIndex by remember { mutableStateOf(0) }
    var showDuplicateDialog by remember { mutableStateOf(false) }
    var showMatchSuccessDialog by remember { mutableStateOf(false) }
    var matchedTransactionCount by remember { mutableStateOf(0) }
    val maxNameLength = 4

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "新建二级分类",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Button(
                    onClick = {
                        if (subCategoryName.trim().isBlank()) return@Button
                        if (selectedIcon == null) return@Button

                        // 检查分类名称是否已存在（包括一级和二级分类）
                        val existingCategories = Category.getCategoriesByType(transactionType)
                        val isDuplicate = existingCategories.any {
                            it.name.equals(subCategoryName.trim(), ignoreCase = true)
                        }
                        if (isDuplicate) {
                            showDuplicateDialog = true
                            return@Button
                        }

                        val newSubCategory = Category.addSubCategory(
                            name = subCategoryName.trim(),
                            type = transactionType,
                            parentId = parentCategory.id,
                            icon = selectedIcon!!
                        )
                        
                        // 调用保存回调
                        if (onSaveSubCategory != null) {
                            onSaveSubCategory(newSubCategory)
                        }
                        
                        // 执行智能匹配
                        if (onAutoMatchTransactions != null) {
                            val matchedCount = onAutoMatchTransactions(newSubCategory)
                            if (matchedCount > 0) {
                                matchedTransactionCount = matchedCount
                                showMatchSuccessDialog = true
                            }
                        }
                        
                        onSubCategoryCreated(newSubCategory)
                        onNavigateBack()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(24.dp)),
                    enabled = subCategoryName.trim().isNotBlank() && selectedIcon != null,
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text("保存", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
        ) {
            // 父分类提示
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "上级分类：",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = getCategoryIcon(parentCategory.icon),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    parentCategory.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                "分类名称",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
            )

            OutlinedTextField(
                value = subCategoryName,
                onValueChange = {
                    if (it.length <= maxNameLength) {
                        subCategoryName = it
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                placeholder = { Text("请输入分类名称") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                trailingIcon = {
                    Text(
                        text = "${subCategoryName.length}/$maxNameLength",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (subCategoryName.length > maxNameLength)
                            MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                "分类图标",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 300.dp, max = 450.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .width(72.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .verticalScroll(rememberScrollState())
                    ) {
                        iconGroups.forEachIndexed { index, group ->
                            val isSelected = selectedGroupIndex == index
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedGroupIndex = index }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = group.name,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (isSelected) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Box(
                                            modifier = Modifier
                                                .width(24.dp)
                                                .height(2.dp)
                                                .clip(RoundedCornerShape(1.dp))
                                                .background(MaterialTheme.colorScheme.primary)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(12.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        val currentGroup = iconGroups[selectedGroupIndex]
                        Text(
                            text = currentGroup.name,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            modifier = Modifier.padding(bottom = 8.dp),
                            color = MaterialTheme.colorScheme.primary
                        )

                        currentGroup.icons.chunked(5).forEach { rowIcons ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                rowIcons.forEach { icon ->
                                    val isSelected = selectedIcon == icon
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(vertical = 4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.surfaceVariant
                                                )
                                                .clickable { selectedIcon = icon },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = getCategoryIcon(icon),
                                                contentDescription = null,
                                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                }
                                repeat(5 - rowIcons.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
    
    // 分类名称重复提示对话框
    if (showDuplicateDialog) {
        BeautifiedAlertDialog(
            title = "分类名称已存在",
            message = "该分类名称已被使用，请使用其他名称",
            onDismiss = { showDuplicateDialog = false },
            icon = "🏷️",
            iconColor = Color(0xFFFF9800)
        )
    }
    
    // 智能匹配成功提示对话框
    if (showMatchSuccessDialog) {
        BeautifiedAlertDialog(
            title = "智能匹配成功",
            message = "已智能匹配 $matchedTransactionCount 笔交易到新分类",
            onDismiss = { showMatchSuccessDialog = false },
            icon = "✅",
            iconColor = Color(0xFF4CAF50)
        )
    }
}
