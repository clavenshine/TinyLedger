package com.tinyledger.app.ui.screens.bills

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tinyledger.app.domain.model.Transaction
import com.tinyledger.app.domain.model.TransactionType
import com.tinyledger.app.ui.components.TransactionCard
import com.tinyledger.app.ui.viewmodel.SearchFilterType
import com.tinyledger.app.ui.viewmodel.SearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onNavigateBack: () -> Unit,
    onEditTransaction: (Long) -> Unit = {},
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showFilterDialog by remember { mutableStateOf(false) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var minAmountText by remember { mutableStateOf("") }
    var maxAmountText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // Top bar
        TopAppBar(
            title = {
                Text(
                    text = "搜索",
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
            actions = {
                IconButton(onClick = { showFilterDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "筛选"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.White
            )
        )

        // Search input
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFEEEEEE)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = Color(0xFF999999),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                androidx.compose.material3.TextField(
                    value = uiState.query,
                    onValueChange = { viewModel.setQuery(it) },
                    placeholder = {
                        Text(
                            text = "输入关键词搜索账单",
                            color = Color(0xFF999999),
                            fontSize = 14.sp
                        )
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = androidx.compose.material3.TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    textStyle = MaterialTheme.typography.bodyMedium
                )
                if (uiState.query.isNotEmpty()) {
                    IconButton(
                        onClick = { viewModel.clearSearch() },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "清除",
                            tint = Color(0xFF999999),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Date range display
        if (uiState.startDate != null && uiState.endDate != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = { viewModel.setDateRange(null, null) },
                    label = {
                        Text(
                            text = "${formatDate(uiState.startDate)} ~ ${formatDate(uiState.endDate)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    trailingIcon = {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "清除日期",
                            modifier = Modifier.size(14.dp)
                        )
                    }
                )
            }
        }

        // Amount range display
        if (uiState.minAmount != null || uiState.maxAmount != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = { viewModel.setAmountRange(null, null) },
                    label = {
                        Text(
                            text = buildString {
                                append("金额: ")
                                append(if (uiState.minAmount != null) String.format("%.2f", uiState.minAmount) else "-")
                                append(" ~ ")
                                append(if (uiState.maxAmount != null) String.format("%.2f", uiState.maxAmount) else "-")
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    trailingIcon = {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "清除金额筛选",
                            modifier = Modifier.size(14.dp)
                        )
                    }
                )
            }
        }

        // Filter chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = uiState.filterType == SearchFilterType.ALL,
                onClick = { viewModel.setFilterType(SearchFilterType.ALL) },
                label = { Text("全部", fontSize = 12.sp) }
            )
            FilterChip(
                selected = uiState.filterType == SearchFilterType.EXPENSE,
                onClick = { viewModel.setFilterType(SearchFilterType.EXPENSE) },
                label = { Text("支出", fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                    selectedLabelColor = MaterialTheme.colorScheme.error
                )
            )
            FilterChip(
                selected = uiState.filterType == SearchFilterType.INCOME,
                onClick = { viewModel.setFilterType(SearchFilterType.INCOME) },
                label = { Text("收入", fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    selectedLabelColor = MaterialTheme.colorScheme.primary
                )
            )
            FilterChip(
                selected = uiState.filterType == SearchFilterType.TRANSFER,
                onClick = { viewModel.setFilterType(SearchFilterType.TRANSFER) },
                label = { Text("转账", fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFFFF9800).copy(alpha = 0.2f),
                    selectedLabelColor = Color(0xFFFF9800)
                )
            )
            FilterChip(
                selected = uiState.filterType == SearchFilterType.LENDING,
                onClick = { viewModel.setFilterType(SearchFilterType.LENDING) },
                label = { Text("借贷", fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF9C27B0).copy(alpha = 0.2f),
                    selectedLabelColor = Color(0xFF9C27B0)
                )
            )
        }

        // Content
        if (!uiState.hasSearched) {
            // Empty state - not yet searched
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 100.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color(0xFFCCCCCC)
                    )
                    Text(
                        text = "请输入关键词进行搜索~",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFF999999)
                    )
                }
            }
        } else if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.results.isEmpty()) {
            // No results
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 100.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SearchOff,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color(0xFFCCCCCC)
                    )
                    Text(
                        text = "未找到相关账单",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFF999999)
                    )
                }
            }
        } else {
            // Search results count
            Text(
                text = "找到 ${uiState.results.size} 条记录",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF999999),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            // Results list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = uiState.results,
                    key = { it.id }
                ) { transaction ->
                    TransactionCard(
                        transaction = transaction,
                        currencySymbol = uiState.currencySymbol,
                        onClick = { onEditTransaction(transaction.id) }
                    )
                }

                // Bottom spacing
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    // Filter dialog (date range + amount range picker)
    if (showFilterDialog) {
        // Sync text fields with current state when dialog opens
        LaunchedEffect(Unit) {
            minAmountText = uiState.minAmount?.toString() ?: ""
            maxAmountText = uiState.maxAmount?.toString() ?: ""
        }
        AlertDialog(
            onDismissRequest = { showFilterDialog = false },
            title = { Text("筛选条件") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Date range
                    Text(
                        text = "日期范围",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showStartDatePicker = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (uiState.startDate != null) formatDate(uiState.startDate) else "开始日期",
                                fontSize = 12.sp
                            )
                        }
                        OutlinedButton(
                            onClick = { showEndDatePicker = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (uiState.endDate != null) formatDate(uiState.endDate) else "结束日期",
                                fontSize = 12.sp
                            )
                        }
                    }
                    if (uiState.startDate != null || uiState.endDate != null) {
                        TextButton(
                            onClick = { viewModel.setDateRange(null, null) }
                        ) {
                            Text("清除日期筛选")
                        }
                    }

                    // Amount range
                    Text(
                        text = "金额范围",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = minAmountText,
                            onValueChange = { minAmountText = it },
                            placeholder = { Text("最低金额", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "~",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        OutlinedTextField(
                            value = maxAmountText,
                            onValueChange = { maxAmountText = it },
                            placeholder = { Text("最高金额", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium
                        )
                    }
                    if (uiState.minAmount != null || uiState.maxAmount != null) {
                        TextButton(
                            onClick = {
                                viewModel.setAmountRange(null, null)
                                minAmountText = ""
                                maxAmountText = ""
                            }
                        ) {
                            Text("清除金额筛选")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val min = minAmountText.toDoubleOrNull()
                    val max = maxAmountText.toDoubleOrNull()
                    viewModel.setAmountRange(min, max)
                    showFilterDialog = false
                }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFilterDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // Start date picker
    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            viewModel.setDateRange(it, uiState.endDate ?: System.currentTimeMillis())
                        }
                        showStartDatePicker = false
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) {
                    Text("取消")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // End date picker
    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            viewModel.setDateRange(uiState.startDate ?: 0L, it)
                        }
                        showEndDatePicker = false
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) {
                    Text("取消")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

private fun formatDate(timestamp: Long?): String {
    if (timestamp == null) return ""
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}
