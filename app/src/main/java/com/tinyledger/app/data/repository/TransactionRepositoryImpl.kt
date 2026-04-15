package com.tinyledger.app.data.repository

import com.tinyledger.app.data.local.dao.AccountDao
import com.tinyledger.app.data.local.dao.TransactionDao
import com.tinyledger.app.data.local.entity.TransactionEntity
import com.tinyledger.app.domain.model.Category
import com.tinyledger.app.domain.model.Transaction
import com.tinyledger.app.domain.model.TransactionType
import com.tinyledger.app.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao
) : TransactionRepository {

    override fun getAllTransactions(): Flow<List<Transaction>> {
        return transactionDao.getAllTransactions().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getTransactionsByDateRange(
        startDate: Long,
        endDate: Long
    ): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByDateRange(startDate, endDate).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getTransactionsByType(type: Int): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByType(type).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getTransactionsByCategory(category: String): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByCategory(category).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun searchTransactions(keyword: String): Flow<List<Transaction>> {
        return transactionDao.searchTransactions(keyword).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun searchTransactionsFull(keyword: String): Flow<List<Transaction>> {
        return transactionDao.searchTransactionsFull(keyword).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getTransactionById(id: Long): Transaction? {
        return transactionDao.getTransactionById(id)?.toDomain()
    }

    override suspend fun insertTransaction(transaction: Transaction): Long {
        val id = transactionDao.insertTransaction(transaction.toEntity())
        // 保存后立即更新账户余额
        transaction.accountId?.let { updateAccountBalanceById(it) }
        return id
    }

    override suspend fun insertDualTransaction(fromTransaction: Transaction, toTransaction: Transaction): Pair<Long, Long> {
        // 先保存第一笔交易（fromAccount，负数金额）
        val fromEntity = fromTransaction.toEntity()
        val fromId = transactionDao.insertTransaction(fromEntity)
        
        // 再保存第二笔交易（toAccount，正数金额），并关联第一笔的ID
        val toEntity = toTransaction.copy(relatedTransactionId = fromId).toEntity()
        val toId = transactionDao.insertTransaction(toEntity)
        
        // 更新第一笔交易的relatedTransactionId
        val updatedFromEntity = fromEntity.copy(id = fromId, relatedTransactionId = toId)
        transactionDao.updateTransaction(updatedFromEntity)
        
        // 更新两个账户的余额
        fromTransaction.accountId?.let { updateAccountBalanceById(it) }
        toTransaction.accountId?.let { updateAccountBalanceById(it) }
        
        return Pair(fromId, toId)
    }

    override suspend fun updateTransaction(transaction: Transaction) {
        transactionDao.updateTransaction(transaction.toEntity())
        // 更新后立即刷新账户余额
        transaction.accountId?.let { updateAccountBalanceById(it) }
    }

    override suspend fun updateDualTransaction(fromTransaction: Transaction, toTransaction: Transaction) {
        // 更新两笔交易
        transactionDao.updateTransaction(fromTransaction.toEntity())
        transactionDao.updateTransaction(toTransaction.toEntity())
        
        // 更新两个账户的余额
        fromTransaction.accountId?.let { updateAccountBalanceById(it) }
        toTransaction.accountId?.let { updateAccountBalanceById(it) }
    }

    override suspend fun deleteTransaction(id: Long) {
        // 删除前获取账户ID，以便删除后更新余额
        val transaction = transactionDao.getTransactionById(id)
        val accountId = transaction?.accountId
        transactionDao.deleteTransactionById(id)
        // 删除后立即更新账户余额
        accountId?.let { updateAccountBalanceById(it) }
    }

    // 根据账户ID计算并更新余额：期初 + 收入 - 支出
    private suspend fun updateAccountBalanceById(accountId: Long) {
        val account = accountDao.getAccountById(accountId) ?: return
        
        val calculatedBalance = if (account.attribute == "credit") {
            // 信用账户：期初余额 - 支出（支出增加负债）
            // 注意：信用账户的 currentBalance 为负数表示负债
            val totalExpense = transactionDao.getTotalExpenseByAccountId(accountId).first()
            account.initialBalance - totalExpense
        } else {
            // 现金账户：期初余额 + 收入 - 支出
            val totalIncome = transactionDao.getTotalIncomeByAccountId(accountId).first()
            val totalExpense = transactionDao.getTotalExpenseByAccountId(accountId).first()
            account.initialBalance + totalIncome - totalExpense
        }
        
        accountDao.updateBalance(accountId, calculatedBalance)
    }

    override fun getTotalByTypeAndDateRange(
        type: Int,
        startDate: Long,
        endDate: Long
    ): Flow<Double> {
        return transactionDao.getTotalByTypeAndDateRange(type, startDate, endDate)
            .map { it ?: 0.0 }
    }

    override fun getExpenseByCategory(
        startDate: Long,
        endDate: Long
    ): Flow<Map<String, Double>> {
        return transactionDao.getExpenseByCategory(startDate, endDate).map { list ->
            list.associate { it.category to it.total }
        }
    }

    override fun getTransactionsByAccountId(accountId: Long): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByAccountId(accountId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getTotalIncomeByAccountId(accountId: Long): Flow<Double> {
        return transactionDao.getTotalIncomeByAccountId(accountId)
    }

    override fun getTotalExpenseByAccountId(accountId: Long): Flow<Double> {
        return transactionDao.getTotalExpenseByAccountId(accountId)
    }

    override suspend fun updateCategoryForTransactions(oldCategoryId: String, newCategoryId: String) {
        transactionDao.updateCategoryForTransactions(oldCategoryId, newCategoryId)
    }

    private fun TransactionEntity.toDomain(): Transaction {
        return Transaction(
            id = id,
            type = TransactionType.fromInt(type),
            category = Category.fromId(category, TransactionType.fromInt(type)),
            amount = amount,
            note = note,
            date = date,
            accountId = accountId,
            relatedTransactionId = relatedTransactionId
        )
    }

    private fun Transaction.toEntity(): TransactionEntity {
        return TransactionEntity(
            id = id,
            type = type.value,
            category = category.id,
            amount = amount,
            note = note,
            date = date,
            accountId = accountId,
            relatedTransactionId = relatedTransactionId,
            updatedAt = System.currentTimeMillis()
        )
    }
}
