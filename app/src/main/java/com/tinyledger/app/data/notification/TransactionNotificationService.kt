package com.tinyledger.app.data.notification

import android.app.Notification
import android.content.Context
import android.content.Intent
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
import com.tinyledger.app.domain.repository.PendingTransactionRepository
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
    @Inject lateinit var pendingTransactionRepository: PendingTransactionRepository
    @Inject lateinit var notificationHelper: TransactionNotificationHelper

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
        "com.miui.sms",                    // 小米某些版本的短信应用
        "com.xiaomi.sms",                   // 小米可能的变体包名
        "com.miui.securitycenter",           // 小米安全中心，有时转发通知
        "com.huawei.message",
        "com.coloros.smsprovider",
        "com.iqoo.secure",
        "com.bbk.etchat",
        "com.android.messaging",
        "com.oneplus.mms",
        "com.oppo.mms",
        "com.vivo.mms",
        "com.hihonor.mms",
        "com.google.android.apps.messaging",
        "com.tencent.android.msgpush",  // 腾讯消息推送
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
        const val KEY_SEAMLESS_ENABLED = "seamless_auto_accounting_enabled"

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

        fun isSeamlessEnabled(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(KEY_SEAMLESS_ENABLED, false)
        }

        fun setSeamlessEnabled(context: Context, enabled: Boolean) {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_SEAMLESS_ENABLED, enabled).apply()
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
        val summaryText = extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)?.toString() ?: ""

        // 用最长的内容来解析（包含 summaryText）
        val fullContent = listOf(bigText, text, summaryText).maxByOrNull { it.length } ?: ""

        Log.d(TAG, "[通知捕获] pkg=$packageName title='$title' text='${text.take(80)}' bigText='${bigText.take(80)}' summaryText='${summaryText.take(80)}'")

        // ── 路径1：支付类应用 → 自动记账 ──────────────────────────────
        val appLabel = paymentPackages[packageName]
        if (appLabel != null) {
            if (!isEnabled(applicationContext)) {
                Log.d(TAG, "[通知捕获] 自动记账未开启，跳过 $packageName")
                return
            }
            
            // 过滤淘宝、天猫的非交易类通知
            if (packageName == "com.taobao.taobao" || packageName == "com.tmall.wireless") {
                val contentToCheck = "$title $fullContent $subText".lowercase()
                // 只处理包含交易关键词的通知
                val hasTransactionKeywords = listOf(
                    "支付", "付款", "扣款", "消费", "订单", "交易", 
                    "支出", "到账", "收款", "退款", "金额", "元",
                    "成功", "完成", "确认"
                ).any { keyword -> contentToCheck.contains(keyword) }
                
                if (!hasTransactionKeywords) {
                    Log.d(TAG, "[通知捕获] 跳过淘宝/天猫非交易类通知: $appLabel - '${title.take(30)}'")
                    return
                }
                Log.d(TAG, "[通知捕获] 检测到淘宝/天猫交易通知: $appLabel")
            }
            
            Log.d(TAG, "[通知捕获] 检测到支付应用: $appLabel ($packageName)")
            handlePaymentNotification(packageName, appLabel, title, text, fullContent, subText)
            return
        }

        // ── 路径2：银行短信通知捕获 ──────────────────────────────────
        if (packageName in smsPackages && isBankSmsCaptureEnabled(applicationContext)) {
            Log.d(TAG, "[通知捕获] 检测到短信应用: $packageName")
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
        if (combined.isBlank()) {
            Log.d(TAG, "[支付通知] 内容为空，跳过")
            return
        }

        // 去重：同一内容5分钟内不重复记账
        val dedupKey = md5("$packageName|${combined.take(100)}")
        val now = System.currentTimeMillis()
        synchronized(recentHashes) {
            val lastTime = recentHashes[dedupKey]
            if (lastTime != null && now - lastTime < 5 * 60 * 1000L) {
                Log.d(TAG, "[支付通知] 跳过重复通知: $dedupKey")
                return
            }
            recentHashes[dedupKey] = now
            // 清理超过10分钟的旧记录
            recentHashes.entries.removeAll { now - it.value > 10 * 60 * 1000L }
        }

        val parsed = parsePaymentNotification(packageName, appLabel, title, text, fullContent)
        if (parsed == null) {
            Log.d(TAG, "[支付通知] 无法解析为支付通知: $combined")
            return
        }

        Log.d(TAG, "[支付通知] 解析成功: ${parsed.type} ¥${parsed.amount} [${parsed.note}]")

        serviceScope.launch {
            try {
                val category = inferCategory(parsed)

                val transaction = Transaction(
                    type = parsed.type,
                    category = category,
                    amount = if (parsed.type == TransactionType.EXPENSE) -parsed.amount else parsed.amount, // 支出存负数，收入存正数
                    note = parsed.note,
                    date = System.currentTimeMillis(),
                    accountId = null  // 支付通知无法自动匹配账户
                )

                val seamlessEnabled = isSeamlessEnabled(applicationContext)
                Log.d(TAG, "[支付通知] 无感模式: $seamlessEnabled")

                if (seamlessEnabled) {
                    // 无感模式：直接自动记账
                    transactionRepository.insertTransaction(transaction)
                    Log.d(TAG, "[无感记账] 成功: ${parsed.type} ¥${parsed.amount} [${parsed.note}]")
                    notificationHelper.playFeedback()
                } else {
                    // 非无感模式：存入待确认表 + 弹出通知
                    val pendingId = pendingTransactionRepository.insertPendingTransaction(transaction)
                    Log.d(TAG, "[待确认] 已保存: pendingId=$pendingId ${parsed.type} ¥${parsed.amount}")
                    Log.d(TAG, "[待确认] 准备弹出确认通知...")
                    notificationHelper.showTransactionConfirmationNotification(pendingId = pendingId, transaction = transaction, sourceLabel = appLabel)
                }
            } catch (e: Exception) {
                Log.e(TAG, "[支付通知] 自动记账失败", e)
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

        // 微信支付通知：title 可能是 "微信支付"、"微信红包"、"微信收款助手"、"转账" 等
        // 某些手机上标题可能只是 "微信" 或包含 "付款" 等关键词
        // 因此放宽匹配：标题包含支付关键词 OR 内容中包含明确的微信支付特征
        val isPaymentTitle = title.contains("微信支付") ||
                title.contains("微信红包") ||
                title.contains("微信收款") ||
                title.contains("微信收款助手") ||
                title.contains("零钱") ||
                title.contains("转账") ||
                title.contains("WeChat Pay") ||
                title.contains("微信转账") ||
                title.contains("付款到账")

        // 内容中也检查是否包含微信支付特征（用于标题不匹配但内容明确是微信支付的情况）
        val isPaymentContent = combined.contains("微信支付凭证") ||
                combined.contains("微信转账凭证") ||
                combined.contains("你已成功付款") ||
                combined.contains("收款到账") ||
                (combined.contains("元") && (combined.contains("收款方") || combined.contains("付款方")))

        if (!isPaymentTitle && !isPaymentContent) {
            Log.d(TAG, "微信非支付通知，跳过: title=$title")
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

        // 支付宝通知的 title 通常包含 "支付宝"、"花呗"、"余额宝" 等
        // 某些手机上可能简化为 "支付成功" 等
        val isPaymentTitle = title.contains("支付宝") ||
                title.contains("花呗") ||
                title.contains("余额宝") ||
                title.contains("借呗") ||
                title.contains("网商银行") ||
                title.contains("Alipay") ||
                title.contains("支付成功") ||
                title.contains("付款成功") ||
                title.contains("收款到账")

        // 如果 title 不含支付关键词，则检查内容是否包含明确的支付宝交易信息
        if (!isPaymentTitle) {
            val hasAlipayContent = combined.contains("支付宝") && hasPaymentKeywords(combined)
            // 也检查是否包含支付宝特有的内容特征
            val hasAlipayPattern = combined.contains("付款方式") ||
                    combined.contains("交易订单") ||
                    combined.contains("商家订单")
            if (!hasAlipayContent && !hasAlipayPattern) {
                Log.d(TAG, "支付宝非支付通知，跳过: title=$title")
                return null
            }
        }

        val amount = extractAmount(combined) ?: return null

        return when {
            combined.containsAny("收款", "到账", "已收款", "转账收到", "退款",
                "红包", "余额宝收益", "收钱") -> {
                ParsedNotification(
                    type = TransactionType.INCOME,
                    amount = amount,
                    note = "支付宝收款: ${text.take(40)}"
                )
            }
            combined.containsAny("付款", "消费", "已扣款", "支付成功", "已付款",
                "花呗", "扣费", "充值", "缴费", "扫码付款", "付款成功") -> {
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
                "已退款", "退款成功", "已收到") -> {
                ParsedNotification(
                    type = TransactionType.INCOME,
                    amount = amount,
                    note = "${appLabel}收入: ${text.take(40)}"
                )
            }
            combined.containsAny("付款", "支付", "消费", "已扣款", "扣费",
                "已付款", "下单", "购买", "订单", "支付成功",
                "付款成功", "充值", "缴费", "已支付", "已消费") -> {
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
                // 存储短信记录
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

                // 尝试从银行短信中解析交易并自动记账
                if (isEnabled(applicationContext)) {
                    val parsed = parseBankSmsTransaction(body)
                    if (parsed != null) {
                        val category = inferCategory(parsed)

                        val transaction = Transaction(
                            type = parsed.type,
                            category = category,
                            amount = if (parsed.type == TransactionType.EXPENSE) -parsed.amount else parsed.amount, // 支出存负数，收入存正数
                            note = parsed.note,
                            date = System.currentTimeMillis(),
                            accountId = null  // 银行短信无法自动匹配账户
                        )

                        if (isSeamlessEnabled(applicationContext)) {
                            transactionRepository.insertTransaction(transaction)
                            Log.d(TAG, "银行短信无感自动记账成功: ${parsed.type} ¥${parsed.amount}")
                            notificationHelper.playFeedback()
                        } else {
                            val pendingId = pendingTransactionRepository.insertPendingTransaction(transaction)
                            Log.d(TAG, "银行短信待确认记账已保存: pendingId=$pendingId")
                            notificationHelper.showTransactionConfirmationNotification(pendingId = pendingId, transaction = transaction, sourceLabel = address)
                        }
                    } else {
                        // 即使无法解析金额，如果内容包含明确银行特征，也存入待确认供用户手动处理
                        val strongBankIndicators = listOf("支出", "消费", "扣款", "收入", "到账", "入账", "收款")
                        if (strongBankIndicators.any { body.contains(it) }) {
                            Log.d(TAG, "银行短信解析金额失败，但包含交易关键词，存入待确认: ${body.take(40)}")
                            val guessedType = if (listOf("收入", "到账", "入账", "收款").any { body.contains(it) }) {
                                TransactionType.INCOME
                            } else {
                                TransactionType.EXPENSE
                            }
                            val transaction = Transaction(
                                type = guessedType,
                                category = Category.fromId("other", guessedType),
                                amount = 0.0,
                                note = "银行短信(需确认): ${body.take(60)}",
                                date = System.currentTimeMillis(),
                                accountId = null
                            )
                            if (!isSeamlessEnabled(applicationContext)) {
                                val pendingId = pendingTransactionRepository.insertPendingTransaction(transaction)
                                notificationHelper.showTransactionConfirmationNotification(pendingId = pendingId, transaction = transaction, sourceLabel = address)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "处理银行短信失败", e)
            }
        }
    }

    /**
     * 从银行短信内容解析交易信息
     * 支持常见银行短信格式，如：
     * - "您尾号1234的储蓄卡于12月01日支出100.00元，可用余额5000.00元"
     * - "您账户1234于12月01日收入5000.00元"
     */
    private fun parseBankSmsTransaction(body: String): ParsedNotification? {
        val amount = extractAmount(body) ?: return null

        return when {
            body.containsAny("收入", "到账", "入账", "收款", "转入", "存入", "汇入") -> {
                ParsedNotification(
                    type = TransactionType.INCOME,
                    amount = amount,
                    note = "银行收款: ${body.take(40)}"
                )
            }
            body.containsAny("支出", "消费", "扣款", "转出", "付款", "已扣款") -> {
                ParsedNotification(
                    type = TransactionType.EXPENSE,
                    amount = amount,
                    note = "银行支出: ${body.take(40)}"
                )
            }
            else -> null
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
     * 支持多种格式：
     * 1. "数字+元" 模式（银行短信常见）：如 "123.45元"
     * 2. "¥/￥+数字" 模式（微信/支付宝常见）：如 "¥123.45" "￥123.45"
     * 3. 交易关键词后跟数字（兜底）：如 "支付100" "消费50.00"
     */
    private fun extractAmount(text: String): Double? {
        // 优先级1：¥/￥ 符号锚定（微信支付、支付宝最常用的金额格式）
        val symbolRegex = Regex("[¥￥]\\s*(\\d+(?:\\.\\d{1,2})?)")
        val symbolMatch = symbolRegex.find(text)
        if (symbolMatch != null) {
            val amount = symbolMatch.groupValues[1].toDoubleOrNull()
            if (amount != null && amount > 0 && amount < 10_000_000) {
                Log.d(TAG, "匹配到'¥/￥+数字'模式: $amount")
                return amount
            }
        }

        // 优先级2："数字+元" 模式（银行短信常见）
        val yuanRegex = Regex("(\\d+(?:\\.\\d{1,2})?)\\s*元")
        val yuanMatch = yuanRegex.find(text)
        if (yuanMatch != null) {
            val amount = yuanMatch.groupValues[1].toDoubleOrNull()
            if (amount != null && amount > 0 && amount < 10_000_000) {
                Log.d(TAG, "匹配到'数字+元'模式: $amount")
                return amount
            }
        }

        // 优先级2.5：RMB/CNY 格式（部分银行短信使用）
        val rmbRegex = Regex("[RC]MB\\s*(\\d+(?:\\.\\d{1,2})?)", RegexOption.IGNORE_CASE)
        val rmbMatch = rmbRegex.find(text)
        if (rmbMatch != null) {
            val amount = rmbMatch.groupValues[1].toDoubleOrNull()
            if (amount != null && amount > 0 && amount < 10_000_000) {
                Log.d(TAG, "匹配到'RMB/CNY+数字'模式: $amount")
                return amount
            }
        }

        // 优先级3：交易关键词后跟数字（兜底模式，需更严格匹配）
        // 如 "支付123.45" "消费100" "付款50.00" "到账200"
        val anchoredRegex = Regex(
            "(?:支付|付款|消费|支出|扣款|扣费|收款|到账|入账|转账|退款|充值|缴费)" +
            "[^\\d\\n]{0,10}?" +  // 关键词后最多10个非数字字符
            "(\\d+(?:\\.\\d{1,2})?)"
        )
        val anchoredMatch = anchoredRegex.find(text)
        if (anchoredMatch != null) {
            val amount = anchoredMatch.groupValues[1].toDoubleOrNull()
            if (amount != null && amount > 0 && amount < 10_000_000) {
                Log.d(TAG, "匹配到'关键词+数字'模式: $amount")
                return amount
            }
        }

        // 优先级4：尝试匹配金额文本前后有"金额"关键词的模式
        val amountKeywordRegex = Regex("金额[：:]?\\s*(\\d+(?:\\.\\d{1,2})?)")
        val amountKwMatch = amountKeywordRegex.find(text)
        if (amountKwMatch != null) {
            val amount = amountKwMatch.groupValues[1].toDoubleOrNull()
            if (amount != null && amount > 0 && amount < 10_000_000) {
                Log.d(TAG, "匹配到'金额+数字'模式: $amount")
                return amount
            }
        }

        Log.d(TAG, "未匹配到有效金额模式: ${text.take(80)}")
        return null
    }

    private fun md5(input: String): String {
        val bytes = MessageDigest.getInstance("MD5").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    // playFeedback() and showTransactionConfirmationNotification() are now in TransactionNotificationHelper

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
            "com.eg.android.AlipayGphone" -> AccountType.CONSUMPTION_PLATFORM
            else -> return null
        }
        return accounts.find { it.type == targetType }?.id
    }

    private fun inferCategory(parsed: ParsedNotification): Category {
        val note = parsed.note
        return if (parsed.type == TransactionType.INCOME) {
            when {
                note.containsAny("工资", "薪资", "薪酬", "代发", "发薪") -> Category.fromId("salary", TransactionType.INCOME)
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
