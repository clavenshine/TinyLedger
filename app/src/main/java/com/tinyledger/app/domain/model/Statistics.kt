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
