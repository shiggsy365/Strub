package com.example.stremiompvplayer.models

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface ChannelGroupDao {
    @Query("SELECT * FROM channel_groups WHERE userId = :userId AND tvGuideSource = :source ORDER BY displayOrder ASC")
    suspend fun getGroupsForUser(userId: String, source: String): List<ChannelGroup>

    @Query("SELECT * FROM channel_groups WHERE userId = :userId AND tvGuideSource = :source AND isHidden = 0 ORDER BY displayOrder ASC")
    suspend fun getVisibleGroupsForUser(userId: String, source: String): List<ChannelGroup>

    @Query("SELECT * FROM channel_groups WHERE id = :id")
    suspend fun getGroupById(id: Long): ChannelGroup?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(group: ChannelGroup): Long

    @Update
    suspend fun update(group: ChannelGroup)

    @Delete
    suspend fun delete(group: ChannelGroup)

    @Query("DELETE FROM channel_groups WHERE userId = :userId AND tvGuideSource != :currentSource")
    suspend fun deleteOldSources(userId: String, currentSource: String)

    @Query("DELETE FROM channel_groups WHERE userId = :userId")
    suspend fun deleteByUser(userId: String)
}
