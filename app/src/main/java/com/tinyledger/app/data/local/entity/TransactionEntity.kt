package com.tinyledger.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: Int, // 0 = expense, 1 = income, 2 = transfer, 3 = lending
    val category: String,
    val amount: Double,
    val note: String?,
    val date: Long,
    val accountId: Long? = null,  // 关联账户ID
    val relatedTransactionId: Long? = null,  // 关联的另一笔交易ID（用于转账/借贷）
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
