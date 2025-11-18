package com.example.stremiompvplayer.models

// NEW: Import Room annotations
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

//
// NEW FILE: STUB MODELS
// These are placeholder classes to fix "Unresolved reference" errors.
// You MUST replace these with your real model class definitions.
//

// NEW: Add @Entity and @PrimaryKey
@Entity
data class User(
    @PrimaryKey
    val authKey: String,
    val email: String,
    val avatar: String,
    val isGuest: Boolean = false
) : Serializable

// NEW: Add @Entity and @PrimaryKey
@Entity
data class UserSettings(
    @PrimaryKey
    val id: String
) : Serializable

// NEW: Add @Entity and @PrimaryKey
@Entity
data class LibraryItem(
    @PrimaryKey
    val id: String,
    val type: String,
    val name: String,
    val poster: String?
) : Serializable

data class MetaDetail(
    val id: String,
    val type: String,
    val name: String,
    val poster: String?,
    val background: String?,
    val description: String?,
    val videos: List<Video>?
) : Serializable

// Used by adapters
data class MetaPreview(
    val id: String,
    val type: String,
    val name: String,
    val poster: String?
) : Serializable

data class WatchProgress(
    val id: String
) : Serializable

// NEW: Add @Entity and @PrimaryKey
@Entity
data class WatchProgress(
    @PrimaryKey
    val id: String
) : Serializable

// NEW: Add @Entity and @PrimaryKey
@Entity
data class NextUpItem(
    @PrimaryKey
    val id: String
) : Serializable