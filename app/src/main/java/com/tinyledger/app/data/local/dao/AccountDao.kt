package com.tinyledger.app.data.local.dao

import androidx.room.*
import com.tinyledger.app.data.local.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts WHERE isDisabled = 0 ORDER BY createdAt DESC")
    fun getAllAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts ORDER BY createdAt DESC")
    fun getAllAccountsIncludingDisabled(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getAccountById(id: Long): AccountEntity?

    @Query("SELECT * FROM accounts WHERE type = :type AND isDisabled = 0")
    fun getAccountsByType(type: String): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE attribute = :attribute AND isDisabled = 0 ORDER BY createdAt DESC")
    fun getAccountsByAttribute(attribute: String): Flow<List<AccountEntity>>

    @Query("SELECT SUM(currentBalance) FROM accounts WHERE isDisabled = 0")
    fun getTotalBalance(): Flow<Double?>

    @Query("SELECT SUM(currentBalance) FROM accounts WHERE attribute = 'cash' AND isDisabled = 0")
    fun getCashTotalBalance(): Flow<Double?>

    @Query("SELECT SUM(currentBalance) FROM accounts WHERE attribute = 'credit' AND isDisabled = 0")
    fun getCreditTotalBalance(): Flow<Double?>

    @Query("SELECT SUM(currentBalance) FROM accounts WHERE attribute = 'credit_account' AND isDisabled = 0")
    fun getCreditAccountTotalBalance(): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity): Long

    @Update
    suspend fun updateAccount(account: AccountEntity)

    @Query("UPDATE accounts SET currentBalance = :balance, updatedAt = :timestamp WHERE id = :accountId")
    suspend fun updateBalance(accountId: Long, balance: Double, timestamp: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteAccount(account: AccountEntity)

    @Query("UPDATE accounts SET isDisabled = 1, updatedAt = :timestamp WHERE id = :accountId")
    suspend fun disableAccount(accountId: Long, timestamp: Long = System.currentTimeMillis())
}
