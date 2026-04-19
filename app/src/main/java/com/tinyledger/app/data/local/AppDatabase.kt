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

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 核心数据逻辑变更：支出金额从正数转为负数存储
        // type=0 表示 EXPENSE，将所有正数的支出金额转为负数
        database.execSQL("UPDATE transactions SET amount = -amount WHERE type = 0 AND amount > 0")
    }
}

val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 外部往来账户类型调整：将旧的信用账户类型映射到新类型
        // hua_bei -> credit_consumption（信用消费）
        database.execSQL("UPDATE accounts SET type = 'credit_consumption' WHERE type = 'hua_bei'")
        // jie_bei -> loan_liability（贷款负债）
        database.execSQL("UPDATE accounts SET type = 'loan_liability' WHERE type = 'jie_bei'")
        // jd_baitiao -> credit_consumption（信用消费）
        database.execSQL("UPDATE accounts SET type = 'credit_consumption' WHERE type = 'jd_baitiao'")
        // meituan_yuefu -> credit_consumption（信用消费）
        database.execSQL("UPDATE accounts SET type = 'credit_consumption' WHERE type = 'meituan_yuefu'")
        // douyin_yuefu -> credit_consumption（信用消费）
        database.execSQL("UPDATE accounts SET type = 'credit_consumption' WHERE type = 'douyin_yuefu'")
    }
}

val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 交易分类调整：将已删除的支出分类迁移到借贷类（type=3）
        // credit_card_repay -> repay（还款）
        database.execSQL("UPDATE transactions SET category = 'repay', type = 3 WHERE category = 'credit_card_repay' AND type = 0")
        // mortgage -> repay（还款）
        database.execSQL("UPDATE transactions SET category = 'repay', type = 3 WHERE category = 'mortgage' AND type = 0")
        // repay_loan -> repay（还款）
        database.execSQL("UPDATE transactions SET category = 'repay', type = 3 WHERE category = 'repay_loan' AND type = 0")
        // alipay_repay -> repay（还款）
        database.execSQL("UPDATE transactions SET category = 'repay', type = 3 WHERE category = 'alipay_repay' AND type = 0")
        // douyin_repay -> repay（还款）
        database.execSQL("UPDATE transactions SET category = 'repay', type = 3 WHERE category = 'douyin_repay' AND type = 0")
        // jd_repay -> repay（还款）
        database.execSQL("UPDATE transactions SET category = 'repay', type = 3 WHERE category = 'jd_repay' AND type = 0")
        // account_transfer（支出）-> transfer（转账，type=2）
        database.execSQL("UPDATE transactions SET category = 'transfer', type = 2 WHERE category = 'account_transfer' AND type = 0")
        // lend（借出资金）-> borrow_out（借出，type=3）
        database.execSQL("UPDATE transactions SET category = 'borrow_out', type = 3 WHERE category = 'lend' AND type = 0")
        // recover_loan（收入）-> collect（收款，type=3）
        database.execSQL("UPDATE transactions SET category = 'collect', type = 3 WHERE category = 'recover_loan' AND type = 1")
        // income_transfer（收入）-> transfer（转账，type=2）
        database.execSQL("UPDATE transactions SET category = 'transfer', type = 2 WHERE category = 'income_transfer' AND type = 1")
    }
}

val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE accounts ADD COLUMN initialBalanceDate TEXT NOT NULL DEFAULT ''")
    }
}

val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 新增 purpose 字段
        database.execSQL("ALTER TABLE accounts ADD COLUMN purpose TEXT NOT NULL DEFAULT ''")

        // 旧信用账户重新分类：信用卡 -> 信用账户
        database.execSQL("UPDATE accounts SET attribute = 'credit_account' WHERE type = 'credit_card' AND attribute = 'credit'")
        // 旧信用消费 -> 消费平台，归属信用账户
        database.execSQL("UPDATE accounts SET type = 'consumption_platform', attribute = 'credit_account' WHERE type = 'credit_consumption' AND attribute = 'credit'")
    }
}

val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 新增 imagePath 字段到 transactions 表
        database.execSQL("ALTER TABLE transactions ADD COLUMN imagePath TEXT")
    }
}

val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 将 isEnabled 字段重命名为 isDisabled，并反转值
        // 重建表以匹配Room期望的schema（无DEFAULT约束）
        // 1. 创建新表（与Room Entity定义完全一致，无DEFAULT约束）
        database.execSQL("""
            CREATE TABLE accounts_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                type TEXT NOT NULL,
                attribute TEXT NOT NULL,
                icon TEXT NOT NULL,
                initialBalance REAL NOT NULL,
                currentBalance REAL NOT NULL,
                color TEXT NOT NULL,
                cardNumber TEXT,
                creditLimit REAL NOT NULL,
                billDay INTEGER NOT NULL,
                repaymentDay INTEGER NOT NULL,
                isDisabled INTEGER NOT NULL,
                initialBalanceDate TEXT NOT NULL,
                purpose TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
        """.trimIndent())
        // 2. 迁移数据：isEnabled=1 -> isDisabled=0, isEnabled=0 -> isDisabled=1
        database.execSQL("""
            INSERT INTO accounts_new (id, name, type, attribute, icon, initialBalance, currentBalance, color, cardNumber, creditLimit, billDay, repaymentDay, isDisabled, initialBalanceDate, purpose, createdAt, updatedAt)
            SELECT id, name, type, attribute, icon, initialBalance, currentBalance, color, cardNumber, creditLimit, billDay, repaymentDay,
                CASE WHEN isEnabled = 1 THEN 0 ELSE 1 END,
                initialBalanceDate, purpose, createdAt, updatedAt
            FROM accounts
        """.trimIndent())
        // 3. 删除旧表
        database.execSQL("DROP TABLE accounts")
        // 4. 重命名新表
        database.execSQL("ALTER TABLE accounts_new RENAME TO accounts")
    }
}

@Database(
    entities = [TransactionEntity::class, AccountEntity::class, NotificationSmsEntity::class, BudgetEntity::class, BudgetCategoryEntity::class, PendingTransactionEntity::class],
    version = 15,
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
