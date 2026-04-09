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
        return if (csv.type == TransactionType.INCOME) {
            when {
                text.containsAny("工资", "薪资", "薪酬") -> Category.fromId("salary", TransactionType.INCOME)
                text.containsAny("奖金", "绩效") -> Category.fromId("bonus", TransactionType.INCOME)
                text.containsAny("退款", "退还", "返还") -> Category.fromId("refund", TransactionType.INCOME)
                text.containsAny("红包", "转账", "汇款") -> Category.fromId("transfer", TransactionType.INCOME)
                text.containsAny("投资", "理财", "分红", "收益") -> Category.fromId("investment", TransactionType.INCOME)
                else -> Category.fromId("other_income", TransactionType.INCOME)
            }
        } else {
            when {
                text.containsAny("餐", "饭", "食", "外卖", "美团", "饿了么") -> Category.fromId("food", TransactionType.EXPENSE)
                text.containsAny("打车", "滴滴", "地铁", "公交", "高铁", "机票", "加油") -> Category.fromId("transport", TransactionType.EXPENSE)
                text.containsAny("购物", "京东", "淘宝", "天猫", "超市", "商场") -> Category.fromId("shopping", TransactionType.EXPENSE)
                text.containsAny("娱乐", "电影", "游戏", "视频", "音乐") -> Category.fromId("entertainment", TransactionType.EXPENSE)
                text.containsAny("医院", "药店", "医疗", "诊所", "挂号") -> Category.fromId("medical", TransactionType.EXPENSE)
                text.containsAny("房租", "物业", "水电", "燃气", "宽带") -> Category.fromId("housing", TransactionType.EXPENSE)
                text.containsAny("话费", "流量", "通讯") -> Category.fromId("communication", TransactionType.EXPENSE)
                text.containsAny("学费", "培训", "教育") -> Category.fromId("education", TransactionType.EXPENSE)
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
        return parts.joinToString(" · ").take(60)
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

    private fun inferCategory(sms: SmsTransaction): Category {
        val body = sms.body
        val type = sms.type ?: TransactionType.EXPENSE
        return if (type == TransactionType.INCOME) {
            when {
                body.containsAny("工资", "薪资", "薪酬") -> Category.fromId("salary", TransactionType.INCOME)
                body.containsAny("奖金", "绩效", "年终奖") -> Category.fromId("bonus", TransactionType.INCOME)
                body.containsAny("退款", "退还", "返还") -> Category.fromId("refund", TransactionType.INCOME)
                body.containsAny("红包", "转账", "汇款") -> Category.fromId("transfer", TransactionType.INCOME)
                body.containsAny("投资", "理财", "分红", "收益") -> Category.fromId("investment", TransactionType.INCOME)
                else -> Category.fromId("other_income", TransactionType.INCOME)
            }
        } else {
            when {
                body.containsAny("餐", "饭", "食", "外卖", "美团", "饿了么") -> Category.fromId("food", TransactionType.EXPENSE)
                body.containsAny("打车", "滴滴", "地铁", "公交", "高铁", "机票", "加油") -> Category.fromId("transport", TransactionType.EXPENSE)
                body.containsAny("购物", "京东", "淘宝", "天猫", "超市", "商场") -> Category.fromId("shopping", TransactionType.EXPENSE)
                body.containsAny("娱乐", "电影", "游戏", "视频", "音乐") -> Category.fromId("entertainment", TransactionType.EXPENSE)
                body.containsAny("医院", "药店", "医疗", "诊所", "挂号") -> Category.fromId("medical", TransactionType.EXPENSE)
                body.containsAny("房租", "物业", "水电", "燃气", "宽带") -> Category.fromId("housing", TransactionType.EXPENSE)
                body.containsAny("话费", "流量", "通讯") -> Category.fromId("communication", TransactionType.EXPENSE)
                body.containsAny("学费", "培训", "教育", "书") -> Category.fromId("education", TransactionType.EXPENSE)
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
        val bodyPreview = sms.body.take(40).replace("\n", " ")
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
