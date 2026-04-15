package com.tinyledger.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tinyledger.app.domain.model.Transaction
import com.tinyledger.app.domain.model.TransactionType
import com.tinyledger.app.domain.repository.AccountRepository
import com.tinyledger.app.domain.repository.PreferencesRepository
import com.tinyledger.app.domain.repository.TransactionRepository
import com.tinyledger.app.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val recentTransactions: List<Transaction> = emptyList(),
    val todayTransactions: List<Transaction> = emptyList(),
    val pendingTransactions: List<Transaction> = emptyList(),
    val monthlyIncome: Double = 0.0,
    val monthlyExpense: Double = 0.0,
    val todayIncome: Double = 0.0,
    val todayExpense: Double = 0.0,
    val dailyAvgExpense: Double = 0.0,
    val totalNetAssets: Double = 0.0,
    val currencySymbol: String = "¥",
    val isLoading: Boolean = false,
    val hasAccounts: Boolean = true,
    val accounts: List<com.tinyledger.app.domain.model.Account> = emptyList(),
    val accountsWithBalance: List<Pair<com.tinyledger.app.domain.model.Account, Double>> = emptyList()
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val preferencesRepository: PreferencesRepository,
    private val accountRepository: AccountRepository,
    private val pendingTransactionRepository: com.tinyledger.app.domain.repository.PendingTransactionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        val (year, month) = DateUtils.getCurrentYearMonth()
        val (startDate, endDate) = DateUtils.getMonthStartEnd(year, month)

        // 当前日期是本月第几天
        val calendar = java.util.Calendar.getInstance()
        val dayOfMonth = calendar.get(java.util.Calendar.DAY_OF_MONTH)

        viewModelScope.launch {
            combine(
                transactionRepository.getTransactionsByDateRange(startDate, endDate),
                transactionRepository.getAllTransactions(),  // 获取所有交易，然后按日期过滤
                preferencesRepository.getSettings()
            ) { monthTransactions, allTransactions, settings ->
                // 动态计算今日日期范围（每次发射时重新计算）
                val todayStart = DateUtils.getTodayStart()
                val todayEnd = DateUtils.getTodayEnd()

                // 严格按交易日期过滤今日账单
                val todayTx = allTransactions.filter { tx ->
                    tx.date in todayStart..todayEnd
                }.sortedByDescending { it.date }

                // Calculate monthly totals using sign-based amounts
                val monthlyInc = monthTransactions.filter { it.amount > 0 }.sumOf { it.amount }
                val monthlyExp = monthTransactions.filter { it.amount < 0 }.sumOf { kotlin.math.abs(it.amount) }

                // Calculate today totals using sign-based amounts
                val todayInc = todayTx.filter { it.amount > 0 }.sumOf { it.amount }
                val todayExp = todayTx.filter { it.amount < 0 }.sumOf { kotlin.math.abs(it.amount) }

                val dailyAvg = if (dayOfMonth > 0) monthlyExp / dayOfMonth else 0.0

                HomeUiState(
                    recentTransactions = monthTransactions.take(10),
                    todayTransactions = todayTx,
                    monthlyIncome = monthlyInc,
                    monthlyExpense = monthlyExp,
                    todayIncome = todayInc,
                    todayExpense = todayExp,
                    dailyAvgExpense = dailyAvg,
                    currencySymbol = settings.currencySymbol,
                    isLoading = false
                )
            }.combine(pendingTransactionRepository.getAllPendingTransactions()) { state, pendingTx ->
                state.copy(pendingTransactions = pendingTx)
            }.combine(accountRepository.getAllAccounts()) { state, accounts ->
                // Calculate balance for each account
                val accountsWithBalance = accounts.map { account ->
                    val calculatedBalance = calculateAccountBalance(account)
                    Pair(account, calculatedBalance)
                }
                state.copy(
                    hasAccounts = accounts.isNotEmpty(),
                    accounts = accounts,
                    accountsWithBalance = accountsWithBalance
                )
            }.combine(accountRepository.getTotalBalance()) { state, totalBalance ->
                state.copy(totalNetAssets = totalBalance)
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            transactionRepository.deleteTransaction(id)
        }
    }

    fun insertTransaction(transaction: Transaction) {
        viewModelScope.launch {
            transactionRepository.insertTransaction(transaction)
        }
    }

    fun deletePendingTransaction(id: Long) {
        viewModelScope.launch {
            pendingTransactionRepository.deletePendingTransaction(id)
        }
    }

    fun confirmPendingTransaction(pendingId: Long, transaction: Transaction) {
        viewModelScope.launch {
            pendingTransactionRepository.confirmPendingTransaction(pendingId, transaction)
        }
    }
    
    // 计算账户余额 = 期初余额 + 收入 - 支出
    // 统一逻辑：期初余额 + 正数金额（收入） - 负数金额绝对值（支出）
    private suspend fun calculateAccountBalance(account: com.tinyledger.app.domain.model.Account): Double {
        val totalIncome = transactionRepository.getTotalIncomeByAccountId(account.id).first()
        val totalExpense = transactionRepository.getTotalExpenseByAccountId(account.id).first()
        return account.initialBalance + totalIncome - totalExpense
    }
}
