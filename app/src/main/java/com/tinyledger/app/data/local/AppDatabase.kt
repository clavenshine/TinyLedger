package com.tinyledger.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.tinyledger.app.data.local.dao.AccountDao
import com.tinyledger.app.data.local.dao.BudgetCategoryDao
import com.tinyledger.app.data.local.dao.BudgetDao
import com.tinyledger.app.data.local.dao.NotificationSmsDao
import com.tinyledger.app.data.local.dao.PendingTransactionDao
import com.tinyledger.app.data.local.dao.TransactionDao
import com.tinyledger.app.data.local.entity.AccountEntity
import com.tinyledger.app.data.local.entity.BudgetCategoryEntity
import com.tinyledger.app.data.local.entity.BudgetEntity
import com.tinyledger.app.data.local.entity.NotificationSmsEntity
import com.tinyledger.app.data.local.entity.PendingTransactionEntity
import com.tinyledger.app.data.local.entity.TransactionEntity

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add new columns to accounts table
        database.execSQL("ALTER TABLE accounts ADD COLUMN attribute TEXT NOT NULL DEFAULT 'cash'")
        database.execSQL("ALTER TABLE accounts ADD COLUMN creditLimit REAL NOT NULL DEFAULT 0.0")
        database.execSQL("ALTER TABLE accounts ADD COLUMN billDay INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE accounts ADD COLUMN repaymentDay INTEGER NOT NULL DEFAULT 0")
        
        // Migrate existing alipay accounts to hua_bei with credit attribute
        database.execSQL("""
            UPDATE accounts 
            SET type = 'hua_bei', 
                attribute = 'credit',
                creditLimit = ABS(currentBalance),
                billDay = 1,
                repaymentDay = 10
            WHERE type = 'alipay' AND currentBalance < 0
        """)
        
        // Set attribute to 'cash' for all other accounts
        database.execSQL("UPDATE accounts SET attribute = 'cash' WHERE type != 'hua_bei'")
    }
}

@Database(
    entities = [TransactionEntity::class, AccountEntity::class, NotificationSmsEntity::class, BudgetEntity::class, BudgetCategoryEntity::class, PendingTransactionEntity::class],
    version = 8,
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
