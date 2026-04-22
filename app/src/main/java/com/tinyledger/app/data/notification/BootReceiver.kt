package com.tinyledger.app.data.notification

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * 开机自启动接收器
 *
 * 手机重启后，NotificationListenerService 会被系统断开且不会自动重连。
 * 此接收器在收到 BOOT_COMPLETED 广播后，主动请求系统重新绑定通知监听服务。
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return

        val action = intent.action
        Log.d(TAG, "收到广播: $action")

        when (action) {
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",        // 小米快速启动
            "com.miui.home.action.LAUNCH",                    // 小米桌面启动
            "android.intent.action.LOCKED_BOOT_COMPLETED",    // 加密后启动完成
            "android.intent.action.MY_PACKAGE_REPLACED" -> {  // 应用更新
                rebindNotificationListener(context)
            }
        }
    }

    private fun rebindNotificationListener(context: Context) {
        try {
            // 检查通知监听权限是否已授予
            if (!TransactionNotificationService.hasPermission(context)) {
                Log.d(TAG, "通知监听权限未授予，跳过重绑")
                return
            }

            // 检查自动记账是否已开启
            if (!TransactionNotificationService.isEnabled(context)) {
                Log.d(TAG, "自动记账未开启，跳过重绑")
                return
            }

            // 请求系统重新绑定 NotificationListenerService
            val cn = ComponentName(context, TransactionNotificationService::class.java)
            // 直接使用反射调用 requestRebind（隐藏 API）
            requestRebindLegacy(context, cn)
        } catch (e: Exception) {
            Log.e(TAG, "重绑通知监听服务失败", e)
        }
    }

    private fun requestRebindLegacy(context: Context, cn: ComponentName) {
        try {
            // 传统方式：通过反射调用 requestRebind
            val method = android.service.notification.NotificationListenerService::class.java
                .getDeclaredMethod("requestRebind", ComponentName::class.java)
            method.isAccessible = true
            method.invoke(null, cn)
            Log.d(TAG, "通知监听服务重绑请求已发送(传统方式)")
        } catch (e: Exception) {
            Log.w(TAG, "传统重绑方式失败", e)
            // 最后手段：启动服务所在包，触发系统重新绑定
            try {
                val pm = context.packageManager
                pm.setComponentEnabledSetting(
                    cn,
                    android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    android.content.pm.PackageManager.DONT_KILL_APP
                )
                pm.setComponentEnabledSetting(
                    cn,
                    android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    android.content.pm.PackageManager.DONT_KILL_APP
                )
                Log.d(TAG, "通过 toggle component state 重绑通知监听服务")
            } catch (e2: Exception) {
                Log.e(TAG, "toggle component state 也失败", e2)
            }
        }
    }
}
