package com.tinyledger.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tinyledger.app.data.local.dao.BudgetDao
import com.tinyledger.app.domain.model.AccountAttribute
import com.tinyledger.app.domain.model.Category
import com.tinyledger.app.domain.model.CategoryAmount
import com.tinyledger.app.domain.model.DailyRecord
import com.tinyledger.app.domain.model.MonthlyRecord
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

enum class StatsTab { EXPENSE, INCOME, BALANCE }
enum class DateMode { MONTHLY, YEARLY }

private data class StatsParams(
    val year: Int,
    val month: Int,
    val dateMode: DateMode,
    val tab: StatsTab,
    val currencySymbol: String
)

data class StatisticsUiState(
    val selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH) + 1,
    val dateMode: DateMode = DateMode.MONTHLY,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val balance: Double = 0.0,

    // 预算相关
    val budgetAmount: Double = 0.0,
    val remainingBudget: Double = 0.0,

    // 支出分类排行（按金额降序）
    val expenseByCategory: List<CategoryAmount> = emptyList(),
    // 收入分类排行
    val incomeByCategory: List<CategoryAmount> = emptyList(),

    // 每日记录（用于趋势图 + 每日概况表）— 月度模式
    val dailyRecords: List<DailyRecord> = emptyList(),
    // 月度记录 — 年度模式
    val monthlyRecords: List<MonthlyRecord> = emptyList(),

    // 日均值
    val dailyAverageIncome: Double = 0.0,
    val dailyAverageExpense: Double = 0.0,

    val currencySymbol: String = "\u00a5",
    val isLoading: Boolean = false,
    val activeTab: StatsTab = StatsTab.EXPENSE
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val preferencesRepository: PreferencesRepository,
    private val accountRepository: AccountRepository,
    private val budgetDao: BudgetDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatisticsUiState(isLoading = true))
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    private val _selectedYearMonth = MutableStateFlow(
        Pair(
            Calendar.getInstance().get(Calendar.YEAR),
            Calendar.getInstance().get(Calendar.MONTH) + 1
        )
    )

    private val _dateMode = MutableStateFlow(DateMode.MONTHLY)
    private val _activeTab = MutableStateFlow(StatsTab.EXPENSE)

    init {
        loadData()
    }

    fun setActiveTab(tab: StatsTab) {
        _activeTab.value = tab
    }

    fun setDateMode(mode: DateMode) {
        _dateMode.value = mode
    }

    fun setYearMonth(year: Int, month: Int) {
        _selectedYearMonth.value = Pair(year, month)
    }

    fun previousMonth() {
        val (year, month) = _selectedYearMonth.value
        if (_dateMode.value == DateMode.YEARLY) {
            _selectedYearMonth.value = Pair(year - 1, month)
        } else {
            if (month == 1) _selectedYearMonth.value = Pair(year - 1, 12)
            else _selectedYearMonth.value = Pair(year, month - 1)
        }
    }

    fun nextMonth() {
        val (year, month) = _selectedYearMonth.value
        if (_dateMode.value == DateMode.YEARLY) {
            _selectedYearMonth.value = Pair(year + 1, month)
        } else {
            if (month == 12) _selectedYearMonth.value = Pair(year + 1, 1)
            else _selectedYearMonth.value = Pair(year, month + 1)
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                _selectedYearMonth,
                _dateMode,
                _activeTab,
                preferencesRepository.getSettings()
            ) { (year, month), mode, tab, currency ->
                StatsParams(year, month, mode, tab, currency.currencySymbol)
            }.flatMapLatest { params ->
                val year = params.year
                val month = params.month
                val mode = params.dateMode

                val (startDate, endDate) = if (mode == DateMode.MONTHLY) {
                    DateUtils.getMonthStartEnd(year, month)
                } else {
                    DateUtils.getYearStartEnd(year)
                }

                val cashAccountIds = accountRepository.getAllAccounts().first()
                    .filter { it.attribute == AccountAttribute.CASH }
                    .map { it.id }

                // 获取预算
                val budgetFlow = if (mode == DateMode.MONTHLY) {
                    budgetDao.getMonthlyBudget(year, month)
                } else {
                    budgetDao.getYearlyBudget(year)
                }

                combine(
                    transactionRepository.getTransactionsByAccountIdsAndDateRange(cashAccountIds, startDate, endDate),
                    transactionRepository.getExpenseByCategoryForAccounts(cashAccountIds, startDate, endDate),
                    budgetFlow
                ) { transactions, expenseMap, budget ->
                    val nonTransfer = transactions.filter {
                        it.type != TransactionType.TRANSFER && it.type != TransactionType.LENDING
                    }

                    val income = nonTransfer.filter {
                        it.amount > 0 && it.type != TransactionType.EXPENSE
                    }.sumOf { it.amount }
                    val expense = nonTransfer.filter {
                        it.amount < 0 || (it.type == TransactionType.EXPENSE && it.amount > 0)
                    }.sumOf { kotlin.math.abs(it.amount) }
                    val bal = nonTransfer.sumOf { it.amount }

                    // 预算
                    val budgetAmount = budget?.totalBudget ?: 0.0
                    val remainingBudget = budgetAmount - kotlin.math.abs(expense)

                    // 支出分类聚合
                    val expenseCategories = if (expense > 0) {
                        expenseMap.map { (categoryId, amount) ->
                            val cat = Category.fromId(categoryId, TransactionType.EXPENSE)
                            val resolved = if (cat.name == categoryId) {
                                Category.getCategoriesByType(TransactionType.EXPENSE)
                                    .find { it.id == categoryId } ?: cat
                            } else cat
                            CategoryAmount(resolved, amount, (amount / expense * 100).toFloat())
                        }.sortedByDescending { it.amount }
                    } else emptyList()

                    // 收入分类聚合
                    val incomeGrouped = nonTransfer.filter {
                        it.amount > 0 && it.type != TransactionType.EXPENSE
                    }.groupBy { it.category }
                    val incomeCategories = if (income > 0) {
                        incomeGrouped.map { (cat, txns) ->
                            val amt = txns.sumOf { it.amount }
                            CategoryAmount(cat, amt, (amt / income * 100).toFloat())
                        }.sortedByDescending { it.amount }
                    } else emptyList()

                    val cal = Calendar.getInstance()

                    // 日均值计算
                    val dayCount = if (mode == DateMode.MONTHLY) {
                        cal.set(year, month - 1, 1)
                        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                        val currentDay = if (year == Calendar.getInstance().get(Calendar.YEAR) &&
                            month == Calendar.getInstance().get(Calendar.MONTH) + 1) {
                            Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
                        } else daysInMonth
                        currentDay.coerceAtLeast(1)
                    } else {
                        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                        if (year == currentYear) {
                            Calendar.getInstance().get(Calendar.DAY_OF_YEAR).coerceAtLeast(1)
                        } else 365
                    }

                    // 月度模式：每日记录
                    val dailyRecords = if (mode == DateMode.MONTHLY) {
                        cal.set(year, month - 1, 1, 0, 0, 0)
                        cal.set(Calendar.MILLISECOND, 0)
                        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

                        val dailyMap = mutableMapOf<Int, DailyRecord>()
                        for (d in 1..daysInMonth) dailyMap[d] = DailyRecord(
                            dateStr = String.format("%02d.%02d", month, d),
                            year = year, month = month, day = d,
                            income = 0.0, expense = 0.0, balance = 0.0
                        )

                        nonTransfer.forEach { txn ->
                            cal.timeInMillis = txn.date
                            val d = cal.get(Calendar.DAY_OF_MONTH)
                            dailyMap[d]?.let { rec ->
                                val isIncome = txn.amount > 0 && txn.type != TransactionType.EXPENSE
                                val isExpense = txn.amount < 0 || (txn.type == TransactionType.EXPENSE && txn.amount > 0)
                                dailyMap[d] = rec.copy(
                                    income = if (isIncome) rec.income + txn.amount else rec.income,
                                    expense = if (isExpense) rec.expense + kotlin.math.abs(txn.amount) else rec.expense
                                )
                            }
                        }

                        var runningBalance = 0.0
                        dailyMap.values.sortedBy { it.day }.map { rec ->
                            runningBalance += rec.income - rec.expense
                            rec.copy(balance = runningBalance)
                        }
                    } else emptyList()

                    // 年度模式：每月记录
                    val monthlyRecords = if (mode == DateMode.YEARLY) {
                        (1..12).map { m ->
                            val (mStart, mEnd) = DateUtils.getMonthStartEnd(year, m)
                            val mTxns = nonTransfer.filter { it.date in mStart..mEnd }
                            val mIncome = mTxns.filter { it.amount > 0 && it.type != TransactionType.EXPENSE }.sumOf { it.amount }
                            val mExpense = mTxns.filter { it.amount < 0 || (it.type == TransactionType.EXPENSE && it.amount > 0) }
                                .sumOf { kotlin.math.abs(it.amount) }
                            MonthlyRecord(month = m, income = mIncome, expense = mExpense, balance = mIncome - mExpense)
                        }
                    } else emptyList()

                    StatisticsUiState(
                        selectedYear = year,
                        selectedMonth = month,
                        dateMode = mode,
                        totalIncome = income,
                        totalExpense = expense,
                        balance = bal,
                        budgetAmount = budgetAmount,
                        remainingBudget = remainingBudget,
                        expenseByCategory = expenseCategories,
                        incomeByCategory = incomeCategories,
                        dailyRecords = dailyRecords,
                        monthlyRecords = monthlyRecords,
                        dailyAverageIncome = if (dayCount > 0) income / dayCount else 0.0,
                        dailyAverageExpense = if (dayCount > 0) expense / dayCount else 0.0,
                        currencySymbol = params.currencySymbol,
                        isLoading = false,
                        activeTab = _activeTab.value
                    )
                }
            }.collect { state ->
                _uiState.value = state
            }
        }
    }
}
