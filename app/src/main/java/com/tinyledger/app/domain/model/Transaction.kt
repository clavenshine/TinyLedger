package com.tinyledger.app.domain.model

data class Transaction(
    val id: Long = 0,
    val type: TransactionType,
    val category: Category,
    val amount: Double,
    val note: String?,
    val date: Long,
    val accountId: Long? = null,  // 关联账户ID
    val relatedTransactionId: Long? = null  // 关联的另一笔交易ID（用于转账/借贷）
)

enum class TransactionType(val value: Int) {
    EXPENSE(0),
    INCOME(1),
    TRANSFER(2),
    LENDING(3);

    companion object {
        fun fromInt(value: Int): TransactionType {
            return when (value) {
                0 -> EXPENSE
                1 -> INCOME
                2 -> TRANSFER
                3 -> LENDING
                else -> EXPENSE
            }
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
            Category("credit_card_repay", "还信用卡", "credit_card_repay", TransactionType.EXPENSE),
            Category("mortgage", "房贷支出", "mortgage", TransactionType.EXPENSE),
            Category("repay_loan", "归还借款", "repay_loan", TransactionType.EXPENSE),
            Category("alipay_repay", "还支付宝", "alipay_repay", TransactionType.EXPENSE),
            Category("douyin_repay", "抖音还款", "douyin_repay", TransactionType.EXPENSE),
            Category("jd_repay", "京东还款", "jd_repay", TransactionType.EXPENSE),
            Category("account_transfer", "账户转账", "account_transfer", TransactionType.EXPENSE),
            Category("lend", "借出资金", "lend", TransactionType.EXPENSE),
            Category("investment_expense", "支出投资", "investment_expense", TransactionType.EXPENSE),
            Category("accommodation", "住宿", "accommodation", TransactionType.EXPENSE),
            Category("charity", "慈善捐赠", "charity", TransactionType.EXPENSE),
            Category("send_redpacket", "派发红包", "send_redpacket", TransactionType.EXPENSE),
            Category("family_living", "生活开支", "family_living", TransactionType.EXPENSE),
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

        val defaultTransferCategories = listOf(
            Category("transfer", "转账", "account_transfer", TransactionType.TRANSFER)
        )

        val defaultLendingCategories = listOf(
            Category("borrow_in", "借入", "lend", TransactionType.LENDING),
            Category("borrow_out", "借出", "lend", TransactionType.LENDING),
            Category("repay", "还款", "credit_card_repay", TransactionType.LENDING),
            Category("collect", "收款", "redpacket", TransactionType.LENDING)
        )

        // 存储自定义分类
        private val customExpenseCategories = mutableListOf<Category>()
        private val customIncomeCategories = mutableListOf<Category>()
        private val customTransferCategories = mutableListOf<Category>()
        private val customLendingCategories = mutableListOf<Category>()

        fun loadCustomCategories(categories: List<Category>) {
            customExpenseCategories.clear()
            customIncomeCategories.clear()
            customTransferCategories.clear()
            customLendingCategories.clear()
            categories.forEach { cat ->
                when (cat.type) {
                    TransactionType.EXPENSE -> customExpenseCategories.add(cat)
                    TransactionType.INCOME -> customIncomeCategories.add(cat)
                    TransactionType.TRANSFER -> customTransferCategories.add(cat)
                    TransactionType.LENDING -> customLendingCategories.add(cat)
                }
            }
        }

        /**
         * 检测自定义分类与默认分类名称重复，返回需要迁移的映射（customId -> defaultId）
         */
        fun findDuplicateCustomCategories(): Map<String, String> {
            val migrationMap = mutableMapOf<String, String>()
            
            fun checkDuplicates(customs: List<Category>, defaults: List<Category>) {
                for (custom in customs) {
                    val matchingDefault = defaults.find { it.name == custom.name }
                    if (matchingDefault != null) {
                        migrationMap[custom.id] = matchingDefault.id
                    }
                }
            }
            
            checkDuplicates(customExpenseCategories, defaultExpenseCategories)
            checkDuplicates(customIncomeCategories, defaultIncomeCategories)
            checkDuplicates(customTransferCategories, defaultTransferCategories)
            checkDuplicates(customLendingCategories, defaultLendingCategories)
            
            return migrationMap
        }

        /**
         * 移除与默认分类重名的自定义分类，并返回被移除的自定义分类列表
         */
        fun removeDuplicateCustomCategories(duplicateIds: Set<String>): List<Category> {
            val removed = mutableListOf<Category>()
            
            fun removeAndCollect(list: MutableList<Category>): List<Category> {
                val toRemove = list.filter { it.id in duplicateIds }
                list.removeAll(toRemove)
                return toRemove
            }
            
            removed.addAll(removeAndCollect(customExpenseCategories))
            removed.addAll(removeAndCollect(customIncomeCategories))
            removed.addAll(removeAndCollect(customTransferCategories))
            removed.addAll(removeAndCollect(customLendingCategories))
            
            return removed
        }

        fun getCategoriesByType(type: TransactionType): List<Category> {
            return when (type) {
                TransactionType.EXPENSE -> defaultExpenseCategories + customExpenseCategories
                TransactionType.INCOME -> defaultIncomeCategories + customIncomeCategories
                TransactionType.TRANSFER -> defaultTransferCategories + customTransferCategories
                TransactionType.LENDING -> defaultLendingCategories + customLendingCategories
            }
        }

        fun addCustomCategory(name: String, type: TransactionType): Category {
            val id = "custom_${System.currentTimeMillis()}"
            val icon = when (type) {
                TransactionType.EXPENSE -> "other"
                TransactionType.INCOME -> "redpacket"
                TransactionType.TRANSFER -> "account_transfer"
                TransactionType.LENDING -> "lend"
            }
            val category = Category(id, name, icon, type, isDefault = false)
            
            when (type) {
                TransactionType.EXPENSE -> customExpenseCategories.add(category)
                TransactionType.INCOME -> customIncomeCategories.add(category)
                TransactionType.TRANSFER -> customTransferCategories.add(category)
                TransactionType.LENDING -> customLendingCategories.add(category)
            }
            
            return category
        }

        fun removeCustomCategory(category: Category): Boolean {
            // Cannot delete default categories
            if (category.isDefault) return false
            val removed = when (category.type) {
                TransactionType.EXPENSE -> customExpenseCategories.remove(category)
                TransactionType.INCOME -> customIncomeCategories.remove(category)
                TransactionType.TRANSFER -> customTransferCategories.remove(category)
                TransactionType.LENDING -> customLendingCategories.remove(category)
            }
            return removed
        }

        fun renameCustomCategory(category: Category, newName: String): Category? {
            // Cannot rename default categories
            if (category.isDefault) return null
            val list = when (category.type) {
                TransactionType.EXPENSE -> customExpenseCategories
                TransactionType.INCOME -> customIncomeCategories
                TransactionType.TRANSFER -> customTransferCategories
                TransactionType.LENDING -> customLendingCategories
            }
            val index = list.indexOfFirst { it.id == category.id }
            if (index < 0) return null
            val renamed = category.copy(name = newName)
            list[index] = renamed
            return renamed
        }

        fun fromId(id: String, type: TransactionType): Category {
            return getCategoriesByType(type).find { it.id == id }
                ?: Category(id, id, "other", type)
        }
    }
}
