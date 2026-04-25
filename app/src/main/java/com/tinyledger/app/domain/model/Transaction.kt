package com.tinyledger.app.domain.model

data class Transaction(
    val id: Long = 0,
    val type: TransactionType,
    val category: Category,
    val amount: Double,
    val note: String?,
    val date: Long,
    val accountId: Long? = null,  // 关联账户ID
    val relatedTransactionId: Long? = null,  // 关联的另一笔交易ID（用于转账/借贷）
    val imagePath: String? = null,  // 图片附件路径
    val reimbursementStatus: ReimbursementStatus = ReimbursementStatus.NONE  // 报销状态
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

enum class ReimbursementStatus(val value: Int) {
    NONE(0),
    PENDING(1),
    REIMBURSED(2);

    companion object {
        fun fromInt(value: Int): ReimbursementStatus {
            return when (value) {
                0 -> NONE
                1 -> PENDING
                2 -> REIMBURSED
                else -> NONE
            }
        }
    }
}

data class Category(
    val id: String,
    val name: String,
    val icon: String,
    val type: TransactionType,
    val isDefault: Boolean = true,
    val parentId: String? = null  // 二级分类的父分类ID，null表示一级分类
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
        )

        val defaultExpenseCategories = mutableListOf(
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
            Category("investment_expense", "支出投资", "investment_expense", TransactionType.EXPENSE),
            Category("accommodation", "住宿", "accommodation", TransactionType.EXPENSE),
            Category("charity", "慈善捐赠", "charity", TransactionType.EXPENSE),
            Category("send_redpacket", "派发红包", "send_redpacket", TransactionType.EXPENSE),
            Category("family_living", "生活开支", "family_living", TransactionType.EXPENSE),
            Category("children", "子女开支", "children", TransactionType.EXPENSE),
            Category("elderly_care", "赡养父母", "elderly_care", TransactionType.EXPENSE),
            Category("other", "其他", "other", TransactionType.EXPENSE)
        )

        val defaultIncomeCategories = mutableListOf(
            Category("salary", "工资", "salary", TransactionType.INCOME),
            Category("bonus", "奖金", "bonus", TransactionType.INCOME),
            Category("investment", "投资", "investment", TransactionType.INCOME),
            Category("financial", "理财", "financial", TransactionType.INCOME),
            Category("dividend", "分红", "dividend", TransactionType.INCOME),
            Category("refund", "收到退款", "refund", TransactionType.INCOME),
            Category("deposit_back", "收回押金", "deposit_back", TransactionType.INCOME),
            Category("redpacket", "红包", "redpacket", TransactionType.INCOME),
            Category("reimbursement", "报销款", "reimbursement", TransactionType.INCOME)
        )

        val defaultTransferCategories = mutableListOf(
            Category("transfer", "转账", "account_transfer", TransactionType.TRANSFER)
        )

        val defaultLendingCategories = mutableListOf(
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
         * 获取一级分类列表（默认分类 + 无parentId的自定义分类）
         */
        fun getTopLevelCategoriesByType(type: TransactionType): List<Category> {
            return getCategoriesByType(type).filter { it.parentId == null }
        }

        /**
         * 获取指定父分类下的二级分类列表
         */
        fun getSubCategories(parentId: String, type: TransactionType): List<Category> {
            return getCategoriesByType(type).filter { it.parentId == parentId }
        }

        /**
         * 添加二级分类
         */
        fun addSubCategory(name: String, type: TransactionType, parentId: String, icon: String? = null, onSaveToDatabase: ((Category) -> Unit)? = null): Category {
            val id = "sub_${System.currentTimeMillis()}"
            val defaultIcon = when (type) {
                TransactionType.EXPENSE -> "other"
                TransactionType.INCOME -> "redpacket"
                TransactionType.TRANSFER -> "account_transfer"
                TransactionType.LENDING -> "lend"
            }
            val category = Category(id, name, icon ?: defaultIcon, type, isDefault = false, parentId = parentId)

            when (type) {
                TransactionType.EXPENSE -> customExpenseCategories.add(category)
                TransactionType.INCOME -> customIncomeCategories.add(category)
                TransactionType.TRANSFER -> customTransferCategories.add(category)
                TransactionType.LENDING -> customLendingCategories.add(category)
            }

            // 回调保存到数据库
            onSaveToDatabase?.invoke(category)

            return category
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

        fun addCustomCategory(name: String, type: TransactionType, icon: String? = null, onSaveToDatabase: ((Category) -> Unit)? = null): Category {
            val id = "custom_${System.currentTimeMillis()}"
            val defaultIcon = when (type) {
                TransactionType.EXPENSE -> "other"
                TransactionType.INCOME -> "redpacket"
                TransactionType.TRANSFER -> "account_transfer"
                TransactionType.LENDING -> "lend"
            }
            val category = Category(id, name, icon ?: defaultIcon, type, isDefault = false)
            
            when (type) {
                TransactionType.EXPENSE -> customExpenseCategories.add(category)
                TransactionType.INCOME -> customIncomeCategories.add(category)
                TransactionType.TRANSFER -> customTransferCategories.add(category)
                TransactionType.LENDING -> customLendingCategories.add(category)
            }
            
            // 回调保存到数据库
            onSaveToDatabase?.invoke(category)
            
            return category
        }

        fun removeCustomCategory(category: Category): List<Category> {
            // Cannot delete default categories
            if (category.isDefault) return emptyList()
            val removed = mutableListOf<Category>()

            fun removeAndCollect(list: MutableList<Category>): List<Category> {
                val toRemove = list.filter { it.id == category.id }
                list.removeAll(toRemove)
                return toRemove
            }

            removed.addAll(removeAndCollect(customExpenseCategories))
            removed.addAll(removeAndCollect(customIncomeCategories))
            removed.addAll(removeAndCollect(customTransferCategories))
            removed.addAll(removeAndCollect(customLendingCategories))

            // 如果删除的是一级分类，同时级联删除其所有二级分类
            if (category.parentId == null) {
                val subRemoved = mutableListOf<Category>()
                fun removeSubAndCollect(list: MutableList<Category>): List<Category> {
                    val toRemove = list.filter { it.parentId == category.id }
                    list.removeAll(toRemove)
                    return toRemove
                }
                subRemoved.addAll(removeSubAndCollect(customExpenseCategories))
                subRemoved.addAll(removeSubAndCollect(customIncomeCategories))
                subRemoved.addAll(removeSubAndCollect(customTransferCategories))
                subRemoved.addAll(removeSubAndCollect(customLendingCategories))
                removed.addAll(subRemoved)
            }

            return removed
        }

        /**
         * 删除任意分类（包括内置分类），同时级联删除子分类
         */
        fun removeCategory(category: Category): List<Category> {
            val removed = mutableListOf<Category>()

            fun removeAndCollect(list: MutableList<Category>): List<Category> {
                val toRemove = list.filter { it.id == category.id }
                list.removeAll(toRemove)
                return toRemove
            }

            removed.addAll(removeAndCollect(defaultExpenseCategories))
            removed.addAll(removeAndCollect(defaultIncomeCategories))
            removed.addAll(removeAndCollect(defaultTransferCategories))
            removed.addAll(removeAndCollect(defaultLendingCategories))
            removed.addAll(removeAndCollect(customExpenseCategories))
            removed.addAll(removeAndCollect(customIncomeCategories))
            removed.addAll(removeAndCollect(customTransferCategories))
            removed.addAll(removeAndCollect(customLendingCategories))

            // 如果删除的是一级分类，同时级联删除其所有二级分类
            if (category.parentId == null) {
                fun removeSubAndCollect(list: MutableList<Category>): List<Category> {
                    val toRemove = list.filter { it.parentId == category.id }
                    list.removeAll(toRemove)
                    return toRemove
                }
                removed.addAll(removeSubAndCollect(defaultExpenseCategories))
                removed.addAll(removeSubAndCollect(defaultIncomeCategories))
                removed.addAll(removeSubAndCollect(defaultTransferCategories))
                removed.addAll(removeSubAndCollect(defaultLendingCategories))
                removed.addAll(removeSubAndCollect(customExpenseCategories))
                removed.addAll(removeSubAndCollect(customIncomeCategories))
                removed.addAll(removeSubAndCollect(customTransferCategories))
                removed.addAll(removeSubAndCollect(customLendingCategories))
            }

            return removed
        }

        /**
         * 更新自定义分类（名称和图标）
         */
        fun updateCustomCategory(category: Category, newName: String, newIcon: String): Category? {
            // Cannot update default categories
            if (category.isDefault) return null
            val list = when (category.type) {
                TransactionType.EXPENSE -> customExpenseCategories
                TransactionType.INCOME -> customIncomeCategories
                TransactionType.TRANSFER -> customTransferCategories
                TransactionType.LENDING -> customLendingCategories
            }
            val index = list.indexOfFirst { it.id == category.id }
            if (index < 0) return null
            val updated = category.copy(name = newName, icon = newIcon)
            list[index] = updated
            return updated
        }

        /**
         * 重命名自定义分类（仅更新名称，保留兼容性）
         */
        fun renameCustomCategory(category: Category, newName: String): Category? {
            return updateCustomCategory(category, newName, category.icon)
        }

        fun fromId(id: String, type: TransactionType): Category {
            return getCategoriesByType(type).find { it.id == id }
                ?: Category(id, id, "other", type)
        }
    }
}
