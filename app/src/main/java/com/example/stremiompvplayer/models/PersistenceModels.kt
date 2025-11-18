package com.example.stremiompvplayer.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.stremiompvplayer.models.MetaItem // Ensure this is imported from StremioModels.kt
import java.io.Serializable

// All models in this file are used for Room persistence

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

// STUB MODELS FOR ROOM (These were likely in your old UserModels.kt)

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
data class WatchProgress(
    @PrimaryKey
    val id: String
) : Serializable

@Entity
data class NextUpItem(
    @PrimaryKey
    val id: String
) : Serializable