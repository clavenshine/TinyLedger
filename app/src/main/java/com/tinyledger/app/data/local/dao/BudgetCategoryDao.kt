package com.tinyledger.app.data.local.dao

import androidx.room.*
import com.tinyledger.app.data.local.entity.BudgetCategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetCategoryDao {

    @Query("SELECT * FROM budget_categories WHERE budgetId = :budgetId")
    fun getCategoriesByBudgetId(budgetId: Long): Flow<List<BudgetCategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: BudgetCategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<BudgetCategoryEntity>)

    @Update
    suspend fun update(category: BudgetCategoryEntity)

    @Query("DELETE FROM budget_categories WHERE budgetId = :budgetId")
    suspend fun deleteByBudgetId(budgetId: Long)

    @Delete
    suspend fun delete(category: BudgetCategoryEntity)
}
