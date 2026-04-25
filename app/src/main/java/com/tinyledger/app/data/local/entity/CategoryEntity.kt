package com.tinyledger.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories",
    indices = [
        androidx.room.Index(value = ["type"]),
        androidx.room.Index(value = ["parentId"])
    ]
)
data class CategoryEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val icon: String,
    val type: Int, // 0=EXPENSE, 1=INCOME, 2=TRANSFER, 3=LENDING
    val isDefault: Boolean = false,
    val parentId: String? = null,
    val sortOrder: Int = 0,
    val createdAt: Long = 0,
    val updatedAt: Long = 0
)
