package com.tinyledger.app.ui.viewmodel

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tinyledger.app.data.sms.SmsReader
import com.tinyledger.app.data.sms.SmsReadStats
import com.tinyledger.app.data.sms.SmsTransaction
import com.tinyledger.app.data.sms.WeChatAlipayCSVParser
import com.tinyledger.app.domain.model.Account
import com.tinyledger.app.domain.model.AccountType
import com.tinyledger.app.domain.model.Category
import com.tinyledger.app.domain.model.Transaction
import com.tinyledger.app.domain.model.TransactionType
import com.tinyledger.app.domain.repository.AccountRepository
import com.tinyledger.app.domain.repository.TransactionRepository
import com.tinyledger.app.ui.screens.automation.ImportSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class AutoImportUiState(
    val isLoading: Boolean = false,
    // SMS 模式数据
    val smsTransactions: List<SmsTransaction> = emptyList(),
    // SMS 诊断统计
    val smsReadStats: SmsReadStats? = null,
    // CSV 模式数据
    val csvTransactions: List<WeChatAlipayCSVParser.CsvTransaction> = emptyList(),
    val selectedTransactionIds: Set<String> = emptySet(),
    val importSuccess: Boolean = false,
    val importCount: Int = 0,
    val errorMessage: String? = null,
    val accounts: List<Account> = emptyList()
)

@HiltViewModel
class AutoImportViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val smsReader: SmsReader
) : ViewModel() {

    private val _uiState = MutableStateFlow(AutoImportUiState())
    val uiState: StateFlow<AutoImportUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            accountRepository.getAllAccounts().collect { accounts ->
                _uiState.update { it.copy(accounts = accounts) }
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // SMS 模式
    // ──────────────────────────────────────────────────────────────────────────

    fun loadSmsTransactions(
        contentResolver: ContentResolver,
        startTime: Long? = null,
        endTime: Long? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val transactions = withContext(Dispatchers.IO) {
                    smsReader.readTransactions(contentResolver, startTime ?: 0L)
                }.filter { sms ->
                    if (startTime != null && sms.date < startTime) false
                    else if (endTime != null && sms.date > endTime) false
                    else true
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        smsTransactions = transactions,
                        smsReadStats = smsReader.lastReadStats
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "加载短信失败: ${e.message}") }
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // CSV 模式
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * 从 URI 读取 CSV 或 xlsx 文件并解析
     * 自动根据文件扩展名/MIME 类型判断格式
     */
    fun loadCsvFromUri(
        contentResolver: ContentResolver,
        uri: Uri,
        importSource: ImportSource
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, selectedTransactionIds = emptySet()) }
            try {
                // 判断是否是 xlsx 格式
                val isXlsx = isXlsxFile(contentResolver, uri)

                val transactions = withContext(Dispatchers.IO) {
                    if (isXlsx) {
                        // ── xlsx 解析路径 ──────────────────────────────
                        contentResolver.openInputStream(uri)?.use { stream ->
                            WeChatAlipayCSVParser.parseXlsx(stream)
                        } ?: emptyList()
                    } else {
                        // ── CSV 解析路径（原有逻辑）────────────────────
                        val csvText = contentResolver.openInputStream(uri)?.use { inputStream ->
                            val bytes = inputStream.readBytes()
                            tryDecodeBytes(bytes)
                        } ?: throw Exception("无法读取文件")

                        when (importSource) {
                            ImportSource.WECHAT -> WeChatAlipayCSVParser.parse(csvText)
                                .filter { it.source.contains("微信") }
                            ImportSource.ALIPAY -> WeChatAlipayCSVParser.parse(csvText)
                                .filter { it.source.contains("支付宝") }
                            else -> emptyList()
                        }
                    }
                }

                if (transactions.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            csvTransactions = emptyList(),
                            errorMessage = "未在文件中找到有效收支记录，请确认是微信/支付宝导出的账单文件（支持 CSV 和 xlsx 格式）"
                        )
                    }
                } else {
                    // 默认全选
                    val allIds = transactions.map { "${it.id}_csv" }.toSet()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            csvTransactions = transactions,
                            selectedTransactionIds = allIds
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "解析文件失败: ${e.message}")
                }
            }
        }
    }

    /**
     * 判断 URI 指向的是否是 xlsx 文件
     * 通过 MIME 类型或文件名后缀判断
     */
    private fun isXlsxFile(contentResolver: ContentResolver, uri: Uri): Boolean {
        // 先检查 MIME 类型
        val mimeType = contentResolver.getType(uri)
        if (mimeType != null) {
            val lower = mimeType.lowercase()
            if (lower.contains("spreadsheet") || lower.contains("excel") || lower.contains("xlsx")) {
                return true
            }
            if (lower.contains("csv") || lower.contains("text")) {
                return false
            }
        }
        // 再检查文件名后缀
        val path = uri.path?.lowercase() ?: ""
        val lastSegment = uri.lastPathSegment?.lowercase() ?: ""
        return path.endsWith(".xlsx") || lastSegment.endsWith(".xlsx")
    }

    /**
     * 尝试多种编码解析字节数组（CSV 可能是 GBK 或 UTF-8）
     */
    private fun tryDecodeBytes(bytes: ByteArray): String {
        // 检测 UTF-8 BOM
        if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
            return String(bytes, 3, bytes.size - 3, Charsets.UTF_8)
        }
        // 尝试 UTF-8
        val utf8 = runCatching { String(bytes, Charsets.UTF_8) }.getOrNull()
        if (utf8 != null && !utf8.contains('\uFFFD')) return utf8
        // 尝试 GBK（常见于微信/支付宝导出）
        val gbk = runCatching {
            String(bytes, charset("GBK"))
        }.getOrNull()
        if (gbk != null) return gbk
        // 兜底 UTF-8
        return String(bytes, Charsets.UTF_8)
    }

    fun clearCsvTransactions() {
        _uiState.update {
            it.copy(csvTransactions = emptyList(), selectedTransactionIds = emptySet())
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 选择管理
    // ──────────────────────────────────────────────────────────────────────────

    fun toggleTransactionSelection(transactionId: String) {
        _uiState.update { state ->
            val newSelection = if (transactionId in state.selectedTransactionIds) {
                state.selectedTransactionIds - transactionId
            } else {
                state.selectedTransactionIds + transactionId
            }
            state.copy(selectedTransactionIds = newSelection)
        }
    }

    fun selectAllTransactions(isCsvMode: Boolean) {
        _uiState.update { state ->
            val ids = if (isCsvMode) {
                state.csvTransactions.map { "${it.id}_csv" }.toSet()
            } else {
                state.smsTransactions.map { "${it.id}_sms" }.toSet()
            }
            state.copy(selectedTransactionIds = ids)
        }
    }

    fun deselectAllTransactions() {
        _uiState.update { it.copy(selectedTransactionIds = emptySet()) }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 导入
    // ──────────────────────────────────────────────────────────────────────────

    fun importSelectedTransactions(isCsvMode: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val accounts = _uiState.value.accounts
                var importCount = 0

                withContext(Dispatchers.IO) {
                    if (isCsvMode) {
                        // ── CSV 导入 ──────────────────────────────────
                        val selected = _uiState.value.csvTransactions.filter {
                            "${it.id}_csv" in _uiState.value.selectedTransactionIds
                        }
                        selected.forEach { csv ->
                            try {
                                val accountId = matchCsvAccount(csv, accounts)
                                val category = inferCsvCategory(csv)
                                val transaction = Transaction(
                                    type = csv.type,
                                    category = category,
                                    amount = csv.amount,
                                    note = buildCsvNote(csv),
                                    date = csv.date,
                                    accountId = accountId
                                )
                                transactionRepository.insertTransaction(transaction)
                                importCount++
                            } catch (_: Exception) {}
                        }
                    } else {
                        // ── SMS 导入 ──────────────────────────────────
                        val selected = _uiState.value.smsTransactions.filter {
                            "${it.id}_sms" in _uiState.value.selectedTransactionIds
                        }
                        selected.forEach { sms ->
                            try {
                                val accountId = matchAccountId(sms, accounts)
                                val category = inferCategory(sms)
                                val transaction = Transaction(
                                    type = sms.type ?: TransactionType.EXPENSE,
                                    category = category,
                                    amount = sms.amount ?: 0.0,
                                    note = buildNote(sms),
                                    date = sms.date,
                                    accountId = accountId
                                )
                                transactionRepository.insertTransaction(transaction)
                                importCount++
                            } catch (_: Exception) {}
                        }
                    }
                }

                _uiState.update {
                    it.copy(isLoading = false, importSuccess = true, importCount = importCount)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "导入失败: ${e.message}")
                }
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // CSV 辅助方法
    // ──────────────────────────────────────────────────────────────────────────

    private fun matchCsvAccount(
        csv: WeChatAlipayCSVParser.CsvTransaction,
        accounts: List<Account>
    ): Long? {
        val targetType = when {
            csv.source.contains("微信") -> AccountType.WECHAT
            csv.source.contains("支付宝") -> AccountType.ALIPAY
            else -> null
        } ?: return null
        return accounts.find { it.type == targetType }?.id
    }

    private fun inferCsvCategory(csv: WeChatAlipayCSVParser.CsvTransaction): Category {
        val text = "${csv.description} ${csv.counterpart}"

        // 先尝试从交易对方名称匹配（仅支出类型）
        if (csv.type == TransactionType.EXPENSE) {
            val merchantCategory = inferCategoryFromMerchant(csv.counterpart)
            if (merchantCategory != null) return merchantCategory
        }

        return if (csv.type == TransactionType.INCOME) {
            when {
                text.containsAny("工资", "薪资", "薪酬", "代发", "发薪") -> Category.fromId("salary", TransactionType.INCOME)
                text.containsAny("奖金", "绩效", "年终奖") -> Category.fromId("bonus", TransactionType.INCOME)
                text.containsAny("分红", "股息") -> Category.fromId("dividend", TransactionType.INCOME)
                text.containsAny("退款", "退还", "返还", "退货") -> Category.fromId("refund", TransactionType.INCOME)
                text.containsAny("押金退", "退押金", "退保证金") -> Category.fromId("deposit_back", TransactionType.INCOME)
                text.containsAny("报销", "报销款") -> Category.fromId("reimbursement", TransactionType.INCOME)
                text.containsAny("红包") -> Category.fromId("redpacket", TransactionType.INCOME)
                text.containsAny("收回借款", "还款", "还钱") -> Category.fromId("recover_loan", TransactionType.INCOME)
                text.containsAny("投资", "理财", "收益", "利息", "基金", "赎回") -> Category.fromId("investment", TransactionType.INCOME)
                text.containsAny("转账", "汇款", "转入") -> Category.fromId("income_transfer", TransactionType.INCOME)
                else -> Category.fromId("redpacket", TransactionType.INCOME)
            }
        } else {
            when {
                // 住宿
                text.containsAny("酒店", "宾馆", "民宿", "旅馆", "客栈", "住宿",
                    "如家", "汉庭", "全季", "亚朵") -> Category.fromId("accommodation", TransactionType.EXPENSE)
                // 慈善捐赠
                text.containsAny("慈善", "捐赠", "捐款", "公益", "红十字",
                    "希望工程", "壹基金", "天使") -> Category.fromId("charity", TransactionType.EXPENSE)
                // 派发红包
                text.containsAny("发红包", "派发红包", "发出红包") -> Category.fromId("send_redpacket", TransactionType.EXPENSE)
                // 餐饮
                text.containsAny("餐", "饭", "食", "外卖", "美团", "饿了么", "肯德基", "麦当劳",
                    "海底捞", "奶茶", "咖啡", "瑞幸", "星巴克", "烧烤", "火锅", "小吃",
                    "食堂", "饮料", "甜品", "蛋糕",
                    "米线", "豆腐", "面馆", "饺子", "包子", "拉面") -> Category.fromId("food", TransactionType.EXPENSE)
                // 交通
                text.containsAny("打车", "滴滴", "地铁", "公交", "高铁", "机票", "加油",
                    "停车", "高速", "出租", "曹操", "T3出行", "花小猪",
                    "铁路", "12306", "航空", "ETC") -> Category.fromId("transport", TransactionType.EXPENSE)
                // 购物
                text.containsAny("购物", "京东", "淘宝", "天猫", "超市", "商场", "拼多多",
                    "唯品会", "苏宁", "当当", "沃尔玛", "永辉",
                    "便利店", "全家", "711", "罗森",
                    "水果", "蔬", "小卖部", "盒马", "零食", "百货") -> Category.fromId("shopping", TransactionType.EXPENSE)
                // 水电气网
                text.containsAny("水费", "电费", "燃气", "天然气", "煤气", "暖气", "宽带",
                    "网费", "物业费", "电力", "自来水", "国网") -> Category.fromId("utilities", TransactionType.EXPENSE)
                // 房贷支出
                text.containsAny("房贷", "按揭", "月供", "公积金", "住房贷款") -> Category.fromId("mortgage", TransactionType.EXPENSE)
                // 信用卡还款
                text.containsAny("信用卡还款", "还信用卡", "信用卡", "账单还款") -> Category.fromId("credit_card_repay", TransactionType.EXPENSE)
                // 支付宝还款
                text.containsAny("花呗", "借呗", "蚂蚁") -> Category.fromId("alipay_repay", TransactionType.EXPENSE)
                // 京东还款
                text.containsAny("京东白条", "白条") -> Category.fromId("jd_repay", TransactionType.EXPENSE)
                // 抖音还款
                text.containsAny("抖音月付", "放心借") -> Category.fromId("douyin_repay", TransactionType.EXPENSE)
                // 转账
                text.containsAny("转账", "汇款", "转出") -> Category.fromId("account_transfer", TransactionType.EXPENSE)
                // 娱乐
                text.containsAny("娱乐", "电影", "游戏", "视频", "音乐", "KTV",
                    "充值", "会员") -> Category.fromId("entertainment", TransactionType.EXPENSE)
                // 医疗
                text.containsAny("医院", "药店", "医疗", "诊所", "挂号", "门诊",
                    "体检", "药房") -> Category.fromId("medical", TransactionType.EXPENSE)
                // 住房
                text.containsAny("房租", "物业", "租房") -> Category.fromId("housing", TransactionType.EXPENSE)
                // 通讯
                text.containsAny("话费", "流量", "通讯", "中国移动", "中国联通", "中国电信") -> Category.fromId("communication", TransactionType.EXPENSE)
                // 教育
                text.containsAny("学费", "培训", "教育", "书", "课程", "网课") -> Category.fromId("education", TransactionType.EXPENSE)
                // 保险
                text.containsAny("保险", "保费", "社保", "医保") -> Category.fromId("insurance", TransactionType.EXPENSE)
                // 旅游
                text.containsAny("旅游", "景区", "门票",
                    "携程", "去哪儿", "飞猪") -> Category.fromId("travel", TransactionType.EXPENSE)
                // 投资
                text.containsAny("投资", "理财", "基金", "股票", "证券") -> Category.fromId("investment_expense", TransactionType.EXPENSE)
                else -> Category.fromId("other", TransactionType.EXPENSE)
            }
        }
    }

    private fun buildCsvNote(csv: WeChatAlipayCSVParser.CsvTransaction): String {
        val parts = listOfNotNull(
            csv.source,
            csv.counterpart.takeIf { it.isNotBlank() },
            csv.description.takeIf { it.isNotBlank() && it != csv.counterpart }
        )
        return parts.joinToString(" · ").take(80)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // SMS 辅助方法（原有逻辑保留）
    // ──────────────────────────────────────────────────────────────────────────

    private fun matchAccountId(sms: SmsTransaction, accounts: List<Account>): Long? {
        if (accounts.isEmpty()) return null
        val cardLastFour = sms.cardLastFour
        if (!cardLastFour.isNullOrBlank()) {
            val matched = accounts.find { account ->
                account.cardNumber?.endsWith(cardLastFour) == true ||
                        account.cardNumber == cardLastFour
            }
            if (matched != null) return matched.id
        }
        val source = sms.source
        val typeMatched = when {
            source == "微信支付" -> accounts.find { it.type == AccountType.WECHAT }
            source == "支付宝" -> accounts.find { it.type == AccountType.ALIPAY }
            source.contains("银行") || source.contains("行") -> {
                accounts.filter { it.type == AccountType.BANK }.find { account ->
                    account.name.contains(source) || source.contains(account.name.take(4))
                } ?: accounts.firstOrNull { it.type == AccountType.BANK }
            }
            else -> null
        }
        return typeMatched?.id
    }

    /**
     * 从短信正文中提取商户名（"向某某支付"模式）
     */
    private fun extractMerchantName(body: String): String? {
        val patterns = listOf(
            Regex("向(.+?)支付"),
            Regex("在(.+?)消费"),
            Regex("付款给(.+?)"),
            Regex("支付给(.+?)[,，。]")
        )
        for (pattern in patterns) {
            val match = pattern.find(body)
            if (match != null) {
                return match.groupValues[1].trim()
            }
        }
        return null
    }

    /**
     * 根据商户名匹配分类
     */
    private fun inferCategoryFromMerchant(merchant: String): Category? {
        return when {
            // 住宿类
            merchant.containsAny("酒店", "宾馆", "民宿", "旅馆", "客栈", "公寓",
                "如家", "汉庭", "全季", "亚朵", "希尔顿", "万豪", "洲际",
                "锦江", "华住", "7天", "七天") ->
                Category.fromId("accommodation", TransactionType.EXPENSE)
            // 慈善捐赠类
            merchant.containsAny("基金", "慈善", "天使", "公益", "红十字",
                "捐赠", "捐款", "希望工程", "壹基金") ->
                Category.fromId("charity", TransactionType.EXPENSE)
            // 餐饮类
            merchant.containsAny("米线", "豆腐", "烧烤", "面馆", "饺子", "包子",
                "粉丝", "麻辣", "串串", "炸鸡", "鸡排", "牛肉", "羊肉",
                "拉面", "馄饨", "煲仔", "粥", "寿司", "料理", "火锅",
                "餐厅", "饭店", "小吃", "快餐", "食堂", "茶餐厅",
                "肯德基", "麦当劳", "海底捞", "瑞幸", "星巴克") ->
                Category.fromId("food", TransactionType.EXPENSE)
            // 购物类
            merchant.containsAny("水果", "果", "蔬", "小卖部", "盒马", "零食",
                "百货", "超市", "商店", "便利店", "杂货", "菜市场",
                "沃尔玛", "永辉", "华润", "大润发", "物美",
                "全家", "711", "罗森", "美宜佳") ->
                Category.fromId("shopping", TransactionType.EXPENSE)
            else -> null
        }
    }

    private fun inferCategory(sms: SmsTransaction): Category {
        val body = sms.body
        val type = sms.type ?: TransactionType.EXPENSE

        // 先尝试从商户名匹配（仅支出类型）
        if (type == TransactionType.EXPENSE) {
            val merchant = extractMerchantName(body)
            if (merchant != null) {
                val merchantCategory = inferCategoryFromMerchant(merchant)
                if (merchantCategory != null) return merchantCategory
            }
        }

        return if (type == TransactionType.INCOME) {
            when {
                body.containsAny("工资", "薪资", "薪酬", "代发", "发薪") -> Category.fromId("salary", TransactionType.INCOME)
                body.containsAny("奖金", "绩效", "年终奖") -> Category.fromId("bonus", TransactionType.INCOME)
                body.containsAny("分红", "股息") -> Category.fromId("dividend", TransactionType.INCOME)
                body.containsAny("退款", "退还", "返还", "退货") -> Category.fromId("refund", TransactionType.INCOME)
                body.containsAny("押金退", "退押金", "退保证金") -> Category.fromId("deposit_back", TransactionType.INCOME)
                body.containsAny("报销", "报销款") -> Category.fromId("reimbursement", TransactionType.INCOME)
                body.containsAny("红包") -> Category.fromId("redpacket", TransactionType.INCOME)
                body.containsAny("收回借款", "还款", "还钱", "归还") -> Category.fromId("recover_loan", TransactionType.INCOME)
                body.containsAny("投资", "理财", "收益", "利息", "基金", "赎回") -> Category.fromId("investment", TransactionType.INCOME)
                body.containsAny("转账", "汇款", "转入") -> Category.fromId("income_transfer", TransactionType.INCOME)
                else -> Category.fromId("redpacket", TransactionType.INCOME)
            }
        } else {
            when {
                // 住宿
                body.containsAny("酒店", "宾馆", "民宿", "旅馆", "客栈", "住宿",
                    "如家", "汉庭", "全季", "亚朵") -> Category.fromId("accommodation", TransactionType.EXPENSE)
                // 慈善捐赠
                body.containsAny("慈善", "捐赠", "捐款", "公益", "红十字",
                    "希望工程", "壹基金", "天使") -> Category.fromId("charity", TransactionType.EXPENSE)
                // 派发红包
                body.containsAny("发红包", "派发红包", "发出红包") -> Category.fromId("send_redpacket", TransactionType.EXPENSE)
                // 餐饮
                body.containsAny("餐", "饭", "食", "外卖", "美团", "饿了么", "肯德基", "麦当劳",
                    "海底捞", "奶茶", "咖啡", "瑞幸", "星巴克", "烧烤", "火锅", "小吃",
                    "早餐", "午餐", "晚餐", "食堂", "饮料", "甜品", "蛋糕",
                    "米线", "豆腐", "面馆", "饺子", "包子", "拉面") -> Category.fromId("food", TransactionType.EXPENSE)
                // 交通
                body.containsAny("打车", "滴滴", "地铁", "公交", "高铁", "机票", "加油",
                    "停车", "高速", "出租", "曹操", "首汽", "T3出行", "花小猪",
                    "铁路", "12306", "航空", "ETC", "过路费", "充电桩") -> Category.fromId("transport", TransactionType.EXPENSE)
                // 购物
                body.containsAny("购物", "京东", "淘宝", "天猫", "超市", "商场", "拼多多",
                    "唯品会", "苏宁", "当当", "亚马逊", "沃尔玛", "永辉",
                    "便利店", "全家", "711", "罗森",
                    "水果", "蔬", "小卖部", "盒马", "零食", "百货") -> Category.fromId("shopping", TransactionType.EXPENSE)
                // 水电气网
                body.containsAny("水费", "电费", "燃气", "天然气", "煤气", "暖气", "宽带",
                    "网费", "物业费", "供暖", "电力", "自来水", "国网", "南方电网") -> Category.fromId("utilities", TransactionType.EXPENSE)
                // 房贷支出
                body.containsAny("房贷", "按揭", "月供", "公积金", "住房贷款", "商业贷款") -> Category.fromId("mortgage", TransactionType.EXPENSE)
                // 信用卡还款
                body.containsAny("信用卡还款", "还信用卡", "信用卡", "账单还款") -> Category.fromId("credit_card_repay", TransactionType.EXPENSE)
                // 支付宝还款
                body.containsAny("花呗", "借呗", "蚂蚁") -> Category.fromId("alipay_repay", TransactionType.EXPENSE)
                // 京东还款
                body.containsAny("京东白条", "白条") -> Category.fromId("jd_repay", TransactionType.EXPENSE)
                // 抖音还款
                body.containsAny("抖音月付", "放心借") -> Category.fromId("douyin_repay", TransactionType.EXPENSE)
                // 归还借款
                body.containsAny("归还借款", "还借款", "借出") -> Category.fromId("repay_loan", TransactionType.EXPENSE)
                // 转账
                body.containsAny("转账", "汇款", "转出") -> Category.fromId("account_transfer", TransactionType.EXPENSE)
                // 娱乐
                body.containsAny("娱乐", "电影", "游戏", "视频", "音乐", "KTV", "网吧",
                    "直播", "充值", "会员", "腾讯视频", "爱奇艺", "优酷", "哔哩哔哩") -> Category.fromId("entertainment", TransactionType.EXPENSE)
                // 医疗
                body.containsAny("医院", "药店", "医疗", "诊所", "挂号", "门诊", "体检",
                    "药房", "药品", "看病") -> Category.fromId("medical", TransactionType.EXPENSE)
                // 住房
                body.containsAny("房租", "物业", "租房") -> Category.fromId("housing", TransactionType.EXPENSE)
                // 通讯
                body.containsAny("话费", "流量", "通讯", "中国移动", "中国联通", "中国电信",
                    "充话费", "手机费") -> Category.fromId("communication", TransactionType.EXPENSE)
                // 教育
                body.containsAny("学费", "培训", "教育", "书", "课程", "网课",
                    "学校", "考试", "辅导") -> Category.fromId("education", TransactionType.EXPENSE)
                // 保险
                body.containsAny("保险", "保费", "社保", "医保", "车险", "人寿") -> Category.fromId("insurance", TransactionType.EXPENSE)
                // 旅游
                body.containsAny("旅游", "景区", "门票",
                    "携程", "去哪儿", "飞猪", "途牛") -> Category.fromId("travel", TransactionType.EXPENSE)
                // 投资
                body.containsAny("投资", "理财", "基金", "股票", "证券", "期货") -> Category.fromId("investment_expense", TransactionType.EXPENSE)
                else -> Category.fromId("other", TransactionType.EXPENSE)
            }
        }
    }

    private fun buildNote(sms: SmsTransaction): String {
        val cardStr = if (!sms.cardLastFour.isNullOrBlank()) "尾号${sms.cardLastFour}" else ""
        val prefix = listOfNotNull(
            sms.source.takeIf { it != "未知来源" },
            cardStr.takeIf { it.isNotBlank() }
        ).joinToString(" ")
        val bodyPreview = sms.body.take(80).replace("\n", " ")
        return if (prefix.isNotBlank()) "$prefix: $bodyPreview" else bodyPreview
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 通用
    // ──────────────────────────────────────────────────────────────────────────

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun resetState() {
        _uiState.update { AutoImportUiState() }
    }

    private fun String.containsAny(vararg keywords: String): Boolean =
        keywords.any { this.contains(it) }
}
