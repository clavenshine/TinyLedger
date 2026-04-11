package com.tinyledger.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.tinyledger.app.data.local.entity.PendingTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingTransactionDao {

    @Query("SELECT * FROM pending_transactions ORDER BY date DESC")
    fun getAllPending(): Flow<List<PendingTransactionEntity>>

    @Insert
    suspend fun insert(entity: PendingTransactionEntity): Long

    @Query("DELETE FROM pending_transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM pending_transactions")
    suspend fun deleteAll()
}
