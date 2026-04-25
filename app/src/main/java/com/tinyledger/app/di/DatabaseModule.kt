package com.tinyledger.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.tinyledger.app.data.local.AppDatabase
import com.tinyledger.app.data.local.dao.AccountDao
import com.tinyledger.app.data.local.dao.BudgetCategoryDao
import com.tinyledger.app.data.local.dao.BudgetDao
import com.tinyledger.app.data.local.dao.CategoryDao
import com.tinyledger.app.data.local.dao.NotificationSmsDao
import com.tinyledger.app.data.local.dao.PendingTransactionDao
import com.tinyledger.app.data.local.dao.TransactionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS accounts (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    type TEXT NOT NULL,
                    icon TEXT NOT NULL,
                    initialBalance REAL NOT NULL DEFAULT 0,
                    currentBalance REAL NOT NULL DEFAULT 0,
                    color TEXT NOT NULL,
                    cardNumber TEXT,
                    isEnabled INTEGER NOT NULL DEFAULT 1,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL
                )
            """.trimIndent())
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE transactions ADD COLUMN accountId INTEGER")
        }
    }

    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS notification_sms (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    address TEXT NOT NULL,
                    body TEXT NOT NULL,
                    date INTEGER NOT NULL,
                    packageName TEXT NOT NULL,
                    uniqueHash TEXT NOT NULL,
                    createdAt INTEGER NOT NULL
                )
            """.trimIndent())
            database.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS index_notification_sms_uniqueHash ON notification_sms (uniqueHash)"
            )
        }
    }

    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS budgets (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    type TEXT NOT NULL,
                    year INTEGER NOT NULL,
                    month INTEGER NOT NULL,
                    totalBudget REAL NOT NULL,
                    reminderEnabled INTEGER NOT NULL DEFAULT 1,
                    reminderPercentage INTEGER NOT NULL DEFAULT 80,
                    modifiedCount INTEGER NOT NULL DEFAULT 0,
                    lastModifiedAt INTEGER NOT NULL DEFAULT 0,
                    createdAt INTEGER NOT NULL
                )
            """.trimIndent())
        }
    }

    private val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS budget_categories (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    budgetId INTEGER NOT NULL,
                    categoryId TEXT NOT NULL,
                    categoryName TEXT NOT NULL,
                    amount REAL NOT NULL
                )
            """.trimIndent())
        }
    }

    private val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS pending_transactions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    type INTEGER NOT NULL,
                    category TEXT NOT NULL,
                    amount REAL NOT NULL,
                    note TEXT,
                    date INTEGER NOT NULL,
                    accountId INTEGER,
                    source TEXT NOT NULL,
                    createdAt INTEGER NOT NULL
                )
            """.trimIndent())
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "tinyledger_database"
        )
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, com.tinyledger.app.data.local.MIGRATION_7_8, com.tinyledger.app.data.local.MIGRATION_8_9, com.tinyledger.app.data.local.MIGRATION_9_10, com.tinyledger.app.data.local.MIGRATION_10_11, com.tinyledger.app.data.local.MIGRATION_11_12, com.tinyledger.app.data.local.MIGRATION_12_13, com.tinyledger.app.data.local.MIGRATION_13_14, com.tinyledger.app.data.local.MIGRATION_14_15, com.tinyledger.app.data.local.MIGRATION_15_16, com.tinyledger.app.data.local.MIGRATION_16_17)
        .build()
    }

    @Provides
    @Singleton
    fun provideTransactionDao(database: AppDatabase): TransactionDao {
        return database.transactionDao()
    }

    @Provides
    @Singleton
    fun provideAccountDao(database: AppDatabase): AccountDao {
        return database.accountDao()
    }

    @Provides
    @Singleton
    fun provideNotificationSmsDao(database: AppDatabase): NotificationSmsDao {
        return database.notificationSmsDao()
    }

    @Provides
    @Singleton
    fun provideBudgetDao(database: AppDatabase): BudgetDao {
        return database.budgetDao()
    }

    @Provides
    @Singleton
    fun provideBudgetCategoryDao(database: AppDatabase): BudgetCategoryDao {
        return database.budgetCategoryDao()
    }

    @Provides
    @Singleton
    fun providePendingTransactionDao(database: AppDatabase): PendingTransactionDao {
        return database.pendingTransactionDao()
    }

    @Provides
    @Singleton
    fun provideCategoryDao(database: AppDatabase): CategoryDao {
        return database.categoryDao()
    }
}
