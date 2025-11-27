package com.example.stremiompvplayer.models

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PendingSyncActionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(action: PendingSyncAction): Long

    @Query("SELECT * FROM pending_sync_actions WHERE userId = :userId ORDER BY timestamp ASC")
    suspend fun getPendingActions(userId: String): List<PendingSyncAction>

    @Query("SELECT * FROM pending_sync_actions WHERE userId = :userId ORDER BY timestamp ASC")
    fun getPendingActionsLive(userId: String): LiveData<List<PendingSyncAction>>

    @Query("SELECT COUNT(*) FROM pending_sync_actions WHERE userId = :userId")
    suspend fun getPendingCount(userId: String): Int

    @Query("DELETE FROM pending_sync_actions WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM pending_sync_actions WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)

    @Query("UPDATE pending_sync_actions SET retryCount = retryCount + 1 WHERE id = :id")
    suspend fun incrementRetryCount(id: Long)
}
