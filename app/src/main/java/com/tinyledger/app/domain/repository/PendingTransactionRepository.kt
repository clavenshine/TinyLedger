package com.tinyledger.app.domain.repository

import com.tinyledger.app.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

interface PendingTransactionRepository {
    fun getAllPendingTransactions(): Flow<List<Transaction>>
    suspend fun insertPendingTransaction(transaction: Transaction): Long
    suspend fun deletePendingTransaction(id: Long)
    suspend fun confirmPendingTransaction(pendingId: Long, confirmedTransaction: Transaction)
}
