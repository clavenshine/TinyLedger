package com.tinyledger.app.domain.repository

import com.tinyledger.app.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun getAllTransactions(): Flow<List<Transaction>>
    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>>
    fun getTransactionsByType(type: Int): Flow<List<Transaction>>
    fun getTransactionsByCategory(category: String): Flow<List<Transaction>>
    fun searchTransactions(keyword: String): Flow<List<Transaction>>
    fun searchTransactionsFull(keyword: String): Flow<List<Transaction>>
    suspend fun getTransactionById(id: Long): Transaction?
    suspend fun getTransactionByRelatedId(relatedId: Long): Transaction?
    suspend fun insertTransaction(transaction: Transaction): Long
    suspend fun insertDualTransaction(fromTransaction: Transaction, toTransaction: Transaction): Pair<Long, Long>
    suspend fun updateTransaction(transaction: Transaction)
    suspend fun updateDualTransaction(fromTransaction: Transaction, toTransaction: Transaction)
    suspend fun deleteTransaction(id: Long)
    fun getTotalByTypeAndDateRange(type: Int, startDate: Long, endDate: Long): Flow<Double>
    fun getExpenseByCategory(startDate: Long, endDate: Long): Flow<Map<String, Double>>
    fun getTransactionsByAccountId(accountId: Long): Flow<List<Transaction>>
    fun getTotalIncomeByAccountId(accountId: Long): Flow<Double>
    fun getTotalExpenseByAccountId(accountId: Long): Flow<Double>
    suspend fun updateCategoryForTransactions(oldCategoryId: String, newCategoryId: String)
    fun getTransactionsByAccountIdsAndDateRange(accountIds: List<Long>, startDate: Long, endDate: Long): Flow<List<Transaction>>
    fun getExpenseByCategoryForAccounts(accountIds: List<Long>, startDate: Long, endDate: Long): Flow<Map<String, Double>>
    
    // 智能匹配相关方法
    suspend fun getTransactionsByTopLevelCategorySync(categoryId: String): List<Transaction>
    suspend fun autoMatchTransactionsToSubCategory(
        parentCategoryId: String,
        newSubCategoryId: String,
        subCategoryName: String,
        firstSubCategoryId: String?
    ): Int

    // 报销相关方法
    fun getTransactionsByReimbursementStatus(status: Int): Flow<List<Transaction>>
    suspend fun updateReimbursementStatus(transactionId: Long, status: Int)
    fun getTotalAmountByReimbursementStatus(status: Int): Flow<Double>
}
