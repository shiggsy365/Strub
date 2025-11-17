package com.example.stremiompvplayer.models

data class FeedList(
    val id: String,
    val userId: String,
    val name: String,
    val catalogUrl: String,
    val type: String, // "movie" or "series"
    val catalogId: String,
    var orderIndex: Int,
    var addedToMovieHub: Boolean = false,
    var addedToSeriesHub: Boolean = false
)

data class HubSlot(
    val id: String,
    val userId: String,
    val hubType: String, // "movie" or "series"
    val slotIndex: Int, // 0-9
    var feedListId: String? = null
)