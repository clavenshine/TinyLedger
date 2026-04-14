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
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@OptIn(kotlin.ExperimentalStdlibApi::class)
enum class AccountAttribute(val value: String, val displayName: String) {
    CASH("cash", "现金账户"),
    CREDIT("credit", "信用账户");

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
    CREDIT_CARD("credit_card", "信用卡", "credit_card", AccountAttribute.CREDIT),
    HUA_BEI("hua_bei", "花呗", "payment", AccountAttribute.CREDIT),
    JIE_BEI("jie_bei", "借呗", "payments", AccountAttribute.CREDIT),
    JD_BAITIAO("jd_baitiao", "京东白条", "shopping_bag", AccountAttribute.CREDIT),
    MEITUAN_YUEFU("meituan_yuefu", "美团月付", "restaurant", AccountAttribute.CREDIT),
    DOUYIN_YUEFU("douyin_yuefu", "抖音月付", "video_library", AccountAttribute.CREDIT);

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
