package com.example.stremiompvplayer.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "channel_mappings")
data class ChannelMapping(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val channelId: String,  // Original channel ID from M3U
    val channelName: String,  // Original channel name
    val customName: String?,  // User's custom name
    val groupId: Long,  // References ChannelGroup.id
    val tvgId: String?,  // Link to TV guide channel ID
    val isHidden: Boolean = false,
    val tvGuideSource: String  // Hash of M3U+EPG URLs to track source changes
)
