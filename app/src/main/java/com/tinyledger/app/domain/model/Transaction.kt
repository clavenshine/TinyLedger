package com.tinyledger.app.domain.model

data class Transaction(
    val id: Long = 0,
    val type: TransactionType,
    val category: Category,
    val amount: Double,
    val note: String?,
    val date: Long,
    val accountId: Long? = null  // 关联账户ID
)

enum class TransactionType(val value: Int) {
    EXPENSE(0),
    INCOME(1);

    companion object {
        fun fromInt(value: Int): TransactionType {
            return entries.find { it.value == value } ?: EXPENSE
        }
    }
}

data class Category(
    val id: String,
    val name: String,
    val icon: String,
    val type: TransactionType,
    val isDefault: Boolean = true
) {
    companion object {
        // 可添加到分类的图标映射
        private val iconMap = mapOf(
            "restaurant" to "ic_restaurant",
            "directions_bus" to "ic_directions_bus",
            "shopping_bag" to "ic_shopping_bag",
            "local_movies" to "ic_local_movies",
            "home" to "ic_home",
            "medical" to "ic_medical",
            "education" to "ic_education",
            "communication" to "ic_communication",
            "insurance" to "ic_insurance",
            "travel" to "ic_travel",
            "lend" to "ic_lend",
            "investment_expense" to "ic_investment_expense",
            "other" to "ic_other",
            "salary" to "ic_salary",
            "bonus" to "ic_bonus",
            "financial" to "ic_financial",
            "redpacket" to "ic_redpacket",
            "收回借款" to "ic_recover_loan"
        )

        val defaultExpenseCategories = listOf(
            Category("food", "餐饮", "restaurant", TransactionType.EXPENSE),
            Category("transport", "交通", "directions_bus", TransactionType.EXPENSE),
            Category("shopping", "购物", "shopping_bag", TransactionType.EXPENSE),
            Category("entertainment", "娱乐", "local_movies", TransactionType.EXPENSE),
            Category("housing", "购房", "home", TransactionType.EXPENSE),
            Category("medical", "医疗", "medical", TransactionType.EXPENSE),
            Category("education", "教育", "education", TransactionType.EXPENSE),
            Category("communication", "通讯", "communication", TransactionType.EXPENSE),
            Category("insurance", "保险", "insurance", TransactionType.EXPENSE),
            Category("travel", "旅游", "travel", TransactionType.EXPENSE),
            Category("lend", "借出资金", "lend", TransactionType.EXPENSE),
            Category("investment_expense", "支出投资", "investment_expense", TransactionType.EXPENSE),
            Category("other", "其他", "other", TransactionType.EXPENSE)
        )

        val defaultIncomeCategories = listOf(
            Category("salary", "工资", "salary", TransactionType.INCOME),
            Category("bonus", "奖金", "bonus", TransactionType.INCOME),
            Category("investment", "投资", "investment", TransactionType.INCOME),
            Category("financial", "理财", "financial", TransactionType.INCOME),
            Category("redpacket", "红包", "redpacket", TransactionType.INCOME),
            Category("recover_loan", "收回借款", "收回借款", TransactionType.INCOME)
        )

        // 存储自定义分类
        private val customExpenseCategories = mutableListOf<Category>()
        private val customIncomeCategories = mutableListOf<Category>()

        fun getCategoriesByType(type: TransactionType): List<Category> {
            return when (type) {
                TransactionType.EXPENSE -> defaultExpenseCategories + customExpenseCategories
                TransactionType.INCOME -> defaultIncomeCategories + customIncomeCategories
            }
        }

        fun addCustomCategory(name: String, type: TransactionType): Category {
            val id = "custom_${System.currentTimeMillis()}"
            val icon = if (type == TransactionType.EXPENSE) "other" else "redpacket"
            val category = Category(id, name, icon, type, isDefault = false)
            
            when (type) {
                TransactionType.EXPENSE -> customExpenseCategories.add(category)
                TransactionType.INCOME -> customIncomeCategories.add(category)
            }
            
            return category
        }

        fun fromId(id: String, type: TransactionType): Category {
            return getCategoriesByType(type).find { it.id == id }
                ?: Category(id, id, "other", type)
        }
    }
}
