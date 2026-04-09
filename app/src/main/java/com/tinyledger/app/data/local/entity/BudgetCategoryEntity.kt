package com.tinyledger.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budget_categories")
data class BudgetCategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val budgetId: Long,            // 关联 budgets 表的 id
    val categoryId: String,        // 支出分类 ID (如 "food", "transport")
    val categoryName: String,      // 分类名称 (如 "餐饮", "交通")
    val amount: Double             // 该分类的预算金额
)
