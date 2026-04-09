package com.tinyledger.app.domain.model

data class Account(
    val id: Long = 0,
    val name: String,
    val type: AccountType,
    val icon: String,
    val initialBalance: Double,
    val currentBalance: Double,
    val color: String,
    val cardNumber: String? = null,
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class AccountType(val value: String, val displayName: String, val icon: String) {
    BANK("bank", "银行卡", "account_balance"),
    WECHAT("wechat", "微信", "chat"),
    ALIPAY("alipay", "支付宝", "payment"),
    CASH("cash", "现金", "wallet"),
    OTHER("other", "其他", "account_balance_wallet");

    companion object {
        fun fromValue(value: String): AccountType {
            return entries.find { it.value == value } ?: OTHER
        }
    }
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
