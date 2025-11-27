package com.example.stremiompvplayer.models

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a pending Trakt sync action to be executed when online
 */
@Entity(tableName = "pending_sync_actions")
data class PendingSyncAction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val actionType: String, // "ADD_TO_COLLECTION", "REMOVE_FROM_COLLECTION", "ADD_TO_WATCHLIST", "REMOVE_FROM_WATCHLIST", "MARK_WATCHED"
    val itemId: String, // e.g., "tmdb:12345"
    val itemType: String, // "movie" or "series"
    val itemName: String, // For display purposes
    val timestamp: Long = System.currentTimeMillis(),
    val retryCount: Int = 0
)
