package com.tinyledger.app.data.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.tinyledger.app.data.local.dao.NotificationSmsDao
import com.tinyledger.app.data.local.entity.NotificationSmsEntity
import com.tinyledger.app.data.notification.TransactionNotificationHelper
import com.tinyledger.app.data.notification.TransactionNotificationService
import com.tinyledger.app.domain.model.Category
import com.tinyledger.app.domain.model.Transaction
import com.tinyledger.app.domain.model.TransactionType
import com.tinyledger.app.domain.repository.PendingTransactionRepository
import com.tinyledger.app.domain.repository.TransactionRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.security.MessageDigest
import javax.inject.Inject

/**
 * 短信接收器 - 用于实时监听新收到的短信
 * 当收到金融类短信时，自动识别并记账或弹出确认通知
 */
@AndroidEntryPoint
class SmsReceiver : BroadcastReceiver() {

    @Inject lateinit var notificationSmsDao: NotificationSmsDao
    @Inject lateinit var transactionRepository: TransactionRepository
    @Inject lateinit var pendingTransactionRepository: PendingTransactionRepository
    @Inject lateinit var parser: SmsTransactionParser
    @Inject lateinit var notificationHelper: TransactionNotificationHelper

    companion object {
        private const val TAG = "SmsReceiver"

        // 金融类短信关键词（与 TransactionNotificationService 保持一致）
        private val bankKeywords = listOf(
            "银行", "建行", "工行", "农行", "中行", "招行", "交行", "邮储",
            "支出", "消费", "扣款", "转出",
            "收入", "到账", "入账", "收款", "转入", "存入",
            "可用余额", "账户余额", "当前余额",
            "尾号", "账户", "储蓄卡", "信用卡",
            "CCB", "ICBC", "ABC", "BOC", "CMB", "CITIC"
        )
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent?.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            return
        }

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        val pendingResult = goAsync()
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        scope.launch {
            try {
                for (message in messages) {
                    val address = message.originatingAddress ?: continue
                    val body = message.messageBody ?: continue

                    // 检查是否是金融类短信
                    val isFinancial = bankKeywords.any { body.contains(it, ignoreCase = true) }
                    if (!isFinancial) {
                        continue
                    }

                    Log.d(TAG, "收到金融短信: $address - ${body.take(50)}")

                    // 存储到 Room 数据库
                    try {
                        val minuteKey = System.currentTimeMillis() / 60000
                        val hashInput = "$address|${body.take(100)}|$minuteKey"
                        val hash = md5(hashInput)

                        notificationSmsDao.insert(
                            NotificationSmsEntity(
                                address = address,
                                body = body,
                                date = System.currentTimeMillis(),
                                packageName = "sms_receiver",
                                uniqueHash = hash
                            )
                        )
                        Log.d(TAG, "银行短信已存储: $address - ${body.take(40)}")
                    } catch (e: Exception) {
                        Log.e(TAG, "存储银行短信失败", e)
                    }

                    // 尝试解析并自动记账
                    if (!TransactionNotificationService.isEnabled(context)) {
                        Log.d(TAG, "自动记账未开启，跳过解析")
                        continue
                    }

                    // Skip auto-accounting if notification listener is active and will handle it
                    val notificationListenerActive = TransactionNotificationService.hasPermission(context)
                        && TransactionNotificationService.isBankSmsCaptureEnabled(context)

                    if (notificationListenerActive) {
                        Log.d(TAG, "Notification listener active for bank SMS, skipping SmsReceiver auto-accounting to avoid duplicates")
                        // Still stored in Room above, just skip auto-accounting
                        continue
                    }

                    try {
                        val normalizedAddress = address
                            .removePrefix("+86")
                            .removePrefix("86")
                            .trim()

                        val parseResult = parser.parseSmsContent(body, normalizedAddress)
                        val bankSource = parser.detectBankSource(body, normalizedAddress)

                        if (parseResult.type == null || parseResult.amount == null || parseResult.amount <= 0) {
                            Log.d(TAG, "短信解析失败或无有效交易: $address")
                            continue
                        }

                        val category = inferCategory(parseResult.type, body)

                        val transaction = Transaction(
                            type = parseResult.type,
                            category = category,
                            amount = parseResult.amount,
                            note = "银行短信: ${body.take(40)}",
                            date = System.currentTimeMillis(),
                            accountId = null
                        )

                        val sourceLabel = if (bankSource != "未知来源") bankSource else address

                        if (TransactionNotificationService.isSeamlessEnabled(context)) {
                            // 无感模式：直接自动记账
                            transactionRepository.insertTransaction(transaction)
                            Log.d(TAG, "无感自动记账成功: ${parseResult.type} ¥${parseResult.amount}")
                            notificationHelper.playFeedback(context)
                        } else {
                            // 非无感模式：存入待确认表 + 弹出确认通知
                            val pendingId = pendingTransactionRepository.insertPendingTransaction(transaction)
                            Log.d(TAG, "待确认记账已保存: pendingId=$pendingId")
                            notificationHelper.showTransactionConfirmationNotification(
                                context = context,
                                pendingId = pendingId,
                                transaction = transaction,
                                sourceLabel = sourceLabel
                            )
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "自动记账处理失败", e)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "处理短信失败", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    /**
     * 根据交易类型和短信内容推断分类
     */
    private fun inferCategory(type: TransactionType, body: String): Category {
        return if (type == TransactionType.INCOME) {
            when {
                body.containsAny("工资", "薪资", "薪酬", "代发", "发薪") ->
                    Category.fromId("salary", TransactionType.INCOME)
                body.containsAny("退款", "退还") ->
                    Category.fromId("redpacket", TransactionType.INCOME)
                body.containsAny("红包", "转账") ->
                    Category.fromId("redpacket", TransactionType.INCOME)
                else -> Category.fromId("redpacket", TransactionType.INCOME)
            }
        } else {
            when {
                body.containsAny("餐", "饭", "外卖", "美团", "饿了么") ->
                    Category.fromId("food", TransactionType.EXPENSE)
                body.containsAny("打车", "滴滴", "地铁", "公交", "出行") ->
                    Category.fromId("transport", TransactionType.EXPENSE)
                body.containsAny("购物", "京东", "淘宝", "天猫", "拼多多") ->
                    Category.fromId("shopping", TransactionType.EXPENSE)
                body.containsAny("娱乐", "电影", "游戏", "抖音") ->
                    Category.fromId("entertainment", TransactionType.EXPENSE)
                body.containsAny("医院", "药", "医疗") ->
                    Category.fromId("medical", TransactionType.EXPENSE)
                body.containsAny("话费", "流量", "通讯") ->
                    Category.fromId("communication", TransactionType.EXPENSE)
                else -> Category.fromId("other", TransactionType.EXPENSE)
            }
        }
    }

    private fun md5(input: String): String {
        val bytes = MessageDigest.getInstance("MD5").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun String.containsAny(vararg keywords: String): Boolean =
        keywords.any { this.contains(it) }
}
