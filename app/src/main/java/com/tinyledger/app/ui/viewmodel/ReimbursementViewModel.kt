package com.tinyledger.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tinyledger.app.domain.model.Account
import com.tinyledger.app.domain.model.ReimbursementStatus
import com.tinyledger.app.domain.model.Transaction
import com.tinyledger.app.domain.model.TransactionType
import com.tinyledger.app.domain.repository.AccountRepository
import com.tinyledger.app.domain.repository.PreferencesRepository
import com.tinyledger.app.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReimbursementUiState(
    val pendingTransactions: List<Transaction> = emptyList(),
    val reimbursedTransactions: List<Transaction> = emptyList(),
    val pendingTotal: Double = 0.0,
    val reimbursedTotal: Double = 0.0,
    val currencySymbol: String = "¥",
    val isLoading: Boolean = false,
    val selectedTab: ReimbursementTab = ReimbursementTab.PENDING,
    val accounts: List<Account> = emptyList()
)

enum class ReimbursementTab {
    PENDING, REIMBURSED
}

@HiltViewModel
class ReimbursementViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val preferencesRepository: PreferencesRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(ReimbursementTab.PENDING)
    private val _uiState = MutableStateFlow(ReimbursementUiState(isLoading = true))
    val uiState: StateFlow<ReimbursementUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        val pendingFlow = combine(
            transactionRepository.getTransactionsByReimbursementStatus(ReimbursementStatus.PENDING.value),
            transactionRepository.getTotalAmountByReimbursementStatus(ReimbursementStatus.PENDING.value)
        ) { list, total -> list to total }

        val reimbursedFlow = combine(
            transactionRepository.getTransactionsByReimbursementStatus(ReimbursementStatus.REIMBURSED.value),
            transactionRepository.getTotalAmountByReimbursementStatus(ReimbursementStatus.REIMBURSED.value)
        ) { list, total -> list to total }

        val settingsTabFlow = combine(
            preferencesRepository.getSettings().map { it.currencySymbol },
            _selectedTab
        ) { symbol, tab -> symbol to tab }

        viewModelScope.launch {
            combine(
                pendingFlow,
                reimbursedFlow,
                settingsTabFlow,
                accountRepository.getAllAccounts()
            ) { pending, reimbursed, (symbol, tab), accounts ->
                ReimbursementUiState(
                    pendingTransactions = pending.first,
                    reimbursedTransactions = reimbursed.first,
                    pendingTotal = pending.second,
                    reimbursedTotal = reimbursed.second,
                    currencySymbol = symbol,
                    isLoading = false,
                    selectedTab = tab,
                    accounts = accounts
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun selectTab(tab: ReimbursementTab) {
        _selectedTab.value = tab
    }

    fun markAsReimbursed(transactionId: Long) {
        viewModelScope.launch {
            transactionRepository.updateReimbursementStatus(transactionId, ReimbursementStatus.REIMBURSED.value)
            // 自动创建报销回款收入记录
            val transaction = transactionRepository.getTransactionById(transactionId)
            transaction?.let { tx ->
                if (tx.type == TransactionType.EXPENSE) {
                    val incomeTransaction = Transaction(
                        type = TransactionType.INCOME,
                        category = com.tinyledger.app.domain.model.Category.fromId("reimbursement", TransactionType.INCOME),
                        amount = kotlin.math.abs(tx.amount),
                        note = "报销回款：${tx.note ?: tx.category.name}",
                        date = System.currentTimeMillis(),
                        accountId = tx.accountId,
                        relatedTransactionId = tx.id
                    )
                    transactionRepository.insertTransaction(incomeTransaction)
                }
            }
        }
    }

    fun markAsPending(transactionId: Long) {
        viewModelScope.launch {
            transactionRepository.updateReimbursementStatus(transactionId, ReimbursementStatus.PENDING.value)
            // 删除关联的报销回款收入记录
            val transaction = transactionRepository.getTransactionById(transactionId)
            transaction?.let { tx ->
                val relatedIncome = transactionRepository.getTransactionByRelatedId(tx.id)
                relatedIncome?.let { income ->
                    if (income.type == TransactionType.INCOME) {
                        transactionRepository.deleteTransaction(income.id)
                    }
                }
            }
        }
    }

    fun removeReimbursementStatus(transactionId: Long) {
        viewModelScope.launch {
            // 先删除关联的报销回款收入记录（如果有）
            val transaction = transactionRepository.getTransactionById(transactionId)
            transaction?.let { tx ->
                val relatedIncome = transactionRepository.getTransactionByRelatedId(tx.id)
                relatedIncome?.let { income ->
                    if (income.type == TransactionType.INCOME) {
                        transactionRepository.deleteTransaction(income.id)
                    }
                }
            }
            transactionRepository.updateReimbursementStatus(transactionId, ReimbursementStatus.NONE.value)
        }
    }

    fun updateTransactionAccount(transactionId: Long, accountId: Long?) {
        viewModelScope.launch {
            val transaction = transactionRepository.getTransactionById(transactionId)
            transaction?.let { tx ->
                val updatedTx = tx.copy(accountId = accountId)
                transactionRepository.updateTransaction(updatedTx)
            }
        }
    }
}
