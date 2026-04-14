package com.tinyledger.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: String, // bank, wechat, cash, yuebao, other, credit_card, hua_bei, jie_bei, jd_baitiao, meituan_yuefu, douyin_yuefu
    val attribute: String = "cash", // cash or credit
    val icon: String, // 图标名称
    val initialBalance: Double, // 期初余额
    val currentBalance: Double, // 当前余额
    val color: String, // 账户颜色
    val cardNumber: String? = null, // 卡号后4位
    val creditLimit: Double = 0.0, // 信用额度
    val billDay: Int = 0, // 账单日 (1-31)
    val repaymentDay: Int = 0, // 还款日 (1-31)
    val isEnabled: Boolean = true, // 是否启用
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
