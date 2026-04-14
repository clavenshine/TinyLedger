package com.tinyledger.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tinyledger.app.domain.model.Account
import com.tinyledger.app.domain.model.AccountType
import com.tinyledger.app.domain.model.Transaction
import com.tinyledger.app.domain.repository.AccountRepository
import com.tinyledger.app.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// 按月份分组的交易记录
data class MonthlyTransactions(
    val yearMonth: String,
    val monthDisplay: String,
    val transactions: List<Transaction>,
    val isExpanded: Boolean = false
)

data class AccountUiState(
    val accounts: List<Account> = emptyList(),
    val accountsWithBalance: List<Pair<Account, Double>> = emptyList(), // 账户和实时计算的余额
    val totalBalance: Double = 0.0,
    val isLoading: Boolean = false,
    val showAddDialog: Boolean = false,
    val showEditDialog: Boolean = false,
    val selectedAccount: Account? = null,
    val expandedAccountId: Long? = null, // 当前展开的账户ID
    val accountTransactions: Map<String, List<Transaction>> = emptyMap(), // accountId -> 月份分组交易
    val monthlyTransactions: List<MonthlyTransactions> = emptyList(), // 当前展开账户的按月分组
    val expandedMonths: Set<String> = emptySet() // 展开的月份集合 (格式: accountId_yearMonth)
)

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountUiState(isLoading = true))
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    init {
        loadAccounts()
    }

    private fun loadAccounts() {
        viewModelScope.launch {
            combine(
                accountRepository.getAllAccounts(),
                accountRepository.getTotalBalance()
            ) { accounts, total ->
                // 计算每个账户的实时余额：期初余额 + 收入 - 支出
                val accountsWithBalance = accounts.map { account ->
                    val calculatedBalance = calculateAccountBalance(account)
                    Pair(account, calculatedBalance)
                }
                AccountUiState(
                    accounts = accounts,
                    accountsWithBalance = accountsWithBalance,
                    totalBalance = total,
                    isLoading = false
                )
            }.collect { state ->
                _uiState.value = state.copy(
                    showAddDialog = _uiState.value.showAddDialog,
                    showEditDialog = _uiState.value.showEditDialog,
                    selectedAccount = _uiState.value.selectedAccount,
                    expandedAccountId = _uiState.value.expandedAccountId,
                    monthlyTransactions = _uiState.value.monthlyTransactions,
                    expandedMonths = _uiState.value.expandedMonths
                )
            }
        }
    }

    // 计算账户余额 = 期初余额 + 收入 - 支出
    private suspend fun calculateAccountBalance(account: Account): Double {
        var totalIncome = 0.0
        var totalExpense = 0.0
        
        transactionRepository.getTotalIncomeByAccountId(account.id).first().let { income ->
            totalIncome = income
        }
        transactionRepository.getTotalExpenseByAccountId(account.id).first().let { expense ->
            totalExpense = expense
        }
        
        return account.initialBalance + totalIncome - totalExpense
    }

    // 切换账户展开/折叠状态
    fun toggleAccountExpanded(accountId: Long) {
        val currentExpanded = _uiState.value.expandedAccountId
        if (currentExpanded == accountId) {
            // 折叠
            _uiState.update { it.copy(
                expandedAccountId = null,
                monthlyTransactions = emptyList(),
                expandedMonths = emptySet()
            )}
        } else {
            // 展开，加载交易明细
            _uiState.update { it.copy(expandedAccountId = accountId) }
            loadAccountTransactions(accountId)
        }
    }

    // 加载账户的交易明细
    private fun loadAccountTransactions(accountId: Long) {
        viewModelScope.launch {
            transactionRepository.getTransactionsByAccountId(accountId).collect { transactions ->
                // 按月份分组
                val grouped = transactions.groupBy { transaction ->
                    val calendar = java.util.Calendar.getInstance()
                    calendar.timeInMillis = transaction.date
                    String.format("%04d-%02d", calendar.get(java.util.Calendar.YEAR), calendar.get(java.util.Calendar.MONTH) + 1)
                }
                
                // 转换为MonthlyTransactions列表
                val monthlyList = grouped.map { (yearMonth, txns) ->
                    val parts = yearMonth.split("-")
                    val year = parts[0].toInt()
                    val month = parts[1].toInt()
                    val monthDisplay = "${year}年${month}月"
                    MonthlyTransactions(
                        yearMonth = yearMonth,
                        monthDisplay = monthDisplay,
                        transactions = txns.sortedByDescending { it.date }
                    )
                }.sortedByDescending { it.yearMonth }

                _uiState.update { it.copy(monthlyTransactions = monthlyList) }
            }
        }
    }

    // 切换月份展开/折叠
    fun toggleMonthExpanded(accountId: Long, yearMonth: String) {
        val key = "${accountId}_$yearMonth"
        val currentExpanded = _uiState.value.expandedMonths.toMutableSet()
        if (currentExpanded.contains(key)) {
            currentExpanded.remove(key)
        } else {
            currentExpanded.add(key)
        }
        _uiState.update { it.copy(expandedMonths = currentExpanded) }
    }

    fun showAddDialog() {
        _uiState.update { it.copy(showAddDialog = true) }
    }

    fun hideAddDialog() {
        _uiState.update { it.copy(showAddDialog = false) }
    }

    fun showEditDialog(account: Account) {
        _uiState.update { it.copy(showEditDialog = true, selectedAccount = account) }
    }

    fun hideEditDialog() {
        _uiState.update { it.copy(showEditDialog = false, selectedAccount = null) }
    }

    fun addAccount(
        name: String,
        type: AccountType,
        icon: String,
        initialBalance: Double,
        color: String,
        cardNumber: String?,
        creditLimit: Double = 0.0,
        billDay: Int = 0,
        repaymentDay: Int = 0
    ) {
        viewModelScope.launch {
            val account = Account(
                name = name,
                type = type,
                attribute = type.attribute,
                icon = icon,
                initialBalance = initialBalance,
                currentBalance = initialBalance,
                color = color,
                cardNumber = cardNumber,
                creditLimit = creditLimit,
                billDay = billDay,
                repaymentDay = repaymentDay
            )
            accountRepository.addAccount(account)
            hideAddDialog()
        }
    }

    fun updateAccount(
        account: Account,
        name: String,
        type: AccountType,
        icon: String,
        color: String,
        cardNumber: String?
    ) {
        viewModelScope.launch {
            val updated = account.copy(
                name = name,
                type = type,
                icon = icon,
                color = color,
                cardNumber = cardNumber,
                updatedAt = System.currentTimeMillis()
            )
            accountRepository.updateAccount(updated)
            hideEditDialog()
        }
    }

    fun updateAccountBalance(accountId: Long, newBalance: Double) {
        viewModelScope.launch {
            accountRepository.updateAccountBalance(accountId, newBalance)
        }
    }

    fun deleteAccount(accountId: Long) {
        viewModelScope.launch {
            accountRepository.deleteAccount(accountId)
            hideEditDialog()
        }
    }
}
