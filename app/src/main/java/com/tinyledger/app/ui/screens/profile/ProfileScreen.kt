package com.tinyledger.app.ui.screens.profile

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tinyledger.app.BuildConfig
import com.tinyledger.app.data.notification.TransactionNotificationService
import com.tinyledger.app.domain.model.ColorTheme
import com.tinyledger.app.domain.model.ThemeMode
import com.tinyledger.app.ui.screens.settings.ImportType
import com.tinyledger.app.ui.theme.IOSColors
import com.tinyledger.app.ui.theme.ThemeColorPreview
import com.tinyledger.app.ui.theme.ThemeColorPreviews
import com.tinyledger.app.ui.viewmodel.SettingsViewModel
import com.tinyledger.app.util.VersionCheckUtil
import com.tinyledger.app.util.VersionInfo
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateToSettings: () -> Unit = {},
    onNavigateToAutoImport: (ImportType) -> Unit = {},
    onNavigateToBudget: () -> Unit = {},
    onNavigateToScreenshotAccounting: () -> Unit = {},
    onNavigateToAccounts: () -> Unit = {},
    onNavigateToHelp: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var showThemeDialog by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showColorThemeDialog by remember { mutableStateOf(false) }
    var showVersionInfoDialog by remember { mutableStateOf(false) }
    var lastDownloadClickTime by remember { mutableLongStateOf(0L) }

    var notificationEnabled by remember {
        mutableStateOf(TransactionNotificationService.isEnabled(context))
    }
    var hasNotificationPermission by remember {
        mutableStateOf(TransactionNotificationService.hasPermission(context))
    }
    var soundEnabled by remember {
        mutableStateOf(TransactionNotificationService.isSoundEnabled(context))
    }
    var vibrationEnabled by remember {
        mutableStateOf(TransactionNotificationService.isVibrationEnabled(context))
    }
    var seamlessEnabled by remember {
        mutableStateOf(TransactionNotificationService.isSeamlessEnabled(context))
    }
    LaunchedEffect(Unit) {
        hasNotificationPermission = TransactionNotificationService.hasPermission(context)
    }

    // Version update check
    var versionInfo by remember { mutableStateOf<VersionInfo?>(null) }
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            versionInfo = VersionCheckUtil.checkUpdate()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        // Top bar
        item {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "我的",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFFF5F5F5)
                )
            )
        }

        // User avatar card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "小小记账本",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "让记账更简单",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Smart accounting section
        item {
            SectionTitle("智能记账")
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column {
                    // Notification listener auto-accounting
                    ProfileSettingsItem(
                        icon = Icons.Default.NotificationsActive,
                        iconTint = IOSColors.SystemOrange,
                        title = "通知自动记账",
                        subtitle = if (hasNotificationPermission && notificationEnabled) "已开启" else "未开启",
                        trailing = {
                            if (hasNotificationPermission) {
                                Switch(
                                    checked = notificationEnabled,
                                    onCheckedChange = { newEnabled ->
                                        notificationEnabled = newEnabled
                                        TransactionNotificationService.setEnabled(context, newEnabled)
                                        if (!newEnabled) {
                                            seamlessEnabled = false
                                            TransactionNotificationService.setSeamlessEnabled(context, false)
                                        }
                                    }
                                )
                            } else {
                                TextButton(onClick = {
                                    try {
                                        context.startActivity(
                                            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                        )
                                    } catch (_: Exception) {}
                                }) {
                                    Text("去授权", color = IOSColors.SystemOrange)
                                }
                            }
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    // Seamless auto-accounting
                    ProfileSettingsItem(
                        icon = Icons.Default.AutoAwesome,
                        iconTint = MaterialTheme.colorScheme.primary,
                        title = "无感自动记账",
                        subtitle = if (seamlessEnabled) "自动完成记账，无需确认" else "捕获交易后需手动确认",
                        trailing = {
                            Switch(
                                checked = seamlessEnabled,
                                enabled = hasNotificationPermission && notificationEnabled,
                                onCheckedChange = { newEnabled ->
                                    seamlessEnabled = newEnabled
                                    TransactionNotificationService.setSeamlessEnabled(context, newEnabled)
                                }
                            )
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    // Allow background running
                    ProfileSettingsItem(
                        icon = Icons.Default.BatteryChargingFull,
                        iconTint = IOSColors.SystemGreen,
                        title = "允许后台运行",
                        subtitle = "保持自动记账持续工作",
                        onClick = {
                            try {
                                val intent = Intent()
                                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                intent.data = android.net.Uri.parse("package:${context.packageName}")
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                try {
                                    context.startActivity(
                                        Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                    )
                                } catch (_: Exception) {}
                            }
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    // Sound toggle
                    ProfileSettingsItem(
                        icon = Icons.Default.VolumeUp,
                        iconTint = IOSColors.Primary,
                        title = "声音",
                        subtitle = "自动记账成功时播放提示音",
                        trailing = {
                            Switch(
                                checked = soundEnabled,
                                onCheckedChange = { newEnabled ->
                                    soundEnabled = newEnabled
                                    TransactionNotificationService.setSoundEnabled(context, newEnabled)
                                }
                            )
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    // Vibration toggle
                    ProfileSettingsItem(
                        icon = Icons.Default.Vibration,
                        iconTint = IOSColors.SystemPurple,
                        title = "震动",
                        subtitle = "自动记账成功时震动提醒",
                        trailing = {
                            Switch(
                                checked = vibrationEnabled,
                                onCheckedChange = { newEnabled ->
                                    vibrationEnabled = newEnabled
                                    TransactionNotificationService.setVibrationEnabled(context, newEnabled)
                                }
                            )
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    // Screenshot accounting
                    ProfileSettingsItem(
                        icon = Icons.Default.Screenshot,
                        iconTint = IOSColors.SystemTeal,
                        title = "截屏记账",
                        subtitle = "选择支付截图自动识别金额",
                        onClick = onNavigateToScreenshotAccounting
                    )
                }
            }
        }

        // Asset accounts section
        item { Spacer(modifier = Modifier.height(20.dp)) }

        item {
            SectionTitle("账户管理")
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column {
                    ProfileSettingsItem(
                        icon = Icons.Default.AccountBalance,
                        iconTint = IOSColors.Primary,
                        title = "我的账户",
                        subtitle = "管理现金账户和信用账户",
                        onClick = onNavigateToAccounts
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }

        // Budget management section
        item {
            SectionTitle("预算管理")
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column {
                    ProfileSettingsItem(
                        icon = Icons.Default.AccountBalanceWallet,
                        iconTint = IOSColors.SystemOrange,
                        title = "预算管理",
                        subtitle = "设置月度/年度预算，控制支出",
                        onClick = onNavigateToBudget
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }

        // Data import section
        item {
            SectionTitle("数据导入")
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column {
                    ProfileSettingsItem(
                        icon = Icons.Default.Sms,
                        iconTint = IOSColors.Primary,
                        title = "导入短信收支记录",
                        subtitle = "从银行短信自动识别收支",
                        onClick = { onNavigateToAutoImport(ImportType.SMS) }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    ProfileSettingsItem(
                        icon = Icons.Default.Chat,
                        iconTint = IOSColors.AccountWechat,
                        title = "导入微信账单",
                        subtitle = "导入微信导出的 CSV / xlsx 账单文件",
                        onClick = { onNavigateToAutoImport(ImportType.WECHAT) }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    ProfileSettingsItem(
                        icon = Icons.Default.Payment,
                        iconTint = IOSColors.AccountAlipay,
                        title = "导入支付宝账单",
                        subtitle = "导入支付宝导出的 CSV / xlsx 账单文件",
                        onClick = { onNavigateToAutoImport(ImportType.ALIPAY) }
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }

        // Appearance section
        item {
            SectionTitle("外观")
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column {
                    ProfileSettingsItem(
                        icon = Icons.Default.Palette,
                        iconTint = Color(uiState.colorScheme.primaryColor),
                        title = "主题颜色",
                        subtitle = uiState.settings.colorTheme.displayName,
                        onClick = { showColorThemeDialog = true }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    // 深色模式 - 已隐藏（保留代码以备后用）
                    // ProfileSettingsItem(
                    //     icon = Icons.Default.DarkMode,
                    //     iconTint = IOSColors.SystemIndigo,
                    //     title = "深色模式",
                    //     subtitle = when (uiState.settings.themeMode) {
                    //         ThemeMode.LIGHT -> "关闭"
                    //         ThemeMode.DARK -> "开启"
                    //         ThemeMode.SYSTEM -> "跟随系统"
                    //     },
                    //     onClick = { showThemeDialog = true }
                    // )

                    // HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    ProfileSettingsItem(
                        icon = Icons.Default.AttachMoney,
                        iconTint = IOSColors.SystemGreen,
                        title = "货币符号",
                        subtitle = uiState.settings.currencySymbol,
                        onClick = { showCurrencyDialog = true }
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }

        // About section
        item {
            SectionTitle("关于")
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column {
                    ProfileSettingsItem(
                        icon = Icons.Default.HelpOutline,
                        iconTint = MaterialTheme.colorScheme.primary,
                        title = "使用帮助",
                        subtitle = "记账逻辑与操作说明",
                        onClick = onNavigateToHelp
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    ProfileSettingsItem(
                        icon = Icons.Default.Info,
                        iconTint = IOSColors.TextSecondary,
                        title = "版本信息",
                        subtitle = "Author: shineclaven",
                        showArrow = false,
                        onClick = { showVersionInfoDialog = true }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    ProfileSettingsItem(
                        icon = Icons.Default.Numbers,
                        iconTint = IOSColors.TextSecondary,
                        title = "版本号",
                        subtitle = "v${BuildConfig.VERSION_NAME}",
                        showArrow = versionInfo != null,
                        trailing = if (versionInfo != null) {
                            {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = IOSColors.SystemRed
                                ) {
                                    Text(
                                        "有更新 v${versionInfo!!.versionName}",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        } else null,
                        onClick = {
                            versionInfo?.let { info ->
                                val now = System.currentTimeMillis()
                                if (now - lastDownloadClickTime >= 60_000L) {
                                    lastDownloadClickTime = now
                                    VersionCheckUtil.downloadAndInstall(context, info)
                                }
                            }
                        }
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }

        // Footer
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "小小记账本",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Text(
                    text = "让记账更简单",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                )
            }
        }
    }

    // Color theme dialog
    if (showColorThemeDialog) {
        ColorThemeDialog(
            currentTheme = uiState.settings.colorTheme,
            onSelect = { theme ->
                viewModel.updateColorTheme(theme)
                showColorThemeDialog = false
            },
            onDismiss = { showColorThemeDialog = false }
        )
    }

    // Theme mode dialog
    if (showThemeDialog) {
        ThemeModeDialog(
            selectedMode = uiState.settings.themeMode,
            onSelect = { mode ->
                viewModel.updateThemeMode(mode)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false }
        )
    }

    // Currency dialog
    if (showCurrencyDialog) {
        CurrencyDialog(
            currentSymbol = uiState.settings.currencySymbol,
            onConfirm = { symbol ->
                viewModel.updateCurrencySymbol(symbol)
                showCurrencyDialog = false
            },
            onDismiss = { showCurrencyDialog = false }
        )
    }

    // Version info dialog
    if (showVersionInfoDialog) {
        AlertDialog(
            onDismissRequest = { showVersionInfoDialog = false },
            shape = RoundedCornerShape(20.dp),
            title = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "小小记账本",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "v${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Author",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Shineclaven",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Email,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "E-mail",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "swufe2003@gmail.com",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                    HorizontalDivider()
                    Text(
                        text = "让记账更简单",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showVersionInfoDialog = false }) {
                    Text("确定")
                }
            }
        )
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = Modifier.padding(start = 32.dp, bottom = 8.dp, top = 8.dp)
    )
}

@Composable
private fun ProfileSettingsItem(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    showArrow: Boolean = true,
    onClick: () -> Unit = {},
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(iconTint.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }

        if (trailing != null) {
            trailing()
        } else if (showArrow) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun ColorThemeDialog(
    currentTheme: ColorTheme,
    onSelect: (ColorTheme) -> Unit,
    onDismiss: () -> Unit
) {
    val groupedThemes = ThemeColorPreviews.byGroup()
    val groupOrder = ThemeColorPreviews.groupOrder
    val groupDescriptions = mapOf(
        "经典" to "iOS 风格经典配色",
        "系统" to "跟随手机品牌风格",
        "商务" to "专业商务，数据可视化",
        "生活" to "清新自然，轻松记账",
        "极简" to "高对比度，数字一目了然",
        "渐变" to "年轻趣味，游戏化记账"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Column {
                Text(
                    text = "选择主题颜色",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "共 ${ThemeColorPreviews.themes.size} 种主题可选",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                groupOrder.forEach { group ->
                    val themesInGroup = groupedThemes[group] ?: return@forEach

                    // Group header
                    item {
                        Column(modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)) {
                            Text(
                                text = group,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                            groupDescriptions[group]?.let { desc ->
                                Text(
                                    text = desc,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Themes in 3-column grid
                    val rows = themesInGroup.chunked(3)
                    rows.forEach { rowItems ->
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowItems.forEach { preview ->
                                    val isSelected = preview.name == currentTheme.displayName
                                    val themeColor = ColorTheme.entries.find { it.displayName == preview.name }

                                    CompactThemeItem(
                                        preview = preview,
                                        isSelected = isSelected,
                                        onClick = { themeColor?.let { onSelect(it) } },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                // Fill remaining space if row is not full
                                repeat(3 - rowItems.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

@Composable
private fun CompactThemeItem(
    preview: ThemeColorPreview,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) preview.primary else Color.Transparent,
        label = "border_color"
    )

    Card(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = preview.background
        ),
        border = if (isSelected) BorderStroke(2.dp, borderColor) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Color dots row
            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(preview.primary)
                )
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(preview.primaryLight)
                )
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(preview.secondary)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = preview.name,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 10.sp
                ),
                color = if (isSelected) preview.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = preview.primary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun ThemeModeDialog(
    selectedMode: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = "深色模式",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column {
                listOf(
                    "跟随系统" to ThemeMode.SYSTEM,
                    "关闭" to ThemeMode.LIGHT,
                    "开启" to ThemeMode.DARK
                ).forEach { (label, mode) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(mode) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        RadioButton(
                            selected = selectedMode == mode,
                            onClick = { onSelect(mode) }
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun CurrencyDialog(
    currentSymbol: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var currencyInput by remember { mutableStateOf(currentSymbol) }
    val commonSymbols = listOf("¥", "$", "€", "£", "₩", "₹")

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = "货币符号",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = currencyInput,
                    onValueChange = { if (it.length <= 3) currencyInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("自定义符号") }
                )

                Text(
                    text = "常用符号",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    commonSymbols.take(3).forEach { symbol ->
                        FilterChip(
                            selected = currencyInput == symbol,
                            onClick = { currencyInput = symbol },
                            label = { Text(symbol) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    commonSymbols.drop(3).forEach { symbol ->
                        FilterChip(
                            selected = currencyInput == symbol,
                            onClick = { currencyInput = symbol },
                            label = { Text(symbol) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (currencyInput.isNotBlank()) {
                        onConfirm(currencyInput)
                    }
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
    )
}
