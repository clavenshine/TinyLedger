package com.tinyledger.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tinyledger.app.data.local.entity.NotificationSmsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationSmsDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: NotificationSmsEntity)

    @Query("SELECT * FROM notification_sms WHERE date >= :afterTime ORDER BY date DESC")
    suspend fun getAfter(afterTime: Long): List<NotificationSmsEntity>

    @Query("SELECT * FROM notification_sms ORDER BY date DESC")
    suspend fun getAll(): List<NotificationSmsEntity>

    @Query("SELECT COUNT(*) FROM notification_sms")
    suspend fun count(): Int

    @Query("DELETE FROM notification_sms WHERE date < :beforeTime")
    suspend fun deleteBefore(beforeTime: Long)
}
