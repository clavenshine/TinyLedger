package com.tinyledger.app.data.repository

import com.tinyledger.app.data.local.dao.AccountDao
import com.tinyledger.app.data.local.dao.TransactionDao
import com.tinyledger.app.data.local.entity.TransactionEntity
import com.tinyledger.app.domain.model.Category
import com.tinyledger.app.domain.model.ReimbursementStatus
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

    override suspend fun getTransactionByRelatedId(relatedId: Long): Transaction? {
        return transactionDao.getTransactionByRelatedId(relatedId)?.toDomain()
    }

    override suspend fun insertTransaction(transaction: Transaction): Long {
        // 校验交易日期不能早于已启用账户的期初余额日期
        transaction.accountId?.let { accountId ->
            val account = accountDao.getAccountById(accountId)
            if (account != null && !account.isDisabled && account.initialBalanceDate.isNotEmpty()) {
                try {
                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                    val initialDate = sdf.parse(account.initialBalanceDate)
                    if (initialDate != null && transaction.date < initialDate.time) {
                        return -1L // 日期早于期初余额日期，拒绝插入
                    }
                } catch (_: Exception) {}
            }
        }
        val id = transactionDao.insertTransaction(transaction.toEntity())
        // 保存后立即更新账户余额
        transaction.accountId?.let { updateAccountBalanceById(it) }
        return id
    }

    override suspend fun insertDualTransaction(fromTransaction: Transaction, toTransaction: Transaction): Pair<Long, Long> {
        // 校验交易日期不能早于已启用账户的期初余额日期
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        for (tx in listOf(fromTransaction, toTransaction)) {
            tx.accountId?.let { accountId ->
                val account = accountDao.getAccountById(accountId)
                if (account != null && !account.isDisabled && account.initialBalanceDate.isNotEmpty()) {
                    try {
                        val initialDate = sdf.parse(account.initialBalanceDate)
                        if (initialDate != null && tx.date < initialDate.time) {
                            return Pair(-1L, -1L)
                        }
                    } catch (_: Exception) {}
                }
            }
        }
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
        // 查找旧记录以识别之前关联的账户
        val oldFrom = fromTransaction.id.let { transactionDao.getTransactionById(it) }
        val oldTo = toTransaction.id.let { transactionDao.getTransactionById(it) }
        
        // 更新两笔交易
        transactionDao.updateTransaction(fromTransaction.toEntity())
        transactionDao.updateTransaction(toTransaction.toEntity())
        
        // 更新新账户的余额
        fromTransaction.accountId?.let { updateAccountBalanceById(it) }
        toTransaction.accountId?.let { updateAccountBalanceById(it) }
        
        // 更新旧账户的余额（如果账户发生了变化）
        oldFrom?.accountId?.let { oldId ->
            if (oldId != fromTransaction.accountId) updateAccountBalanceById(oldId)
        }
        oldTo?.accountId?.let { oldId ->
            if (oldId != toTransaction.accountId) updateAccountBalanceById(oldId)
        }
    }

    override suspend fun deleteTransaction(id: Long) {
        // 查找要删除的交易
        val transaction = transactionDao.getTransactionById(id)
        val accountId = transaction?.accountId
        
        // 删除关联的双记录交易（如果存在）
        transaction?.relatedTransactionId?.let { relatedId ->
            val relatedTx = transactionDao.getTransactionById(relatedId)
            val relatedAccountId = relatedTx?.accountId
            transactionDao.deleteTransactionById(relatedId)
            relatedAccountId?.let { updateAccountBalanceById(it) }
        }
        
        // 也检查是否有另一笔交易指向这笔（反向关联）
        val reverseRelated = transactionDao.getTransactionByRelatedId(id)
        if (reverseRelated != null) {
            val reverseAccountId = reverseRelated.accountId
            transactionDao.deleteTransactionById(reverseRelated.id)
            reverseAccountId?.let { updateAccountBalanceById(it) }
        }
        
        // 删除主交易
        transactionDao.deleteTransactionById(id)
        accountId?.let { updateAccountBalanceById(it) }
    }

    // 根据账户ID计算并更新余额
    // 统一逻辑：期初余额 + 所有正数金额（收入） - 所有负数金额的绝对值（支出）
    // 对于信用账户，期初余额通常为0或负数（负债），还款（正数）会增加余额
    private suspend fun updateAccountBalanceById(accountId: Long) {
        val account = accountDao.getAccountById(accountId) ?: return
        
        val totalIncome = transactionDao.getTotalIncomeByAccountId(accountId).first()
        val totalExpense = transactionDao.getTotalExpenseByAccountId(accountId).first()
        val calculatedBalance = account.initialBalance + totalIncome - totalExpense
        
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

    override fun getTransactionsByAccountIdsAndDateRange(accountIds: List<Long>, startDate: Long, endDate: Long): Flow<List<Transaction>> {
        return if (accountIds.isEmpty()) {
            kotlinx.coroutines.flow.flowOf(emptyList())
        } else {
            transactionDao.getTransactionsByAccountIdsAndDateRange(accountIds, startDate, endDate).map { entities ->
                entities.map { it.toDomain() }
            }
        }
    }

    override fun getExpenseByCategoryForAccounts(accountIds: List<Long>, startDate: Long, endDate: Long): Flow<Map<String, Double>> {
        return if (accountIds.isEmpty()) {
            kotlinx.coroutines.flow.flowOf(emptyMap())
        } else {
            transactionDao.getExpenseByCategoryForAccounts(accountIds, startDate, endDate).map { list ->
                list.associate { it.category to it.total }
            }
        }
    }

    // 报销相关方法实现
    override fun getTransactionsByReimbursementStatus(status: Int): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByReimbursementStatus(status).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun updateReimbursementStatus(transactionId: Long, status: Int) {
        transactionDao.updateReimbursementStatus(transactionId, status)
    }

    override fun getTotalAmountByReimbursementStatus(status: Int): Flow<Double> {
        return transactionDao.getTotalAmountByReimbursementStatus(status).map { it ?: 0.0 }
    }

    override suspend fun getTransactionsByTopLevelCategorySync(categoryId: String): List<Transaction> {
        return transactionDao.getTransactionsByTopLevelCategory(categoryId).map { it.toDomain() }
    }

    override suspend fun autoMatchTransactionsToSubCategory(
        parentCategoryId: String,
        newSubCategoryId: String,
        subCategoryName: String,
        firstSubCategoryId: String?
    ): Int {
        // 获取该一级分类下的所有交易记录
        val transactions = transactionDao.getTransactionsByTopLevelCategory(parentCategoryId)
        if (transactions.isEmpty()) return 0

        val matchedIds = mutableListOf<Long>()
        val unmatchedIds = mutableListOf<Long>()

        // 遍历交易记录，尝试智能匹配
        transactions.forEach { tx ->
            val note = tx.note ?: ""
            // 如果note中包含新二级分类的名称，则匹配到该二级分类
            if (note.contains(subCategoryName, ignoreCase = true)) {
                matchedIds.add(tx.id)
            } else {
                unmatchedIds.add(tx.id)
            }
        }

        // 批量更新匹配的交易记录到新二级分类
        var updatedCount = 0
        if (matchedIds.isNotEmpty()) {
            transactionDao.batchUpdateCategory(matchedIds, newSubCategoryId)
            updatedCount += matchedIds.size
        }

        // 对于未匹配的交易，默认匹配到该一级分类下的第一个二级分类
        if (unmatchedIds.isNotEmpty() && firstSubCategoryId != null) {
            transactionDao.batchUpdateCategory(unmatchedIds, firstSubCategoryId)
            updatedCount += unmatchedIds.size
        }

        return updatedCount
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
            relatedTransactionId = relatedTransactionId,
            imagePath = imagePath,
            reimbursementStatus = ReimbursementStatus.fromInt(reimbursementStatus)
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
            imagePath = imagePath,
            reimbursementStatus = reimbursementStatus.value,
            updatedAt = System.currentTimeMillis()
        )
    }
}
