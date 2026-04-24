package com.tinyledger.app.ui.screens.backup

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tinyledger.app.ui.components.DeleteConfirmationDialog
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

data class ExportRecord(
    val fileName: String,
    val filePath: String,
    val fileSize: Long,
    val exportTime: Long
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupExportScreen(
    onNavigateBack: () -> Unit = {},
    onExportComplete: () -> Unit = {}
) {
    val context = LocalContext.current
    var selectedStoragePath by remember { mutableStateOf(getDefaultExportPath(context)) }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    var exportRecords by remember { mutableStateOf(loadExportRecords(context)) }
    var showExportDialog by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    var recordToDelete by remember { mutableStateOf<ExportRecord?>(null) }

    // 文件夹选择器
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            val path = getRealPathFromUri(context, it)
            if (path != null) {
                selectedStoragePath = path
            }
        }
    }

    // 日期选择器
    val startPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { _ -> }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "备份导出",
                        style = MaterialTheme.typography.headlineSmall.copy(
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
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // 导出期间选择
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "导出期间",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 开始日期 - 字体自适应
                        var startDateFontSize by remember { mutableStateOf(12.sp) }
                        OutlinedTextField(
                            value = startDate,
                            onValueChange = {},
                            label = { Text("开始日期", fontSize = (startDateFontSize.value * 0.9f).sp, maxLines = 1) },
                            placeholder = { Text("2025-01-01", fontSize = (startDateFontSize.value * 0.9f).sp, maxLines = 1) },
                            modifier = Modifier.weight(1f).onSizeChanged { size ->
                                if (size.width > 0) {
                                    val est = 10 * startDateFontSize.value * 0.62f
                                    if (est > size.width * 0.85f) {
                                        startDateFontSize = ((size.width * 0.85f) / (10 * 0.62f)).coerceAtLeast(9f).sp
                                    } else if (startDateFontSize.value < 12f) {
                                        startDateFontSize = ((size.width * 0.85f) / (10 * 0.62f)).coerceAtMost(12f).sp
                                    }
                                }
                            },
                            readOnly = true,
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(fontSize = startDateFontSize),
                            trailingIcon = {
                                IconButton(onClick = { showStartPicker = true }) {
                                    Icon(Icons.Default.DateRange, contentDescription = "选择开始日期", modifier = Modifier.size(18.dp))
                                }
                            }
                        )
                        Text("~", style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                        // 结束日期 - 字体自适应
                        var endDateFontSize by remember { mutableStateOf(12.sp) }
                        OutlinedTextField(
                            value = endDate,
                            onValueChange = {},
                            label = { Text("结束日期", fontSize = (endDateFontSize.value * 0.9f).sp, maxLines = 1) },
                            placeholder = { Text("2025-12-31", fontSize = (endDateFontSize.value * 0.9f).sp, maxLines = 1) },
                            modifier = Modifier.weight(1f).onSizeChanged { size ->
                                if (size.width > 0) {
                                    val est = 10 * endDateFontSize.value * 0.62f
                                    if (est > size.width * 0.85f) {
                                        endDateFontSize = ((size.width * 0.85f) / (10 * 0.62f)).coerceAtLeast(9f).sp
                                    } else if (endDateFontSize.value < 12f) {
                                        endDateFontSize = ((size.width * 0.85f) / (10 * 0.62f)).coerceAtMost(12f).sp
                                    }
                                }
                            },
                            readOnly = true,
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(fontSize = endDateFontSize),
                            trailingIcon = {
                                IconButton(onClick = { showEndPicker = true }) {
                                    Icon(Icons.Default.DateRange, contentDescription = "选择结束日期", modifier = Modifier.size(18.dp))
                                }
                            }
                        )
                    }
                    Text(
                        text = "不选择期间则导出全部数据",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 存储位置选择
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "导出存储位置",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    OutlinedTextField(
                        value = selectedStoragePath,
                        onValueChange = { selectedStoragePath = it },
                        label = { Text("存储路径") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = { folderPickerLauncher.launch(null) }) {
                                Icon(Icons.Default.Folder, contentDescription = "选择文件夹")
                            }
                        },
                        singleLine = true
                    )
                    Text(
                        text = "默认存储在内部存储的TinyLedger/Backup目录下",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 导出按钮
            Button(
                onClick = { showExportDialog = true },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                enabled = !isExporting,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isExporting) "正在导出..." else "立即导出",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            // 历史导出记录
            Text(
                text = "历史导出记录",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            if (exportRecords.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.CloudOff, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("暂无导出记录", style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                }
            } else {
                exportRecords.forEach { record ->
                    ExportRecordItem(
                        record = record,
                        onDelete = { recordToDelete = record }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // 开始日期选择器（底部弹出）
    if (showStartPicker) {
        BottomDatePickerDialog(
            onDateSelected = { date ->
                startDate = date
                showStartPicker = false
            },
            onDismiss = { showStartPicker = false }
        )
    }

    // 结束日期选择器（底部弹出）
    if (showEndPicker) {
        BottomDatePickerDialog(
            onDateSelected = { date ->
                endDate = date
                showEndPicker = false
            },
            onDismiss = { showEndPicker = false }
        )
    }

    // 导出确认对话框
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("确认导出") },
            text = {
                Column {
                    Text("将导出以下数据：")
                    Spacer(modifier = Modifier.height(8.dp))
                    val period = if (startDate.isNotEmpty() && endDate.isNotEmpty()) "$startDate ~ $endDate" else "全部期间"
                    Text("期间：$period")
                    Text("• 所有账户信息")
                    Text("• 所有交易记录")
                    Text("• 分类设置")
                    Text("• 预算设置")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExportDialog = false
                        isExporting = true
                        // 执行导出
                        val backupDir = File(selectedStoragePath)
                        if (!backupDir.exists()) backupDir.mkdirs()
                        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                        val fileName = "TinyLedger_backup_${sdf.format(Date())}.json"
                        val exportFile = File(backupDir, fileName)
                        try {
                            // TODO: 实际的数据库导出逻辑
                            exportFile.writeText("{ \"exportTime\": ${System.currentTimeMillis()}, \"period\": \"$startDate~$endDate\" }")
                            // 添加到记录列表
                            val newRecord = ExportRecord(
                                fileName = fileName,
                                filePath = exportFile.absolutePath,
                                fileSize = exportFile.length(),
                                exportTime = System.currentTimeMillis()
                            )
                            exportRecords = exportRecords + newRecord
                            // 保存记录到SharedPreferences
                            saveExportRecords(context, exportRecords)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        isExporting = false
                    },
                    enabled = !isExporting
                ) { Text("确认导出") }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) { Text("取消") }
            }
        )
    }

    // 删除确认对话框（使用统一的删除确认弹窗样式）
    recordToDelete?.let { record ->
        DeleteConfirmationDialog(
            title = "删除导出记录？",
            onDismiss = { recordToDelete = null },
            onConfirm = {
                // 删除文件
                val file = File(record.filePath)
                if (file.exists()) file.delete()
                exportRecords = exportRecords.filter { it != record }
                saveExportRecords(context, exportRecords)
                recordToDelete = null
            }
        )
    }
}

@Composable
private fun ExportRecordItem(
    record: ExportRecord,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }
    val fileSizeText = formatFileSize(record.fileSize)

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = record.fileName,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${dateFormat.format(Date(record.exportTime))}  •  $fileSizeText",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun DatePickerDialog(
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val calendar = Calendar.getInstance()
    var selectedYear by remember { mutableStateOf(calendar.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableStateOf(calendar.get(Calendar.MONTH) + 1) }
    var selectedDay by remember { mutableStateOf(calendar.get(Calendar.DAY_OF_MONTH)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择日期") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = { selectedYear-- }) { Icon(Icons.Default.ChevronLeft, null) }
                    Text("${selectedYear}年", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    IconButton(onClick = { selectedYear++ }) { Icon(Icons.Default.ChevronRight, null) }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = { if (selectedMonth > 1) selectedMonth-- else { selectedMonth = 12; selectedYear-- } }) { Icon(Icons.Default.ChevronLeft, null) }
                    Text("${selectedMonth}月", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    IconButton(onClick = { if (selectedMonth < 12) selectedMonth++ else { selectedMonth = 1; selectedYear++ } }) { Icon(Icons.Default.ChevronRight, null) }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = { if (selectedDay > 1) selectedDay-- }) { Icon(Icons.Default.ChevronLeft, null) }
                    Text("${selectedDay}日", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    IconButton(onClick = { if (selectedDay < 31) selectedDay++ }) { Icon(Icons.Default.ChevronRight, null) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onDateSelected(String.format("%04d-%02d-%02d", selectedYear, selectedMonth, selectedDay))
            }) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

/**
 * 底部弹出的日期选择器（美化版）
 * 从屏幕底部弹出，宽度与屏幕齐平，底部与屏幕底部齐平
 * 年月同排，下方显示当月完整日历
 */
@Composable
private fun BottomDatePickerDialog(
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val calendar = Calendar.getInstance()
    var selectedYear by remember { mutableStateOf(calendar.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableStateOf(calendar.get(Calendar.MONTH) + 1) }
    var selectedDay by remember { mutableStateOf(calendar.get(Calendar.DAY_OF_MONTH)) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f)),
            contentAlignment = Alignment.BottomCenter
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 顶部指示条（装饰）
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .background(
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                RoundedCornerShape(2.dp)
                            )
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 标题
                    Text(
                        text = "选择日期",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // 年月选择器（同一行）
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 年份选择
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = { selectedYear-- },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.ChevronLeft,
                                    contentDescription = "上一年",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = "${selectedYear}年",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                            IconButton(
                                onClick = { selectedYear++ },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = "下一年",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(24.dp))
                        
                        // 月份选择
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = { 
                                    if (selectedMonth > 1) selectedMonth-- 
                                    else { selectedMonth = 12; selectedYear-- }
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.ChevronLeft,
                                    contentDescription = "上一月",
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }
                            Text(
                                text = "${selectedMonth}月",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            )
                            IconButton(
                                onClick = { 
                                    if (selectedMonth < 12) selectedMonth++ 
                                    else { selectedMonth = 1; selectedYear++ }
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = "下一月",
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 日历网格区域
                    CalendarGrid(
                        year = selectedYear,
                        month = selectedMonth,
                        selectedDay = selectedDay,
                        onDaySelected = { day -> selectedDay = day }
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // 按钮区域
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("取消", style = MaterialTheme.typography.titleMedium)
                        }
                        
                        Button(
                            onClick = {
                                onDateSelected(String.format("%04d-%02d-%02d", selectedYear, selectedMonth, selectedDay))
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "确定",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

/**
 * 日历网格组件 - 显示指定年月的完整日历
 */
@Composable
private fun CalendarGrid(
    year: Int,
    month: Int,
    selectedDay: Int,
    onDaySelected: (Int) -> Unit
) {
    // 计算当月第一天是星期几（0=周日, 1=周一, ... 6=周六）
    val firstDayCalendar = Calendar.getInstance().apply {
        set(year, month - 1, 1)
    }
    val firstDayOfWeek = firstDayCalendar.get(Calendar.DAY_OF_WEEK) - 1 // 转为0-6
    
    // 计算当月总天数
    val daysInMonth = firstDayCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    
    // 计算需要显示的总格子数（包括上月填补的空格）
    val totalCells = firstDayOfWeek + daysInMonth
    val totalRows = (totalCells + 6) / 7 // 向上取整到完整的周数
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        // 星期标题行
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val weekDays = listOf("日", "一", "二", "三", "四", "五", "六")
            weekDays.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 日期网格
        for (row in 0 until totalRows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (col in 0..6) {
                    val cellIndex = row * 7 + col
                    val dayNumber = cellIndex - firstDayOfWeek + 1
                    
                    if (dayNumber in 1..daysInMonth) {
                        val isSelected = dayNumber == selectedDay
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .background(
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        Color.Transparent
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { onDaySelected(dayNumber) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dayNumber.toString(),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            )
                        }
                    } else {
                        // 空白格子
                        Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                    }
                }
            }
        }
    }
}

private fun getDefaultExportPath(context: Context): String {
    // 使用应用私有外部存储目录，无需额外权限
    return File(context.getExternalFilesDir(null), "TinyLedger/Backup").absolutePath
}

private fun getRealPathFromUri(context: Context, uri: Uri): String? {
    // 尝试从URI获取真实路径
    val docId = android.provider.DocumentsContract.getTreeDocumentId(uri)
    return try {
        val split = docId.split(":")
        if (split.size > 1) {
            "${Environment.getExternalStorageDirectory()}/${split[1]}"
        } else {
            "${Environment.getExternalStorageDirectory()}/${docId}"
        }
    } catch (e: Exception) {
        null
    }
}

private fun loadExportRecords(context: Context): List<ExportRecord> {
    val prefs = context.getSharedPreferences("backup_export_records", Context.MODE_PRIVATE)
    val recordsStr = prefs.getString("records", "") ?: ""
    if (recordsStr.isEmpty()) return emptyList()
    return recordsStr.split("||").mapNotNull { entry ->
        val parts = entry.split("|")
        if (parts.size == 4) {
            try {
                ExportRecord(parts[0], parts[1], parts[2].toLong(), parts[3].toLong())
            } catch (e: Exception) { null }
        } else null
    }
}

private fun saveExportRecords(context: Context, records: List<ExportRecord>) {
    val prefs = context.getSharedPreferences("backup_export_records", Context.MODE_PRIVATE)
    val recordsStr = records.joinToString("||") { "${it.fileName}|${it.filePath}|${it.fileSize}|${it.exportTime}" }
    prefs.edit().putString("records", recordsStr).apply()
}

private fun formatFileSize(size: Long): String {
    return when {
        size < 1024 -> "$size B"
        size < 1024 * 1024 -> "${size / 1024} KB"
        else -> String.format("%.2f MB", size / (1024.0 * 1024.0))
    }
}
