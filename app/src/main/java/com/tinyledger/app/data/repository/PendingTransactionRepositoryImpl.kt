package com.tinyledger.app.data.repository

import com.tinyledger.app.data.local.dao.PendingTransactionDao
import com.tinyledger.app.data.local.entity.PendingTransactionEntity
import com.tinyledger.app.domain.model.Category
import com.tinyledger.app.domain.model.Transaction
import com.tinyledger.app.domain.model.TransactionType
import com.tinyledger.app.domain.repository.PendingTransactionRepository
import com.tinyledger.app.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class PendingTransactionRepositoryImpl @Inject constructor(
    private val pendingTransactionDao: PendingTransactionDao,
    private val transactionRepository: TransactionRepository
) : PendingTransactionRepository {

    override fun getAllPendingTransactions(): Flow<List<Transaction>> {
        return pendingTransactionDao.getAllPending().map { entities ->
            entities.map { entity ->
                Transaction(
                    id = entity.id,
                    type = TransactionType.fromInt(entity.type),
                    category = Category.fromId(entity.category, TransactionType.fromInt(entity.type)),
                    amount = entity.amount,
                    note = entity.note,
                    date = entity.date,
                    accountId = entity.accountId
                )
            }
        }
    }

    override suspend fun insertPendingTransaction(transaction: Transaction): Long {
        val entity = PendingTransactionEntity(
            type = transaction.type.value,
            category = transaction.category.id,
            amount = transaction.amount,
            note = transaction.note,
            date = transaction.date,
            accountId = transaction.accountId,
            source = "notification" // Default source
        )
        return pendingTransactionDao.insert(entity)
    }

    override suspend fun deletePendingTransaction(id: Long) {
        pendingTransactionDao.deleteById(id)
    }

    override suspend fun confirmPendingTransaction(pendingId: Long, confirmedTransaction: Transaction) {
        // Insert the confirmed transaction into the main transactions table
        transactionRepository.insertTransaction(confirmedTransaction)
        // Delete from pending table
        pendingTransactionDao.deleteById(pendingId)
    }
}
