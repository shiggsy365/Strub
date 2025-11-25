package com.example.stremiompvplayer.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "channel_groups")
data class ChannelGroup(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val name: String,
    val originalName: String,  // Original name from M3U
    val displayOrder: Int,
    val isHidden: Boolean = false,
    val tvGuideSource: String  // Hash of M3U+EPG URLs to track source changes
)
