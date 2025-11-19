package com.example.stremiompvplayer.models

import com.squareup.moshi.Json
import java.io.Serializable

// TMDB Movie Response Models
data class TMDBMovieListResponse(
    val page: Int,
    val results: List<TMDBMovie>,
    @Json(name = "total_pages") val totalPages: Int,
    @Json(name = "total_results") val totalResults: Int
) : Serializable

data class TMDBMovie(
    val adult: Boolean,
    @Json(name = "backdrop_path") val backdropPath: String?,
    @Json(name = "genre_ids") val genreIds: List<Int>,
    val id: Int,
    @Json(name = "original_language") val originalLanguage: String,
    @Json(name = "original_title") val originalTitle: String,
    val overview: String,
    val popularity: Double,
    @Json(name = "poster_path") val posterPath: String?,
    @Json(name = "release_date") val releaseDate: String,
    val title: String,
    val video: Boolean,
    @Json(name = "vote_average") val voteAverage: Double,
    @Json(name = "vote_count") val voteCount: Int
) : Serializable {
    // Convert to MetaItem for compatibility with existing code
    fun toMetaItem(): MetaItem {
        return MetaItem(
            id = "tmdb:$id",
            type = "movie",
            name = title,
            poster = posterPath?.let { "https://image.tmdb.org/t/p/w500$it" },
            background = backdropPath?.let { "https://image.tmdb.org/t/p/original$it" },
            description = overview
        )
    }
}

// TMDB Series Response Models
data class TMDBSeriesListResponse(
    val page: Int,
    val results: List<TMDBSeries>,
    @Json(name = "total_pages") val totalPages: Int,
    @Json(name = "total_results") val totalResults: Int
) : Serializable

data class TMDBSeries(
    @Json(name = "backdrop_path") val backdropPath: String?,
    @Json(name = "first_air_date") val firstAirDate: String?,
    @Json(name = "genre_ids") val genreIds: List<Int>,
    val id: Int,
    val name: String,
    @Json(name = "origin_country") val originCountry: List<String>,
    @Json(name = "original_language") val originalLanguage: String,
    @Json(name = "original_name") val originalName: String,
    val overview: String,
    val popularity: Double,
    @Json(name = "poster_path") val posterPath: String?,
    @Json(name = "vote_average") val voteAverage: Double,
    @Json(name = "vote_count") val voteCount: Int
) : Serializable {
    // Convert to MetaItem for compatibility with existing code
    fun toMetaItem(): MetaItem {
        return MetaItem(
            id = "tmdb:$id",
            type = "series",
            name = name,
            poster = posterPath?.let { "https://image.tmdb.org/t/p/w500$it" },
            background = backdropPath?.let { "https://image.tmdb.org/t/p/original$it" },
            description = overview
        )
    }
}

// TMDB Series Detail Response (includes seasons/episodes)
data class TMDBSeriesDetailResponse(
    val id: Int,
    val name: String,
    val overview: String,
    @Json(name = "poster_path") val posterPath: String?,
    @Json(name = "backdrop_path") val backdropPath: String?,
    val seasons: List<TMDBSeason>
) : Serializable

data class TMDBSeason(
    @Json(name = "air_date") val airDate: String?,
    @Json(name = "episode_count") val episodeCount: Int,
    val id: Int,
    val name: String,
    val overview: String,
    @Json(name = "poster_path") val posterPath: String?,
    @Json(name = "season_number") val seasonNumber: Int
) : Serializable

// TMDB Multi Search Response (searches both movies and TV)
data class TMDBMultiSearchResponse(
    val page: Int,
    val results: List<TMDBMultiSearchResult>,
    @Json(name = "total_pages") val totalPages: Int,
    @Json(name = "total_results") val totalResults: Int
) : Serializable

data class TMDBMultiSearchResult(
    val id: Int,
    @Json(name = "media_type") val mediaType: String, // "movie" or "tv"
    @Json(name = "poster_path") val posterPath: String?,
    @Json(name = "backdrop_path") val backdropPath: String?,
    val overview: String,
    val popularity: Double,
    @Json(name = "vote_average") val voteAverage: Double,
    @Json(name = "vote_count") val voteCount: Int,

    // Movie-specific fields
    val title: String?,
    @Json(name = "original_title") val originalTitle: String?,
    @Json(name = "release_date") val releaseDate: String?,

    // TV-specific fields
    val name: String?,
    @Json(name = "original_name") val originalName: String?,
    @Json(name = "first_air_date") val firstAirDate: String?
) : Serializable {
    // Convert to MetaItem for compatibility with existing code
    fun toMetaItem(): MetaItem {
        return MetaItem(
            id = "tmdb:$id",
            type = when(mediaType) {
                "movie" -> "movie"
                "tv" -> "series"
                else -> mediaType
            },
            name = title ?: name ?: "Unknown",
            poster = posterPath?.let { "https://image.tmdb.org/t/p/w500$it" },
            background = backdropPath?.let { "https://image.tmdb.org/t/p/original$it" },
            description = overview
        )
    }
}

// TMDB External IDs Response (for getting IMDB ID)
data class TMDBExternalIdsResponse(
    val id: Int,
    @Json(name = "imdb_id") val imdbId: String?,
    @Json(name = "freebase_mid") val freebaseMid: String?,
    @Json(name = "freebase_id") val freebaseId: String?,
    @Json(name = "tvdb_id") val tvdbId: Int?,
    @Json(name = "tvrage_id") val tvrageId: Int?,
    @Json(name = "wikidata_id") val wikidataId: String?,
    @Json(name = "facebook_id") val facebookId: String?,
    @Json(name = "instagram_id") val instagramId: String?,
    @Json(name = "twitter_id") val twitterId: String?
) : Serializable