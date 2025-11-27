package com.example.stremiompvplayer.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

object TraktClient {
    private const val BASE_URL = "https://api.trakt.tv/"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val api: TraktApi = retrofit.create(TraktApi::class.java)
}

interface TraktApi {
    // --- AUTH ---
    @POST("oauth/device/code")
    suspend fun getDeviceCode(@Body body: Map<String, String>): TraktDeviceCodeResponse

    @POST("oauth/device/token")
    suspend fun getDeviceToken(@Body body: Map<String, String>): TraktTokenResponse

    // --- SYNC & HISTORY ---
    @GET("sync/watched/movies")
    suspend fun getWatchedMovies(
        @Header("Authorization") token: String,
        @Header("trakt-api-key") clientId: String
    ): List<TraktWatchedItem>

    @GET("sync/watched/shows")
    suspend fun getWatchedShows(
        @Header("Authorization") token: String,
        @Header("trakt-api-key") clientId: String
    ): List<TraktWatchedItem>

    // --- PLAYBACK (CONTINUE WATCHING) ---
    @GET("sync/playback/movies")
    suspend fun getPausedMovies(
        @Header("Authorization") token: String,
        @Header("trakt-api-key") clientId: String,
        @Query("limit") limit: Int = 20
    ): List<TraktPlaybackItem>
    // NEW: Remove from Collection

    @POST("sync/collection/remove")
    suspend fun removeFromCollection(
        @Header("Authorization") token: String,
        @Header("trakt-api-key") clientId: String,
        @Body body: TraktHistoryBody
    ): TraktSyncResponse

    @GET("sync/playback/episodes")
    suspend fun getPausedEpisodes(
        @Header("Authorization") token: String,
        @Header("trakt-api-key") clientId: String,
        @Query("limit") limit: Int = 20
    ): List<TraktPlaybackItem>

    // --- COLLECTIONS & LISTS ---
    @GET("sync/collection/movies")
    suspend fun getMovieCollection(
        @Header("Authorization") token: String,
        @Header("trakt-api-key") clientId: String
    ): List<TraktCollectionItem>

    @GET("sync/collection/shows")
    suspend fun getShowCollection(
        @Header("Authorization") token: String,
        @Header("trakt-api-key") clientId: String
    ): List<TraktCollectionItem>

    @GET("sync/watchlist")
    suspend fun getWatchlist(
        @Header("Authorization") token: String,
        @Header("trakt-api-key") clientId: String,
        @Query("type") type: String? = null
    ): List<TraktListItem>

    // --- USER LISTS ---
    @GET("users/{username}/lists/{list_id}/items")
    suspend fun getUserListItems(
        @Header("trakt-api-key") clientId: String,
        @retrofit2.http.Path("username") username: String,
        @retrofit2.http.Path("list_id") listId: String
    ): List<TraktListItem>

    // --- ACTIONS ---
    @POST("sync/history")
    suspend fun addToHistory(
        @Header("Authorization") token: String,
        @Header("trakt-api-key") clientId: String,
        @Body body: TraktHistoryBody
    ): TraktSyncResponse

    @POST("sync/history/remove")
    suspend fun removeFromHistory(
        @Header("Authorization") token: String,
        @Header("trakt-api-key") clientId: String,
        @Body body: TraktHistoryBody
    ): TraktSyncResponse

    @retrofit2.http.DELETE("sync/playback/{id}")
    suspend fun removePlaybackProgress(
        @Header("Authorization") token: String,
        @Header("trakt-api-key") clientId: String,
        @retrofit2.http.Path("id") playbackId: Long
    )

    // --- COLLECTION MANAGEMENT ---
    @POST("sync/collection")
    suspend fun addToCollection(
        @Header("Authorization") token: String,
        @Header("trakt-api-key") clientId: String,
        @Body body: TraktHistoryBody
    ): TraktSyncResponse

    // --- WATCHLIST MANAGEMENT ---
    @POST("sync/watchlist")
    suspend fun addToWatchlist(
        @Header("Authorization") token: String,
        @Header("trakt-api-key") clientId: String,
        @Body body: TraktHistoryBody
    ): TraktSyncResponse

    @POST("sync/watchlist/remove")
    suspend fun removeFromWatchlist(
        @Header("Authorization") token: String,
        @Header("trakt-api-key") clientId: String,
        @Body body: TraktHistoryBody
    ): TraktSyncResponse

    // --- RATINGS ---
    @POST("sync/ratings")
    suspend fun addRatings(
        @Header("Authorization") token: String,
        @Header("trakt-api-key") clientId: String,
        @Body body: TraktRatingBody
    ): TraktSyncResponse

    @POST("sync/ratings/remove")
    suspend fun removeRatings(
        @Header("Authorization") token: String,
        @Header("trakt-api-key") clientId: String,
        @Body body: TraktHistoryBody
    ): TraktSyncResponse

    // --- CUSTOM LISTS ---
    @GET("users/{username}/lists")
    suspend fun getUserLists(
        @Header("Authorization") token: String,
        @Header("trakt-api-key") clientId: String,
        @retrofit2.http.Path("username") username: String
    ): List<TraktList>

    @POST("users/{username}/lists")
    suspend fun createList(
        @Header("Authorization") token: String,
        @Header("trakt-api-key") clientId: String,
        @retrofit2.http.Path("username") username: String,
        @Body body: TraktCreateListBody
    ): TraktList

    @POST("users/{username}/lists/{list_id}/items")
    suspend fun addItemsToList(
        @Header("Authorization") token: String,
        @Header("trakt-api-key") clientId: String,
        @retrofit2.http.Path("username") username: String,
        @retrofit2.http.Path("list_id") listId: String,
        @Body body: TraktHistoryBody
    ): TraktSyncResponse

    @POST("users/{username}/lists/{list_id}/items/remove")
    suspend fun removeItemsFromList(
        @Header("Authorization") token: String,
        @Header("trakt-api-key") clientId: String,
        @retrofit2.http.Path("username") username: String,
        @retrofit2.http.Path("list_id") listId: String,
        @Body body: TraktHistoryBody
    ): TraktSyncResponse

    // --- DISCOVER ---
    @GET("movies/popular")
    suspend fun getPopularMovies(@Header("trakt-api-key") clientId: String): List<TraktMovie>
    @GET("shows/popular")
    suspend fun getPopularShows(@Header("trakt-api-key") clientId: String): List<TraktShow>
    @GET("movies/trending")
    suspend fun getTrendingMovies(@Header("trakt-api-key") clientId: String): List<TraktTrendingItem>
    @GET("shows/trending")
    suspend fun getTrendingShows(@Header("trakt-api-key") clientId: String): List<TraktTrendingItem>

    // --- SCROBBLE ---
    @POST("scrobble/start")
    suspend fun startScrobble(
        @Header("Authorization") token: String,
        @Header("trakt-api-key") clientId: String,
        @Body body: TraktScrobbleBody
    ): TraktScrobbleResponse

    @POST("scrobble/pause")
    suspend fun pauseScrobble(
        @Header("Authorization") token: String,
        @Header("trakt-api-key") clientId: String,
        @Body body: TraktScrobbleBody
    ): TraktScrobbleResponse

    @POST("scrobble/stop")
    suspend fun stopScrobble(
        @Header("Authorization") token: String,
        @Header("trakt-api-key") clientId: String,
        @Body body: TraktScrobbleBody
    ): TraktScrobbleResponse
}

// --- Data Models ---
data class TraktDeviceCodeResponse(val device_code: String, val user_code: String, val verification_url: String, val expires_in: Int, val interval: Int)
data class TraktTokenResponse(val access_token: String, val refresh_token: String)
data class TraktWatchedItem(val last_watched_at: String?, val plays: Int, val movie: TraktMovie?, val show: TraktShow?, val seasons: List<TraktSeason>?)
data class TraktCollectionItem(val last_collected_at: String?, val movie: TraktMovie?, val show: TraktShow?)
data class TraktListItem(val type: String, val movie: TraktMovie?, val show: TraktShow?)
data class TraktTrendingItem(val watchers: Int, val movie: TraktMovie?, val show: TraktShow?)

// NEW: Playback (Paused) Item
data class TraktPlaybackItem(
    val progress: Float?,
    val paused_at: String?,
    val id: Long?,
    val type: String?,
    val movie: TraktMovie?,
    val show: TraktShow?,
    val episode: TraktEpisode?
)

data class TraktMovie(val title: String, val year: Int?, val ids: TraktIds)

// [FIX] Added seasons field to TraktShow to allow nested sync history
data class TraktShow(
    val title: String,
    val year: Int?,
    val ids: TraktIds,
    val seasons: List<TraktSeason>? = null
)

data class TraktSeason(val number: Int, val episodes: List<TraktEpisode>)
data class TraktEpisode(val season: Int = 1, val number: Int, val plays: Int = 0, val last_watched_at: String? = null, val ids: TraktIds? = null)
data class TraktIds(val trakt: Int, val tmdb: Int?, val imdb: String?, val slug: String?)

data class TraktHistoryBody(val movies: List<TraktMovie>? = null, val shows: List<TraktShow>? = null, val episodes: List<TraktEpisode>? = null)
data class TraktSyncResponse(val added: TraktSyncStats?, val deleted: TraktSyncStats?, val not_found: TraktSyncNotFound?)
data class TraktSyncStats(val movies: Int, val episodes: Int)
data class TraktSyncNotFound(val movies: List<TraktIds>?, val shows: List<TraktIds>?, val episodes: List<TraktIds>?)

data class TraktScrobbleBody(val progress: Float, val movie: TraktMovie? = null, val show: TraktShow? = null, val episode: TraktEpisode? = null)
data class TraktScrobbleResponse(val action: String?, val progress: Float?)

// --- RATINGS ---
data class TraktRatingBody(
    val movies: List<TraktMovieRating>? = null,
    val shows: List<TraktShowRating>? = null,
    val episodes: List<TraktEpisodeRating>? = null
)

data class TraktMovieRating(val rating: Int, val rated_at: String? = null, val ids: TraktIds)
data class TraktShowRating(val rating: Int, val rated_at: String? = null, val ids: TraktIds)
data class TraktEpisodeRating(val rating: Int, val rated_at: String? = null, val ids: TraktIds)

// --- CUSTOM LISTS ---
data class TraktList(
    val name: String,
    val description: String?,
    val privacy: String, // "private", "friends", "public"
    val display_numbers: Boolean,
    val allow_comments: Boolean,
    val sort_by: String, // "rank", "added", "title", "released", "runtime", "popularity", "percentage", "votes"
    val sort_how: String, // "asc", "desc"
    val created_at: String,
    val updated_at: String,
    val item_count: Int,
    val comment_count: Int,
    val likes: Int,
    val ids: TraktListIds,
    val user: TraktUser?
)

data class TraktListIds(val trakt: Int, val slug: String)
data class TraktUser(val username: String, val private: Boolean, val name: String?, val vip: Boolean?, val vip_ep: Boolean?)

data class TraktCreateListBody(
    val name: String,
    val description: String? = null,
    val privacy: String = "private",
    val display_numbers: Boolean = false,
    val allow_comments: Boolean = true,
    val sort_by: String = "rank",
    val sort_how: String = "asc"
)