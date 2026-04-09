package com.tinyledger.app.data.local.dao

import androidx.room.*
import com.tinyledger.app.data.local.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    @Query("SELECT * FROM budgets WHERE type = :type AND year = :year AND month = :month LIMIT 1")
    fun getBudget(type: String, year: Int, month: Int): Flow<BudgetEntity?>

    @Query("SELECT * FROM budgets WHERE type = 'MONTHLY' AND year = :year AND month = :month LIMIT 1")
    fun getMonthlyBudget(year: Int, month: Int): Flow<BudgetEntity?>

    @Query("SELECT * FROM budgets WHERE type = 'YEARLY' AND year = :year LIMIT 1")
    fun getYearlyBudget(year: Int): Flow<BudgetEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(budget: BudgetEntity): Long

    @Update
    suspend fun update(budget: BudgetEntity)

    @Delete
    suspend fun delete(budget: BudgetEntity)

    @Query("SELECT * FROM budgets ORDER BY year DESC, month DESC")
    fun getAllBudgets(): Flow<List<BudgetEntity>>
}
