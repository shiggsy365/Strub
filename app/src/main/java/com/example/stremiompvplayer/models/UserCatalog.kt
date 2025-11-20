package com.example.stremiompvplayer.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

/**
 * Represents a user's saved catalog configuration
 */
@Entity(tableName = "user_catalogs")
data class UserCatalog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val userId: String, // Which user this belongs to

    val catalogId: String, // Original catalog ID from manifest
    val catalogType: String, // "movie" or "series"
    val catalogName: String, // Original name from manifest

    val customName: String?, // User's custom name (if renamed)
    val displayOrder: Int, // Position in the list (for reordering)

    val pageType: String, // "movies" or "series" - which page it's shown on

    val addonUrl: String, // Which addon this catalog comes from
    val manifestId: String, // Manifest ID to track if catalog still exists

    // NEW FIELDS for Toggle Switches
    val showInDiscover: Boolean = true,
    val showInUser: Boolean = true,

    val dateAdded: Long = System.currentTimeMillis()
) : Serializable {
    // Display name: use custom name if set, otherwise original name
    val displayName: String
        get() = customName ?: catalogName
}
// ... (CollectedItem and LibrarySortOption remain unchanged)
/**
 * Represents a collected/saved item in user's library
 */
@Entity(tableName = "collected_items")
data class CollectedItem(
    @PrimaryKey
    val id: String, // Format: "{userId}_{itemId}" for uniqueness

    val userId: String,
    val itemId: String, // IMDB ID or other identifier
    val itemType: String, // "movie" or "series"

    val name: String,
    val poster: String?,
    val background: String?,
    val description: String?,

    val collectedDate: Long = System.currentTimeMillis(),

    // Optional metadata
    val year: String? = null,
    val genres: String? = null, // JSON array stored as string
    val rating: String? = null
) : Serializable {
    companion object {
        fun fromMetaItem(userId: String, metaItem: MetaItem): CollectedItem {
            return CollectedItem(
                id = "${userId}_${metaItem.id}",
                userId = userId,
                itemId = metaItem.id,
                itemType = metaItem.type,
                name = metaItem.name,
                poster = metaItem.poster,
                background = metaItem.background,
                description = metaItem.description
            )
        }
    }
}

/**
 * Sort options for library
 */
enum class LibrarySortOption {
    DATE_ADDED_DESC,
    DATE_ADDED_ASC,
    NAME_ASC,
    NAME_DESC
}