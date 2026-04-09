package com.tinyledger.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: String, // bank, wechat, alipay, cash, other
    val icon: String, // 图标名称
    val initialBalance: Double, // 期初余额
    val currentBalance: Double, // 当前余额
    val color: String, // 账户颜色
    val cardNumber: String? = null, // 卡号后4位
    val isEnabled: Boolean = true, // 是否启用
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
