package com.tinyledger.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.text.SimpleDateFormat
import java.util.*

/**
 * 日期范围数据类
 */
data class DateRange(
    val startDate: Long? = null,
    val endDate: Long? = null
) {
    val isEmpty: Boolean get() = startDate == null && endDate == null
    val hasRange: Boolean get() = startDate != null && endDate != null
    
    // 用于SmsReader过滤的开始时间戳
    val startTime: Long?
        get() = startDate?.let { 
            // 将日期的开始时间（00:00:00）作为开始时间戳
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = it
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            calendar.timeInMillis
        }
    
    // 用于SmsReader过滤的结束时间戳
    val endTime: Long?
        get() = endDate?.let { 
            // 将日期的结束时间（23:59:59）作为结束时间戳
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = it
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            calendar.timeInMillis
        }
}

/**
 * 日期范围选择器对话框
 */
@Composable
fun DateRangePickerDialog(
    title: String = "选择时间范围",
    initialRange: DateRange = DateRange(),
    onConfirm: (DateRange) -> Unit,
    onDismiss: () -> Unit
) {
    var startDate by remember { mutableStateOf(initialRange.startDate) }
    var endDate by remember { mutableStateOf(initialRange.endDate) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // 标题
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "不选择时间则导入全部记录",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // 开始日期
                DatePickerRow(
                    label = "开始日期",
                    value = startDate?.let { dateFormat.format(Date(it)) } ?: "全部",
                    onClick = { showStartDatePicker = true }
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // 结束日期
                DatePickerRow(
                    label = "结束日期",
                    value = endDate?.let { dateFormat.format(Date(it)) } ?: "全部",
                    onClick = { showEndDatePicker = true }
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // 按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = {
                            onConfirm(DateRange(startDate, endDate))
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("确定")
                    }
                }
            }
        }
    }
    
    // 开始日期选择器
    if (showStartDatePicker) {
        DatePickerDialog(
            initialDate = startDate,
            onDateSelected = { date ->
                startDate = date
                showStartDatePicker = false
            },
            onDismiss = { showStartDatePicker = false }
        )
    }
    
    // 结束日期选择器
    if (showEndDatePicker) {
        DatePickerDialog(
            initialDate = endDate,
            onDateSelected = { date ->
                endDate = date
                showEndDatePicker = false
            },
            onDismiss = { showEndDatePicker = false }
        )
    }
}

@Composable
private fun DatePickerRow(
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(8.dp)
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.CalendarMonth,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerDialog(
    initialDate: Long?,
    onDateSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate ?: System.currentTimeMillis()
    )
    
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { onDateSelected(it) }
                }
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

/**
 * 内联日期范围选择器
 */
@Composable
fun DateRangeSelector(
    label: String = "时间范围",
    dateRange: DateRange,
    onRangeSelected: (DateRange) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    
    val dateFormat = remember { SimpleDateFormat("MM-dd", Locale.getDefault()) }
    
    val displayText = when {
        dateRange.isEmpty -> "全部时间"
        dateRange.hasRange -> "${dateFormat.format(Date(dateRange.startDate!!))} ~ ${dateFormat.format(Date(dateRange.endDate!!))}"
        dateRange.startDate != null -> "${dateFormat.format(Date(dateRange.startDate))} ~ 至今"
        dateRange.endDate != null -> "开始 ~ ${dateFormat.format(Date(dateRange.endDate))}"
        else -> "全部时间"
    }
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { showDialog = true }
            .background(
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.CalendarMonth,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = displayText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        
        if (!dateRange.isEmpty) {
            IconButton(
                onClick = { onRangeSelected(DateRange()) },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "清除",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
    
    if (showDialog) {
        DateRangePickerDialog(
            title = "选择时间范围",
            initialRange = dateRange,
            onConfirm = { range ->
                onRangeSelected(range)
                showDialog = false
            },
            onDismiss = { showDialog = false }
        )
    }
}
