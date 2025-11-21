package com.example.stremiompvplayer.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.stremiompvplayer.models.MetaItem // Ensure this is imported from StremioModels.kt
import java.io.Serializable

// All models in this file are used for Room persistence
@Entity(tableName = "watch_progress", primaryKeys = ["userId", "itemId"])
data class WatchProgress(
    val userId: String,
    val itemId: String, // tmdb:123 or tmdb:123:1:5
    val type: String,   // movie or episode
    val progress: Long, // current position in ms
    val duration: Long, // total duration in ms
    val isWatched: Boolean,
    val lastUpdated: Long = System.currentTimeMillis(),

    // Metadata cache for UI display
    val name: String? = null,
    val poster: String? = null,
    val background: String? = null,

    // For Series/Episode logic
    val parentId: String? = null, // tmdb:123 (for grouping episodes)
    val season: Int? = null,
    val episode: Int? = null
) : Serializable
@Entity
data class FeedList(
    @PrimaryKey
    val id: String,
    val userId: String,
    val name: String,
    val catalogUrl: String,
    val type: String, // "movie" or "series"
    val catalogId: String,
    var orderIndex: Int,
    var addedToMovieHub: Boolean = false,
    var addedToSeriesHub: Boolean = false
) {
    // Keep this field marked @Transient for database ignoring
    @Transient
    var content: List<MetaItem> = emptyList()
}

@Entity
data class HubSlot(
    @PrimaryKey
    val id: String,
    val userId: String,
    val hubType: String, // "movie" or "series"
    val slotIndex: Int, // 0-9
    var feedListId: String? = null
)

@Entity
data class User(
    @PrimaryKey // Using authKey for primary key
    val authKey: String,
    val email: String,
    val avatar: String,
    val isGuest: Boolean = false
) : Serializable

@Entity
data class UserSettings(
    @PrimaryKey // Ensure UserSettings is linked to a user or has a fixed ID
    val id: String
) : Serializable

@Entity
data class LibraryItem(
    @PrimaryKey
    val id: String,
    val type: String,
    val name: String,
    val poster: String?
) : Serializable

@Entity
data class NextUpItem(
    @PrimaryKey
    val id: String
) : Serializable