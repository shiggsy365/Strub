package com.example.stremiompvplayer.models

import java.io.Serializable

/**
 * Represents a row configuration for Movies or Series pages.
 * Used to define what content rows appear on each page.
 */
data class PageRowConfig(
    val id: String,              // unique ID
    val label: String,           // "Trending", "Popular", etc.
    val sourceType: RowSourceType,
    val isProtected: Boolean = false,  // cannot be deleted
    val order: Int = 0           // display order
) : Serializable

/**
 * Enum representing different source types for page rows.
 */
enum class RowSourceType {
    TMDB_TRENDING_MOVIES,
    TMDB_LATEST_MOVIES,
    TMDB_POPULAR_MOVIES,
    TMDB_TRENDING_TV,
    TMDB_LATEST_TV,
    TMDB_POPULAR_TV,
    TRAKT_WATCHLIST,
    LOCAL_WATCHLIST,
    GENRES
}

/**
 * Provides default row configurations for Movies and Series pages.
 */
object DefaultPageRowConfigs {
    
    /**
     * Default rows for Movies page.
     * These rows are pre-populated and protected (cannot be deleted).
     */
    fun getDefaultMovieRows(): List<PageRowConfig> = listOf(
        PageRowConfig(
            id = "movies_trending",
            label = "Trending",
            sourceType = RowSourceType.TMDB_TRENDING_MOVIES,
            isProtected = true,
            order = 0
        ),
        PageRowConfig(
            id = "movies_latest",
            label = "Latest",
            sourceType = RowSourceType.TMDB_LATEST_MOVIES,
            isProtected = true,
            order = 1
        ),
        PageRowConfig(
            id = "movies_popular",
            label = "Popular",
            sourceType = RowSourceType.TMDB_POPULAR_MOVIES,
            isProtected = true,
            order = 2
        ),
        PageRowConfig(
            id = "movies_watchlist",
            label = "Watchlist",
            sourceType = RowSourceType.TRAKT_WATCHLIST,
            isProtected = true,
            order = 3
        ),
        PageRowConfig(
            id = "movies_genres",
            label = "Genres",
            sourceType = RowSourceType.GENRES,
            isProtected = true,
            order = 4
        )
    )
    
    /**
     * Default rows for Series page.
     * These rows are pre-populated and protected (cannot be deleted).
     */
    fun getDefaultSeriesRows(): List<PageRowConfig> = listOf(
        PageRowConfig(
            id = "series_trending",
            label = "Trending",
            sourceType = RowSourceType.TMDB_TRENDING_TV,
            isProtected = true,
            order = 0
        ),
        PageRowConfig(
            id = "series_latest",
            label = "Latest",
            sourceType = RowSourceType.TMDB_LATEST_TV,
            isProtected = true,
            order = 1
        ),
        PageRowConfig(
            id = "series_popular",
            label = "Popular",
            sourceType = RowSourceType.TMDB_POPULAR_TV,
            isProtected = true,
            order = 2
        ),
        PageRowConfig(
            id = "series_watchlist",
            label = "Watchlist",
            sourceType = RowSourceType.TRAKT_WATCHLIST,
            isProtected = true,
            order = 3
        ),
        PageRowConfig(
            id = "series_genres",
            label = "Genres",
            sourceType = RowSourceType.GENRES,
            isProtected = true,
            order = 4
        )
    )
}
