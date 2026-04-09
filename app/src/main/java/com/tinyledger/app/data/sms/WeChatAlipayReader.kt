package com.tinyledger.app.data.sms

import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony
import androidx.core.content.ContextCompat
import com.tinyledger.app.domain.model.TransactionType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 微信和支付宝交易记录读取器
 */
data class ExternalTransaction(
    val id: String,
    val platform: String,         // "wechat" 或 "alipay"
    val type: TransactionType,
    val amount: Double,
    val title: String,            // 交易描述
    val counterparty: String?,    // 交易对方
    val date: Long,
    val status: String,           // "success", "pending", "failed"
    val confidence: Float = 0f    // 识别置信度
)

@Singleton
class WeChatAlipayReader @Inject constructor(
    private val context: Context
) {

    // ========== 支付宝短信识别 ==========
    // 发件号码白名单（运营商短号，降低误识别）
    private val alipayAddressPatterns = listOf(
        Regex("^10690+"),   // 支付宝短号前缀
        Regex("alipay", RegexOption.IGNORE_CASE),
        Regex("zhifubao", RegexOption.IGNORE_CASE)
    )

    // 支付宝短信内容关键词（必须同时命中来源词）
    private val alipaySmsKeywords = listOf("支付宝", "Alipay", "蚂蚁金服")

    // ========== 微信支付短信识别 ==========
    private val wechatAddressPatterns = listOf(
        Regex("^10690+"),
        Regex("wechat", RegexOption.IGNORE_CASE),
        Regex("weixin", RegexOption.IGNORE_CASE)
    )

    private val wechatSmsKeywords = listOf("微信支付", "财付通", "微信收款", "微信转账")

    // ========== 精准金额正则（有货币符号锚定）==========
    private val amountPatternsHighConf = listOf(
        // ¥1234.56 / ￥999
        Regex("(?:¥|￥|\\$)\\s*(\\d{1,8}(?:\\.\\d{1,2})?)"),
        // 人民币/金额 + 数字 + 元
        Regex("(?:人民币|金额|收款|付款|消费|到账)[：:，,\\s]*(\\d{1,8}(?:\\.\\d{1,2})?)\\s*(?:元|块)?"),
        // 数字 + 元（要求有小数或大于100，避免匹配尾号）
        Regex("(\\d{1,8}\\.\\d{2})\\s*元"),
        Regex("(?<![\\d*])(\\d{3,8})\\s*元(?!起|以|左右)")
    )

    // ========== 支付宝专属解析 ==========
    /**
     * 支付宝短信格式举例：
     * 【支付宝】您已成功付款¥88.00，收款方为某商户，如有疑问致电95188。
     * 【支付宝】你的账户收到¥200.00，来自张三，请注意查收。
     */
    private fun parseAlipaySms(id: Long, body: String, date: Long): ExternalTransaction? {
        // 判断收支类型
        val incomeKeywords = listOf("收款成功", "收到", "到账", "退款", "收入", "转入", "入账")
        val expenseKeywords = listOf("付款成功", "支付成功", "扣款", "消费", "转出", "付款", "支付")

        val isIncome = incomeKeywords.any { body.contains(it) }
        val isExpense = expenseKeywords.any { body.contains(it) }

        val type = when {
            isIncome && !isExpense -> TransactionType.INCOME
            isExpense && !isIncome -> TransactionType.EXPENSE
            isIncome && isExpense -> {
                // 二义性处理：根据关键词位置优先
                val incomePos = incomeKeywords.minOfOrNull { kw ->
                    val idx = body.indexOf(kw); if (idx >= 0) idx else Int.MAX_VALUE
                } ?: Int.MAX_VALUE
                val expensePos = expenseKeywords.minOfOrNull { kw ->
                    val idx = body.indexOf(kw); if (idx >= 0) idx else Int.MAX_VALUE
                } ?: Int.MAX_VALUE
                if (incomePos <= expensePos) TransactionType.INCOME else TransactionType.EXPENSE
            }
            else -> return null
        }

        // 精准提取金额
        val amount = extractAmountFromBody(body) ?: return null

        // 提取商户/对方名称
        val counterparty = extractAlipayCounterparty(body)

        val title = when (type) {
            TransactionType.INCOME -> "支付宝收款"
            TransactionType.EXPENSE -> "支付宝支付"
        }

        return ExternalTransaction(
            id = "alipay_sms_$id",
            platform = "alipay",
            type = type,
            amount = amount,
            title = title,
            counterparty = counterparty,
            date = date,
            status = "success",
            confidence = 0.85f
        )
    }

    /**
     * 提取支付宝交易对方名称
     * 支持多种格式：
     *   收款方为XX  /  来自XX  /  向XX付款  /  商户：XX
     */
    private fun extractAlipayCounterparty(body: String): String? {
        val patterns = listOf(
            Regex("收款方(?:为|：|:)([^，,。.\\s]{1,20})"),
            Regex("来自([^，,。.\\s]{1,20})"),
            Regex("向([^，,。.\\s付款]{1,20})(?:付款|转账)"),
            Regex("商户[：:]([^，,。.\\s]{1,20})")
        )
        for (pattern in patterns) {
            val match = pattern.find(body)
            if (match != null) {
                return match.groupValues[1].trim()
            }
        }
        return null
    }

    // ========== 微信专属解析 ==========
    /**
     * 微信短信格式举例：
     * 【微信支付】微信支付收款¥50.00成功。
     * 【微信支付】您已成功向张三付款¥100.00，如有疑问请致电95017。
     */
    private fun parseWechatSms(id: Long, body: String, date: Long): ExternalTransaction? {
        val incomeKeywords = listOf("收款", "到账", "收到", "转入")
        val expenseKeywords = listOf("付款成功", "已成功付款", "扣款", "支付", "消费", "转出")

        val isIncome = incomeKeywords.any { body.contains(it) }
        val isExpense = expenseKeywords.any { body.contains(it) }

        val type = when {
            isIncome && !isExpense -> TransactionType.INCOME
            isExpense && !isIncome -> TransactionType.EXPENSE
            isIncome && isExpense -> {
                val incomePos = incomeKeywords.minOfOrNull { kw ->
                    val idx = body.indexOf(kw); if (idx >= 0) idx else Int.MAX_VALUE
                } ?: Int.MAX_VALUE
                val expensePos = expenseKeywords.minOfOrNull { kw ->
                    val idx = body.indexOf(kw); if (idx >= 0) idx else Int.MAX_VALUE
                } ?: Int.MAX_VALUE
                if (incomePos <= expensePos) TransactionType.INCOME else TransactionType.EXPENSE
            }
            else -> return null
        }

        val amount = extractAmountFromBody(body) ?: return null

        // 提取微信交易对方
        val counterparty = extractWechatCounterparty(body)

        val title = when (type) {
            TransactionType.INCOME -> "微信收款"
            TransactionType.EXPENSE -> "微信支付"
        }

        return ExternalTransaction(
            id = "wechat_sms_$id",
            platform = "wechat",
            type = type,
            amount = amount,
            title = title,
            counterparty = counterparty,
            date = date,
            status = "success",
            confidence = 0.85f
        )
    }

    /**
     * 提取微信交易对方
     * 支持格式：向XX付款  /  来自XX  /  XX转账给你
     */
    private fun extractWechatCounterparty(body: String): String? {
        val patterns = listOf(
            Regex("向([^，,。.\\s付款]{1,20})(?:付款|转账)"),
            Regex("来自([^，,。.\\s]{1,20})"),
            Regex("([^，,。.\\s]{1,10})转账给你")
        )
        for (pattern in patterns) {
            val match = pattern.find(body)
            if (match != null) {
                return match.groupValues[1].trim()
            }
        }
        return null
    }

    // ========== 通用金额提取 ==========
    private fun extractAmountFromBody(body: String): Double? {
        for (pattern in amountPatternsHighConf) {
            val matches = pattern.findAll(body)
                .mapNotNull { it.groupValues[1].toDoubleOrNull() }
                .filter { it > 0 && it < 10_000_000 }
                .toList()
            if (matches.isNotEmpty()) {
                return matches.maxOrNull()
            }
        }
        return null
    }

    /**
     * 通过短信读取微信/支付宝交易记录
     */
    fun readFromSms(
        contentResolver: ContentResolver,
        afterTime: Long = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000
    ): List<ExternalTransaction> {
        val transactions = mutableListOf<ExternalTransaction>()

        val uri: Uri = Telephony.Sms.CONTENT_URI
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE
        )

        val selection = "${Telephony.Sms.DATE} > ?"
        val selectionArgs = arrayOf(afterTime.toString())

        var cursor: Cursor? = null
        try {
            cursor = contentResolver.query(uri, projection, selection, selectionArgs, null)

            cursor?.let {
                val idIndex = it.getColumnIndex(Telephony.Sms._ID)
                val addressIndex = it.getColumnIndex(Telephony.Sms.ADDRESS)
                val bodyIndex = it.getColumnIndex(Telephony.Sms.BODY)
                val dateIndex = it.getColumnIndex(Telephony.Sms.DATE)
                val typeIndex = it.getColumnIndex(Telephony.Sms.TYPE)

                while (it.moveToNext()) {
                    val id = it.getLong(idIndex)
                    val address = it.getString(addressIndex) ?: ""
                    val body = it.getString(bodyIndex) ?: continue
                    val date = it.getLong(dateIndex)
                    val smsType = it.getInt(typeIndex)

                    // 只处理接收到的短信
                    if (smsType != 1) continue

                    // 排除验证码类短信
                    if (body.contains("验证码") || body.contains("校验码") || body.contains("动态密码")) continue

                    // 检测支付宝交易（内容关键词 + 可选号码特征）
                    val isAlipay = alipaySmsKeywords.any { body.contains(it) } ||
                        alipayAddressPatterns.any { it.containsMatchIn(address) }

                    if (isAlipay && alipaySmsKeywords.any { body.contains(it) }) {
                        parseAlipaySms(id, body, date)?.let { transaction ->
                            transactions.add(transaction)
                        }
                    }

                    // 检测微信交易
                    val isWechat = wechatSmsKeywords.any { body.contains(it) } ||
                        wechatAddressPatterns.any { it.containsMatchIn(address) }

                    if (isWechat && wechatSmsKeywords.any { body.contains(it) }) {
                        parseWechatSms(id, body, date)?.let { transaction ->
                            transactions.add(transaction)
                        }
                    }
                }
            }
        } catch (e: SecurityException) {
            // 没有短信权限
        } finally {
            cursor?.close()
        }

        return transactions.sortedByDescending { it.date }
    }

    /**
     * 检查是否有必要的权限
     */
    fun hasRequiredPermissions(): Map<String, Boolean> {
        val hasSmsPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED

        return mapOf("sms" to hasSmsPermission)
    }
}
