package com.tinyledger.app.data.notification

import android.app.Notification
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.tinyledger.app.data.local.dao.NotificationSmsDao
import com.tinyledger.app.data.local.entity.NotificationSmsEntity
import com.tinyledger.app.domain.model.AccountType
import com.tinyledger.app.domain.model.Category
import com.tinyledger.app.domain.model.Transaction
import com.tinyledger.app.domain.model.TransactionType
import com.tinyledger.app.domain.repository.AccountRepository
import com.tinyledger.app.domain.repository.TransactionRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.security.MessageDigest
import javax.inject.Inject

/**
 * 通知监听服务
 *
 * 功能一：监听微信、支付宝、京东、淘宝、美团、抖音等应用的收支通知，自动解析并记账。
 * 功能二：捕获银行短信通知，持久化到 Room 数据库。
 * 功能三：记账成功时根据用户设置播放提示音或震动。
 */
@AndroidEntryPoint
class TransactionNotificationService : NotificationListenerService() {

    @Inject lateinit var transactionRepository: TransactionRepository
    @Inject lateinit var accountRepository: AccountRepository
    @Inject lateinit var notificationSmsDao: NotificationSmsDao

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // 用于去重：防止短时间内同一笔通知重复记账
    private val recentHashes = LinkedHashMap<String, Long>(50, 0.75f, true)

    // ── 所有需要监听的支付类应用包名 ────────────────────────────────
    private val paymentPackages = mapOf(
        "com.tencent.mm" to "微信",                       // 微信
        "com.eg.android.AlipayGphone" to "支付宝",        // 支付宝
        "com.jingdong.app.mall" to "京东",                // 京东
        "com.jd.lib.unification" to "京东",               // 京东金融
        "com.taobao.taobao" to "淘宝",                    // 淘宝
        "com.tmall.wireless" to "天猫",                    // 天猫
        "com.meituan.retail.v2" to "美团",                // 美团
        "com.sankuai.meituan" to "美团",                   // 美团
        "com.dianping.v1" to "大众点评",                   // 大众点评
        "com.ss.android.ugc.aweme" to "抖音",             // 抖音
        "com.ss.android.ugc.aweme.lite" to "抖音极速版",  // 抖音极速版
        "com.kuaishou.nebula" to "快手",                   // 快手
        "com.unionpay" to "云闪付",                        // 云闪付
        "com.chinamworld.bocmbci" to "中国银行",           // 中国银行
        "com.icbc" to "工商银行",                           // 工商银行
        "com.chinamworld.main" to "建设银行",              // 建设银行
        "cmb.pb" to "招商银行",                             // 招商银行
        "com.abchina.abc" to "农业银行",                   // 农业银行
    )

    // 短信/消息类应用包名（银行短信捕获）
    private val smsPackages = setOf(
        "com.android.mms",
        "com.google.android.apps.messaging",
        "com.samsung.android.messaging",
        "com.miui.notification",
        "com.huawei.message",
        "com.coloros.smsprovider",
        "com.iqoo.secure",
        "com.bbk.etchat",
        "com.android.messaging",
        "com.oneplus.mms",
        "com.oppo.mms",
        "com.vivo.mms",
        "com.hihonor.mms",
    )

    // 银行短信特征关键词
    private val bankKeywords = listOf(
        "银行", "建行", "工行", "农行", "中行", "招行", "交行", "邮储",
        "支出", "消费", "扣款", "转出",
        "收入", "到账", "入账", "收款", "转入", "存入",
        "可用余额", "账户余额", "当前余额",
        "尾号", "账户", "储蓄卡", "信用卡",
        "CCB", "ICBC", "ABC", "BOC", "CMB", "CITIC"
    )

    companion object {
        private const val TAG = "TxNotifService"
        const val PREF_NAME = "notification_settings"
        const val KEY_ENABLED = "auto_notification_import_enabled"
        const val KEY_BANK_SMS_CAPTURE_ENABLED = "bank_sms_capture_enabled"
        const val KEY_SOUND_ENABLED = "notification_sound_enabled"
        const val KEY_VIBRATION_ENABLED = "notification_vibration_enabled"

        fun isEnabled(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(KEY_ENABLED, false)
        }

        fun setEnabled(context: Context, enabled: Boolean) {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
        }

        fun isBankSmsCaptureEnabled(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(KEY_BANK_SMS_CAPTURE_ENABLED, true)
        }

        fun setBankSmsCaptureEnabled(context: Context, enabled: Boolean) {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_BANK_SMS_CAPTURE_ENABLED, enabled).apply()
        }

        fun isSoundEnabled(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(KEY_SOUND_ENABLED, false)
        }

        fun setSoundEnabled(context: Context, enabled: Boolean) {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply()
        }

        fun isVibrationEnabled(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(KEY_VIBRATION_ENABLED, false)
        }

        fun setVibrationEnabled(context: Context, enabled: Boolean) {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_VIBRATION_ENABLED, enabled).apply()
        }

        fun hasPermission(context: Context): Boolean {
            val enabledListeners = android.provider.Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            ) ?: return false
            return enabledListeners.contains(context.packageName)
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "通知监听服务已连接")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d(TAG, "通知监听服务已断开，尝试重连")
        // 请求系统重新绑定
        requestRebind(android.content.ComponentName(this, TransactionNotificationService::class.java))
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        val packageName = sbn.packageName ?: return
        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: ""

        // 用最长的内容来解析
        val fullContent = listOf(bigText, text).maxByOrNull { it.length } ?: ""

        Log.d(TAG, "收到通知 pkg=$packageName title=$title text=${text.take(80)} bigText=${bigText.take(80)}")

        // ── 路径1：支付类应用 → 自动记账 ──────────────────────────────
        val appLabel = paymentPackages[packageName]
        if (appLabel != null) {
            if (!isEnabled(applicationContext)) {
                Log.d(TAG, "自动记账未开启，跳过 $packageName")
                return
            }
            handlePaymentNotification(packageName, appLabel, title, text, fullContent, subText)
            return
        }

        // ── 路径2：银行短信通知捕获 ──────────────────────────────────
        if (packageName in smsPackages && isBankSmsCaptureEnabled(applicationContext)) {
            handleBankSmsNotification(packageName, title, fullContent.ifBlank { text })
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // 路径1：支付通知 → 解析 → 自动记账
    // ══════════════════════════════════════════════════════════════════

    private fun handlePaymentNotification(
        packageName: String,
        appLabel: String,
        title: String,
        text: String,
        fullContent: String,
        subText: String
    ) {
        val combined = "$title $fullContent $subText".trim()
        if (combined.isBlank()) return

        // 去重：同一内容5分钟内不重复记账
        val dedupKey = md5("$packageName|${combined.take(100)}")
        val now = System.currentTimeMillis()
        synchronized(recentHashes) {
            val lastTime = recentHashes[dedupKey]
            if (lastTime != null && now - lastTime < 5 * 60 * 1000L) {
                Log.d(TAG, "跳过重复通知: $dedupKey")
                return
            }
            recentHashes[dedupKey] = now
            // 清理超过10分钟的旧记录
            recentHashes.entries.removeAll { now - it.value > 10 * 60 * 1000L }
        }

        val parsed = parsePaymentNotification(packageName, appLabel, title, text, fullContent)
        if (parsed == null) {
            Log.d(TAG, "无法解析为支付通知: $combined")
            return
        }

        Log.d(TAG, "解析成功: ${parsed.type} ¥${parsed.amount} [${parsed.note}]")

        serviceScope.launch {
            try {
                val accounts = accountRepository.getAllAccounts().first()
                val accountId = matchAccount(packageName, accounts)
                val category = inferCategory(parsed)

                val transaction = Transaction(
                    type = parsed.type,
                    category = category,
                    amount = parsed.amount,
                    note = parsed.note,
                    date = System.currentTimeMillis(),
                    accountId = accountId
                )
                transactionRepository.insertTransaction(transaction)
                Log.d(TAG, "自动记账成功: ${parsed.type} ¥${parsed.amount} [${parsed.note}]")

                // 播放提示音/震动
                playFeedback()
            } catch (e: Exception) {
                Log.e(TAG, "自动记账失败", e)
            }
        }
    }

    /**
     * 解析各平台支付通知
     */
    private fun parsePaymentNotification(
        packageName: String,
        appLabel: String,
        title: String,
        text: String,
        fullContent: String
    ): ParsedNotification? {
        return when (packageName) {
            "com.tencent.mm" -> parseWeChatNotification(title, text, fullContent)
            "com.eg.android.AlipayGphone" -> parseAlipayNotification(title, text, fullContent)
            else -> parseGenericPaymentNotification(appLabel, title, text, fullContent)
        }
    }

    // ── 微信通知解析 ──────────────────────────────────────────────
    private fun parseWeChatNotification(
        title: String,
        text: String,
        fullContent: String
    ): ParsedNotification? {
        val combined = "$title $fullContent".trim()

        // 微信支付相关的通知来自 "微信支付" 或 title 包含支付关键词
        val isPaymentNotif = title.contains("微信支付") ||
                title.contains("微信红包") ||
                title.contains("零钱") ||
                title.contains("转账") ||
                combined.contains("微信支付") ||
                combined.contains("支付成功") ||
                combined.contains("付款成功") ||
                combined.contains("收款成功") ||
                combined.contains("已收款") ||
                combined.contains("已付款")

        // 微信普通聊天消息 - 不解析
        if (!isPaymentNotif && !hasPaymentKeywords(combined)) {
            return null
        }

        val amount = extractAmount(combined) ?: return null

        return when {
            // 收入关键词
            combined.containsAny("收款", "到账", "已收款", "转账收款", "收到转账",
                "红包", "收到红包", "退款成功", "退款到账") -> {
                ParsedNotification(
                    type = TransactionType.INCOME,
                    amount = amount,
                    note = "微信收款: ${text.take(40)}"
                )
            }
            // 支出关键词
            combined.containsAny("付款", "支付成功", "已扣款", "已付款", "消费",
                "扣费", "充值成功", "支出", "付款成功") -> {
                ParsedNotification(
                    type = TransactionType.EXPENSE,
                    amount = amount,
                    note = "微信付款: ${text.take(40)}"
                )
            }
            else -> null
        }
    }

    // ── 支付宝通知解析 ────────────────────────────────────────────
    private fun parseAlipayNotification(
        title: String,
        text: String,
        fullContent: String
    ): ParsedNotification? {
        val combined = "$title $fullContent".trim()

        val isPaymentNotif = title.contains("支付宝") ||
                title.contains("花呗") ||
                title.contains("余额宝") ||
                combined.contains("支付宝") ||
                combined.contains("付款成功") ||
                combined.contains("收款成功")

        if (!isPaymentNotif && !hasPaymentKeywords(combined)) {
            return null
        }

        val amount = extractAmount(combined) ?: return null

        return when {
            combined.containsAny("收款", "到账", "已收款", "转账收到", "退款",
                "红包", "余额宝收益") -> {
                ParsedNotification(
                    type = TransactionType.INCOME,
                    amount = amount,
                    note = "支付宝收款: ${text.take(40)}"
                )
            }
            combined.containsAny("付款", "消费", "已扣款", "支付成功", "已付款",
                "花呗", "扣费", "充值", "缴费") -> {
                ParsedNotification(
                    type = TransactionType.EXPENSE,
                    amount = amount,
                    note = "支付宝消费: ${text.take(40)}"
                )
            }
            else -> null
        }
    }

    // ── 通用支付通知解析（京东、淘宝、美团、抖音等） ─────────────────
    private fun parseGenericPaymentNotification(
        appLabel: String,
        title: String,
        text: String,
        fullContent: String
    ): ParsedNotification? {
        val combined = "$title $fullContent".trim()

        // 必须包含支付相关关键词
        if (!hasPaymentKeywords(combined)) return null

        val amount = extractAmount(combined) ?: return null

        return when {
            combined.containsAny("收款", "到账", "退款", "退还", "收入", "返现",
                "已退款", "退款成功") -> {
                ParsedNotification(
                    type = TransactionType.INCOME,
                    amount = amount,
                    note = "${appLabel}收入: ${text.take(40)}"
                )
            }
            combined.containsAny("付款", "支付", "消费", "已扣款", "扣费",
                "已付款", "下单", "购买", "订单", "支付成功",
                "付款成功", "充值", "缴费") -> {
                ParsedNotification(
                    type = TransactionType.EXPENSE,
                    amount = amount,
                    note = "${appLabel}消费: ${text.take(40)}"
                )
            }
            else -> null
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // 路径2：银行短信通知
    // ══════════════════════════════════════════════════════════════════

    private fun handleBankSmsNotification(
        packageName: String,
        title: String,
        body: String
    ) {
        if (body.isBlank()) return

        val isBankSms = bankKeywords.any { body.contains(it, ignoreCase = true) }
        if (!isBankSms) return

        val address = title.trim()
        if (address.isBlank()) return

        Log.d(TAG, "捕获银行短信通知 [$packageName] $address: ${body.take(60)}")

        val minuteKey = System.currentTimeMillis() / 60000
        val hashInput = "$address|${body.take(100)}|$minuteKey"
        val hash = md5(hashInput)

        serviceScope.launch {
            try {
                notificationSmsDao.insert(
                    NotificationSmsEntity(
                        address = address,
                        body = body,
                        date = System.currentTimeMillis(),
                        packageName = packageName,
                        uniqueHash = hash
                    )
                )
                Log.d(TAG, "银行短信已存储: $address - ${body.take(40)}")
            } catch (e: Exception) {
                Log.e(TAG, "存储银行短信失败", e)
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // 辅助方法
    // ══════════════════════════════════════════════════════════════════

    /** 判断文本是否包含支付相关关键词 */
    private fun hasPaymentKeywords(text: String): Boolean {
        val keywords = listOf(
            "付款", "支付", "消费", "扣款", "扣费",
            "收款", "到账", "退款", "转账",
            "红包", "充值", "缴费",
            "支付成功", "付款成功", "收款成功",
            "¥", "￥", "元"
        )
        return keywords.any { text.contains(it) }
    }

    /**
     * 从文本中提取金额。
     * 强制要求：必须匹配到 "数字+元" 模式才认为是有效的交易金额。
     * 如果文本中没有 "XX元" 或 "XX.XX元" 的格式，一律返回 null，不予记账。
     */
    private fun extractAmount(text: String): Double? {
        // 必须包含 "数字+元" 模式，否则直接返回 null
        val yuanRegex = Regex("([0-9]+(?:\\.[0-9]{1,2})?)\\s*元")
        val yuanMatch = yuanRegex.find(text)
        if (yuanMatch == null) {
            Log.d(TAG, "未匹配到'数字+元'模式，跳过: ${text.take(80)}")
            return null
        }

        val amount = yuanMatch.groupValues[1].toDoubleOrNull()
        if (amount != null && amount > 0 && amount < 10_000_000) {
            return amount
        }

        return null
    }

    private fun md5(input: String): String {
        val bytes = MessageDigest.getInstance("MD5").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /** 记账成功后播放提示音/震动 */
    private fun playFeedback() {
        val context = applicationContext
        try {
            // 声音
            if (isSoundEnabled(context)) {
                val toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
                toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 200)
                // 延迟释放
                android.os.Handler(mainLooper).postDelayed({ toneGenerator.release() }, 500)
            }

            // 震动
            if (isVibrationEnabled(context)) {
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                    vm.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(200)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "播放反馈失败", e)
        }
    }

    data class ParsedNotification(
        val type: TransactionType,
        val amount: Double,
        val counterpart: String = "",
        val note: String = ""
    )

    private fun matchAccount(
        packageName: String,
        accounts: List<com.tinyledger.app.domain.model.Account>
    ): Long? {
        val targetType = when (packageName) {
            "com.tencent.mm" -> AccountType.WECHAT
            "com.eg.android.AlipayGphone" -> AccountType.ALIPAY
            else -> return null
        }
        return accounts.find { it.type == targetType }?.id
    }

    private fun inferCategory(parsed: ParsedNotification): Category {
        val note = parsed.note
        return if (parsed.type == TransactionType.INCOME) {
            when {
                note.containsAny("工资", "薪资") -> Category.fromId("salary", TransactionType.INCOME)
                note.containsAny("退款", "退还") -> Category.fromId("redpacket", TransactionType.INCOME)
                note.containsAny("红包", "转账") -> Category.fromId("redpacket", TransactionType.INCOME)
                else -> Category.fromId("redpacket", TransactionType.INCOME)
            }
        } else {
            when {
                note.containsAny("餐", "饭", "外卖", "美团", "饿了么") ->
                    Category.fromId("food", TransactionType.EXPENSE)
                note.containsAny("打车", "滴滴", "地铁", "公交", "出行") ->
                    Category.fromId("transport", TransactionType.EXPENSE)
                note.containsAny("购物", "京东", "淘宝", "天猫", "拼多多") ->
                    Category.fromId("shopping", TransactionType.EXPENSE)
                note.containsAny("娱乐", "电影", "游戏", "抖音") ->
                    Category.fromId("entertainment", TransactionType.EXPENSE)
                note.containsAny("医院", "药", "医疗") ->
                    Category.fromId("medical", TransactionType.EXPENSE)
                note.containsAny("话费", "流量", "通讯") ->
                    Category.fromId("communication", TransactionType.EXPENSE)
                else -> Category.fromId("other", TransactionType.EXPENSE)
            }
        }
    }

    private fun String.containsAny(vararg keywords: String): Boolean =
        keywords.any { this.contains(it) }
}
