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

// REMOVED: This stub is no longer needed. We will use MetaItem everywhere.
/*
// Used by adapters
data class MetaPreview(
    val id: String,
    val type: String,
    val name: String,
    val poster: String?
) : Serializable
*/


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