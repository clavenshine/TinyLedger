package com.tinyledger.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tinyledger.app.domain.model.Account
import com.tinyledger.app.domain.model.AccountAttribute
import com.tinyledger.app.domain.repository.AccountRepository
import com.tinyledger.app.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

data class CreditAccountInfo(
    val account: Account,
    val currentAvailableBalance: Double, // 当前可用余额
    val currentBillAmount: Double,      // 本期账单金额
    val calculatedBalance: Double       // 实时计算余额
)

data class CreditAccountsUiState(
    val creditAccounts: List<CreditAccountInfo> = emptyList(),
    val currencySymbol: String = "¥",
    val isLoading: Boolean = false
)

@HiltViewModel
class CreditAccountsViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreditAccountsUiState(isLoading = true))
    val uiState: StateFlow<CreditAccountsUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            accountRepository.getAccountsByAttribute(AccountAttribute.CREDIT_ACCOUNT).collect { accounts ->
                val creditAccountInfos = accounts.map { account ->
                    calculateCreditAccountInfo(account)
                }
                _uiState.value = CreditAccountsUiState(
                    creditAccounts = creditAccountInfos,
                    isLoading = false
                )
            }
        }
    }

    private suspend fun calculateCreditAccountInfo(account: Account): CreditAccountInfo {
        // 计算实时余额
        val totalIncome = transactionRepository.getTotalIncomeByAccountId(account.id).first()
        val totalExpense = transactionRepository.getTotalExpenseByAccountId(account.id).first()
        val calculatedBalance = account.initialBalance + totalIncome - totalExpense

        // 当前可用余额 = 期初余额 + 信用额度 + 收入 - 支出
        val currentAvailableBalance = account.initialBalance + account.creditLimit + totalIncome - totalExpense

        // 计算本期账单金额
        val currentBillAmount = calculateCurrentBillAmount(account, totalIncome, totalExpense)

        return CreditAccountInfo(
            account = account,
            currentAvailableBalance = currentAvailableBalance,
            currentBillAmount = currentBillAmount,
            calculatedBalance = calculatedBalance
        )
    }

    /**
     * 本期账单金额计算逻辑:
     * 账单日账户可用余额 = 账户期初余额 + 账户信用额度 + 期初余额日期之后的累计收入金额 - 期初余额日期之后的账户支出金额
     * 本期账单金额 = 上期账单日至本期账单日之间信用账户的支出明细之和 - (账单日账户可用余额 - 账户信用额度)
     */
    private suspend fun calculateCurrentBillAmount(
        account: Account,
        totalIncome: Double,
        totalExpense: Double
    ): Double {
        if (account.billDay == 0) return 0.0

        val calendar = Calendar.getInstance()
        val today = calendar.get(Calendar.DAY_OF_MONTH)

        // 确定本期账单日和上期账单日
        val currentBillDate: Long
        val lastBillDate: Long

        // 如果今天 >= 账单日，本期账单日是本月账单日；否则是上月账单日
        if (today >= account.billDay) {
            calendar.set(Calendar.DAY_OF_MONTH, account.billDay)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            currentBillDate = calendar.timeInMillis
            calendar.add(Calendar.MONTH, -1)
            calendar.set(Calendar.DAY_OF_MONTH, account.billDay)
            lastBillDate = calendar.timeInMillis
        } else {
            calendar.add(Calendar.MONTH, -1)
            calendar.set(Calendar.DAY_OF_MONTH, account.billDay)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            currentBillDate = calendar.timeInMillis
            calendar.add(Calendar.MONTH, -1)
            calendar.set(Calendar.DAY_OF_MONTH, account.billDay)
            lastBillDate = calendar.timeInMillis
        }

        // 上期账单日至本期账单日之间的支出明细之和
        val billPeriodTransactions = transactionRepository.getTransactionsByAccountId(account.id).first()
        val billPeriodExpenses = billPeriodTransactions
            .filter { it.date in lastBillDate..currentBillDate && it.amount < 0 }
            .sumOf { kotlin.math.abs(it.amount) }

        // 账单日账户可用余额 = 期初余额 + 信用额度 + 累计收入 - 累计支出
        val billDayAvailableBalance = account.initialBalance + account.creditLimit + totalIncome - totalExpense

        // 本期账单金额 = 支出之和 - (账单日可用余额 - 信用额度)
        val billAmount = billPeriodExpenses - (billDayAvailableBalance - account.creditLimit)

        return if (billAmount > 0) billAmount else 0.0
    }
}
