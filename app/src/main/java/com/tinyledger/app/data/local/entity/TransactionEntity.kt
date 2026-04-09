package com.tinyledger.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: Int, // 0 = expense, 1 = income
    val category: String,
    val amount: Double,
    val note: String?,
    val date: Long,
    val accountId: Long? = null,  // 关联账户ID
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
