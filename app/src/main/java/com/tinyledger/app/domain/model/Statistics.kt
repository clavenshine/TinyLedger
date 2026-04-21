package com.tinyledger.app.domain.model

data class MonthlyStatistics(
    val year: Int,
    val month: Int,
    val totalIncome: Double,
    val totalExpense: Double,
    val balance: Double,
    val expenseByCategory: List<CategoryAmount>
)

data class CategoryAmount(
    val category: Category,
    val amount: Double,
    val percentage: Float
)

data class DailyRecord(
    val dateStr: String,        // "04.01"
    val year: Int,
    val month: Int,
    val day: Int,
    val income: Double,
    val expense: Double,
    val balance: Double
)

/** 月度趋势记录（用于年度视图） */
data class MonthlyRecord(
    val month: Int,
    val income: Double,
    val expense: Double,
    val balance: Double
)
