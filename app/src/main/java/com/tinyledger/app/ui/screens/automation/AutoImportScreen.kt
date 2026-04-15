package com.tinyledger.app.ui.screens.automation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.tinyledger.app.data.notification.TransactionNotificationService
import com.tinyledger.app.data.sms.SmsReadStats
import com.tinyledger.app.data.sms.SmsTransaction
import com.tinyledger.app.domain.model.TransactionType
import com.tinyledger.app.ui.components.DateRange
import com.tinyledger.app.ui.components.DateRangeSelector
import com.tinyledger.app.ui.theme.IOSColors
import com.tinyledger.app.ui.viewmodel.AutoImportViewModel
import java.text.SimpleDateFormat
import java.util.*

enum class ImportSource {
    SMS, WECHAT, ALIPAY
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoImportScreen(
    importSource: ImportSource = ImportSource.SMS,
    onNavigateBack: () -> Unit = {},
    onImportComplete: () -> Unit = {},
    viewModel: AutoImportViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // 根据导入来源区分 CSV 模式还是 SMS 模式
    val isCsvMode = importSource == ImportSource.WECHAT || importSource == ImportSource.ALIPAY

    // ── SMS 权限处理（仅 SMS 模式需要）──────────────────────────────────
    var hasSmsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_SMS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted -> hasSmsPermission = isGranted }

    // ── CSV/xlsx 文件选择器 ────────────────────────────────────────────────
    val csvFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.loadCsvFromUri(context.contentResolver, uri, importSource)
        }
    }

    // 支持的文件 MIME 类型（CSV + xlsx）
    val supportedMimeTypes = arrayOf(
        "text/csv",
        "text/comma-separated-values",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "application/vnd.ms-excel",
        "*/*"
    )

    var dateRange by remember { mutableStateOf(DateRange()) }

    // SMS 模式：有权限时自动加载
    LaunchedEffect(hasSmsPermission, dateRange) {
        if (!isCsvMode && hasSmsPermission) {
            viewModel.loadSmsTransactions(
                contentResolver = context.contentResolver,
                startTime = dateRange.startTime,
                endTime = dateRange.endTime
            )
        }
    }

    // 导入成功后自动返回
    LaunchedEffect(uiState.importSuccess) {
        if (uiState.importSuccess) {
            kotlinx.coroutines.delay(1500)
            onImportComplete()
        }
    }

    // 标题 / 图标 / 颜色
    val title = when (importSource) {
        ImportSource.SMS -> "导入短信收支记录"
        ImportSource.WECHAT -> "导入微信账单"
        ImportSource.ALIPAY -> "导入支付宝账单"
    }
    val sourceIcon = when (importSource) {
        ImportSource.SMS -> Icons.Default.Sms
        ImportSource.WECHAT -> Icons.Default.Chat
        ImportSource.ALIPAY -> Icons.Default.Payment
    }
    val sourceColor = when (importSource) {
        ImportSource.SMS -> IOSColors.Primary
        ImportSource.WECHAT -> IOSColors.AccountWechat
        ImportSource.ALIPAY -> IOSColors.AccountAlipay
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        title,
                        style = MaterialTheme.typography.headlineSmall.copy(
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
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when {
                // ── 导入成功 ──────────────────────────────────────────
                uiState.importSuccess -> ImportSuccessCard(count = uiState.importCount)

                // ── SMS 模式：需要权限 ─────────────────────────────────
                !isCsvMode && !hasSmsPermission -> {
                    PermissionRequestCard(
                        icon = sourceIcon,
                        iconColor = sourceColor,
                        title = "需要短信读取权限",
                        subtitle = "为了自动识别收支记录，需要读取短信权限。我们只会读取金融类短信通知。",
                        onRequestPermission = {
                            permissionLauncher.launch(Manifest.permission.READ_SMS)
                        }
                    )
                }

                // ── CSV/xlsx 模式：未选文件时显示引导界面 ────────────────────
                isCsvMode && uiState.csvTransactions.isEmpty() && !uiState.isLoading -> {
                    CsvImportGuideCard(
                        importSource = importSource,
                        sourceIcon = sourceIcon,
                        sourceColor = sourceColor,
                        onPickFile = {
                            csvFileLauncher.launch(supportedMimeTypes)
                        }
                    )
                }

                // ── 加载中 ─────────────────────────────────────────────
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(color = sourceColor)
                    Text(
                        text = if (isCsvMode) "正在解析文件..." else "正在读取短信...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // ── 有数据：展示列表 ──────────────────────────────────
                else -> {
                    val transactions = if (isCsvMode) uiState.csvTransactions else uiState.smsTransactions

                    // SMS 模式：时间范围选择
                    if (!isCsvMode) {
                        // 权限检查卡片
                        SmsPermissionCheckCard(context = context)

                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            DateRangeSelector(
                                label = "记录时间范围",
                                dateRange = dateRange,
                                onRangeSelected = { range -> dateRange = range }
                            )
                        }

                        // 通知使用权引导（用于捕获 1069xxxx 通知短信）
                        val hasNotifPermission = remember {
                            TransactionNotificationService.hasPermission(context)
                        }
                        if (!hasNotifPermission) {
                            NotificationPermissionCard(
                                onOpenSettings = {
                                    context.startActivity(
                                        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                    )
                                }
                            )
                        }

                        // 诊断统计信息（帮助排查短信读取问题）
                        uiState.smsReadStats?.let { stats ->
                            SmsReadStatsCard(stats = stats)
                        }
                    }

                    // CSV 模式：顶部操作栏（重新选择文件）
                    if (isCsvMode) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "已解析 ${transactions.size} 条记录",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = {
                                    viewModel.clearCsvTransactions()
                                    csvFileLauncher.launch(supportedMimeTypes)
                                }
                            ) {
                                Icon(
                                    Icons.Default.FolderOpen,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("重新选择")
                            }
                        }
                    }

                    // 全选操作行
                    if (transactions.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextButton(
                                onClick = {
                                    if (uiState.selectedTransactionIds.size == transactions.size) {
                                        viewModel.deselectAllTransactions()
                                    } else {
                                        viewModel.selectAllTransactions(isCsvMode)
                                    }
                                }
                            ) {
                                Text(
                                    if (uiState.selectedTransactionIds.size == transactions.size)
                                        "取消全选" else "全选"
                                )
                            }
                            Text(
                                text = "共 ${transactions.size} 条",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.align(Alignment.CenterVertically)
                            )
                        }
                    }

                    // 空列表
                    if (transactions.isEmpty()) {
                        EmptyTransactionsView(
                            sourceIcon = sourceIcon,
                            sourceColor = sourceColor,
                            importSource = importSource,
                            isCsvMode = isCsvMode
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isCsvMode) {
                                items(
                                    items = uiState.csvTransactions,
                                    key = { "${it.id}_csv" }
                                ) { tx ->
                                    CsvTransactionItem(
                                        id = tx.id,
                                        type = tx.type,
                                        amount = tx.amount,
                                        description = tx.description,
                                        counterpart = tx.counterpart,
                                        paymentMethod = tx.paymentMethod,
                                        date = tx.date,
                                        isSelected = "${tx.id}_csv" in uiState.selectedTransactionIds,
                                        sourceColor = sourceColor,
                                        onToggle = { viewModel.toggleTransactionSelection("${tx.id}_csv") }
                                    )
                                }
                            } else {
                                items(
                                    items = uiState.smsTransactions,
                                    key = { "${it.id}_sms" }
                                ) { sms ->
                                    SmsTransactionItem(
                                        transaction = sms,
                                        isSelected = "${sms.id}_sms" in uiState.selectedTransactionIds,
                                        onToggle = { viewModel.toggleTransactionSelection("${sms.id}_sms") }
                                    )
                                }
                            }
                        }

                        ImportBottomBar(
                            selectedCount = uiState.selectedTransactionIds.size,
                            onImport = { viewModel.importSelectedTransactions(isCsvMode) },
                            isLoading = uiState.isLoading
                        )
                    }
                }
            }
        }
    }

    // 错误提示 Snackbar
    uiState.errorMessage?.let { error ->
        LaunchedEffect(error) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearError()
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// CSV 导入引导卡片
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun CsvImportGuideCard(
    importSource: ImportSource,
    sourceIcon: ImageVector,
    sourceColor: Color,
    onPickFile: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // 图标
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(sourceColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = sourceIcon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = sourceColor
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = if (importSource == ImportSource.WECHAT) "导入微信账单" else "导入支付宝账单",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 步骤说明卡片
        val steps = if (importSource == ImportSource.WECHAT) {
            listOf(
                "打开微信 → 我 → 服务 → 钱包",
                "点击右上角「…」→ 账单 → 常见问题",
                "选择「下载账单」→ 选择时间范围",
                "将下载的账单文件（CSV 或 xlsx）传到手机",
                "点击下方按钮选择文件导入"
            )
        } else {
            listOf(
                "打开支付宝 → 我的 → 账单",
                "点击右上角「…」→ 开具交易流水证明",
                "或：账单 → 筛选 → 导出账单",
                "将下载的账单文件（CSV 或 xlsx）传到手机",
                "点击下方按钮选择文件导入"
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "导出步骤",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                steps.forEachIndexed { index, step ->
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(sourceColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = sourceColor
                                )
                            )
                        }
                        Text(
                            text = step,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 选择文件按钮
        Button(
            onClick = onPickFile,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = sourceColor)
        ) {
            Icon(Icons.Default.FolderOpen, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "选择账单文件",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "支持 .csv 和 .xlsx 格式文件",
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// CSV 交易条目
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun CsvTransactionItem(
    id: String,
    type: TransactionType,
    amount: Double,
    description: String,
    counterpart: String,
    paymentMethod: String,
    date: Long,
    isSelected: Boolean,
    sourceColor: Color,
    onToggle: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "border"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .border(2.dp, borderColor, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = isSelected, onCheckedChange = { onToggle() })

            Spacer(modifier = Modifier.width(8.dp))

            // 收/支图标
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (type == TransactionType.INCOME)
                            IOSColors.SystemGreen.copy(alpha = 0.2f)
                        else IOSColors.SystemRed.copy(alpha = 0.2f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (type == TransactionType.INCOME)
                        Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                    contentDescription = null,
                    tint = if (type == TransactionType.INCOME)
                        IOSColors.SystemGreen else IOSColors.SystemRed,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 信息区
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = description.ifBlank { counterpart }.ifBlank { "未知商户" },
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (counterpart.isNotBlank() && counterpart != description) {
                    Text(
                        text = counterpart,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = formatDate(date),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    if (paymentMethod.isNotBlank()) {
                        Text(
                            text = paymentMethod,
                            style = MaterialTheme.typography.labelSmall.copy(color = sourceColor.copy(alpha = 0.8f)),
                            modifier = Modifier
                                .background(sourceColor.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 金额
            Text(
                text = "${if (type == TransactionType.INCOME) "+" else "-"}¥${String.format("%.2f", amount)}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = if (type == TransactionType.INCOME) IOSColors.SystemGreen else IOSColors.SystemRed
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// SMS 交易条目（原有样式保留）
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun SmsTransactionItem(
    transaction: SmsTransaction,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "border"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .border(2.dp, borderColor, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = isSelected, onCheckedChange = { onToggle() })
            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (transaction.type == TransactionType.INCOME)
                            IOSColors.SystemGreen.copy(alpha = 0.2f)
                        else IOSColors.SystemRed.copy(alpha = 0.2f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (transaction.type == TransactionType.INCOME)
                        Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                    contentDescription = null,
                    tint = if (transaction.type == TransactionType.INCOME)
                        IOSColors.SystemGreen else IOSColors.SystemRed,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = transaction.source,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!transaction.cardLastFour.isNullOrBlank()) {
                        Text(
                            text = "尾号${transaction.cardLastFour}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
                Text(
                    text = transaction.body.take(80),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = formatDate(transaction.date),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    val confColor = when {
                        transaction.confidence >= 0.85f -> IOSColors.SystemGreen
                        transaction.confidence >= 0.65f -> IOSColors.SystemOrange
                        else -> IOSColors.SystemRed
                    }
                    Text(
                        text = "识别${(transaction.confidence * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall.copy(color = confColor),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = if (transaction.type == TransactionType.INCOME)
                    "+¥${String.format("%.2f", transaction.amount ?: 0.0)}"
                else
                    "-¥${String.format("%.2f", transaction.amount ?: 0.0)}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = if (transaction.type == TransactionType.INCOME)
                    IOSColors.SystemGreen else IOSColors.SystemRed
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// 权限请求卡片
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun PermissionRequestCard(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    onRequestPermission: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(64.dp), tint = iconColor)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onRequestPermission,
                modifier = Modifier.fillMaxWidth(0.6f),
                shape = RoundedCornerShape(12.dp)
            ) { Text("授权权限") }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// 空数据提示
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun EmptyTransactionsView(
    sourceIcon: ImageVector,
    sourceColor: Color,
    importSource: ImportSource,
    isCsvMode: Boolean
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(imageVector = sourceIcon, contentDescription = null, modifier = Modifier.size(80.dp), tint = sourceColor.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "未找到收支记录", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold))
        Spacer(modifier = Modifier.height(8.dp))
        val hint = when {
            isCsvMode && importSource == ImportSource.WECHAT -> "文件中未发现有效收支记录，\n请确认导出的是微信账单（CSV/xlsx）"
            isCsvMode && importSource == ImportSource.ALIPAY -> "文件中未发现有效收支记录，\n请确认导出的是支付宝账单（CSV/xlsx）"
            else -> "请确保您的短信中包含银行收支通知"
        }
        Text(
            text = hint,
            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// 导入成功卡片
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ImportSuccessCard(count: Int) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "success")
        val scale by infiniteTransition.animateFloat(
            initialValue = 0.8f, targetValue = 1.1f,
            animationSpec = infiniteRepeatable(animation = tween(600), repeatMode = RepeatMode.Reverse),
            label = "scale"
        )

        Box(
            modifier = Modifier.size((100 * scale).dp).clip(CircleShape)
                .background(IOSColors.SystemGreen.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(60.dp), tint = IOSColors.SystemGreen)
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(text = "导入成功！", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), color = IOSColors.SystemGreen)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "已成功导入 $count 条收支记录", style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "正在返回...", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)))
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// 底部导入栏
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ImportBottomBar(
    selectedCount: Int,
    onImport: () -> Unit,
    isLoading: Boolean
) {
    Surface(modifier = Modifier.fillMaxWidth(), shadowElevation = 8.dp, color = MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "已选择 $selectedCount 条",
                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = onImport,
                enabled = selectedCount > 0 && !isLoading,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("导入记账")
                }
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

// ══════════════════════════════════════════════════════════════════════════════
// 通知使用权引导卡片
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun NotificationPermissionCard(
    onOpenSettings: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = IOSColors.SystemOrange.copy(alpha = 0.1f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.NotificationsActive,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = IOSColors.SystemOrange
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "开启通知使用权可读取更多银行短信",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = IOSColors.SystemOrange
                    )
                    Text(
                        text = "部分手机将建设银行等 1069 号码的通知短信存储在独立数据库，" +
                                "需要开启「通知使用权」才能读取。开启后新收到的银行短信将被自动捕获。",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = IOSColors.SystemOrange
                )
            ) {
                Text("前往开启通知使用权", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// SMS 读取诊断统计卡片
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun SmsReadStatsCard(stats: SmsReadStats) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "短信库${stats.cursorRowCount}条 | 通知${stats.notificationSmsCaptured}条 | 识别${stats.parsedOk}条",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))

                // 深度诊断
                val diagLabel = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold
                )
                val diagValue = MaterialTheme.typography.labelSmall
                val diagColor = MaterialTheme.colorScheme.onSurfaceVariant

                Text(text = "ContentProvider 原始行数: ${stats.cursorRowCount}", style = diagLabel, color = diagColor)
                Text(text = "通知栏捕获银行短信: ${stats.notificationSmsCaptured}条, 解析成功${stats.notificationSmsParsed}条",
                    style = diagLabel,
                    color = if (stats.notificationSmsCaptured > 0) IOSColors.SystemGreen else diagColor)
                Text(text = "地址含95533的行数: ${stats.ccbAddressMatchCount}", style = diagLabel,
                    color = if (stats.ccbAddressMatchCount > 0) IOSColors.SystemGreen else IOSColors.SystemRed)

                val skipDetails = buildList {
                    if (stats.nullAddressCount > 0) add("地址为空: ${stats.nullAddressCount}")
                    if (stats.nullBodyCount > 0) add("内容为空: ${stats.nullBodyCount}")
                    if (stats.duplicateCount > 0) add("重复跳过: ${stats.duplicateCount}")
                }
                if (skipDetails.isNotEmpty()) {
                    skipDetails.forEach { Text(text = "  $it", style = diagValue, color = diagColor) }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 各银行命中统计
                if (stats.bankHitCounts.isNotEmpty()) {
                    Text(text = "各银行短信数量:", style = diagLabel, color = diagColor)
                    stats.bankHitCounts.entries
                        .sortedByDescending { it.value }
                        .forEach { (bank, count) ->
                            Text(text = "  $bank: $count 条", style = diagValue, color = diagColor)
                        }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // 失败原因统计
                val details = buildList {
                    if (stats.unknownSourceCount > 0) add("非银行短信: ${stats.unknownSourceCount}")
                    if (stats.excludedCount > 0) add("被排除(验证码等): ${stats.excludedCount}")
                    if (stats.noAmountCount > 0) add("无法提取金额: ${stats.noAmountCount}")
                    if (stats.noTypeCount > 0) add("无法识别收支: ${stats.noTypeCount}")
                    if (stats.lowConfidenceCount > 0) add("置信度过低: ${stats.lowConfidenceCount}")
                    if (stats.exceptionCount > 0) add("处理异常: ${stats.exceptionCount}")
                }
                if (details.isNotEmpty()) {
                    Text(text = "未识别原因:", style = diagLabel, color = diagColor)
                    details.forEach { detail ->
                        Text(text = "  $detail", style = diagValue, color = diagColor)
                    }
                }

                // 发件人号码样本
                if (stats.sampleAddresses.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "发件人号码样本:", style = diagLabel, color = diagColor)
                    stats.sampleAddresses.forEach { addr ->
                        val highlight = addr.contains("95533")
                        Text(
                            text = "  $addr",
                            style = diagValue,
                            color = if (highlight) IOSColors.SystemGreen else diagColor
                        )
                    }
                }

                // URI 探测结果
                if (stats.uriProbeResults.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "URI探测结果:", style = diagLabel, color = diagColor)
                    stats.uriProbeResults.forEach { result ->
                        val highlight = result.contains("ccb=") && !result.contains("ccb=0")
                        Text(
                            text = "  $result",
                            style = diagValue,
                            color = if (highlight) IOSColors.SystemGreen else diagColor
                        )
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// 短信导入权限检查卡片
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun SmsPermissionCheckCard(context: android.content.Context) {
    // 检查各项权限状态
    val hasReadSms = remember {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
    }
    val hasReceiveSms = remember {
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
    }
    val hasNotificationAccess = remember {
        TransactionNotificationService.hasPermission(context)
    }
    val hasOverlayPermission = remember {
        Settings.canDrawOverlays(context)
    }

    // 所有权限都已授予时不显示
    if (hasReadSms && hasReceiveSms && hasNotificationAccess && hasOverlayPermission) {
        return
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = IOSColors.SystemRed.copy(alpha = 0.08f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = IOSColors.SystemRed
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "部分权限未授权，短信导入可能不全或异常",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = IOSColors.SystemRed
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 权限列表
            val permissionItems = buildList {
                if (!hasReadSms) add("读取短信与彩信" to {
                    context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    })
                })
                if (!hasReceiveSms) add("接收短信与彩信" to {
                    context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    })
                })
                if (!hasNotificationAccess) add("通知类短信" to {
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                })
                if (!hasOverlayPermission) add("后台弹出界面" to {
                    context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    })
                })
            }

            permissionItems.forEach { (label, action) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = IOSColors.SystemRed.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = action,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text("去设置", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
