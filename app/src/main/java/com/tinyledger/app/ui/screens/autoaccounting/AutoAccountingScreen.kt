package com.tinyledger.app.ui.screens.autoaccounting

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tinyledger.app.data.notification.TransactionNotificationService
import com.tinyledger.app.ui.theme.IOSColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoAccountingScreen(
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    
    // 通知监听开关状态
    var notificationEnabled by remember {
        mutableStateOf(TransactionNotificationService.isEnabled(context))
    }
    var hasNotificationPermission by remember {
        mutableStateOf(TransactionNotificationService.hasPermission(context))
    }
    
    // 系统权限状态
    var canDrawOverlays by remember { mutableStateOf(false) } // 悬浮窗权限
    var isIgnoringBatteryOptimizations by remember { mutableStateOf(false) } // 忽略电池优化
    var isAutoStartEnabled by remember { mutableStateOf(false) } // 自启动
    var isLockedToBackground by remember { mutableStateOf(false) } // 锁定到后台
    var showLockInstructions by remember { mutableStateOf(false) } // 显示锁定说明
    
    // 每次进入页面重新检查权限
    LaunchedEffect(Unit) {
        hasNotificationPermission = TransactionNotificationService.hasPermission(context)
        canDrawOverlays = Settings.canDrawOverlays(context)
        isIgnoringBatteryOptimizations = checkBatteryOptimization(context)
        isAutoStartEnabled = checkAutoStartPermission(context)
    }
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "自动记账",
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp)
        ) {
            // 通知自动记账
            item {
                SettingsSection(title = "通知自动记账")
            }
            
            item {
                IosSettingsCard {
                    NotificationListenerItem(
                        hasPermission = hasNotificationPermission,
                        enabled = notificationEnabled,
                        onToggle = { newEnabled ->
                            if (!hasNotificationPermission) {
                                openNotificationListenerSettings(context)
                            } else {
                                notificationEnabled = newEnabled
                                TransactionNotificationService.setEnabled(context, newEnabled)
                            }
                        },
                        onGoToSettings = { openNotificationListenerSettings(context) }
                    )
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            // 系统权限设置
            item {
                SettingsSection(title = "系统权限设置")
            }
            
            item {
                IosSettingsCard {
                    // 通知权限
                    PermissionItem(
                        icon = Icons.Default.Notifications,
                        iconTint = IOSColors.SystemIndigo,
                        title = "通知权限",
                        subtitle = "允许应用发送通知",
                        enabled = checkNotificationPermission(context),
                        onToggle = { openNotificationSettings(context) }
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    
                    // 允许应用自动启动
                    PermissionItem(
                        icon = Icons.Default.PlayArrow,
                        iconTint = IOSColors.SystemGreen,
                        title = "允许应用自动启动",
                        subtitle = "确保重启后自动记账仍能工作",
                        enabled = isAutoStartEnabled,
                        onToggle = { openAutoStartSettings(context) }
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    
                    // 忽略电池优化
                    PermissionItem(
                        icon = Icons.Default.BatteryFull,
                        iconTint = IOSColors.SystemOrange,
                        title = "忽略电池优化",
                        subtitle = "避免系统杀掉后台服务",
                        enabled = isIgnoringBatteryOptimizations,
                        onToggle = { openBatteryOptimizationSettings(context) }
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    
                    // 关闭省电模式
                    PermissionItem(
                        icon = Icons.Default.PowerSettingsNew,
                        iconTint = IOSColors.SystemRed,
                        title = "关闭省电模式",
                        subtitle = "确保后台服务正常运行",
                        enabled = !isPowerSaveMode(context),
                        onToggle = { openPowerSaveSettings(context) }
                    )
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            // 后台锁定
            item {
                SettingsSection(title = "后台管理")
            }
            
            item {
                IosSettingsCard {
                    // 应用锁定至后台
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
                                    .background(IOSColors.SystemIndigo.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = IOSColors.SystemIndigo,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "应用锁定至后台（重要）",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "防止应用被系统清理",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                            
                            Switch(
                                checked = isLockedToBackground,
                                onCheckedChange = { 
                                    isLockedToBackground = it
                                    showLockInstructions = it
                                }
                            )
                        }
                        
                        // 锁定说明
                        if (showLockInstructions) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = IOSColors.SystemIndigo,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "请回到手机桌面，向上滑动手指，打开任务切换界面，长按小小记账本，点击[🔒锁定]图标",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 24.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
            
            // 提示文字
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = IOSColors.SystemIndigo.copy(alpha = 0.1f)
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = IOSColors.SystemIndigo,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "温馨提示",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = IOSColors.SystemIndigo
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "为了确保自动记账功能稳定运行，建议您开启以上所有权限。不同手机品牌的设置路径可能略有不同，如有疑问请查看使用帮助。",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = IOSColors.SystemIndigo,
                                lineHeight = 20.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String) {
    Text(
        text = title,
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
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(content = content)
    }
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
                    text = "前往设置 → 通知使用权，为小小记账本开启权限",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                )
            }
        }
    }
}

@Composable
private fun PermissionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    enabled: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
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
        
        TextButton(onClick = onToggle) {
            Text(
                text = if (enabled) "已开启" else "去开启",
                color = if (enabled) IOSColors.SystemGreen else IOSColors.SystemIndigo
            )
        }
    }
}

// 权限检查和设置跳转辅助函数

private fun checkBatteryOptimization(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        powerManager.isIgnoringBatteryOptimizations(context.packageName)
    } else {
        true
    }
}

private fun checkNotificationPermission(context: Context): Boolean {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
    return notificationManager?.areNotificationsEnabled() ?: false
}

private fun checkAutoStartPermission(context: Context): Boolean {
    // 不同厂商的自启动权限检查
    return try {
        when {
            isXiaomi() -> checkXiaomiAutoStart(context)
            isHuawei() -> checkHuaweiAutoStart(context)
            isOppo() -> checkOppoAutoStart(context)
            isVivo() -> checkVivoAutoStart(context)
            else -> true // 其他品牌默认认为已开启
        }
    } catch (e: Exception) {
        true
    }
}

private fun isPowerSaveMode(context: Context): Boolean {
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return powerManager.isPowerSaveMode
}

private fun isXiaomi() = Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true)
private fun isHuawei() = Build.MANUFACTURER.equals("Huawei", ignoreCase = true) || 
                         Build.MANUFACTURER.equals("Honor", ignoreCase = true)
private fun isOppo() = Build.MANUFACTURER.equals("Oppo", ignoreCase = true) || 
                       Build.MANUFACTURER.equals("OnePlus", ignoreCase = true)
private fun isVivo() = Build.MANUFACTURER.equals("Vivo", ignoreCase = true)

private fun checkXiaomiAutoStart(context: Context): Boolean {
    return try {
        val intent = Intent().setClassName(
            "com.miui.securitycenter",
            "com.miui.permcenter.autostart.AutoStartManagementActivity"
        )
        context.packageManager.resolveActivity(intent, 0) != null
    } catch (e: Exception) {
        false
    }
}

private fun checkHuaweiAutoStart(context: Context): Boolean {
    return try {
        val intent = Intent().setClassName(
            "com.huawei.systemmanager",
            "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
        )
        context.packageManager.resolveActivity(intent, 0) != null
    } catch (e: Exception) {
        false
    }
}

private fun checkOppoAutoStart(context: Context): Boolean {
    return try {
        val intent = Intent().setClassName(
            "com.coloros.safecenter",
            "com.coloros.safecenter.permission.startup.StartupAppListActivity"
        )
        context.packageManager.resolveActivity(intent, 0) != null
    } catch (e: Exception) {
        false
    }
}

private fun checkVivoAutoStart(context: Context): Boolean {
    return try {
        val intent = Intent().setClassName(
            "com.iqoo.secure",
            "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"
        )
        context.packageManager.resolveActivity(intent, 0) != null
    } catch (e: Exception) {
        false
    }
}

private fun openNotificationListenerSettings(context: Context) {
    try {
        context.startActivity(
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    } catch (_: Exception) {}
}

private fun openNotificationSettings(context: Context) {
    try {
        val intent = Intent().apply {
            action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        //  fallback to app settings
        openAppSettings(context)
    }
}

private fun openAutoStartSettings(context: Context) {
    try {
        when {
            isXiaomi() -> {
                Intent().apply {
                    setClassName(
                        "com.miui.securitycenter",
                        "com.miui.permcenter.autostart.AutoStartManagementActivity"
                    )
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }.let { context.startActivity(it) }
            }
            isHuawei() -> {
                Intent().apply {
                    setClassName(
                        "com.huawei.systemmanager",
                        "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
                    )
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }.let { context.startActivity(it) }
            }
            isOppo() -> {
                Intent().apply {
                    setClassName(
                        "com.coloros.safecenter",
                        "com.coloros.safecenter.permission.startup.StartupAppListActivity"
                    )
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }.let { context.startActivity(it) }
            }
            isVivo() -> {
                Intent().apply {
                    setClassName(
                        "com.iqoo.secure",
                        "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"
                    )
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }.let { context.startActivity(it) }
            }
            else -> openAppSettings(context)
        }
    } catch (_: Exception) {
        openAppSettings(context)
    }
}

private fun openBatteryOptimizationSettings(context: Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent().apply {
                action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }.let { context.startActivity(it) }
        } else {
            openAppSettings(context)
        }
    } catch (_: Exception) {
        openAppSettings(context)
    }
}

private fun openPowerSaveSettings(context: Context) {
    try {
        Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }.let { context.startActivity(it) }
    } catch (_: Exception) {
        openAppSettings(context)
    }
}

private fun openAppSettings(context: Context) {
    try {
        Intent().apply {
            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }.let { context.startActivity(it) }
    } catch (_: Exception) {}
}
