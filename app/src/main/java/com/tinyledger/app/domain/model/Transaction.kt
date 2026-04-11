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
            Category("utilities", "水电气网", "utilities", TransactionType.EXPENSE),
            Category("insurance", "保险", "insurance", TransactionType.EXPENSE),
            Category("travel", "旅游", "travel", TransactionType.EXPENSE),
            Category("credit_card_repay", "归还信用卡", "credit_card_repay", TransactionType.EXPENSE),
            Category("mortgage", "房贷支出", "mortgage", TransactionType.EXPENSE),
            Category("repay_loan", "归还借款", "repay_loan", TransactionType.EXPENSE),
            Category("alipay_repay", "支付宝还款", "alipay_repay", TransactionType.EXPENSE),
            Category("douyin_repay", "抖音还款", "douyin_repay", TransactionType.EXPENSE),
            Category("jd_repay", "京东还款", "jd_repay", TransactionType.EXPENSE),
            Category("account_transfer", "账户转账", "account_transfer", TransactionType.EXPENSE),
            Category("lend", "借出资金", "lend", TransactionType.EXPENSE),
            Category("investment_expense", "支出投资", "investment_expense", TransactionType.EXPENSE),
            Category("accommodation", "住宿", "accommodation", TransactionType.EXPENSE),
            Category("charity", "慈善捐赠", "charity", TransactionType.EXPENSE),
            Category("send_redpacket", "派发红包", "send_redpacket", TransactionType.EXPENSE),
            Category("family_living", "家庭生活费", "family_living", TransactionType.EXPENSE),
            Category("other", "其他", "other", TransactionType.EXPENSE)
        )

        val defaultIncomeCategories = listOf(
            Category("salary", "工资", "salary", TransactionType.INCOME),
            Category("bonus", "奖金", "bonus", TransactionType.INCOME),
            Category("investment", "投资", "investment", TransactionType.INCOME),
            Category("financial", "理财", "financial", TransactionType.INCOME),
            Category("dividend", "分红", "dividend", TransactionType.INCOME),
            Category("refund", "收到退款", "refund", TransactionType.INCOME),
            Category("deposit_back", "收回押金", "deposit_back", TransactionType.INCOME),
            Category("redpacket", "红包", "redpacket", TransactionType.INCOME),
            Category("recover_loan", "收回借款", "收回借款", TransactionType.INCOME),
            Category("income_transfer", "账户转账", "income_transfer", TransactionType.INCOME),
            Category("reimbursement", "报销款", "reimbursement", TransactionType.INCOME)
        )

        // 存储自定义分类
        private val customExpenseCategories = mutableListOf<Category>()
        private val customIncomeCategories = mutableListOf<Category>()

        fun loadCustomCategories(categories: List<Category>) {
            customExpenseCategories.clear()
            customIncomeCategories.clear()
            categories.forEach { cat ->
                when (cat.type) {
                    TransactionType.EXPENSE -> customExpenseCategories.add(cat)
                    TransactionType.INCOME -> customIncomeCategories.add(cat)
                }
            }
        }

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
