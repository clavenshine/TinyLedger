package com.tinyledger.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,          // "MONTHLY" or "YEARLY"
    val year: Int,
    val month: Int,            // 0 for yearly budgets
    val totalBudget: Double,
    val reminderEnabled: Boolean = true,
    val reminderPercentage: Int = 80,
    val modifiedCount: Int = 0,
    val lastModifiedAt: Long = 0,
    val createdAt: Long = System.currentTimeMillis()
)
