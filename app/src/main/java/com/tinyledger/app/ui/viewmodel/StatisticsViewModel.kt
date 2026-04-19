package com.tinyledger.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tinyledger.app.domain.model.AccountAttribute
import com.tinyledger.app.domain.model.Category
import com.tinyledger.app.domain.model.CategoryAmount
import com.tinyledger.app.domain.model.TransactionType
import com.tinyledger.app.domain.repository.AccountRepository
import com.tinyledger.app.domain.repository.PreferencesRepository
import com.tinyledger.app.domain.repository.TransactionRepository
import com.tinyledger.app.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class StatisticsUiState(
    val selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH) + 1,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val balance: Double = 0.0,
    val expenseByCategory: List<CategoryAmount> = emptyList(),
    val currencySymbol: String = "¥",
    val isLoading: Boolean = false,
    val isYearlyMode: Boolean = false  // 是否为年累计模式
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val preferencesRepository: PreferencesRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatisticsUiState(isLoading = true))
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    private val _selectedYearMonth = MutableStateFlow(
        Pair(
            Calendar.getInstance().get(Calendar.YEAR),
            Calendar.getInstance().get(Calendar.MONTH) + 1
        )
    )
    
    private val _isYearlyMode = MutableStateFlow(false)  // 控制是否为年累计模式

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                _selectedYearMonth,
                _isYearlyMode,
                preferencesRepository.getSettings()
            ) { (year, month), isYearly, settings ->
                Triple(year, month, settings.currencySymbol)
            }.flatMapLatest { (year, month, currency) ->
                val (startDate, endDate) = if (_isYearlyMode.value) {
                    // 年累计模式：从1月1日到12月31日
                    DateUtils.getYearStartEnd(year)
                } else {
                    // 月度模式：该月第一天到最后一天
                    DateUtils.getMonthStartEnd(year, month)
                }
            
                // 先获取现金账户ID列表
                val cashAccountIds = accountRepository.getAllAccounts().first()
                    .filter { it.attribute == AccountAttribute.CASH }
                    .map { it.id }
            
                combine(
                    transactionRepository.getTransactionsByAccountIdsAndDateRange(cashAccountIds, startDate, endDate),
                    transactionRepository.getExpenseByCategoryForAccounts(cashAccountIds, startDate, endDate)
                ) { monthTransactions, expenseMap ->
                    // 排除转账交易（TRANSFER和LENDING类型）
                    val nonTransferTransactions = monthTransactions.filter { 
                        it.type != TransactionType.TRANSFER && it.type != TransactionType.LENDING 
                    }
                    
                    // Calculate totals using sign-based amounts
                    // 兼容迁移前数据：type=EXPENSE但amount>0的也视为支出
                    val income = nonTransferTransactions.filter {
                        it.amount > 0 && it.type != TransactionType.EXPENSE
                    }.sumOf { it.amount }
                    val expense = nonTransferTransactions.filter {
                        it.amount < 0 || (it.type == TransactionType.EXPENSE && it.amount > 0)
                    }.sumOf { kotlin.math.abs(it.amount) }
                    val totalExpense = expense
                    val categoryAmounts = expenseMap.map { (categoryId, amount) ->
                        val resolvedCategory = Category.fromId(categoryId, TransactionType.EXPENSE)
                        // 如果分类名与ID相同，说明找不到匹配的默认分类，尝试从自定义分类中查找
                        val category = if (resolvedCategory.name == categoryId) {
                            Category.getCategoriesByType(TransactionType.EXPENSE)
                                .find { it.id == categoryId } ?: resolvedCategory
                        } else {
                            resolvedCategory
                        }
                        CategoryAmount(
                            category = category,
                            amount = amount,
                            percentage = if (totalExpense > 0) (amount / totalExpense * 100).toFloat() else 0f
                        )
                    }.sortedByDescending { it.amount }

                    StatisticsUiState(
                        selectedYear = year,
                        selectedMonth = month,
                        totalIncome = income,
                        totalExpense = expense,
                        balance = nonTransferTransactions.sumOf { it.amount }, // 结余 = 收入 + 支出（支出为负数）
                        expenseByCategory = categoryAmounts,
                        currencySymbol = currency,
                        isLoading = false,
                        isYearlyMode = _isYearlyMode.value
                    )
                }
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun setYearMonth(year: Int, month: Int) {
        _selectedYearMonth.value = Pair(year, month)
    }

    // 切换到年累计模式或月度模式
    fun toggleYearlyMode() {
        _isYearlyMode.value = !_isYearlyMode.value
    }

    // 进入年累计模式
    fun enterYearlyMode() {
        _isYearlyMode.value = true
    }

    // 返回月度模式
    fun exitYearlyMode() {
        _isYearlyMode.value = false
    }

    fun previousMonth() {
        val (year, month) = _selectedYearMonth.value
        if (_isYearlyMode.value) {
            // 年度模式：上一年
            _selectedYearMonth.value = Pair(year - 1, month)
        } else {
            // 月度模式：上一个月
            if (month == 1) {
                _selectedYearMonth.value = Pair(year - 1, 12)
            } else {
                _selectedYearMonth.value = Pair(year, month - 1)
            }
        }
    }

    fun nextMonth() {
        val (year, month) = _selectedYearMonth.value
        if (_isYearlyMode.value) {
            // 年度模式：下一年
            _selectedYearMonth.value = Pair(year + 1, month)
        } else {
            // 月度模式：下一个月
            if (month == 12) {
                _selectedYearMonth.value = Pair(year + 1, 1)
            } else {
                _selectedYearMonth.value = Pair(year, month + 1)
            }
        }
    }
}
