package com.tinyledger.app.data.local.dao

import androidx.room.*
import com.tinyledger.app.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY date DESC")
    fun getTransactionsByType(type: Int): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE category = :category ORDER BY date DESC")
    fun getTransactionsByCategory(category: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE note LIKE '%' || :keyword || '%' ORDER BY date DESC")
    fun searchTransactions(keyword: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE note LIKE '%' || :keyword || '%' OR category LIKE '%' || :keyword || '%' ORDER BY date DESC")
    fun searchTransactionsFull(keyword: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE relatedTransactionId = :relatedId")
    suspend fun getTransactionByRelatedId(relatedId: Long): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    @Query("DELETE FROM transactions WHERE accountId = :accountId")
    suspend fun deleteTransactionsByAccountId(accountId: Long)

    @Query("SELECT SUM(amount) FROM transactions WHERE type = :type AND date BETWEEN :startDate AND :endDate")
    fun getTotalByTypeAndDateRange(type: Int, startDate: Long, endDate: Long): Flow<Double?>

    @Query("SELECT category, SUM(ABS(amount)) as total FROM transactions WHERE (amount < 0 OR (type = 0 AND amount > 0)) AND date BETWEEN :startDate AND :endDate GROUP BY category")
    fun getExpenseByCategory(startDate: Long, endDate: Long): Flow<List<CategoryTotal>>

    // 按账户ID查询所有交易记录
    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY date DESC")
    fun getTransactionsByAccountId(accountId: Long): Flow<List<TransactionEntity>>

    // 按账户ID和类型查询交易记录
    @Query("SELECT * FROM transactions WHERE accountId = :accountId AND type = :type ORDER BY date DESC")
    fun getTransactionsByAccountIdAndType(accountId: Long, type: Int): Flow<List<TransactionEntity>>

    // 按账户ID计算收入总额（正数金额且非支出类型的交易，兼容迁移前数据）
    @Query("SELECT COALESCE(SUM(CASE WHEN amount > 0 AND type != 0 THEN amount ELSE 0 END), 0.0) FROM transactions WHERE accountId = :accountId")
    fun getTotalIncomeByAccountId(accountId: Long): Flow<Double>

    // 按账户ID计算支出总额（负数金额或支出类型正数金额的绝对值，兼容迁移前数据）
    @Query("SELECT COALESCE(SUM(CASE WHEN amount < 0 OR (type = 0 AND amount > 0) THEN ABS(amount) ELSE 0 END), 0.0) FROM transactions WHERE accountId = :accountId")
    fun getTotalExpenseByAccountId(accountId: Long): Flow<Double>

    // 迁移分类：将旧分类ID更新为新分类ID
    @Query("UPDATE transactions SET category = :newCategoryId, updatedAt = :timestamp WHERE category = :oldCategoryId")
    suspend fun updateCategoryForTransactions(oldCategoryId: String, newCategoryId: String, timestamp: Long = System.currentTimeMillis())

    // 按账户ID列表和日期范围查询交易
    @Query("SELECT * FROM transactions WHERE accountId IN (:accountIds) AND date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getTransactionsByAccountIdsAndDateRange(accountIds: List<Long>, startDate: Long, endDate: Long): Flow<List<TransactionEntity>>

    // 按账户ID列表和日期范围查询支出分类（排除TRANSFER和LENDING类型）
    @Query("SELECT category, SUM(ABS(amount)) as total FROM transactions WHERE (amount < 0 OR (type = 0 AND amount > 0)) AND type != 2 AND type != 3 AND date BETWEEN :startDate AND :endDate AND accountId IN (:accountIds) GROUP BY category")
    fun getExpenseByCategoryForAccounts(accountIds: List<Long>, startDate: Long, endDate: Long): Flow<List<CategoryTotal>>

    // 查询某个一级分类下的所有交易记录（包括其所有二级分类）
    @Query("SELECT * FROM transactions WHERE category = :categoryId ORDER BY date DESC")
    suspend fun getTransactionsByTopLevelCategory(categoryId: String): List<TransactionEntity>

    // 通过关键字搜索交易记录的note字段
    @Query("SELECT * FROM transactions WHERE category = :categoryId AND note LIKE '%' || :keyword || '%' ORDER BY date DESC")
    suspend fun searchTransactionsByCategoryAndKeyword(categoryId: String, keyword: String): List<TransactionEntity>

    // 批量更新交易记录的分类
    @Query("UPDATE transactions SET category = :newCategoryId, updatedAt = :timestamp WHERE id IN (:transactionIds)")
    suspend fun batchUpdateCategory(transactionIds: List<Long>, newCategoryId: String, timestamp: Long = System.currentTimeMillis())
}

data class CategoryTotal(
    val category: String,
    val total: Double
)
