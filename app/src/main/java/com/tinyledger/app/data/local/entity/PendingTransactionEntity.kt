package com.tinyledger.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 待确认交易记录
 * 
 * 用于存储从通知消息（微信、支付宝等）和手机短信自动识别但尚未确认的交易记录
 * 用户可以在首页查看这些待确认记录，并选择确认导入或删除
 */
@Entity(tableName = "pending_transactions")
data class PendingTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: Int,              // 0=支出, 1=收入
    val category: String,       // 分类ID
    val amount: Double,         // 金额
    val note: String?,          // 备注
    val date: Long,             // 时间戳
    val accountId: Long?,       // 关联账户ID
    val source: String,         // 来源: "sms" 或 "notification"
    val createdAt: Long = System.currentTimeMillis()
)
