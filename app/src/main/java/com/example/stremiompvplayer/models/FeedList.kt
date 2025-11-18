package com.example.stremiompvplayer.models

// NEW: Import Room annotations
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.stremiompvplayer.models.MetaItem

// NEW: Add @Entity annotation
@Entity
data class FeedList(
    // NEW: Add @PrimaryKey annotation
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
    /**
     * NEW: This property is added to hold the catalog content (the list of posters)
     * that we fetch from the add-on in the ViewModel.
     *
     * We mark it @Transient so it doesn't interfere with database operations
     * or JSON serialization if this class is used for that.
     */
    @Transient
    var content: List<MetaItem> = emptyList()
}

// NEW: Add @Entity annotation
@Entity
data class HubSlot(
    // NEW: Add @PrimaryKey annotation
    @PrimaryKey
    val id: String,
    val userId: String,
    val hubType: String, // "movie" or "series"
    val slotIndex: Int, // 0-9
    var feedListId: String? = null
)