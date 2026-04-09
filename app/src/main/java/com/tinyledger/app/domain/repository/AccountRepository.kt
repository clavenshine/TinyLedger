package com.tinyledger.app.domain.repository

import com.tinyledger.app.domain.model.Account
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    fun getAllAccounts(): Flow<List<Account>>
    suspend fun getAccountById(id: Long): Account?
    fun getAccountsByType(type: String): Flow<List<Account>>
    fun getTotalBalance(): Flow<Double>
    suspend fun addAccount(account: Account): Long
    suspend fun updateAccount(account: Account)
    suspend fun updateAccountBalance(accountId: Long, balance: Double)
    suspend fun deleteAccount(accountId: Long)
}
