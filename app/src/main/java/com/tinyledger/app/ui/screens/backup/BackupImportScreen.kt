package com.tinyledger.app.ui.screens.backup

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class BackupFile(
    val uri: Uri,
    val fileName: String,
    val fileSize: Long,
    val isValid: Boolean = false,
    val errorMessage: String? = null
)

enum class ImportMode(val label: String, val description: String) {
    FULL_MERGE("数据全部合并", "将备份数据与手机数据合并，不修改原有数据"),
    INCREMENTAL("数据增量合并", "合并备份数据，跳过重复数据，仅导入不同数据"),
    FRESH_IMPORT("数据全新导入", "清空手机原有数据，完全使用导入的数据")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupImportScreen(
    onNavigateBack: () -> Unit = {},
    onImportComplete: () -> Unit = {}
) {
    val context = LocalContext.current
    var selectedFile by remember { mutableStateOf<BackupFile?>(null) }
    var importMode by remember { mutableStateOf(ImportMode.FRESH_IMPORT) }
    var isImporting by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // 文件选择器
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val fileName = getFileName(context, it)
            val fileSize = getFileSize(context, it)
            val (isValid, error) = validateBackupFile(context, it, fileName)

            selectedFile = BackupFile(
                uri = it,
                fileName = fileName,
                fileSize = fileSize,
                isValid = isValid,
                errorMessage = error
            )

            if (!isValid) {
                showErrorDialog = true
                errorMessage = error ?: "数据格式错误或损坏，无法导入"
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "备份导入",
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 文件选择区域
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { filePickerLauncher.launch("*/*") },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(56.dp))
                    Text("选择备份文件",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp),
                        color = MaterialTheme.colorScheme.onSurface)
                    Text("点击选择 .json 或 .db 格式的备份文件",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
            }

            // 已选文件信息
            selectedFile?.let { file ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (file.isValid) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(
                                    if (file.isValid) Icons.Default.CheckCircle else Icons.Default.Error,
                                    contentDescription = null,
                                    tint = if (file.isValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(file.fileName, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface)
                            }
                            Text(formatFileSize(file.fileSize), style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { selectedFile = null }) {
                            Icon(Icons.Default.Close, contentDescription = "移除",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // 数据覆盖模式选择
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "数据覆盖模式",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    ImportMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { importMode = mode }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = importMode == mode,
                                onClick = { importMode = mode }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = mode.label,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = mode.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // 导入按钮
            Button(
                onClick = { showImportDialog = true },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                enabled = selectedFile?.isValid == true && !isImporting,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isImporting) "正在导入..." else "确认导入",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            // 提示信息
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("💡 导入说明",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer))
                    Text(
                        "• 支持 .json 和 .db 格式的备份文件\n" +
                                "• 选择\"数据全新导入\"会清空手机原有数据，请谨慎操作\n" +
                                "• 导入过程中请勿关闭应用",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }

    // 导入确认对话框
    if (showImportDialog && selectedFile?.isValid == true) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("确认导入") },
            text = {
                Column {
                    Text("即将导入备份文件：")
                    Text(selectedFile!!.fileName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("导入模式：${importMode.label}")
                    Text(importMode.description)
                    if (importMode == ImportMode.FRESH_IMPORT) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("⚠️ 注意：此操作将清空手机中的所有现有数据！", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showImportDialog = false
                        isImporting = true
                        // TODO: 根据importMode执行不同的导入逻辑
                        when (importMode) {
                            ImportMode.FULL_MERGE -> { /* 全部合并 */ }
                            ImportMode.INCREMENTAL -> { /* 增量合并 */ }
                            ImportMode.FRESH_IMPORT -> { /* 全新导入 */ }
                        }
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            isImporting = false
                            onImportComplete()
                        }, 2000)
                    },
                    enabled = !isImporting,
                    colors = if (importMode == ImportMode.FRESH_IMPORT)
                        ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    else ButtonDefaults.buttonColors()
                ) { Text("确认导入") }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) { Text("取消") }
            }
        )
    }

    // 错误提示对话框
    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = false },
            icon = { Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("导入失败") },
            text = { Text(errorMessage) },
            confirmButton = {
                Button(onClick = { showErrorDialog = false }) { Text("确定") }
            }
        )
    }
}

private fun getFileName(context: Context, uri: Uri): String {
    var fileName = "unknown"
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        if (cursor.moveToFirst() && nameIndex != -1) fileName = cursor.getString(nameIndex)
    }
    return fileName
}

private fun getFileSize(context: Context, uri: Uri): Long {
    var size = 0L
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
        if (cursor.moveToFirst() && sizeIndex != -1) size = cursor.getLong(sizeIndex)
    }
    return size
}

private fun validateBackupFile(context: Context, uri: Uri, fileName: String): Pair<Boolean, String?> {
    val extension = fileName.substringAfterLast('.', "").lowercase()
    if (extension !in listOf("json", "db")) {
        return Pair(false, "不支持的文件格式，请选择 .json 或 .db 文件")
    }
    return Pair(true, null)
}

private fun formatFileSize(size: Long): String {
    return when {
        size < 1024 -> "$size B"
        size < 1024 * 1024 -> "${size / 1024} KB"
        else -> String.format("%.2f MB", size / (1024.0 * 1024.0))
    }
}
