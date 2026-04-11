package com.tinyledger.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.tinyledger.app.data.local.dao.AccountDao
import com.tinyledger.app.data.local.dao.BudgetCategoryDao
import com.tinyledger.app.data.local.dao.BudgetDao
import com.tinyledger.app.data.local.dao.NotificationSmsDao
import com.tinyledger.app.data.local.dao.TransactionDao
import com.tinyledger.app.data.local.entity.AccountEntity
import com.tinyledger.app.data.local.entity.BudgetCategoryEntity
import com.tinyledger.app.data.local.entity.BudgetEntity
import com.tinyledger.app.data.local.entity.NotificationSmsEntity
import com.tinyledger.app.data.local.entity.PendingTransactionEntity
import com.tinyledger.app.data.local.entity.TransactionEntity

@Database(
    entities = [TransactionEntity::class, AccountEntity::class, NotificationSmsEntity::class, BudgetEntity::class, BudgetCategoryEntity::class, PendingTransactionEntity::class],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun accountDao(): AccountDao
    abstract fun notificationSmsDao(): NotificationSmsDao
    abstract fun budgetDao(): BudgetDao
    abstract fun budgetCategoryDao(): BudgetCategoryDao
    abstract fun pendingTransactionDao(): PendingTransactionDao
}
