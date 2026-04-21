package com.tinyledger.app.ui.screens.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.*
import android.content.res.Configuration
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
    onNavigateToAutoAccounting: () -> Unit = {},
    onNavigateToThemeColor: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
    updateCheckViewModel: UpdateCheckViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val updateCheckState by updateCheckViewModel.uiState.collectAsState()
    var showThemeDialog by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }

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
                        onClick = onNavigateToThemeColor,
                        enabled = true
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
                    
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    
                    // 截屏数据导入
                    IosSettingsItem(
                        icon = Icons.Default.Image,
                        iconTint = IOSColors.SystemPurple,
                        title = "截屏数据导入",
                        subtitle = "从截屏识别收支记录",
                        onClick = { /* TODO: Navigate to screenshot import */ }
                    )
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            // 系统设置
            item {
                SettingsSection(title = "系统设置")
            }
            
            item {
                IosSettingsCard {
                    // 自动记账设置入口
                    IosSettingsItem(
                        icon = Icons.Default.AutoFixHigh,
                        iconTint = IOSColors.SystemOrange,
                        title = "自动记账",
                        subtitle = "通知监听、权限管理、后台锁定",
                        onClick = onNavigateToAutoAccounting
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
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    iconTint.copy(alpha = if (enabled) 0.1f else 0.05f),
                    RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) iconTint else iconTint.copy(alpha = 0.4f),
                modifier = Modifier.size(18.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
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
        containerColor = MaterialTheme.colorScheme.surface,
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
        containerColor = MaterialTheme.colorScheme.surface,
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
