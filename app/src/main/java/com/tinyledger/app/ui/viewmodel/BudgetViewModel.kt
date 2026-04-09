package com.tinyledger.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tinyledger.app.data.local.dao.BudgetCategoryDao
import com.tinyledger.app.data.local.dao.BudgetDao
import com.tinyledger.app.data.local.entity.BudgetCategoryEntity
import com.tinyledger.app.data.local.entity.BudgetEntity
import com.tinyledger.app.domain.model.Category
import com.tinyledger.app.domain.model.TransactionType
import com.tinyledger.app.domain.repository.PreferencesRepository
import com.tinyledger.app.domain.repository.TransactionRepository
import com.tinyledger.app.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

enum class BudgetViewMode { MONTHLY, YEARLY }

data class CategoryBudgetItem(
    val categoryId: String,
    val categoryName: String,
    val amount: Double
)

data class BudgetUiState(
    val viewMode: BudgetViewMode = BudgetViewMode.MONTHLY,
    val selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH) + 1,
    val totalBudget: Double = 0.0,
    val spent: Double = 0.0,
    val remaining: Double = 0.0,
    val overBudget: Double = 0.0,
    val dailyAvg: Double = 0.0,
    val usagePercent: Int = 0,
    val budgetExists: Boolean = false,
    val modifiedCount: Int = 0,
    val canModify: Boolean = true,
    val modifyHint: String = "",
    val reminderEnabled: Boolean = true,
    val reminderPercentage: Int = 80,
    val currencySymbol: String = "¥",
    val isLoading: Boolean = false,
    val categoryBudgets: List<CategoryBudgetItem> = emptyList()
)

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val budgetDao: BudgetDao,
    private val budgetCategoryDao: BudgetCategoryDao,
    private val transactionRepository: TransactionRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _viewMode = MutableStateFlow(BudgetViewMode.MONTHLY)
    private val _selectedYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    private val _selectedMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH) + 1)

    private val _uiState = MutableStateFlow(BudgetUiState(isLoading = true))
    val uiState: StateFlow<BudgetUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                _viewMode,
                _selectedYear,
                _selectedMonth
            ) { mode, year, month ->
                Triple(mode, year, month)
            }.collectLatest { (mode, year, month) ->
                val budgetFlow = if (mode == BudgetViewMode.MONTHLY) {
                    budgetDao.getMonthlyBudget(year, month)
                } else {
                    budgetDao.getYearlyBudget(year)
                }

                val (startDate, endDate) = if (mode == BudgetViewMode.MONTHLY) {
                    DateUtils.getMonthStartEnd(year, month)
                } else {
                    getYearStartEnd(year)
                }

                combine(
                    budgetFlow,
                    transactionRepository.getTotalByTypeAndDateRange(TransactionType.EXPENSE.value, startDate, endDate),
                    preferencesRepository.getSettings()
                ) { budget, expense, settings ->
                    Triple(budget, expense, settings)
                }.collectLatest { (budget, expense, settings) ->
                    val categoryBudgetsFlow = if (budget != null) {
                        budgetCategoryDao.getCategoriesByBudgetId(budget.id)
                    } else {
                        flowOf(emptyList())
                    }

                    categoryBudgetsFlow.collect { categoryEntities ->
                        val totalBudget = budget?.totalBudget ?: 0.0
                        val remaining = totalBudget - expense
                        val overBudget = if (remaining < 0) -remaining else 0.0
                        val usagePercent = if (totalBudget > 0) ((expense / totalBudget) * 100).toInt().coerceAtMost(100) else 0

                        val daysInPeriod = if (mode == BudgetViewMode.MONTHLY) {
                            val cal = Calendar.getInstance()
                            cal.set(year, month - 1, 1)
                            val dayOfMonth = if (year == cal.get(Calendar.YEAR) && month == cal.get(Calendar.MONTH) + 1) {
                                cal.get(Calendar.DAY_OF_MONTH)
                            } else {
                                cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                            }
                            dayOfMonth.coerceAtLeast(1)
                        } else {
                            val cal = Calendar.getInstance()
                            val dayOfYear = if (year == cal.get(Calendar.YEAR)) {
                                cal.get(Calendar.DAY_OF_YEAR)
                            } else {
                                365
                            }
                            dayOfYear.coerceAtLeast(1)
                        }
                        val dailyAvg = expense / daysInPeriod

                        val canModify: Boolean
                        val modifyHint: String
                        if (budget == null) {
                            canModify = true
                            modifyHint = ""
                        } else if (mode == BudgetViewMode.MONTHLY) {
                            canModify = budget.modifiedCount < 1
                            modifyHint = if (!canModify) "月度预算提交后仅允许修改一次，本月修改次数已用完" else "月度预算提交后仅允许修改一次"
                        } else {
                            val currentQuarter = getCurrentQuarter()
                            val lastModQuarter = getQuarterFromTimestamp(budget.lastModifiedAt)
                            canModify = budget.modifiedCount == 0 ||
                                    (budget.lastModifiedAt > 0 && lastModQuarter != currentQuarter)
                            modifyHint = if (!canModify) "年度预算每个季度内仅允许修改一次，本季度修改次数已用完" else "年度预算每个季度内仅允许修改一次"
                        }

                        val categoryBudgets = categoryEntities.map {
                            CategoryBudgetItem(
                                categoryId = it.categoryId,
                                categoryName = it.categoryName,
                                amount = it.amount
                            )
                        }

                        _uiState.value = BudgetUiState(
                            viewMode = mode,
                            selectedYear = year,
                            selectedMonth = month,
                            totalBudget = totalBudget,
                            spent = expense,
                            remaining = remaining.coerceAtLeast(0.0),
                            overBudget = overBudget,
                            dailyAvg = dailyAvg,
                            usagePercent = usagePercent,
                            budgetExists = budget != null,
                            modifiedCount = budget?.modifiedCount ?: 0,
                            canModify = canModify,
                            modifyHint = modifyHint,
                            reminderEnabled = budget?.reminderEnabled ?: true,
                            reminderPercentage = budget?.reminderPercentage ?: 80,
                            currencySymbol = settings.currencySymbol,
                            isLoading = false,
                            categoryBudgets = categoryBudgets
                        )
                    }
                }
            }
        }
    }

    fun setViewMode(mode: BudgetViewMode) {
        _viewMode.value = mode
    }

    fun previousPeriod() {
        if (_viewMode.value == BudgetViewMode.MONTHLY) {
            val m = _selectedMonth.value
            if (m == 1) {
                _selectedYear.value -= 1
                _selectedMonth.value = 12
            } else {
                _selectedMonth.value = m - 1
            }
        } else {
            _selectedYear.value -= 1
        }
    }

    fun nextPeriod() {
        if (_viewMode.value == BudgetViewMode.MONTHLY) {
            val m = _selectedMonth.value
            if (m == 12) {
                _selectedYear.value += 1
                _selectedMonth.value = 1
            } else {
                _selectedMonth.value = m + 1
            }
        } else {
            _selectedYear.value += 1
        }
    }

    /**
     * 保存预算（包含分类预算列表）
     * @param categoryBudgets 各分类的预算金额
     */
    fun saveBudget(categoryBudgets: List<CategoryBudgetItem>) {
        viewModelScope.launch {
            val mode = _viewMode.value
            val year = _selectedYear.value
            val month = if (mode == BudgetViewMode.MONTHLY) _selectedMonth.value else 0
            val typeStr = if (mode == BudgetViewMode.MONTHLY) "MONTHLY" else "YEARLY"

            val totalBudget = categoryBudgets.sumOf { it.amount }
            if (totalBudget <= 0) return@launch

            val existingFlow = if (mode == BudgetViewMode.MONTHLY) {
                budgetDao.getMonthlyBudget(year, month)
            } else {
                budgetDao.getYearlyBudget(year)
            }
            val existing = existingFlow.first()

            val budgetId: Long
            if (existing != null) {
                budgetDao.update(existing.copy(
                    totalBudget = totalBudget,
                    modifiedCount = existing.modifiedCount + 1,
                    lastModifiedAt = System.currentTimeMillis()
                ))
                budgetId = existing.id
            } else {
                budgetId = budgetDao.insert(BudgetEntity(
                    type = typeStr,
                    year = year,
                    month = month,
                    totalBudget = totalBudget,
                    createdAt = System.currentTimeMillis()
                ))
            }

            // 清除旧分类预算，写入新的
            budgetCategoryDao.deleteByBudgetId(budgetId)
            val entities = categoryBudgets.filter { it.amount > 0 }.map {
                BudgetCategoryEntity(
                    budgetId = budgetId,
                    categoryId = it.categoryId,
                    categoryName = it.categoryName,
                    amount = it.amount
                )
            }
            budgetCategoryDao.insertAll(entities)
        }
    }

    fun updateBudgetSettings(reminderEnabled: Boolean, reminderPercentage: Int) {
        viewModelScope.launch {
            val mode = _viewMode.value
            val year = _selectedYear.value
            val month = if (mode == BudgetViewMode.MONTHLY) _selectedMonth.value else 0

            val existingFlow = if (mode == BudgetViewMode.MONTHLY) {
                budgetDao.getMonthlyBudget(year, month)
            } else {
                budgetDao.getYearlyBudget(year)
            }
            val existing = existingFlow.first()

            if (existing != null) {
                budgetDao.update(existing.copy(
                    reminderEnabled = reminderEnabled,
                    reminderPercentage = reminderPercentage
                ))
            }
        }
    }

    private fun getYearStartEnd(year: Int): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(year, 0, 1, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis

        cal.set(year, 11, 31, 23, 59, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val end = cal.timeInMillis

        return Pair(start, end)
    }

    private fun getCurrentQuarter(): Int {
        val month = Calendar.getInstance().get(Calendar.MONTH) + 1
        return (month - 1) / 3 + 1
    }

    private fun getQuarterFromTimestamp(timestamp: Long): Int {
        if (timestamp == 0L) return 0
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        val month = cal.get(Calendar.MONTH) + 1
        return (month - 1) / 3 + 1
    }
}
