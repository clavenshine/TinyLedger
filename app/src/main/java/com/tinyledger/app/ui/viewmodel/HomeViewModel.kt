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
    val accounts: List<com.tinyledger.app.domain.model.Account> = emptyList()
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
        val todayStart = DateUtils.getTodayStart()
        val todayEnd = DateUtils.getTodayEnd()

        // 当前日期是本月第几天
        val calendar = java.util.Calendar.getInstance()
        val dayOfMonth = calendar.get(java.util.Calendar.DAY_OF_MONTH)

        viewModelScope.launch {
            combine(
                transactionRepository.getTransactionsByDateRange(startDate, endDate),
                transactionRepository.getTotalByTypeAndDateRange(TransactionType.INCOME.value, startDate, endDate),
                transactionRepository.getTotalByTypeAndDateRange(TransactionType.EXPENSE.value, startDate, endDate),
                transactionRepository.getTransactionsByDateRange(todayStart, todayEnd),
                preferencesRepository.getSettings(),
                pendingTransactionRepository.getAllPendingTransactions()
            ) { monthTransactions, income, expense, todayTx, settings, pendingTx ->
                arrayOf(monthTransactions, income, expense, todayTx, settings, pendingTx)
            }.combine(accountRepository.getAllAccounts()) { arr, accounts ->
                arrayOf(*arr, accounts)
            }.combine(accountRepository.getTotalBalance()) { arr, totalBalance ->
                val monthTransactions = arr[0] as List<Transaction>
                val income = arr[1] as Double
                val expense = arr[2] as Double
                val todayTx = arr[3] as List<Transaction>
                val settings = arr[4] as com.tinyledger.app.domain.model.AppSettings
                val pendingTx = arr[5] as List<Transaction>
                val accounts = arr[6] as List<com.tinyledger.app.domain.model.Account>

                val todayInc = todayTx.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
                val todayExp = todayTx.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
                val dailyAvg = if (dayOfMonth > 0) expense / dayOfMonth else 0.0

                HomeUiState(
                    recentTransactions = monthTransactions.take(10),
                    todayTransactions = todayTx,
                    pendingTransactions = pendingTx,
                    monthlyIncome = income,
                    monthlyExpense = expense,
                    todayIncome = todayInc,
                    todayExpense = todayExp,
                    dailyAvgExpense = dailyAvg,
                    totalNetAssets = totalBalance,
                    currencySymbol = settings.currencySymbol,
                    isLoading = false,
                    hasAccounts = accounts.isNotEmpty(),
                    accounts = accounts
                )
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
}
