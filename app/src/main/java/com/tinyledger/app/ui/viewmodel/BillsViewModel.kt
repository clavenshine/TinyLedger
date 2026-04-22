package com.tinyledger.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tinyledger.app.domain.model.AccountAttribute
import com.tinyledger.app.domain.model.Category
import com.tinyledger.app.domain.model.Transaction
import com.tinyledger.app.domain.model.TransactionType
import com.tinyledger.app.domain.repository.AccountRepository
import com.tinyledger.app.domain.repository.PreferencesRepository
import com.tinyledger.app.domain.repository.TransactionRepository
import com.tinyledger.app.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

enum class FilterType {
    ALL, INCOME, EXPENSE
}

enum class BillsViewMode {
    LIST, CALENDAR, ALBUM
}

data class BillsUiState(
    val transactions: List<Transaction> = emptyList(),
    val filteredTransactions: List<Transaction> = emptyList(),
    val filterType: FilterType = FilterType.ALL,
    val searchKeyword: String = "",
    val currencySymbol: String = "¥",
    val isLoading: Boolean = false,
    // Calendar/month support
    val viewMode: BillsViewMode = BillsViewMode.LIST,
    val selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH) + 1,
    val selectedDay: Int? = null,
    val monthlyIncome: Double = 0.0,
    val monthlyExpense: Double = 0.0,
    val monthlyBalance: Double = 0.0,
    // Day -> list of transactions for calendar dots
    val dailyTransactionMap: Map<Int, List<Transaction>> = emptyMap(),
    // Transactions for selected day
    val selectedDayTransactions: List<Transaction> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BillsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val preferencesRepository: PreferencesRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _filterType = MutableStateFlow(FilterType.ALL)
    private val _searchKeyword = MutableStateFlow("")
    private val _viewMode = MutableStateFlow(BillsViewMode.LIST)
    private val _selectedYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    private val _selectedMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH) + 1)
    private val _selectedDay = MutableStateFlow<Int?>(null)

    private val _uiState = MutableStateFlow(BillsUiState(isLoading = true))
    val uiState: StateFlow<BillsUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                _selectedYear,
                _selectedMonth,
                _selectedDay,
                _viewMode,
                _filterType
            ) { year, month, day, viewMode, filterType ->
                DataParams(year, month, day, viewMode, filterType)
            }.flatMapLatest { params ->
                val (startDate, endDate) = DateUtils.getMonthStartEnd(params.year, params.month)
                combine(
                    transactionRepository.getTransactionsByDateRange(startDate, endDate),
                    _searchKeyword,
                    preferencesRepository.getSettings(),
                    accountRepository.getAllAccounts()
                ) { monthTransactions, keyword, settings, accounts ->
                    // 获取现金账户ID集合
                    val cashAccountIds = accounts
                        .filter { it.attribute == AccountAttribute.CASH }
                        .map { it.id }.toSet()

                    // 仅显示现金账户的交易
                    val cashMonthTransactions = monthTransactions.filter { it.accountId in cashAccountIds }
                    // Build daily map (仅现金账户)
                    val dailyMap = mutableMapOf<Int, MutableList<Transaction>>()
                    val cal = Calendar.getInstance()
                    cashMonthTransactions.forEach { tx ->
                        cal.timeInMillis = tx.date
                        val day = cal.get(Calendar.DAY_OF_MONTH)
                        dailyMap.getOrPut(day) { mutableListOf() }.add(tx)
                    }

                    // Selected day transactions
                    val dayTx = if (params.day != null) {
                        dailyMap[params.day] ?: emptyList()
                    } else {
                        emptyList()
                    }

                    // Filtered transactions (for list view) - 仅现金账户
                    val filtered = cashMonthTransactions
                        .filter { transaction ->
                            when (params.filterType) {
                                FilterType.ALL -> true
                                FilterType.INCOME -> transaction.type == TransactionType.INCOME ||
                                    ((transaction.type == TransactionType.TRANSFER || transaction.type == TransactionType.LENDING) && transaction.amount > 0)
                                FilterType.EXPENSE -> transaction.type == TransactionType.EXPENSE ||
                                    ((transaction.type == TransactionType.TRANSFER || transaction.type == TransactionType.LENDING) && transaction.amount < 0)
                            }
                        }
                        .filter { transaction ->
                            if (keyword.isBlank()) true
                            else {
                                val lowerKeyword = keyword.lowercase()
                                // 搜索分类名称（一级-二级）
                                val parentCatName = transaction.category.parentId?.let { pid ->
                                    Category.getCategoriesByType(transaction.type)
                                        .find { it.id == pid }?.name ?: ""
                                } ?: ""
                                val categoryName = if (parentCatName.isNotEmpty())
                                    "$parentCatName-${transaction.category.name}" else transaction.category.name
                                // 搜索账户名称
                                val accountName = accounts.find { it.id == transaction.accountId }?.name ?: ""
                                // 搜索金额
                                val amountStr = kotlin.math.abs(transaction.amount).toString()
                                // 匹配任意字段
                                categoryName.lowercase().contains(lowerKeyword) ||
                                    transaction.note?.lowercase()?.contains(lowerKeyword) == true ||
                                    accountName.lowercase().contains(lowerKeyword) ||
                                    amountStr.contains(lowerKeyword)
                            }
                        }

                    // Calculate monthly totals using sign-based amounts (仅现金账户)
                    // 兼容迁移前数据：type=EXPENSE但amount>0的也视为支出
                    val calculatedIncome = cashMonthTransactions
                        .filter { it.amount > 0 && it.type != TransactionType.EXPENSE }
                        .sumOf { it.amount }
                    val calculatedExpense = cashMonthTransactions
                        .filter { it.amount < 0 || (it.type == TransactionType.EXPENSE && it.amount > 0) }
                        .sumOf { kotlin.math.abs(it.amount) }

                    BillsUiState(
                        transactions = cashMonthTransactions,
                        filteredTransactions = filtered,
                        filterType = params.filterType,
                        searchKeyword = keyword,
                        currencySymbol = settings.currencySymbol,
                        isLoading = false,
                        viewMode = params.viewMode,
                        selectedYear = params.year,
                        selectedMonth = params.month,
                        selectedDay = params.day,
                        monthlyIncome = calculatedIncome,
                        monthlyExpense = calculatedExpense,
                        monthlyBalance = cashMonthTransactions.sumOf { it.amount }, // 结余 = 收入 + 支出（支出为负数）
                        dailyTransactionMap = dailyMap,
                        selectedDayTransactions = dayTx
                    )
                }
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun setFilterType(filterType: FilterType) {
        _filterType.value = filterType
    }

    fun setSearchKeyword(keyword: String) {
        _searchKeyword.value = keyword
    }

    fun setViewMode(mode: BillsViewMode) {
        _viewMode.value = mode
        if (mode == BillsViewMode.LIST) {
            _selectedDay.value = null
        }
    }

    fun selectDay(day: Int?) {
        _selectedDay.value = day
    }

    fun changeMonth(year: Int, month: Int) {
        _selectedYear.value = year
        _selectedMonth.value = month
        _selectedDay.value = null
    }

    fun previousMonth() {
        val currentMonth = _selectedMonth.value
        val currentYear = _selectedYear.value
        if (currentMonth == 1) {
            _selectedYear.value = currentYear - 1
            _selectedMonth.value = 12
        } else {
            _selectedMonth.value = currentMonth - 1
        }
        _selectedDay.value = null
    }

    fun nextMonth() {
        val currentMonth = _selectedMonth.value
        val currentYear = _selectedYear.value
        if (currentMonth == 12) {
            _selectedYear.value = currentYear + 1
            _selectedMonth.value = 1
        } else {
            _selectedMonth.value = currentMonth + 1
        }
        _selectedDay.value = null
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            transactionRepository.deleteTransaction(id)
        }
    }

    private data class DataParams(
        val year: Int,
        val month: Int,
        val day: Int?,
        val viewMode: BillsViewMode,
        val filterType: FilterType
    )
}
