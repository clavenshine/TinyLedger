package com.tinyledger.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tinyledger.app.domain.model.Category
import com.tinyledger.app.domain.model.CategoryAmount
import com.tinyledger.app.domain.model.TransactionType
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
    private val preferencesRepository: PreferencesRepository
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

                combine(
                    transactionRepository.getTotalByTypeAndDateRange(
                        TransactionType.INCOME.value,
                        startDate,
                        endDate
                    ),
                    transactionRepository.getTotalByTypeAndDateRange(
                        TransactionType.EXPENSE.value,
                        startDate,
                        endDate
                    ),
                    transactionRepository.getExpenseByCategory(startDate, endDate)
                ) { income, expense, expenseMap ->
                    val totalExpense = expense
                    val categoryAmounts = expenseMap.map { (categoryId, amount) ->
                        CategoryAmount(
                            category = Category.fromId(categoryId, TransactionType.EXPENSE),
                            amount = amount,
                            percentage = if (totalExpense > 0) (amount / totalExpense * 100).toFloat() else 0f
                        )
                    }.sortedByDescending { it.amount }

                    StatisticsUiState(
                        selectedYear = year,
                        selectedMonth = month,
                        totalIncome = income,
                        totalExpense = expense,
                        balance = income - expense,
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
