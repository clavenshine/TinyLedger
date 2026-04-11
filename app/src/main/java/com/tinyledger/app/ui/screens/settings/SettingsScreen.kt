package com.tinyledger.app.ui.screens.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import android.content.res.Configuration
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tinyledger.app.BuildConfig
import com.tinyledger.app.data.notification.TransactionNotificationService
import com.tinyledger.app.domain.model.AppColorScheme
import com.tinyledger.app.domain.model.ColorTheme
import com.tinyledger.app.domain.model.ThemeMode
import com.tinyledger.app.ui.components.UpdateCheckDialog
import com.tinyledger.app.ui.components.UpdateCheckingDialog
import com.tinyledger.app.ui.theme.IOSColors
import com.tinyledger.app.ui.theme.ThemeColorPreview
import com.tinyledger.app.ui.theme.ThemeColorPreviews
import com.tinyledger.app.ui.viewmodel.SettingsViewModel
import com.tinyledger.app.ui.viewmodel.UpdateCheckViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToAutoImport: (ImportType) -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
    updateCheckViewModel: UpdateCheckViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val updateCheckState by updateCheckViewModel.uiState.collectAsState()
    var showThemeDialog by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showColorThemeDialog by remember { mutableStateOf(false) }

    // 通知监听开关状态（从 SharedPreferences 读取）
    var notificationEnabled by remember {
        mutableStateOf(TransactionNotificationService.isEnabled(context))
    }
    // 是否已授权通知监听权限
    var hasNotificationPermission by remember {
        mutableStateOf(TransactionNotificationService.hasPermission(context))
    }
    // 每次进入页面重新检查权限
    LaunchedEffect(Unit) {
        hasNotificationPermission = TransactionNotificationService.hasPermission(context)
    }

    // 自动检查版本更新（静默，只显示红点提示）
    LaunchedEffect(Unit) {
        updateCheckViewModel.silentCheckForUpdate()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "设置",
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp)
        ) {
            // 外观设置
            item {
                SettingsSection(title = "外观")
            }
            
            item {
                IosSettingsCard {
                    // 主题颜色
                    IosSettingsItem(
                        icon = Icons.Default.Palette,
                        iconTint = Color(uiState.colorScheme.primaryColor),
                        title = "主题颜色",
                        subtitle = uiState.settings.colorTheme.displayName,
                        onClick = { showColorThemeDialog = true }
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    
                    // 深色模式 - 已隐藏（功能代码保留）
                    // IosSettingsItem(
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
                    // 
                    // HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    
                    // 货币符号
                    IosSettingsItem(
                        icon = Icons.Default.AttachMoney,
                        iconTint = IOSColors.SystemGreen,
                        title = "货币符号",
                        subtitle = uiState.settings.currencySymbol,
                        onClick = { showCurrencyDialog = true }
                    )
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            // 数据导入
            item {
                SettingsSection(title = "数据导入")
            }
            
            item {
                IosSettingsCard {
                    // 导入短信记录
                    IosSettingsItem(
                        icon = Icons.Default.Sms,
                        iconTint = IOSColors.Primary,
                        title = "导入短信收支记录",
                        subtitle = "从银行短信自动识别收支",
                        onClick = { onNavigateToAutoImport(ImportType.SMS) }
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    
                    // 导入微信记录（CSV/xlsx）
                    IosSettingsItem(
                        icon = Icons.Default.Chat,
                        iconTint = IOSColors.AccountWechat,
                        title = "导入微信账单",
                        subtitle = "导入微信导出的 CSV / xlsx 账单文件",
                        onClick = { onNavigateToAutoImport(ImportType.WECHAT) }
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    
                    // 导入支付宝记录（CSV/xlsx）
                    IosSettingsItem(
                        icon = Icons.Default.Payment,
                        iconTint = IOSColors.AccountAlipay,
                        title = "导入支付宝账单",
                        subtitle = "导入支付宝导出的 CSV / xlsx 账单文件",
                        onClick = { onNavigateToAutoImport(ImportType.ALIPAY) }
                    )
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            // 智能记账
            item {
                SettingsSection(title = "智能记账")
            }
            
            item {
                IosSettingsCard {
                    // 通知监听自动记账
                    NotificationListenerItem(
                        hasPermission = hasNotificationPermission,
                        enabled = notificationEnabled,
                        onToggle = { newEnabled ->
                            if (!hasNotificationPermission) {
                                // 跳转系统通知使用权设置页
                                try {
                                    context.startActivity(
                                        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                    )
                                } catch (_: Exception) {}
                            } else {
                                notificationEnabled = newEnabled
                                TransactionNotificationService.setEnabled(context, newEnabled)
                            }
                        },
                        onGoToSettings = {
                            try {
                                context.startActivity(
                                    Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                )
                            } catch (_: Exception) {}
                        }
                    )
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            // 关于
            item {
                SettingsSection(title = "关于")
            }
            
            item {
                IosSettingsCard {
                    IosSettingsItem(
                        icon = Icons.Default.Info,
                        iconTint = IOSColors.TextSecondary,
                        title = "版本信息",
                        subtitle = "Author: shineclaven",
                        showArrow = false,
                        onClick = {}
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    
                    // 版本号项 - 支持点击检查更新
                    Box(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IosSettingsItem(
                            icon = Icons.Default.Numbers,
                            iconTint = IOSColors.TextSecondary,
                            title = "版本号",
                            subtitle = "v${BuildConfig.VERSION_NAME}",
                            showArrow = updateCheckState.hasNewVersion,
                            onClick = {
                                // 点击版本号检查更新
                                updateCheckViewModel.checkForUpdate()
                            }
                        )
                        
                        // 版本更新提示标记
                        if (updateCheckState.hasNewVersion) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .padding(start = 16.dp)
                                    .clip(CircleShape)
                                    .size(10.dp)
                                    .background(Color.Red)
                            )
                        }
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
            
            // 底部版权
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

    // 主题颜色选择对话框
    if (showColorThemeDialog) {
        ColorThemeDialog(
            currentTheme = uiState.settings.colorTheme,
            themeMode = uiState.settings.themeMode,
            onSelect = { theme ->
                viewModel.updateColorTheme(theme)
                showColorThemeDialog = false
            },
            onDismiss = { showColorThemeDialog = false }
        )
    }

    // 主题模式对话框
    if (showThemeDialog) {
        IosSelectDialog(
            title = "深色模式",
            options = listOf(
                "跟随系统" to ThemeMode.SYSTEM,
                "关闭" to ThemeMode.LIGHT,
                "开启" to ThemeMode.DARK
            ),
            selectedOption = uiState.settings.themeMode,
            onSelect = { mode ->
                viewModel.updateThemeMode(mode)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false }
        )
    }

    // 货币符号对话框
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

    // 版本检查 - 检查中
    if (updateCheckState.isChecking) {
        UpdateCheckingDialog(
            onDismiss = { updateCheckViewModel.dismissDialog() },
            message = "检查更新中..."
        )
    }

    // 版本检查 - 发现新版本
    if (updateCheckState.showDialog && updateCheckState.latestRelease != null) {
        UpdateCheckDialog(
            releaseInfo = updateCheckState.latestRelease!!,
            onInstall = {
                updateCheckViewModel.downloadAndInstall(context)
            },
            onCancel = {
                updateCheckViewModel.dismissDialog()
            },
            isDownloading = updateCheckState.isDownloading
        )
    }
}

enum class ImportType {
    SMS, WECHAT, ALIPAY
}

@Composable
private fun NotificationListenerItem(
    hasPermission: Boolean,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onGoToSettings: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(IOSColors.SystemOrange.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = IOSColors.SystemOrange,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "通知自动记账",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = if (hasPermission) "监听微信/支付宝通知，自动识别收支"
                    else "需要开启通知使用权限",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (hasPermission)
                            MaterialTheme.colorScheme.onSurfaceVariant
                        else IOSColors.SystemOrange
                    )
                )
            }

            if (hasPermission) {
                Switch(
                    checked = enabled,
                    onCheckedChange = onToggle
                )
            } else {
                TextButton(onClick = onGoToSettings) {
                    Text("去授权", color = IOSColors.SystemOrange)
                }
            }
        }

        if (!hasPermission) {
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onGoToSettings)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "前往设置 → 通知使用权，为小小记账本开启权限，即可在收到微信/支付宝收款通知时自动记账",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                )
            }
        }
    }
}

@Composable
private fun ColorThemeDialog(
    currentTheme: ColorTheme,
    themeMode: ThemeMode,
    onSelect: (ColorTheme) -> Unit,
    onDismiss: () -> Unit
) {
    // 判断当前是否处于深色模式
    val isSystemDark = (LocalContext.current.resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    val isDarkMode = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemDark
        ThemeMode.LIGHT -> false
    }

    // 深色模式下不适合的主题集合
    val unsuitableThemes = if (isDarkMode) AppColorScheme.darkModeUnsuitableThemes else emptySet()
    // 分组标签与 ColorTheme 枚举的对应映射（顺序与 ThemeColorPreviews.themes 一致）
    val previewToTheme: Map<String, ColorTheme> = mapOf(
        "iOS蓝" to ColorTheme.IOS_BLUE,
        "优雅紫" to ColorTheme.PURPLE,
        "自然绿" to ColorTheme.GREEN,
        "活力橙" to ColorTheme.ORANGE,
        "少女粉" to ColorTheme.PINK,
        "清新青" to ColorTheme.TEAL,
        "深邃靛" to ColorTheme.INDIGO,
        "经典棕" to ColorTheme.BROWN,
        "iOS系统" to ColorTheme.IOS_THEME,
        "华为系统" to ColorTheme.HUAWEI_THEME,
        "小米系统" to ColorTheme.XIAOMI_THEME,
        "F1 经典金融" to ColorTheme.F1_FINANCE,
        "F2 资产增长" to ColorTheme.F2_WEALTH,
        "F3 冷静智慧" to ColorTheme.F3_RATIONAL,
        "F4 复古账本" to ColorTheme.F4_VINTAGE,
        "F5 高端理财" to ColorTheme.F5_PREMIUM,
        "F6 警示超支" to ColorTheme.F6_ALERT,
        "F7 清爽商务" to ColorTheme.F7_BUSINESS,
        "L1 经典存钱" to ColorTheme.L1_SAVINGS,
        "L2 充满活力" to ColorTheme.L2_VITALITY,
        "L3 女性向记账" to ColorTheme.L3_BLOSSOM,
        "L4 治愈旅行" to ColorTheme.L4_HEALING,
        "L5 梦幻清单" to ColorTheme.L5_DREAM,
        "L6 冷淡极简" to ColorTheme.L6_MINIMAL,
        "L7 日常开销" to ColorTheme.L7_DAILY,
        "M1 纸质账本" to ColorTheme.M1_BLACKWHITE,
        "M2 柔和黑白" to ColorTheme.M2_SOFTBLACK,
        "M3 高对比" to ColorTheme.M3_CONTRAST,
        "M4 红黑冲击" to ColorTheme.M4_REDBLACK,
        "M5 冷静克制" to ColorTheme.M5_CYANDARK,
        "M7 传统会计" to ColorTheme.M7_ACCOUNTING,
        "Y1 科技感" to ColorTheme.Y1_TECH,
        "Y2 少女心" to ColorTheme.Y2_GIRL,
        "Y3 梦想基金" to ColorTheme.Y3_DREAM_FUND,
        "Y4 搞钱" to ColorTheme.Y4_MONEY,
        "Y5 活力运动" to ColorTheme.Y5_SPORT,
        "Y7 绿色生活" to ColorTheme.Y7_ECO,
        "Y8 清爽夏季" to ColorTheme.Y8_SUMMER
    )

    val groupOrder = listOf("原有", "系统", "商务", "生活", "极简", "渐变")
    val groupLabel = mapOf(
        "原有" to "经典配色",
        "系统" to "系统品牌主题",
        "商务" to "专业商务型",
        "生活" to "清新生活型",
        "极简" to "极简高效型",
        "渐变" to "年轻渐变型"
    )
    val grouped = ThemeColorPreviews.byGroup()

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = "选择主题颜色",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(480.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                groupOrder.forEach { groupKey ->
                    val previews = (grouped[groupKey] ?: return@forEach).filter { preview ->
                        val mapped = previewToTheme[preview.name]
                        mapped == null || mapped !in unsuitableThemes
                    }
                    if (previews.isEmpty()) return@forEach
                    item {
                        Text(
                            text = groupLabel[groupKey] ?: groupKey,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = androidx.compose.ui.Modifier.padding(
                                start = 4.dp, top = 12.dp, bottom = 6.dp
                            )
                        )
                    }
                    // 每行两列：手动分组为 pairs
                    val rows = previews.chunked(2)
                    rows.forEach { rowItems ->
                        item {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = androidx.compose.ui.Modifier.fillMaxWidth()
                            ) {
                                rowItems.forEach { preview ->
                                    val mapped = previewToTheme[preview.name]
                                    val isSelected = mapped == currentTheme
                                    Box(modifier = androidx.compose.ui.Modifier.weight(1f)) {
                                        ColorThemeItem(
                                            preview = preview,
                                            isSelected = isSelected,
                                            onClick = { mapped?.let { onSelect(it) } }
                                        )
                                    }
                                }
                                // 如果最后一行只有1项，补空
                                if (rowItems.size == 1) {
                                    Box(modifier = androidx.compose.ui.Modifier.weight(1f))
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
                Text("取消")
            }
        }
    )
}

@Composable
private fun ColorThemeItem(
    preview: ThemeColorPreview,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) preview.primary else Color.Transparent,
        label = "border_color"
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (isSelected) {
            BorderStroke(2.dp, borderColor)
        } else null
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 颜色预览
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(preview.primary)
                )
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(preview.primaryLight)
                )
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(preview.secondary)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = preview.name,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                ),
                color = if (isSelected) preview.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (isSelected) {
                Spacer(modifier = Modifier.height(4.dp))
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = preview.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp, top = 8.dp)
    )
}

@Composable
private fun IosSettingsCard(
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(content = content)
    }
}

@Composable
private fun IosSettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    showArrow: Boolean = true,
    onClick: () -> Unit
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
                .background(
                    iconTint.copy(alpha = 0.1f),
                    RoundedCornerShape(8.dp)
                ),
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
        
        if (showArrow) {
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
private fun IosSelectDialog(
    title: String,
    options: List<Pair<String, ThemeMode>>,
    selectedOption: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column {
                options.forEach { (label, mode) ->
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
                            selected = selectedOption == mode,
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
