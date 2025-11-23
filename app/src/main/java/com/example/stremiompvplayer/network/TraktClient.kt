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
data class TraktShow(val title: String, val year: Int?, val ids: TraktIds)
data class TraktSeason(val number: Int, val episodes: List<TraktEpisode>)
data class TraktEpisode(val number: Int, val plays: Int = 0, val last_watched_at: String? = null, val ids: TraktIds? = null)
data class TraktIds(val trakt: Int, val tmdb: Int?, val imdb: String?, val slug: String?)

data class TraktHistoryBody(val movies: List<TraktMovie>? = null, val shows: List<TraktShow>? = null, val episodes: List<TraktEpisode>? = null)
data class TraktSyncResponse(val added: TraktSyncStats?, val deleted: TraktSyncStats?, val not_found: TraktSyncNotFound?)
data class TraktSyncStats(val movies: Int, val episodes: Int)
data class TraktSyncNotFound(val movies: List<TraktIds>?, val shows: List<TraktIds>?, val episodes: List<TraktIds>?)

data class TraktScrobbleBody(val progress: Float, val movie: TraktMovie? = null, val show: TraktShow? = null, val episode: TraktEpisode? = null)
data class TraktScrobbleResponse(val action: String?, val progress: Float?)