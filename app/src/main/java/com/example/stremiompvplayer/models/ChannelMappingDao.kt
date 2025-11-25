package com.example.stremiompvplayer.models

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface ChannelMappingDao {
    @Query("SELECT * FROM channel_mappings WHERE userId = :userId AND tvGuideSource = :source")
    suspend fun getMappingsForUser(userId: String, source: String): List<ChannelMapping>

    @Query("SELECT * FROM channel_mappings WHERE userId = :userId AND groupId = :groupId AND tvGuideSource = :source ORDER BY channelName ASC")
    suspend fun getMappingsByGroup(userId: String, groupId: Long, source: String): List<ChannelMapping>

    @Query("SELECT * FROM channel_mappings WHERE userId = :userId AND groupId = :groupId AND tvGuideSource = :source AND isHidden = 0 ORDER BY channelName ASC")
    suspend fun getVisibleMappingsByGroup(userId: String, groupId: Long, source: String): List<ChannelMapping>

    @Query("SELECT * FROM channel_mappings WHERE userId = :userId AND channelId = :channelId AND tvGuideSource = :source")
    suspend fun getMappingByChannelId(userId: String, channelId: String, source: String): ChannelMapping?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(mapping: ChannelMapping): Long

    @Update
    suspend fun update(mapping: ChannelMapping)

    @Delete
    suspend fun delete(mapping: ChannelMapping)

    @Query("DELETE FROM channel_mappings WHERE userId = :userId AND tvGuideSource != :currentSource")
    suspend fun deleteOldSources(userId: String, currentSource: String)

    @Query("DELETE FROM channel_mappings WHERE userId = :userId")
    suspend fun deleteByUser(userId: String)

    @Query("DELETE FROM channel_mappings WHERE groupId = :groupId")
    suspend fun deleteByGroup(groupId: Long)
}
