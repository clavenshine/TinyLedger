package com.tinyledger.app.data.repository

import com.tinyledger.app.data.local.dao.AccountDao
import com.tinyledger.app.data.local.dao.TransactionDao
import com.tinyledger.app.data.local.entity.AccountEntity
import com.tinyledger.app.domain.model.Account
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

    override fun getTotalBalance(): Flow<Double> {
        return accountDao.getTotalBalance().map { it ?: 0.0 }
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
            icon = icon,
            initialBalance = initialBalance,
            currentBalance = currentBalance,
            color = color,
            cardNumber = cardNumber,
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
            icon = icon,
            initialBalance = initialBalance,
            currentBalance = currentBalance,
            color = color,
            cardNumber = cardNumber,
            isEnabled = isEnabled,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}
