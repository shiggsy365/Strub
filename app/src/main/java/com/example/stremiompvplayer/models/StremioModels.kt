package com.example.stremiompvplayer.models

import com.google.gson.annotations.SerializedName

// User Management
data class User(
    val id: String,
    val name: String,
    val avatarColor: Int,
    val createdAt: Long = System.currentTimeMillis(),
    val lastActive: Long = System.currentTimeMillis(),
    val addonUrls: List<String> = emptyList()
)

// Library Items
data class LibraryItem(
    val id: String,
    val userId: String,
    val metaId: String,
    val type: String, // movie, series
    val name: String,
    val poster: String?,
    val background: String?,
    val addedAt: Long = System.currentTimeMillis(),
    val genres: List<String>? = null
)

// Watch Progress
data class WatchProgress(
    val id: String,
    val userId: String,
    val metaId: String,
    val type: String,
    val videoId: String?, // For series episodes
    val position: Int, // Current position in seconds
    val duration: Int, // Total duration in seconds
    val completed: Boolean = false,
    val lastWatched: Long = System.currentTimeMillis(),
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null
)

// Next Up Item (for the "Continue Watching" section)
data class NextUpItem(
    val metaId: String,
    val type: String,
    val name: String,
    val poster: String?,
    val background: String?,
    val videoId: String?, // Next episode ID for series
    val videoTitle: String?, // Next episode title
    val seasonNumber: Int?,
    val episodeNumber: Int?,
    val progress: Float, // 0.0 to 1.0
    val lastWatched: Long,
    val thumbnail: String? = null
)

// User Settings
data class UserSettings(
    val userId: String,
    val autoPlayFirstStream: Boolean = false,
    val subtitlesEnabled: Boolean = true,
    val subtitleSize: Int = 20, // Font size in SP
    val subtitleColor: String = "#FFFFFF", // Hex color
    val subtitleBackgroundEnabled: Boolean = true,
    val subtitleBackgroundColor: String = "#80000000", // Semi-transparent black
    val theme: String = "dark",
    val defaultQuality: String = "auto"
)

// Enhanced Meta with watch progress
data class MetaWithProgress(
    val meta: MetaDetail,
    val watchProgress: WatchProgress?,
    val inLibrary: Boolean = false,
    val nextEpisode: Video? = null
)

// Stremio Models
data class AddonManifest(
    val id: String,
    val version: String,
    val name: String,
    val description: String,
    val resources: List<String>,
    val types: List<String>,
    val catalogs: List<Catalog>? = null,
    val idPrefixes: List<String>? = null
)

data class Catalog(
    val type: String,
    val id: String,
    val name: String,
    val extra: List<Extra>? = null
)

data class Extra(
    val name: String,
    val isRequired: Boolean = false,
    val options: List<String>? = null,
    val optionsLimit: Int? = null
)

data class MetaPreview(
    val id: String,
    val type: String,
    val name: String,
    val poster: String? = null,
    val background: String? = null,
    val logo: String? = null,
    val description: String? = null,
    val releaseInfo: String? = null,
    val genres: List<String>? = null
)

data class MetaDetail(
    val id: String,
    val type: String,
    val name: String,
    val poster: String? = null,
    val background: String? = null,
    val logo: String? = null,
    val description: String? = null,
    val releaseInfo: String? = null,
    val runtime: String? = null,
    val genres: List<String>? = null,
    val imdbRating: String? = null,
    val director: List<String>? = null,
    val cast: List<String>? = null,
    val videos: List<Video>? = null
)

data class Video(
    val id: String,
    val title: String,
    val released: String? = null,
    val season: Int? = null,
    val episode: Int? = null,
    val thumbnail: String? = null,
    val overview: String? = null
)

data class Stream(
    val name: String? = null,
    val title: String? = null,
    val url: String? = null,
    val ytId: String? = null,
    val infoHash: String? = null,
    val fileIdx: Int? = null,
    val externalUrl: String? = null,
    val behaviorHints: BehaviorHints? = null,
    val subtitles: List<Subtitle>? = null
)

data class Subtitle(
    val id: String,
    val url: String,
    val lang: String,
    val label: String? = null
)

data class BehaviorHints(
    val notWebReady: Boolean? = null,
    val bingeGroup: String? = null,
    val countryWhitelist: List<String>? = null
)

data class StreamsResponse(
    val streams: List<Stream>
)

data class CatalogResponse(
    val metas: List<MetaPreview>
)
