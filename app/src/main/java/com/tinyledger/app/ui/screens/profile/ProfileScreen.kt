package com.tinyledger.app.ui.screens.profile

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import kotlinx.coroutines.delay
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
    onNavigateToAutoAccounting: () -> Unit = {},
    onNavigateToDarkModeSettings: () -> Unit = {},
    onNavigateToThemeColor: () -> Unit = {},
    onNavigateToBackup: () -> Unit = {},
    onNavigateToReimbursement: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var showThemeDialog by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showVersionInfoDialog by remember { mutableStateOf(false) }
    var versionExpanded by remember { mutableStateOf(false) }
    var lastDownloadClickTime by remember { mutableLongStateOf(0L) }

    var soundEnabled by remember {
        mutableStateOf(TransactionNotificationService.isSoundEnabled(context))
    }
    var vibrationEnabled by remember {
        mutableStateOf(TransactionNotificationService.isVibrationEnabled(context))
    }

    // Version update check
    var versionInfo by remember { mutableStateOf<VersionInfo?>(null) }
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            versionInfo = VersionCheckUtil.checkUpdate()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 固定区域：小小记账本卡片
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .padding(top = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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

        // 固定区域：四个快捷入口（全部账户、预算管理、备份管理、报销管理）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SmallActionCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.AccountBalance,
                label = "全部账户",
                tint = IOSColors.Primary,
                onClick = onNavigateToAccounts
            )
            SmallActionCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.AccountBalanceWallet,
                label = "预算管理",
                tint = IOSColors.SystemOrange,
                onClick = onNavigateToBudget
            )
            SmallActionCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.CloudSync,
                label = "报销管理",
                tint = IOSColors.SystemGreen,
                onClick = onNavigateToReimbursement
            )
            SmallActionCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.ReceiptLong,
                label = "备份管理",
                tint = IOSColors.SystemPurple,
                onClick = onNavigateToBackup
            )
        }

        // 可滚动区域
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
        // System settings section
        item {
            SectionTitle("系统设置")
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    // Auto-accounting entry
                    ProfileSettingsItem(
                        icon = Icons.Default.AutoFixHigh,
                        iconTint = IOSColors.SystemOrange,
                        title = "自动记账",
                        subtitle = "通知监听、权限管理、后台锁定",
                        onClick = onNavigateToAutoAccounting
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
                }
            }
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }

        // Data management section
        item {
            SectionTitle("数据管理")
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    // 截屏数据导入
                    ProfileSettingsItem(
                        icon = Icons.Default.Screenshot,
                        iconTint = IOSColors.SystemTeal,
                        title = "截屏数据导入",
                        subtitle = "选择支付截图自动识别金额",
                        onClick = onNavigateToScreenshotAccounting
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
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    ProfileSettingsItem(
                        icon = Icons.Default.Palette,
                        iconTint = Color(uiState.colorScheme.primaryColor),
                        title = "主题颜色",
                        subtitle = uiState.settings.colorTheme.displayName,
                        onClick = onNavigateToThemeColor
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    // 深色模式
                    ProfileSettingsItem(
                        icon = Icons.Default.DarkMode,
                        iconTint = IOSColors.SystemIndigo,
                        title = "深色模式",
                        subtitle = when (uiState.settings.themeMode) {
                            ThemeMode.LIGHT -> "关闭"
                            ThemeMode.DARK -> "开启"
                            ThemeMode.SYSTEM -> "跟随系统"
                        },
                        onClick = onNavigateToDarkModeSettings
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

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
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                        title = "版权信息",
                        subtitle = "Author: shineclaven",
                        showArrow = false,
                        onClick = { showVersionInfoDialog = true }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    // 版本号 - 支持展开显示更新内容
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (versionInfo != null) {
                                        versionExpanded = !versionExpanded
                                    }
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(
                                        IOSColors.TextSecondary.copy(alpha = 0.1f),
                                        RoundedCornerShape(8.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Numbers,
                                    contentDescription = null,
                                    tint = IOSColors.TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "版本号",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "v${BuildConfig.VERSION_NAME}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }

                            if (versionInfo != null) {
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
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = if (versionExpanded)
                                        Icons.Default.KeyboardArrowUp
                                    else
                                        Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // 展开的更新内容
                        if (versionExpanded && versionInfo != null) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .padding(bottom = 16.dp)
                            ) {
                                // "确认更新"按钮
                                Button(
                                    onClick = {
                                        val now = System.currentTimeMillis()
                                        if (now - lastDownloadClickTime >= 60_000L) {
                                            lastDownloadClickTime = now
                                            VersionCheckUtil.downloadAndInstall(context, versionInfo!!)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF4CAF50)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Text(
                                        "确认更新",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "更新内容：",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Text(
                                    text = versionInfo!!.updateLog.ifBlank { "暂无更新说明" },
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 20.sp
                                    )
                                )
                            }
                        }
                    }
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
        // 弹性抖动动画
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
            onDismissRequest = { showVersionInfoDialog = false },
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
                        .graphicsLayer {
                            shadowElevation = 16f
                        },
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // 图标和标题
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                        
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "小小记账本",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "v${BuildConfig.VERSION_NAME}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        HorizontalDivider()
                        
                        // 作者信息
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Author",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Shineclaven",
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = FontWeight.SemiBold
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
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "E-mail",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "swufe2003@gmail.com",
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                }
                            }
                        }
                        
                        HorizontalDivider()
                        
                        Text(
                            text = "让记账更简单",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                        
                        // 确定按钮 - 居中
                        Button(
                            onClick = { showVersionInfoDialog = false },
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .height(48.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 8.dp,
                                pressedElevation = 4.dp
                            )
                        ) {
                            Text(
                                "确定",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
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
private fun ThemeModeDialog(
    selectedMode: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        containerColor = MaterialTheme.colorScheme.surface,
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
    
    // 弹性抖动动画
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
                    .graphicsLayer {
                        shadowElevation = 16f
                    },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // 标题
                    Text(
                        text = "货币符号",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    // 输入框
                    OutlinedTextField(
                        value = currencyInput,
                        onValueChange = { if (it.length <= 3) currencyInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("自定义符号") },
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    // 常用符号标题
                    Text(
                        text = "常用符号",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )
                    
                    // 常用符号网格
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            commonSymbols.take(3).forEach { symbol ->
                                FilterChip(
                                    selected = currencyInput == symbol,
                                    onClick = { currencyInput = symbol },
                                    label = { 
                                        Text(
                                            symbol,
                                            style = MaterialTheme.typography.titleMedium
                                        ) 
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
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
                                    label = { 
                                        Text(
                                            symbol,
                                            style = MaterialTheme.typography.titleMedium
                                        ) 
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                    }
                    
                    // 按钮区域
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 确定按钮
                        Button(
                            onClick = {
                                if (currencyInput.isNotBlank()) {
                                    onConfirm(currencyInput)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .height(48.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 8.dp,
                                pressedElevation = 4.dp
                            )
                        ) {
                            Text(
                                "确定",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        // 取消按钮
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .height(48.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            border = BorderStroke(
                                2.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            )
                        ) {
                            Text(
                                "取消",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SmallActionCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null,
                    tint = tint, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium))
        }
    }
}
