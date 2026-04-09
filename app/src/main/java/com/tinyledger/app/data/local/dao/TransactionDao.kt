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

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): TransactionEntity?

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

    @Query("SELECT category, SUM(amount) as total FROM transactions WHERE type = 0 AND date BETWEEN :startDate AND :endDate GROUP BY category")
    fun getExpenseByCategory(startDate: Long, endDate: Long): Flow<List<CategoryTotal>>

    // 按账户ID查询所有交易记录
    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY date DESC")
    fun getTransactionsByAccountId(accountId: Long): Flow<List<TransactionEntity>>

    // 按账户ID和类型查询交易记录
    @Query("SELECT * FROM transactions WHERE accountId = :accountId AND type = :type ORDER BY date DESC")
    fun getTransactionsByAccountIdAndType(accountId: Long, type: Int): Flow<List<TransactionEntity>>

    // 按账户ID计算收入总额
    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE accountId = :accountId AND type = 1")
    fun getTotalIncomeByAccountId(accountId: Long): Flow<Double>

    // 按账户ID计算支出总额
    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE accountId = :accountId AND type = 0")
    fun getTotalExpenseByAccountId(accountId: Long): Flow<Double>
}

data class CategoryTotal(
    val category: String,
    val total: Double
)
