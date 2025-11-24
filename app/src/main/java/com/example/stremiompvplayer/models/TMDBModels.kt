package com.example.stremiompvplayer.models

import com.squareup.moshi.Json
import java.io.Serializable


// --- MOVIE CREDITS (Standard) ---
data class TMDBCreditsResponse(
    val cast: List<TMDBCast>,
    val crew: List<TMDBCrew>
)

data class TMDBCast(
    val id: Int,
    val name: String,
    val character: String?,
    val profile_path: String?
)

data class TMDBCrew(
    val id: Int,
    val name: String,
    val job: String
)

// --- TV CREDITS (Aggregate) ---
data class TMDBAggregateCreditsResponse(
    val cast: List<TMDBAggregateCast>,
    val crew: List<TMDBAggregateCrew>
)

data class TMDBAggregateCast(
    val id: Int,
    val name: String,
    val profile_path: String?,
    val roles: List<TMDBRole>?
)

data class TMDBRole(
    val character: String,
    val episode_count: Int
)

data class TMDBAggregateCrew(
    val id: Int,
    val name: String,
    val jobs: List<TMDBJob>?
)

data class TMDBJob(
    val job: String,
    val episode_count: Int
)

// --- SEARCH PERSON ---
data class TMDBPersonListResponse(
    val page: Int,
    val results: List<TMDBPerson>,
    val total_pages: Int,
    val total_results: Int
)

data class TMDBPerson(
    val id: Int,
    val name: String,
    val profile_path: String?,
    val known_for_department: String?
) {
    fun toMetaItem(): MetaItem {
        return MetaItem(
            id = "tmdb:$id",
            type = "person",
            name = name,
            poster = if (profile_path != null) "https://image.tmdb.org/t/p/w500$profile_path" else null,
            background = null,
            description = known_for_department ?: "Person"
        )
    }
}

// --- TV DETAILS ---
data class TMDBTVDetails(
    val id: Int,
    val name: String,
    val overview: String?,
    val poster_path: String?,
    val backdrop_path: String?,
    val seasons: List<TMDBSeason>?
)

data class TMDBSeason(
    val id: Int,
    val name: String,
    val season_number: Int,
    val episode_count: Int,
    val poster_path: String?
)

data class TMDBSeasonDetails(
    val _id: String,
    val air_date: String?,
    val episodes: List<TMDBEpisode>
)

data class TMDBEpisode(
    val id: Int,
    val name: String,
    val episode_number: Int,
    val season_number: Int,
    val still_path: String?,
    val overview: String?,
    @Json(name = "air_date") val airDate: String? // <--- ADD THIS LINE
)

// --- TV CREDITS (Aggregate) ---


data class TMDBImagesResponse(
    val logos: List<TMDBImage>
)

data class TMDBMovie(
    val id: Int,
    val title: String,
    val poster_path: String?,
    val backdrop_path: String?,
    val overview: String?,
    val release_date: String?,
    val vote_average: Double?,
    val genre_ids: List<Int>? = null
) {
    fun toMetaItem(): MetaItem {
        return MetaItem(
            id = "tmdb:$id",
            type = "movie",
            name = title,
            poster = if (poster_path != null) "https://image.tmdb.org/t/p/w500$poster_path" else null,
            background = if (backdrop_path != null) "https://image.tmdb.org/t/p/original$backdrop_path" else null,
            description = overview,
            // MAP NEW FIELDS
            releaseDate = release_date,
            rating = vote_average?.let { String.format("%.1f", it) }
        )
    }
}

data class TMDBPaginatedResponse(
    val page: Int,
    val total_pages: Int,
    val total_results: Int,
    val results: List<TMDBMultiSearchResult>
)

// --- MOVIE LIST RESPONSE (UPDATED) ---
data class TMDBMovieListResponse(
    val page: Int,
    val results: List<TMDBMovie>,
    val total_pages: Int, // ADDED
    val total_results: Int // ADDED
)

// --- SERIES LIST RESPONSE (UPDATED) ---
data class TMDBSeriesListResponse(
    val page: Int,
    val results: List<TMDBSeries>,
    val total_pages: Int, // ADDED
    val total_results: Int // ADDED
)

data class TMDBSeries(
    val id: Int,
    val name: String,
    val poster_path: String?,
    val backdrop_path: String?,
    val overview: String?,
    val first_air_date: String?,
    val vote_average: Double?,
    val genre_ids: List<Int>? = null
) {
    fun toMetaItem(): MetaItem {
        return MetaItem(
            id = "tmdb:$id",
            type = "series",
            name = name,
            poster = if (poster_path != null) "https://image.tmdb.org/t/p/w500$poster_path" else null,
            background = if (backdrop_path != null) "https://image.tmdb.org/t/p/original$backdrop_path" else null,
            description = overview,
            // MAP NEW FIELDS
            releaseDate = first_air_date,
            rating = vote_average?.let { String.format("%.1f", it) }
        )
    }
}

data class TMDBMultiSearchResponse(
    val page: Int,
    val results: List<TMDBMultiSearchResult>
)

data class TMDBMultiSearchResult(
    val id: Int,
    val media_type: String,
    val name: String?,
    val title: String?,
    val poster_path: String?,
    val backdrop_path: String?,
    val overview: String?,
    val release_date: String?,
    val first_air_date: String?,
    val vote_average: Double?,
    val genre_ids: List<Int>? = null
) {
    val mediaType: String get() = media_type

    fun toMetaItem(): MetaItem {
        val finalTitle = title ?: name ?: "Unknown"
        val date = release_date ?: first_air_date
        return MetaItem(
            id = "tmdb:$id",
            type = media_type,
            name = finalTitle,
            poster = if (poster_path != null) "https://image.tmdb.org/t/p/w500$poster_path" else null,
            background = if (backdrop_path != null) "https://image.tmdb.org/t/p/original$backdrop_path" else null,
            description = overview,
            releaseDate = date,
            rating = vote_average?.let { String.format("%.1f", it) }
        )
    }
}
data class TMDBImage(
    val file_path: String,
    val iso_639_1: String?
)

// ... Series Details ...

data class TMDBSeriesDetailResponse(
    val id: Int,
    val name: String,
    val overview: String,
    @Json(name = "poster_path") val posterPath: String?,
    @Json(name = "backdrop_path") val backdropPath: String?,
    val seasons: List<TMDBSeason>
) : Serializable



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

// UPDATED: Include Crew


// ... Auth & Account Models ...
data class TMDBRequestTokenResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "expires_at") val expiresAt: String,
    @Json(name = "request_token") val requestToken: String
) : Serializable

data class TMDBSessionResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "session_id") val sessionId: String
) : Serializable

data class TMDBAccountDetails(
    val id: Int,
    val username: String,
    @Json(name = "include_adult") val includeAdult: Boolean = false
) : Serializable

data class TMDBWatchlistBody(
    @Json(name = "media_type") val mediaType: String,
    @Json(name = "media_id") val mediaId: Int,
    val watchlist: Boolean
) : Serializable

data class TMDBPostResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "status_code") val statusCode: Int,
    @Json(name = "status_message") val statusMessage: String?
) : Serializable

data class TMDBPersonCreditsResponse(
    val cast: List<TMDBPersonCastItem>,
    val id: Int
) : Serializable

data class TMDBPersonCastItem(
    val id: Int,
    @Json(name = "media_type") val mediaType: String,
    val title: String?,
    val name: String?,
    @Json(name = "poster_path") val posterPath: String?,
    @Json(name = "backdrop_path") val backdropPath: String?,
    val overview: String?,
    @Json(name = "vote_average") val voteAverage: Double?,
    @Json(name = "release_date") val releaseDate: String?,
    @Json(name = "first_air_date") val firstAirDate: String?
) : Serializable {
    fun toMetaItem(): MetaItem {
        return MetaItem(
            id = "tmdb:$id",
            type = if (mediaType == "tv") "series" else "movie",
            name = title ?: name ?: "Unknown",
            poster = posterPath?.let { "https://image.tmdb.org/t/p/w500$it" },
            background = backdropPath?.let { "https://image.tmdb.org/t/p/original$it" },
            description = overview
        )
    }
}

// --- TMDB LIST MODELS ---
data class TMDBListDetailsResponse(
    val id: String,
    val name: String,
    val description: String?,
    @Json(name = "item_count") val itemCount: Int,
    val items: List<TMDBListItem>
) : Serializable

data class TMDBListItem(
    val id: Int,
    @Json(name = "media_type") val mediaType: String,
    val title: String?,
    val name: String?,
    @Json(name = "poster_path") val posterPath: String?,
    @Json(name = "backdrop_path") val backdropPath: String?,
    val overview: String?,
    @Json(name = "release_date") val releaseDate: String?,
    @Json(name = "first_air_date") val firstAirDate: String?,
    @Json(name = "vote_average") val voteAverage: Double?
) : Serializable {
    fun toMetaItem(): MetaItem {
        val type = when (mediaType) {
            "tv" -> "series"
            "movie" -> "movie"
            else -> "movie"
        }
        return MetaItem(
            id = "tmdb:$id",
            type = type,
            name = title ?: name ?: "Unknown",
            poster = posterPath?.let { "https://image.tmdb.org/t/p/w500$it" },
            background = backdropPath?.let { "https://image.tmdb.org/t/p/original$it" },
            description = overview,
            releaseDate = releaseDate ?: firstAirDate,
            rating = voteAverage?.let { String.format("%.1f", it) }
        )
    }
}

// --- VIDEOS (Trailers) ---
data class TMDBVideosResponse(
    val id: Int,
    val results: List<TMDBVideo>
) : Serializable

data class TMDBVideo(
    val id: String,
    @Json(name = "iso_639_1") val language: String,
    @Json(name = "iso_3166_1") val country: String,
    val key: String,
    val name: String,
    val site: String,
    val size: Int,
    val type: String,
    val official: Boolean,
    @Json(name = "published_at") val publishedAt: String?
) : Serializable