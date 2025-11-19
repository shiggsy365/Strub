package com.example.stremiompvplayer.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_catalogs")
data class UserCatalog(
    @PrimaryKey(autoGenerate = true) val dbId: Long = 0,
    val addonId: String,
    val catalogId: String,
    val type: String, // e.g., "movie", "series"
    val name: String, // Display name e.g. "Cinemeta Popular"
    val isDiscoverEnabled: Boolean = true,
    val isUserEnabled: Boolean = true,
    val sortOrder: Int = 0
)
