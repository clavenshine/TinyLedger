package com.tinyledger.app.domain.model

@OptIn(kotlin.ExperimentalStdlibApi::class)
data class Account(
    val id: Long = 0,
    val name: String,
    val type: AccountType,
    val attribute: AccountAttribute = AccountAttribute.CASH,
    val icon: String,
    val initialBalance: Double,
    val currentBalance: Double,
    val color: String,
    val cardNumber: String? = null,
    val creditLimit: Double = 0.0,
    val billDay: Int = 0,
    val repaymentDay: Int = 0,
    val isDisabled: Boolean = false,
    val initialBalanceDate: String = "", // 期初余额日期，格式 yyyy-MM-dd
    val purpose: String = "", // 用途
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@OptIn(kotlin.ExperimentalStdlibApi::class)
enum class AccountAttribute(val value: String, val displayName: String) {
    CASH("cash", "现金账户"),
    CREDIT_ACCOUNT("credit_account", "信用账户"),
    CREDIT("credit", "外部往来");

    @OptIn(kotlin.ExperimentalStdlibApi::class)
    companion object {
        fun fromValue(value: String): AccountAttribute {
            return entries.find { it.value == value } ?: CASH
        }
    }
}

@OptIn(kotlin.ExperimentalStdlibApi::class)
enum class AccountType(val value: String, val displayName: String, val icon: String, val attribute: AccountAttribute) {
    // 现金账户类型
    BANK("bank", "银行卡", "account_balance", AccountAttribute.CASH),
    WECHAT("wechat", "微信", "chat", AccountAttribute.CASH),
    CASH("cash", "现金", "wallet", AccountAttribute.CASH),
    YUEBAO("yuebao", "余额宝", "account_balance_wallet", AccountAttribute.CASH),
    OTHER("other", "其他", "help_outline", AccountAttribute.CASH),

    // 信用账户类型
    CREDIT_CARD("credit_card", "信用卡", "credit_card", AccountAttribute.CREDIT_ACCOUNT),
    CONSUMPTION_PLATFORM("consumption_platform", "消费平台", "shopping_bag", AccountAttribute.CREDIT_ACCOUNT),

    // 外部往来账户类型
    PERSONAL_TRANSACTION("personal_transaction", "外部个人往来", "person", AccountAttribute.CREDIT),
    LOAN_LIABILITY("loan_liability", "外部贷款负债", "account_balance", AccountAttribute.CREDIT);

    @OptIn(kotlin.ExperimentalStdlibApi::class)
    companion object {
        fun fromValue(value: String): AccountType {
            return entries.find { it.value == value } ?: OTHER
        }
    }
}

@OptIn(kotlin.ExperimentalStdlibApi::class)
fun getAccountTypesByAttribute(attribute: AccountAttribute): List<AccountType> {
    return AccountType.entries.filter { it.attribute == attribute }
}

// 预设账户颜色
val accountColors = listOf(
    "#10B981", // 薄荷绿
    "#3B82F6", // 天蓝色
    "#8B5CF6", // 紫色
    "#F59E0B", // 橙色
    "#EF4444", // 红色
    "#EC4899", // 粉色
    "#06B6D4", // 青色
    "#84CC16", // 亮绿色
    "#6366F1", // 靛蓝色
    "#14B8A6"  // 绿松石色
)
