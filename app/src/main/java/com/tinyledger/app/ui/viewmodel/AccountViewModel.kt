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
    val accountTransactions: Map<Long, List<Transaction>> = emptyMap() // accountId -> transactions
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
                accountRepository.getAllAccountsIncludingDisabled(),
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
                    accountTransactions = _uiState.value.accountTransactions
                )
            }
        }
    }

    // 计算账户余额 = 期初余额 + 收入 - 支出
    private suspend fun calculateAccountBalance(account: Account): Double {
        return when (account.attribute) {
            com.tinyledger.app.domain.model.AccountAttribute.CREDIT -> {
                // 外部往来账户：期初余额 - 支出（支出增加负债）
                val totalExpense = transactionRepository.getTotalExpenseByAccountId(account.id).first()
                account.initialBalance - totalExpense
            }
            com.tinyledger.app.domain.model.AccountAttribute.CREDIT_ACCOUNT -> {
                // 信用账户：期初余额 + 收入 - 支出
                val totalIncome = transactionRepository.getTotalIncomeByAccountId(account.id).first()
                val totalExpense = transactionRepository.getTotalExpenseByAccountId(account.id).first()
                account.initialBalance + totalIncome - totalExpense
            }
            else -> {
                // 现金账户：期初余额 + 收入 - 支出
                val totalIncome = transactionRepository.getTotalIncomeByAccountId(account.id).first()
                val totalExpense = transactionRepository.getTotalExpenseByAccountId(account.id).first()
                account.initialBalance + totalIncome - totalExpense
            }
        }
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
        repaymentDay: Int = 0,
        isEnabled: Boolean = true,
        initialBalanceDate: String = "",
        purpose: String = ""
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
                repaymentDay = repaymentDay,
                isDisabled = !isEnabled,
                initialBalanceDate = initialBalanceDate,
                purpose = purpose
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
        cardNumber: String?,
        creditLimit: Double = 0.0,
        billDay: Int = 0,
        repaymentDay: Int = 0,
        initialBalance: Double = 0.0,
        initialBalanceDate: String = "",
        purpose: String = ""
    ) {
        viewModelScope.launch {
            val updated = account.copy(
                name = name,
                type = type,
                icon = icon,
                color = color,
                cardNumber = cardNumber,
                creditLimit = creditLimit,
                billDay = billDay,
                repaymentDay = repaymentDay,
                initialBalance = initialBalance,
                initialBalanceDate = initialBalanceDate,
                purpose = purpose,
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

    // 切换账户停用状态
    fun toggleAccountDisabled(accountId: Long, disabled: Boolean) {
        viewModelScope.launch {
            accountRepository.toggleAccountDisabled(accountId, disabled)
        }
    }

    // 加载账户的交易记录
    fun loadAccountTransactions(accountId: Long) {
        viewModelScope.launch {
            transactionRepository.getTransactionsByAccountId(accountId).collect { transactions ->
                val currentMap = _uiState.value.accountTransactions.toMutableMap()
                currentMap[accountId] = transactions.sortedByDescending { it.date }
                _uiState.update { it.copy(accountTransactions = currentMap) }
            }
        }
    }
}
