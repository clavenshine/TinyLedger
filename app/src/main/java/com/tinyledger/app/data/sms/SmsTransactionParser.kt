package com.tinyledger.app.data.sms

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony
import com.tinyledger.app.data.local.dao.NotificationSmsDao
import com.tinyledger.app.domain.model.TransactionType
import javax.inject.Inject
import javax.inject.Singleton

data class SmsTransaction(
    val id: Long,
    val address: String,        // 发送方号码
    val body: String,           // 短信内容
    val date: Long,             // 时间戳
    val type: TransactionType?, // 识别出的交易类型
    val amount: Double?,        // 识别出的金额
    val source: String,         // 来源(银行名称)
    val isTransfer: Boolean = false,  // 是否是转账
    val cardLastFour: String? = null, // 关联银行卡尾号
    val confidence: Float = 0f        // 识别置信度 0~1
)

/**
 * 智能短信解析引擎
 *
 * 核心策略：
 * 1. 精准金额锚定：要求金额前后有货币符号/单位/专属金融词汇紧贴，避免误匹配卡号/订单号
 * 2. 卡号屏蔽：过滤掉已知格式的卡号（尾号4位、16位等）周围的数字
 * 3. 置信度评分：多维度打分，低于阈值的不展示
 * 4. 银行来源校验：必须能识别出金融机构，否则丢弃
 */
@Singleton
class SmsTransactionParser @Inject constructor() {

    // ========== 银行/机构识别（内容关键词）==========
    private val bankPatterns = mapOf(
        "工商银行" to listOf("工商银行", "工行", "ICBC"),
        "建设银行" to listOf("建设银行", "建行", "CCB", "建设银"),
        "农业银行" to listOf("农业银行", "农行", "ABC"),
        "中国银行" to listOf("中国银行", "中行", "BOC"),
        "招商银行" to listOf("招商银行", "招行", "CMB"),
        "交通银行" to listOf("交通银行", "交行", "BOCOM"),
        "浦发银行" to listOf("浦发银行", "浦发", "SPDB"),
        "民生银行" to listOf("民生银行", "民生", "CMBC"),
        "中信银行" to listOf("中信银行", "中信", "CITIC"),
        "兴业银行" to listOf("兴业银行", "兴业", "CIB"),
        "平安银行" to listOf("平安银行", "平安", "PAB"),
        "华夏银行" to listOf("华夏银行", "华夏", "HXB"),
        "光大银行" to listOf("光大银行", "光大", "CEB"),
        "广发银行" to listOf("广发银行", "广发", "GDB"),
        "邮政储蓄" to listOf("邮政储蓄", "邮储", "邮政银行", "PSBC"),
        "北京银行" to listOf("北京银行", "北京行", "BOB"),
        "上海银行" to listOf("上海银行", "上海行"),
        "江苏银行" to listOf("江苏银行"),
        "宁波银行" to listOf("宁波银行"),
        "南京银行" to listOf("南京银行"),
        "成都银行" to listOf("成都银行"),
        "杭州银行" to listOf("杭州银行"),
        "中原银行" to listOf("中原银行"),
        "渤海银行" to listOf("渤海银行"),
        "恒丰银行" to listOf("恒丰银行"),
        "徽商银行" to listOf("徽商银行"),
        "汉口银行" to listOf("汉口银行"),
        "天津银行" to listOf("天津银行"),
        "东莞银行" to listOf("东莞银行"),
        "农村商业银行" to listOf("农村商业银行", "农商银行", "农商行"),
        "农村信用社" to listOf("农村信用社", "农信社"),
        "支付宝" to listOf("支付宝", "Alipay", "蚂蚁金服"),
        "微信支付" to listOf("微信支付", "财付通", "微信收款", "微信转账")
    )

    // ========== 银行服务号码识别（从发件人辅助判断）==========
    // 当短信内容不含银行名称时，通过发件号码辅助识别
    private val bankPhonePatterns = mapOf(
        "工商银行" to listOf(Regex("^95588"), Regex("^1069095588"), Regex("^10690.*95588")),
        "建设银行" to listOf(Regex("^95533"), Regex("^1069095533"), Regex("^10690.*95533"),
                             Regex("^106980095533"), Regex("^1069[0-9]*95533"),
                             Regex("^10698\\d*95533"),   // 10698 开头、95533 结尾的所有变体
                             Regex("^106980+9553")),     // 宽松匹配：10698009553x
        "农业银行" to listOf(Regex("^95599"), Regex("^1069095599"), Regex("^10690.*95599")),
        "中国银行" to listOf(Regex("^95566"), Regex("^1069095566"), Regex("^10690.*95566")),
        "招商银行" to listOf(Regex("^95555"), Regex("^1069095555"), Regex("^10690.*95555")),
        "交通银行" to listOf(Regex("^95559"), Regex("^1069095559"), Regex("^10690.*95559")),
        "浦发银行" to listOf(Regex("^95528"), Regex("^1069095528"), Regex("^10690.*95528")),
        "民生银行" to listOf(Regex("^95568"), Regex("^1069095568"), Regex("^10690.*95568")),
        "中信银行" to listOf(Regex("^95558"), Regex("^9558\\d+"), Regex("^1069095558"), Regex("^10690.*95558")),
        "兴业银行" to listOf(Regex("^95561"), Regex("^1069095561"), Regex("^10690.*95561")),
        "平安银行" to listOf(Regex("^95511"), Regex("^1069095511"), Regex("^10690.*95511")),
        "华夏银行" to listOf(Regex("^95577"), Regex("^1069095577"), Regex("^10690.*95577")),
        "光大银行" to listOf(Regex("^95595"), Regex("^1069095595"), Regex("^10690.*95595")),
        "广发银行" to listOf(Regex("^95508"), Regex("^1069095508"), Regex("^10690.*95508")),
        "邮政储蓄" to listOf(Regex("^95580"), Regex("^1069095580"), Regex("^10690.*95580"))
    )

    // ========== 收入关键词（带权重）==========
    // 权重越高，置信度越高
    // 注意：复合收入词（如「机构提现」「消费退货」）作为整体加入，确保被识别
    private val incomeKeywordsWeighted = mapOf(
        "工资" to 0.95f,
        "薪资" to 0.95f,
        "机构提现存入" to 0.95f, // 「机构提现存入」= 提现收入（整体匹配，优先于单独的「存入」）
        "消费退货存入" to 0.95f,  // 「消费退货存入」= 退货收入（整体匹配）
        "到账" to 0.9f,
        "收款成功" to 0.9f,
        "收到" to 0.85f,
        "收入" to 0.85f,
        "转入" to 0.8f,
        "存入" to 0.8f,
        "汇入" to 0.8f,
        "入账" to 0.8f,
        "退款" to 0.75f,
        "退还" to 0.75f,
        "奖励" to 0.7f,
        "红包" to 0.7f,
        "返现" to 0.7f,
        "分红" to 0.7f,
        "获得" to 0.65f
    )

    // ========== 支出关键词（带权重）==========
    private val expenseKeywordsWeighted = mapOf(
        "消费" to 0.95f,
        "扣款" to 0.9f,
        "支出" to 0.9f,
        "付款成功" to 0.9f,
        "支付成功" to 0.9f,
        "付款" to 0.85f,
        "扣除" to 0.85f,
        "转出" to 0.8f,
        "购买" to 0.8f,
        "刷卡" to 0.8f,
        "扫码支付" to 0.8f,
        "代扣" to 0.8f,
        "还款" to 0.75f,
        "缴费" to 0.75f,
        "支付" to 0.7f
    )

    // ========== 排除词：包含这些词的短信不处理（验证码/营销/通知等）==========
    // 注意：大幅精简排除词列表，避免误杀交易短信
    // 原来的 "活动"、"积分"、"兑换"、"领取"、"登录"、"开通"、"注册"、"注销"、"激活"、"流量"
    // 会导致商户名或交易描述中包含这些字的交易短信被误排除（尤其是建设银行）
    private val excludeKeywords = listOf(
        "验证码", "校验码", "动态密码", "短信验证", "OTP",
        "优惠券",
        "系统通知", "服务提醒",
        "尾号\\d{4}(?:绑定|解绑|注册|认证)"  // 卡号绑定类短信
    )

    // 交易关键词白名单：如果短信包含这些词，即使命中了排除词也不应被排除
    // （交易短信的商户名/附加文案可能碰巧包含排除词）
    private val transactionIndicators = listOf(
        "支出", "消费", "扣款", "付款", "转出", "刷卡", "购买", "代扣", "还款", "缴费",
        "收入", "到账", "入账", "收款", "收到", "转入", "存入", "汇入", "退款", "退还",
        "可用余额", "账户余额"
    )

    /**
     * 交易关键词直接锚定金额的正则（第一优先级）
     *
     * 核心设计思路：
     *   银行账户变动通知短信结构固定：「...向XXX支出20.28元，可用余额87365.55元」
     *   余额类词（可用余额/账户余额/余额）已在 maskCardNumbers 中被屏蔽为 BBBB，
     *   因此只需在「支付/支出/消费/到账/收到/转账/扣款/付款...」等交易关键词
     *   后面直接提取紧跟的金额即可。金额大小不作为判断依据，0.01元到1000000元均合法。
     *
     * 格式覆盖：
     *   「支出20.28元」「消费 ¥100」「到账：1000元」「收到20元」「扣款0.01元」
     *   「付款100元整」「转出100.00元」「支付成功100元」「向XXX支出100元」
     */
    // 交易关键词后紧跟金额（最高置信：精准上下文锚定）
    private val anchoredAmountPattern = Regex(
        "(?:支出|支付|消费|扣款|扣除|付款|转出|转账|汇款|代扣|还款|缴费|刷卡|购买|" +
        "收入|到账|入账|收款|收到|汇入|转入|存入|退款|退还|获得|奖励|返现)" +
        "[^，,。；;\\n元¥￥]{0,40}?" + // 允许中间有商户名等修饰词，最多40字符（建设银行商户名较长）
        "(?:¥|￥)?\\s*(\\d{1,10}(?:\\.\\d{1,2})?)\\s*元"
    )

    /**
     * 货币符号直接锚定（第二优先级）
     * 覆盖：「¥20.28」「￥100」「RMB 500」
     */
    private val currencySymbolPattern = Regex(
        "(?:¥|￥|RMB|CNY)\\s*(\\d{1,10}(?:\\.\\d{1,2})?)"
    )

    /**
     * 全文扫描兜底（第三优先级，仅在前两者都无结果时使用）
     * 此时余额已被屏蔽为 BBBB，账户尾号已被屏蔽为 XXXX，
     * 剩余的「数字+元」即为交易金额
     */
    private val fallbackAmountPattern = Regex(
        "(?<![\\d*BXVUO])(\\d{1,10}(?:\\.\\d{1,2})?)\\s*元(?!起|以|左右|内|外|多|余|上|下)"
    )

    // 卡号尾号正则（用于排除干扰）—— 支持多种银行格式：
    //   "尾号1234"、"末四位1234"、"**** 1234"、"账户1234"（建设银行）、"储蓄卡1234"、"信用卡1234"
    private val cardNumberRegex = Regex("(?:尾号|末四位|后四位|卡号末尾|\\*{2,}|账户|储蓄卡|信用卡|银行卡)[\\s*]*(\\d{4})")
    // 16位纯数字（信用卡/借记卡完整卡号）
    private val fullCardRegex = Regex("\\b\\d{16}\\b")
    // 验证码类数字（4~8位，且短信中有"验证码"字样时已被excludeKeywords过滤）
    private val verificationCodeRegex = Regex("(?:验证码|校验码)[：:\\s]*(\\d{4,8})")
    // 余额类正则（用于屏蔽余额数字，防止被误识别为交易金额）
    private val balanceRegex = Regex("(?:可用余额|账户余额|当前余额|余额|剩余额度|可用额度|余额为)[：:为\\s]*(\\d{1,10}(?:\\.\\d{1,2})?)")

    // ========== 置信度阈值 ==========
    private val CONFIDENCE_THRESHOLD = 0.5f

    /**
     * 主解析入口
     * @param body 短信内容
     * @param address 发件人号码（可选，用于辅助识别银行来源）
     * 返回：(交易类型, 金额, 置信度, 卡尾号)
     */
    fun parseSmsContent(body: String, address: String = ""): ParseResult {
        // 1. 排除明显非金融短信
        if (shouldExclude(body)) {
            return ParseResult(null, null, 0f, null)
        }

        // 2. 必须能识别银行/机构（内容关键词 OR 发件号码）
        val source = detectBankSource(body, address)
        if (source == "未知来源") {
            return ParseResult(null, null, 0f, null)
        }

        // 3. 提取卡尾号（用于后续匹配账户）
        val cardLastFour = extractCardLastFour(body)

        // 4. 屏蔽卡号区域，防止金额误识别
        val cleanedBody = maskCardNumbers(body)

        // 5. 精准提取金额
        val (amount, amountConfidence) = extractAmount(cleanedBody)
            ?: return ParseResult(null, null, 0f, cardLastFour)

        // 6. 判断收支类型
        val (type, typeConfidence) = detectTransactionType(body)
            ?: return ParseResult(null, null, 0f, cardLastFour)

        // 7. 综合置信度
        val totalConfidence = (amountConfidence + typeConfidence) / 2f

        if (totalConfidence < CONFIDENCE_THRESHOLD) {
            return ParseResult(null, null, totalConfidence, cardLastFour)
        }

        return ParseResult(type, amount, totalConfidence, cardLastFour)
    }

    /**
     * 是否应该排除（验证码/营销/绑卡等非交易短信）
     * 优化：如果短信包含交易指示词（支出/消费/到账等），则不排除
     * 这防止了交易短信因商户名/附加文案中碰巧包含排除词而被误杀
     */
    private fun shouldExclude(body: String): Boolean {
        val hitExclude = excludeKeywords.any { pattern ->
            if (pattern.contains("\\")) {
                Regex(pattern).containsMatchIn(body)
            } else {
                body.contains(pattern)
            }
        }
        if (!hitExclude) return false

        // 命中排除词，但如果同时包含交易指示词，说明是交易短信（不排除）
        val hasTransaction = transactionIndicators.any { body.contains(it) }
        return !hasTransaction
    }

    /**
     * 识别金融机构来源
     * 先通过内容关键词匹配，不行再通过发件号码匹配
     */
    fun detectBankSource(body: String, address: String = ""): String {
        // 标准化发件人号码（去除国家码前缀）
        val normalizedAddress = address
            .removePrefix("+86")
            .removePrefix("86")
            .trim()

        // 优先通过内容关键词识别
        for ((bankName, keywords) in bankPatterns) {
            if (keywords.any { body.contains(it, ignoreCase = true) }) {
                return bankName
            }
        }
        // 通过发件号码辅助识别
        if (normalizedAddress.isNotBlank()) {
            for ((bankName, patterns) in bankPhonePatterns) {
                if (patterns.any { it.containsMatchIn(normalizedAddress) }) {
                    return bankName
                }
            }
            // 通用银行号码特征（10658xx、10690xx、10698xx 类服务号，以及 955xx 类客服号）
            if (normalizedAddress.matches(Regex("^10658\\d+")) ||
                normalizedAddress.matches(Regex("^955\\d{2,}")) ||
                normalizedAddress.matches(Regex("^1069[0-9]\\d+")) ||
                normalizedAddress.matches(Regex("^1069[0-9]{2,}\\d+")) ||
                normalizedAddress.matches(Regex("^10698\\d+"))
            ) {
                // 号码特征符合银行，但无法精确识别具体银行
                // 检查内容中是否有金融相关特征词
                if (body.contains("储蓄卡") || body.contains("信用卡") ||
                    body.contains("银行卡") || body.contains("银行") ||
                    body.contains("卡消费") || body.contains("卡收入") ||
                    body.contains("卡尾号") || body.contains("账户") ||
                    body.contains("可用余额") || body.contains("账户余额") ||
                    body.contains("当前余额") || body.contains("支出") ||
                    body.contains("收入") || body.contains("转入") ||
                    body.contains("转出")
                ) {
                    return "银行"
                }
            }
        }
        return "未知来源"
    }

    /**
     * 提取银行卡尾号
     */
    fun extractCardLastFour(body: String): String? {
        return cardNumberRegex.find(body)?.groupValues?.get(1)
    }

    /**
     * 屏蔽银行卡号和余额区域，防止金额误识别
     * 例如：
     *   "尾号1234" -> "尾号XXXX"
     *   "账户1234" -> "账户XXXX"（建设银行格式）
     *   "可用余额87365.55元" -> "可用余额BBBB元"（避免余额被识别为交易金额）
     */
    private fun maskCardNumbers(body: String): String {
        var result = body
        // 屏蔽余额数字（必须在金额提取前先屏蔽，否则余额会被误识别为交易金额）
        result = balanceRegex.replace(result) { match ->
            match.value.replace(match.groupValues[1], "BBBB")
        }
        // 屏蔽卡尾号 / 账户号
        result = cardNumberRegex.replace(result) { match ->
            match.value.replace(match.groupValues[1], "XXXX")
        }
        // 屏蔽完整16位卡号
        result = fullCardRegex.replace(result, "XXXXXXXXXXXXXXXX")
        // 屏蔽验证码数字
        result = verificationCodeRegex.replace(result) { match ->
            match.value.replace(match.groupValues[1], "VVVV")
        }
        return result
    }

    /**
     * 精准提取金额（三级优先级策略）
     *
     * 设计原则：金额**绝对不能靠大小来猜**，0.01元和1000000元都是合法的交易金额。
     * 正确做法是**依靠上下文关键词定位**金额所在位置。
     *
     * 第1级（置信度 0.95）：交易关键词直接锚定
     *   匹配「支出/消费/到账/收到/付款/扣款/转账...」后面紧跟的金额
     *   这是最可靠的来源——银行账变短信的交易金额一定出现在这些词的附近
     *   余额在 maskCardNumbers 中已被替换为 BBBB，不会被这个正则匹配到
     *
     * 第2级（置信度 0.85）：货币符号直接锚定
     *   匹配「¥100」「￥20.28」「RMB 500」等格式
     *
     * 第3级（置信度 0.70）：全文兜底扫描
     *   此时余额已是 BBBB、卡尾号已是 XXXX，剩余「数字+元」即为交易金额
     *   若仍有多个候选，取第一个出现的（银行短信里交易金额通常先于其他说明出现）
     *
     * 返回：(金额, 置信度) 或 null
     */
    private fun extractAmount(cleanedBody: String): Pair<Double, Float>? {
        // === 第1级：交易关键词直接锚定（最高置信）===
        val anchoredMatches = anchoredAmountPattern.findAll(cleanedBody)
            .mapNotNull { it.groupValues[1].toDoubleOrNull() }
            .filter { it > 0 && it < 100_000_000 }
            .toList()
        if (anchoredMatches.isNotEmpty()) {
            // 关键词锚定时可能同一条短信有多笔（如分期账单），取第一笔
            return Pair(anchoredMatches.first(), 0.95f)
        }

        // === 第2级：货币符号直接锚定 ===
        val currencyMatches = currencySymbolPattern.findAll(cleanedBody)
            .mapNotNull { it.groupValues[1].toDoubleOrNull() }
            .filter { it > 0 && it < 100_000_000 }
            .toList()
        if (currencyMatches.isNotEmpty()) {
            return Pair(currencyMatches.first(), 0.85f)
        }

        // === 第3级：全文兜底（余额和卡尾号已被屏蔽）===
        val fallbackMatches = fallbackAmountPattern.findAll(cleanedBody)
            .mapNotNull { it.groupValues[1].toDoubleOrNull() }
            .filter { it > 0 && it < 100_000_000 }
            .toList()
        if (fallbackMatches.isNotEmpty()) {
            // 兜底时取第一个出现的金额（交易金额通常出现在余额/手续费之前）
            return Pair(fallbackMatches.first(), 0.70f)
        }

        return null
    }

    /**
     * 检测交易类型（收入/支出）
     * 返回：(类型, 置信度)
     *
     * 决策逻辑（优先级从高到低）：
     * 1. 只有收入关键词 → 收入
     * 2. 只有支出关键词 → 支出
     * 3. 收入关键词出现在支出关键词**之后** → 收入（复合词如「消费退货存入」「支付机构提现存入」
     *    中，收入的性质由后面的关键词决定，前面的「消费」「支付」仅描述渠道）
     * 4. 收入关键词出现在支出关键词之前或同等位置 → 支出（优先）
     */
    private fun detectTransactionType(body: String): Pair<TransactionType, Float>? {
        var maxIncomeConfidence = 0f
        var maxExpenseConfidence = 0f
        var incomePosition = Int.MAX_VALUE
        var expensePosition = Int.MAX_VALUE

        // 收入匹配
        for ((keyword, weight) in incomeKeywordsWeighted) {
            val idx = body.indexOf(keyword)
            if (idx >= 0 && weight > maxIncomeConfidence) {
                maxIncomeConfidence = weight
                incomePosition = idx
            }
        }

        // 支出匹配
        for ((keyword, weight) in expenseKeywordsWeighted) {
            val idx = body.indexOf(keyword)
            if (idx >= 0 && weight > maxExpenseConfidence) {
                maxExpenseConfidence = weight
                expensePosition = idx
            }
        }

        return when {
            maxIncomeConfidence == 0f && maxExpenseConfidence == 0f -> null
            maxIncomeConfidence > 0f && maxExpenseConfidence == 0f -> Pair(TransactionType.INCOME, maxIncomeConfidence)
            maxExpenseConfidence > 0f && maxIncomeConfidence == 0f -> Pair(TransactionType.EXPENSE, maxExpenseConfidence)
            // 两者都存在：
            // 情况A：收入关键词出现在**之后**（复合词如「机构提现存入」「消费退货存入」）
            // 情况B：收入关键词与支出关键词在**相同位置**（复合词以支出词开头，如「消费退货存入」以「消费」开头）
            //       → 复合词更精确，应判定为收入
            maxIncomeConfidence > 0f && maxExpenseConfidence > 0f && incomePosition >= expensePosition ->
                Pair(TransactionType.INCOME, maxIncomeConfidence)
            else -> Pair(TransactionType.EXPENSE, maxExpenseConfidence)
        }
    }

    /**
     * 是否是转账类短信
     */
    fun isTransferSms(body: String): Boolean {
        val transferKeywords = listOf(
            "转账", "汇款", "跨行转账", "同行转账",
            "转出到", "转入到", "账户间转账", "向.*转账"
        )
        return transferKeywords.any { pattern ->
            if (pattern.contains(".*")) Regex(pattern).containsMatchIn(body)
            else body.contains(pattern)
        }
    }
}

/**
 * 解析结果数据类
 */
data class ParseResult(
    val type: TransactionType?,
    val amount: Double?,
    val confidence: Float,
    val cardLastFour: String?
)

/**
 * SMS 读取诊断统计（用于 UI 展示调试信息）
 */
data class SmsReadStats(
    val totalSmsRead: Int = 0,
    val parsedOk: Int = 0,
    val excludedCount: Int = 0,
    val unknownSourceCount: Int = 0,
    val noAmountCount: Int = 0,
    val noTypeCount: Int = 0,
    val lowConfidenceCount: Int = 0,
    val exceptionCount: Int = 0,
    val bankHitCounts: Map<String, Int> = emptyMap(),
    // 深度诊断字段
    val cursorRowCount: Int = 0,
    val nullAddressCount: Int = 0,
    val nullBodyCount: Int = 0,
    val duplicateCount: Int = 0,
    val ccbAddressMatchCount: Int = 0,
    val sampleAddresses: List<String> = emptyList(),
    // 多 URI 探测结果
    val uriProbeResults: List<String> = emptyList(),
    // 通知栏捕获的银行短信数量
    val notificationSmsCaptured: Int = 0,
    val notificationSmsParsed: Int = 0
)

@Singleton
class SmsReader @Inject constructor(
    private val parser: SmsTransactionParser,
    private val notificationSmsDao: NotificationSmsDao
) {
    // 最近一次读取的诊断统计
    var lastReadStats: SmsReadStats = SmsReadStats()
        private set

    suspend fun readTransactions(
        contentResolver: ContentResolver,
        afterTime: Long = System.currentTimeMillis() - 2190L * 24 * 60 * 60 * 1000
    ): List<SmsTransaction> {
        val transactions = mutableListOf<SmsTransaction>()
        val seenIds = mutableSetOf<Long>()

        // 统计变量
        var totalRead = 0
        var parsedOk = 0
        var excludedCount = 0
        var unknownSourceCount = 0
        var noAmountCount = 0
        var noTypeCount = 0
        var lowConfCount = 0
        var exceptionCount = 0
        val bankHits = mutableMapOf<String, Int>()
        var cursorRowCount = 0
        var nullAddressCount = 0
        var nullBodyCount = 0
        var duplicateCount = 0
        var ccbAddressMatchCount = 0
        val addressSet = mutableSetOf<String>()
        val uriProbeResults = mutableListOf<String>()

        // ── 第一阶段：从标准 URI 读取 SMS ──
        val standardUris = listOf(
            Telephony.Sms.CONTENT_URI,                    // content://sms
            Uri.parse("content://sms/inbox"),             // content://sms/inbox
            Uri.parse("content://sms/sent"),              // content://sms/sent
        )

        for (smsUri in standardUris) {
            try {
                val uriStats = readFromUri(
                    contentResolver, smsUri, afterTime, seenIds,
                    transactions, bankHits, addressSet
                )
                cursorRowCount += uriStats.cursorRows
                nullAddressCount += uriStats.nullAddress
                nullBodyCount += uriStats.nullBody
                duplicateCount += uriStats.duplicate
                ccbAddressMatchCount += uriStats.ccbMatch
                totalRead += uriStats.totalProcessed
                parsedOk += uriStats.parsed
                excludedCount += uriStats.excluded
                unknownSourceCount += uriStats.unknownSource
                noAmountCount += uriStats.noAmount
                noTypeCount += uriStats.noType
                lowConfCount += uriStats.lowConf
                exceptionCount += uriStats.exceptions
                if (uriStats.cursorRows > 0) {
                    uriProbeResults.add("$smsUri: ${uriStats.cursorRows}行, ccb=${uriStats.ccbMatch}")
                }
            } catch (_: Exception) { }
        }

        // ── 第二阶段：探测其他可能存储 SMS 的 URI ──
        // 国产 ROM (MIUI/ColorOS/HarmonyOS) 可能把通知类短信存在不同位置
        val probeUris = listOf(
            "content://mms-sms",
            "content://mms-sms/conversations",
            "content://mms-sms/complete-conversations",
            "content://sms/icc",
            "content://sms/icc2",
            "content://mms",
            "content://mms/inbox",
        )

        for (uriStr in probeUris) {
            try {
                val uri = Uri.parse(uriStr)
                val cursor = contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    val count = it.count
                    val cols = it.columnNames?.toList() ?: emptyList()
                    val hasAddress = cols.any { c -> c.equals("address", true) }
                    val hasBody = cols.any { c -> c.equals("body", true) }

                    // 如果有 address 和 body 列，尝试从中读取 CCB 短信
                    var ccbFound = 0
                    if (hasAddress && count > 0) {
                        val addrIdx = it.getColumnIndex("address")
                        while (it.moveToNext()) {
                            try {
                                val addr = it.getString(addrIdx) ?: continue
                                if (addr.contains("95533")) ccbFound++
                            } catch (_: Exception) { break }
                        }
                    }

                    uriProbeResults.add("$uriStr: ${count}行, cols=${cols.size}, addr=$hasAddress, body=$hasBody, ccb=$ccbFound")

                    // 如果在此 URI 发现了 CCB 短信，尝试完整读取
                    if (ccbFound > 0 && hasBody) {
                        it.moveToPosition(-1)  // reset cursor
                        readFromGenericCursor(
                            it, seenIds, transactions, bankHits, addressSet,
                            { totalRead++ }, { parsedOk++ }, { excludedCount++ },
                            { unknownSourceCount++ }, { noAmountCount++ },
                            { noTypeCount++ }, { lowConfCount++ }, { exceptionCount++ }
                        ).let { stats ->
                            ccbAddressMatchCount += stats.ccbMatch
                            nullAddressCount += stats.nullAddress
                            nullBodyCount += stats.nullBody
                            duplicateCount += stats.duplicate
                        }
                    }
                }
            } catch (e: Exception) {
                uriProbeResults.add("$uriStr: ERR(${e.javaClass.simpleName})")
            }
        }

        // ── 第三阶段：直接按地址搜索 CCB 短信 ──
        // 在标准 SMS provider 中用 WHERE address LIKE '%95533%' 直接搜索
        try {
            val ccbCursor = contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                arrayOf(Telephony.Sms._ID, Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE),
                "${Telephony.Sms.ADDRESS} LIKE ?",
                arrayOf("%95533%"),
                null
            )
            ccbCursor?.use {
                val directCcbCount = it.count
                uriProbeResults.add("直接搜CCB(LIKE%95533%): ${directCcbCount}行")
                if (directCcbCount > 0) {
                    val addrIdx = it.getColumnIndex(Telephony.Sms.ADDRESS)
                    val bodyIdx = it.getColumnIndex(Telephony.Sms.BODY)
                    val dateIdx = it.getColumnIndex(Telephony.Sms.DATE)
                    val idIdx = it.getColumnIndex(Telephony.Sms._ID)
                    while (it.moveToNext()) {
                        try {
                            val id = it.getLong(idIdx)
                            if (!seenIds.add(id)) continue
                            val address = it.getString(addrIdx) ?: continue
                            val body = it.getString(bodyIdx) ?: continue
                            val date = it.getLong(dateIdx)
                            ccbAddressMatchCount++
                            totalRead++
                            addressSet.add(address)
                            val normalizedAddress = address.removePrefix("+86").removePrefix("86").removePrefix("12580").removePrefix("12520").trim()
                            val source = parser.detectBankSource(body, normalizedAddress)
                            if (source != "未知来源") bankHits[source] = (bankHits[source] ?: 0) + 1
                            val parseResult = parser.parseSmsContent(body, normalizedAddress)
                            if (parseResult.type != null && parseResult.amount != null && parseResult.amount > 0) {
                                parsedOk++
                                transactions.add(SmsTransaction(id = id, address = address, body = body, date = date,
                                    type = parseResult.type, amount = parseResult.amount, source = source,
                                    isTransfer = parser.isTransferSms(body), cardLastFour = parseResult.cardLastFour, confidence = parseResult.confidence))
                            }
                        } catch (_: Exception) { exceptionCount++ }
                    }
                }
            }
        } catch (e: Exception) {
            uriProbeResults.add("直接搜CCB: ERR(${e.javaClass.simpleName})")
        }

        // ── 第四阶段：尝试通过 thread_id 方式查找 ──
        // 有些 ROM 的 ContentProvider 不支持 address 列搜索，但支持 thread_id 查询
        try {
            // 查找所有会话线程
            val threadCursor = contentResolver.query(
                Uri.parse("content://sms/conversations"),
                arrayOf("thread_id", "msg_count"),
                null, null, null
            )
            threadCursor?.use {
                val threadCount = it.count
                uriProbeResults.add("会话线程数: $threadCount")
            }
        } catch (e: Exception) {
            uriProbeResults.add("会话线程: ERR(${e.javaClass.simpleName})")
        }

        // ── 第五阶段：从 Room 数据库读取通知栏捕获的银行短信 ──
        // 这些短信来自 NotificationListenerService 捕获的 1069xxxx 通知短信
        // 标准 content://sms 读不到它们，只能通过通知栏实时捕获后持久化
        var notifCaptured = 0
        var notifParsed = 0
        try {
            val notifSmsEntities = if (afterTime > 0) {
                notificationSmsDao.getAfter(afterTime)
            } else {
                notificationSmsDao.getAll()
            }
            notifCaptured = notifSmsEntities.size

            for (entity in notifSmsEntities) {
                try {
                    // 用 entity.id + offset 生成不与标准 SMS id 冲突的唯一 ID
                    val syntheticId = entity.id + 2_000_000_000L
                    if (!seenIds.add(syntheticId)) {
                        duplicateCount++
                        continue
                    }

                    val address = entity.address
                    val body = entity.body

                    if (addressSet.size < 50) addressSet.add("(通知)$address")
                    if (address.contains("95533")) ccbAddressMatchCount++

                    totalRead++

                    val normalizedAddress = address
                        .removePrefix("+86")
                        .removePrefix("86")
                        .trim()

                    val source = parser.detectBankSource(body, normalizedAddress)
                    if (source != "未知来源") {
                        bankHits[source] = (bankHits[source] ?: 0) + 1
                    }

                    val parseResult = parser.parseSmsContent(body, normalizedAddress)

                    if (parseResult.type == null && parseResult.amount == null && parseResult.confidence == 0f) {
                        if (source == "未知来源") unknownSourceCount++ else excludedCount++
                        continue
                    }
                    if (parseResult.amount == null) { noAmountCount++; continue }
                    if (parseResult.type == null) {
                        if (parseResult.confidence > 0f && parseResult.confidence < 0.5f) lowConfCount++ else noTypeCount++
                        continue
                    }
                    if (parseResult.amount <= 0) continue

                    parsedOk++
                    notifParsed++
                    transactions.add(
                        SmsTransaction(
                            id = syntheticId,
                            address = "(通知)$address",
                            body = body,
                            date = entity.date,
                            type = parseResult.type,
                            amount = parseResult.amount,
                            source = source,
                            isTransfer = parser.isTransferSms(body),
                            cardLastFour = parseResult.cardLastFour,
                            confidence = parseResult.confidence
                        )
                    )
                } catch (_: Exception) { exceptionCount++ }
            }
            uriProbeResults.add("通知栏短信(Room): 共${notifCaptured}条, 解析成功${notifParsed}条")
        } catch (e: Exception) {
            uriProbeResults.add("通知栏短信(Room): ERR(${e.javaClass.simpleName})")
        }

        lastReadStats = SmsReadStats(
            totalSmsRead = totalRead,
            parsedOk = parsedOk,
            excludedCount = excludedCount,
            unknownSourceCount = unknownSourceCount,
            noAmountCount = noAmountCount,
            noTypeCount = noTypeCount,
            lowConfidenceCount = lowConfCount,
            exceptionCount = exceptionCount,
            bankHitCounts = bankHits.toMap(),
            cursorRowCount = cursorRowCount,
            nullAddressCount = nullAddressCount,
            nullBodyCount = nullBodyCount,
            duplicateCount = duplicateCount,
            ccbAddressMatchCount = ccbAddressMatchCount,
            sampleAddresses = addressSet.take(30).toList(),
            uriProbeResults = uriProbeResults,
            notificationSmsCaptured = notifCaptured,
            notificationSmsParsed = notifParsed
        )

        return transactions.sortedByDescending { it.date }
    }

    private data class UriReadResult(
        val cursorRows: Int = 0,
        val nullAddress: Int = 0,
        val nullBody: Int = 0,
        val duplicate: Int = 0,
        val ccbMatch: Int = 0,
        val totalProcessed: Int = 0,
        val parsed: Int = 0,
        val excluded: Int = 0,
        val unknownSource: Int = 0,
        val noAmount: Int = 0,
        val noType: Int = 0,
        val lowConf: Int = 0,
        val exceptions: Int = 0
    )

    private fun readFromUri(
        contentResolver: ContentResolver,
        uri: Uri,
        afterTime: Long,
        seenIds: MutableSet<Long>,
        transactions: MutableList<SmsTransaction>,
        bankHits: MutableMap<String, Int>,
        addressSet: MutableSet<String>
    ): UriReadResult {
        var cursorRows = 0
        var nullAddress = 0
        var nullBody = 0
        var duplicate = 0
        var ccbMatch = 0
        var totalProcessed = 0
        var parsed = 0
        var excluded = 0
        var unknownSource = 0
        var noAmount = 0
        var noType = 0
        var lowConf = 0
        var exceptions = 0

        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.DATE_SENT
        )

        // 当 afterTime <= 0 时不加任何过滤条件，确保读取全部短信
        val selection: String? = if (afterTime > 0) "${Telephony.Sms.DATE} > ?" else null
        val selectionArgs: Array<String>? = if (afterTime > 0) arrayOf(afterTime.toString()) else null

        var cursor: Cursor? = null
        try {
            cursor = contentResolver.query(uri, projection, selection, selectionArgs, null)

            cursor?.let {
                cursorRows = it.count  // 原始行数

                val idIndex = it.getColumnIndex(Telephony.Sms._ID)
                val addressIndex = it.getColumnIndex(Telephony.Sms.ADDRESS)
                val bodyIndex = it.getColumnIndex(Telephony.Sms.BODY)
                val dateIndex = it.getColumnIndex(Telephony.Sms.DATE)
                val dateSentIndex = it.getColumnIndex(Telephony.Sms.DATE_SENT)

                while (it.moveToNext()) {
                    try {
                        val id = it.getLong(idIndex)

                        if (!seenIds.add(id)) {
                            duplicate++
                            continue
                        }

                        val address = it.getString(addressIndex)
                        if (address == null) {
                            nullAddress++
                            continue
                        }

                        val body = it.getString(bodyIndex)
                        if (body == null) {
                            nullBody++
                            continue
                        }

                        // 收集不同发件人号码（诊断用）
                        if (addressSet.size < 50) {
                            addressSet.add(address)
                        }

                        // 检测是否为 CCB 相关号码
                        if (address.contains("95533")) {
                            ccbMatch++
                        }

                        val date = it.getLong(dateIndex)
                        val dateSent = if (dateSentIndex >= 0) it.getLong(dateSentIndex) else 0L
                        val effectiveDate = if (dateSent > 0 && dateSent > date) dateSent else date

                        totalProcessed++

                        val normalizedAddress = address
                            .removePrefix("+86")
                            .removePrefix("86")
                            .removePrefix("12580")
                            .removePrefix("12520")
                            .trim()

                        val source = parser.detectBankSource(body, normalizedAddress)
                        if (source != "未知来源") {
                            bankHits[source] = (bankHits[source] ?: 0) + 1
                        }

                        val parseResult = parser.parseSmsContent(body, normalizedAddress)

                        if (parseResult.type == null && parseResult.amount == null && parseResult.confidence == 0f) {
                            if (source == "未知来源") {
                                unknownSource++
                            } else {
                                excluded++
                            }
                            continue
                        }
                        if (parseResult.amount == null) {
                            noAmount++
                            continue
                        }
                        if (parseResult.type == null) {
                            if (parseResult.confidence > 0f && parseResult.confidence < 0.5f) {
                                lowConf++
                            } else {
                                noType++
                            }
                            continue
                        }
                        if (parseResult.amount <= 0) continue

                        parsed++
                        val isTransfer = parser.isTransferSms(body)
                        transactions.add(
                            SmsTransaction(
                                id = id,
                                address = address,
                                body = body,
                                date = effectiveDate,
                                type = parseResult.type,
                                amount = parseResult.amount,
                                source = source,
                                isTransfer = isTransfer,
                                cardLastFour = parseResult.cardLastFour,
                                confidence = parseResult.confidence
                            )
                        )
                    } catch (_: Exception) {
                        exceptions++
                    }
                }
            }
        } catch (_: SecurityException) {
            // 没有短信读取权限
        } catch (_: Exception) {
            // 该 URI 查询失败
        } finally {
            cursor?.close()
        }

        return UriReadResult(
            cursorRows, nullAddress, nullBody, duplicate, ccbMatch,
            totalProcessed, parsed, excluded, unknownSource,
            noAmount, noType, lowConf, exceptions
        )
    }

    private data class GenericCursorResult(
        val ccbMatch: Int = 0,
        val nullAddress: Int = 0,
        val nullBody: Int = 0,
        val duplicate: Int = 0
    )

    /**
     * 从通用 Cursor 中读取短信并解析交易
     * 用于 Phase 2 探测到的非标准 URI（如 content://mms-sms）
     */
    private fun readFromGenericCursor(
        cursor: Cursor,
        seenIds: MutableSet<Long>,
        transactions: MutableList<SmsTransaction>,
        bankHits: MutableMap<String, Int>,
        addressSet: MutableSet<String>,
        onTotalRead: () -> Unit,
        onParsedOk: () -> Unit,
        onExcluded: () -> Unit,
        onUnknownSource: () -> Unit,
        onNoAmount: () -> Unit,
        onNoType: () -> Unit,
        onLowConf: () -> Unit,
        onException: () -> Unit
    ): GenericCursorResult {
        var ccbMatch = 0
        var nullAddress = 0
        var nullBody = 0
        var duplicate = 0

        val idIdx = cursor.getColumnIndex("_id")
        val addrIdx = cursor.getColumnIndex("address")
        val bodyIdx = cursor.getColumnIndex("body")
        val dateIdx = cursor.getColumnIndex("date")

        if (addrIdx < 0 || bodyIdx < 0) {
            return GenericCursorResult()
        }

        while (cursor.moveToNext()) {
            try {
                val id = if (idIdx >= 0) cursor.getLong(idIdx) else cursor.position.toLong() + 1_000_000_000L
                if (!seenIds.add(id)) {
                    duplicate++
                    continue
                }

                val address = cursor.getString(addrIdx)
                if (address == null) {
                    nullAddress++
                    continue
                }

                val body = cursor.getString(bodyIdx)
                if (body == null) {
                    nullBody++
                    continue
                }

                if (address.contains("95533")) ccbMatch++

                if (addressSet.size < 50) addressSet.add(address)

                val date = if (dateIdx >= 0) cursor.getLong(dateIdx) else 0L

                onTotalRead()

                val normalizedAddress = address
                    .removePrefix("+86")
                    .removePrefix("86")
                    .removePrefix("12580")
                    .removePrefix("12520")
                    .trim()

                val source = parser.detectBankSource(body, normalizedAddress)
                if (source != "未知来源") {
                    bankHits[source] = (bankHits[source] ?: 0) + 1
                }

                val parseResult = parser.parseSmsContent(body, normalizedAddress)

                if (parseResult.type == null && parseResult.amount == null && parseResult.confidence == 0f) {
                    if (source == "未知来源") onUnknownSource() else onExcluded()
                    continue
                }
                if (parseResult.amount == null) { onNoAmount(); continue }
                if (parseResult.type == null) {
                    if (parseResult.confidence > 0f && parseResult.confidence < 0.5f) onLowConf() else onNoType()
                    continue
                }
                if (parseResult.amount <= 0) continue

                onParsedOk()
                transactions.add(
                    SmsTransaction(
                        id = id, address = address, body = body, date = date,
                        type = parseResult.type, amount = parseResult.amount,
                        source = source, isTransfer = parser.isTransferSms(body),
                        cardLastFour = parseResult.cardLastFour, confidence = parseResult.confidence
                    )
                )
            } catch (_: Exception) {
                onException()
            }
        }

        return GenericCursorResult(ccbMatch, nullAddress, nullBody, duplicate)
    }

    fun hasSmsPermission(): Boolean {
        return true
    }
}
