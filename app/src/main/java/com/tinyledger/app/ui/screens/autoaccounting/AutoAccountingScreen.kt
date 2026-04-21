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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
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
    var hasNotificationListenerPermission by remember {
        mutableStateOf(TransactionNotificationService.hasPermission(context))
    }
    
    // 无感自动记账
    var seamlessEnabled by remember {
        mutableStateOf(TransactionNotificationService.isSeamlessEnabled(context))
    }
    
    // 系统权限状态
    var hasSystemNotificationPermission by remember { mutableStateOf(checkNotificationPermission(context)) } // 通知权限
    var canDrawOverlays by remember { mutableStateOf(false) } // 悬浮窗权限
    var isIgnoringBatteryOptimizations by remember { mutableStateOf(false) } // 忽略电池优化
    var isAutoStartEnabled by remember { mutableStateOf(false) } // 自启动
    var isNotPowerSaveMode by remember { mutableStateOf(!isPowerSaveMode(context)) } // 非省电模式
    var isLockedToBackground by remember { mutableStateOf(false) } // 锁定到后台（自动检测）
    var showLockConfirmDialog by remember { mutableStateOf(false) }
    var pendingLockConfirm by remember { mutableStateOf(false) }
    
    // 每次进入页面重新检查权限
    LaunchedEffect(Unit) {
        hasNotificationPermission = TransactionNotificationService.hasPermission(context)
        hasNotificationListenerPermission = TransactionNotificationService.hasPermission(context)
        hasSystemNotificationPermission = checkNotificationPermission(context)
        canDrawOverlays = Settings.canDrawOverlays(context)
        isIgnoringBatteryOptimizations = checkBatteryOptimization(context)
        isAutoStartEnabled = checkAutoStartPermission(context)
        // 检测应用是否已锁定至后台
        isLockedToBackground = checkIfLockedToBackground(context)
    }
    
    // 从其他设置页面返回时重新检查权限状态
    LaunchedEffect(hasNotificationPermission, isIgnoringBatteryOptimizations, isAutoStartEnabled) {
        hasNotificationPermission = TransactionNotificationService.hasPermission(context)
        hasNotificationListenerPermission = TransactionNotificationService.hasPermission(context)
        isIgnoringBatteryOptimizations = checkBatteryOptimization(context)
        isAutoStartEnabled = checkAutoStartPermission(context)
        isLockedToBackground = checkIfLockedToBackground(context)
    }
    
    // 检测从系统设置返回时触发锁定确认对话框并刷新所有权限状态
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // 刷新所有权限和设置状态
                hasNotificationPermission = TransactionNotificationService.hasPermission(context)
                hasNotificationListenerPermission = TransactionNotificationService.hasPermission(context)
                hasSystemNotificationPermission = checkNotificationPermission(context)
                notificationEnabled = TransactionNotificationService.isEnabled(context)
                seamlessEnabled = TransactionNotificationService.isSeamlessEnabled(context)
                canDrawOverlays = Settings.canDrawOverlays(context)
                isIgnoringBatteryOptimizations = checkBatteryOptimization(context)
                isAutoStartEnabled = checkAutoStartPermission(context)
                isNotPowerSaveMode = !isPowerSaveMode(context)
                isLockedToBackground = checkIfLockedToBackground(context)
                
                // 处理锁定确认
                if (pendingLockConfirm) {
                    showLockConfirmDialog = true
                    pendingLockConfirm = false
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
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
                                if (!newEnabled) {
                                    seamlessEnabled = false
                                    TransactionNotificationService.setSeamlessEnabled(context, false)
                                }
                            }
                        },
                        onGoToSettings = { openNotificationListenerSettings(context) }
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    
                    // 无感自动记账
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "无感自动记账",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = if (seamlessEnabled) "自动完成记账，无需确认" else "捕获交易后需手动确认",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                        
                        Switch(
                            checked = seamlessEnabled,
                            enabled = hasNotificationPermission && notificationEnabled,
                            onCheckedChange = { newEnabled ->
                                seamlessEnabled = newEnabled
                                TransactionNotificationService.setSeamlessEnabled(context, newEnabled)
                            }
                        )
                    }
                    
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    
                    // 允许后台运行
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                try {
                                    val intent = Intent()
                                    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                    intent.data = Uri.parse("package:${context.packageName}")
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    openAppSettings(context)
                                }
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(IOSColors.SystemGreen.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.BatteryChargingFull,
                                contentDescription = null,
                                tint = IOSColors.SystemGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "允许后台运行",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "保持自动记账持续工作",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                        
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
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
                        subtitle = "允许应用发送通知提醒",
                        enabled = hasSystemNotificationPermission,
                        onToggle = { openNotificationSettings(context) }
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    
                    // 通知使用权（核心）
                    PermissionItem(
                        icon = Icons.Default.NotificationsActive,
                        iconTint = IOSColors.SystemOrange,
                        title = "通知使用权（核心）",
                        subtitle = "监听银行短信通知，实现自动记账",
                        enabled = hasNotificationListenerPermission,
                        onToggle = { openNotificationListenerSettings(context) }
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
                        enabled = isNotPowerSaveMode,
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
                                    if (!isLockedToBackground) {
                                        // 标记等待确认，用户返回后会弹出确认对话框
                                        pendingLockConfirm = true
                                        openAppSettings(context)
                                    } else {
                                        isLockedToBackground = false
                                        context.getSharedPreferences("auto_accounting", Context.MODE_PRIVATE)
                                            .edit().putBoolean("locked_to_background", false).apply()
                                    }
                                }
                            )
                        }
                        
                        // 锁定说明 - 只在未锁定时显示
                        if (!isLockedToBackground) {
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
        
        // 后台锁定确认对话框 - 美化版
        if (showLockConfirmDialog) {
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
                onDismissRequest = { showLockConfirmDialog = false },
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
                            // 图标
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
                                    Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(44.dp)
                                )
                            }
                            
                            // 标题
                            Text(
                                text = "确认锁定状态",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            
                            // 说明文字
                            Text(
                                text = "您是否已在任务管理器中锁定了小小记账本？\n锁定后可确保自动记账服务持续运行",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                lineHeight = 24.sp
                            )
                            
                            // 按钮区域
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // 已锁定按钮
                                Button(
                                    onClick = {
                                        isLockedToBackground = true
                                        context.getSharedPreferences("auto_accounting", Context.MODE_PRIVATE)
                                            .edit().putBoolean("locked_to_background", true).apply()
                                        showLockConfirmDialog = false
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
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "已锁定",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                
                                // 尚未锁定按钮
                                OutlinedButton(
                                    onClick = {
                                        showLockConfirmDialog = false
                                    },
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
                                    Icon(
                                        Icons.Default.Cancel,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "尚未锁定",
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

private fun checkIfLockedToBackground(context: Context): Boolean {
    // 检测应用是否已锁定至后台
    // Android 没有直接的 API 检测任务锁定状态，但可以通过 ActivityManager 检测
    // 使用 SharedPreferences 记录用户设置的状态作为辅助
    val prefs = context.getSharedPreferences("auto_accounting", Context.MODE_PRIVATE)
    return prefs.getBoolean("locked_to_background", false)
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
