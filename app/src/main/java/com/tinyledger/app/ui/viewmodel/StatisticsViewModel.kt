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
    val isLoading: Boolean = false
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

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                _selectedYearMonth,
                preferencesRepository.getSettings()
            ) { (year, month), settings ->
                Triple(year, month, settings.currencySymbol)
            }.flatMapLatest { (year, month, currency) ->
                val (startDate, endDate) = DateUtils.getMonthStartEnd(year, month)

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
                        isLoading = false
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

    fun previousMonth() {
        val (year, month) = _selectedYearMonth.value
        if (month == 1) {
            _selectedYearMonth.value = Pair(year - 1, 12)
        } else {
            _selectedYearMonth.value = Pair(year, month - 1)
        }
    }

    fun nextMonth() {
        val (year, month) = _selectedYearMonth.value
        if (month == 12) {
            _selectedYearMonth.value = Pair(year + 1, 1)
        } else {
            _selectedYearMonth.value = Pair(year, month + 1)
        }
    }
}
