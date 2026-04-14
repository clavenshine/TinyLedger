package com.tinyledger.app.data.repository

import com.tinyledger.app.data.local.dao.AccountDao
import com.tinyledger.app.data.local.dao.TransactionDao
import com.tinyledger.app.data.local.entity.AccountEntity
import com.tinyledger.app.domain.model.Account
import com.tinyledger.app.domain.model.AccountAttribute
import com.tinyledger.app.domain.model.AccountType
import com.tinyledger.app.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepositoryImpl @Inject constructor(
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao
) : AccountRepository {

    override fun getAllAccounts(): Flow<List<Account>> {
        return accountDao.getAllAccounts().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getAccountById(id: Long): Account? {
        return accountDao.getAccountById(id)?.toDomain()
    }

    override fun getAccountsByType(type: String): Flow<List<Account>> {
        return accountDao.getAccountsByType(type).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getAccountsByAttribute(attribute: AccountAttribute): Flow<List<Account>> {
        return accountDao.getAccountsByAttribute(attribute.value).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getTotalBalance(): Flow<Double> {
        return accountDao.getTotalBalance().map { it ?: 0.0 }
    }

    override fun getCashTotalBalance(): Flow<Double> {
        return accountDao.getCashTotalBalance().map { it ?: 0.0 }
    }

    override fun getCreditTotalBalance(): Flow<Double> {
        return accountDao.getCreditTotalBalance().map { it ?: 0.0 }
    }

    override suspend fun addAccount(account: Account): Long {
        return accountDao.insertAccount(account.toEntity())
    }

    override suspend fun updateAccount(account: Account) {
        // 更新时同步计算余额
        val calculatedBalance = calculateAndUpdateBalance(account.id, account.initialBalance)
        val updatedAccount = account.copy(
            currentBalance = calculatedBalance,
            updatedAt = System.currentTimeMillis()
        )
        accountDao.updateAccount(updatedAccount.toEntity())
    }

    override suspend fun updateAccountBalance(accountId: Long, balance: Double) {
        accountDao.updateBalance(accountId, balance)
    }

    override suspend fun deleteAccount(accountId: Long) {
        // 先删除该账户的所有交易记录
        transactionDao.deleteTransactionsByAccountId(accountId)
        // 然后禁用账户
        accountDao.disableAccount(accountId)
    }

    override suspend fun hasUnsettledDebt(accountId: Long): Boolean {
        val account = accountDao.getAccountById(accountId) ?: return false
        // For credit accounts, check if balance is non-zero (has debt)
        return account.attribute == "credit" && account.currentBalance != 0.0
    }

    // 计算并更新账户余额：期初余额 + 收入 - 支出
    private suspend fun calculateAndUpdateBalance(accountId: Long, initialBalance: Double): Double {
        val totalIncome = transactionDao.getTotalIncomeByAccountId(accountId).first()
        val totalExpense = transactionDao.getTotalExpenseByAccountId(accountId).first()
        return initialBalance + totalIncome - totalExpense
    }

    private fun AccountEntity.toDomain(): Account {
        return Account(
            id = id,
            name = name,
            type = AccountType.fromValue(type),
            attribute = AccountAttribute.fromValue(attribute),
            icon = icon,
            initialBalance = initialBalance,
            currentBalance = currentBalance,
            color = color,
            cardNumber = cardNumber,
            creditLimit = creditLimit,
            billDay = billDay,
            repaymentDay = repaymentDay,
            isEnabled = isEnabled,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun Account.toEntity(): AccountEntity {
        return AccountEntity(
            id = id,
            name = name,
            type = type.value,
            attribute = attribute.value,
            icon = icon,
            initialBalance = initialBalance,
            currentBalance = currentBalance,
            color = color,
            cardNumber = cardNumber,
            creditLimit = creditLimit,
            billDay = billDay,
            repaymentDay = repaymentDay,
            isEnabled = isEnabled,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}
