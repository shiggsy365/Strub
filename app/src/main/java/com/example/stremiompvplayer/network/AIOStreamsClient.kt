package com.example.stremiompvplayer.network

import com.example.stremiompvplayer.models.Stream
import com.example.stremiompvplayer.models.SubtitleResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Url
import java.util.concurrent.TimeUnit

// Response wrapper for new AIOStreams API
data class AIOStreamsResponse(
    val streams: List<Stream>
)

interface AIOStreamsApiService {

    @GET
    suspend fun getStreams(@Url url: String): AIOStreamsResponse

    @GET
    suspend fun getSubtitles(@Url url: String): SubtitleResponse
}

object AIOStreamsClient {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    // OPTIMIZATION: Singleton Client
    // We use 'by lazy' to create this once and reuse it for the app's lifetime.
    // This keeps the connection pool alive (SSL handshakes are cached).
    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            // Optional: Add logging interceptor here if needed for debug
            .build()
    }

    // OPTIMIZATION: Singleton Retrofit instance
    // Uses a dummy base URL because we override it with @Url in the interface
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://dummy.base/") 
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    // Cached API implementation
    private val api: AIOStreamsApiService by lazy {
        retrofit.create(AIOStreamsApiService::class.java)
    }

    /**
     * Extract base URL from manifest URL
     */
    private fun extractBaseUrl(manifestUrl: String): String {
        return manifestUrl.removeSuffix("/manifest.json").removeSuffix("manifest.json")
    }

    /**
     * Build stream URL for movies
     */
    fun buildMovieStreamUrl(manifestUrl: String, imdbId: String): String {
        val baseUrl = extractBaseUrl(manifestUrl)
        return "$baseUrl/stream/movie/$imdbId.json"
    }

    /**
     * Build stream URL for series
     */
    fun buildSeriesStreamUrl(manifestUrl: String, imdbId: String, season: Int, episode: Int): String {
        val baseUrl = extractBaseUrl(manifestUrl)
        return "$baseUrl/stream/series/$imdbId:$season:$episode.json"
    }

    /**
     * Build subtitle URL for movies
     */
    fun buildMovieSubtitleUrl(manifestUrl: String, imdbId: String): String {
        val baseUrl = extractBaseUrl(manifestUrl)
        return "$baseUrl/subtitles/movie/$imdbId.json"
    }

    /**
     * Build subtitle URL for series
     */
    fun buildSeriesSubtitleUrl(manifestUrl: String, imdbId: String, season: Int, episode: Int): String {
        val baseUrl = extractBaseUrl(manifestUrl)
        return "$baseUrl/subtitles/series/$imdbId:$season:$episode.json"
    }

    // Simply return the cached singleton instance
    fun getApi(manifestUrl: String): AIOStreamsApiService {
        return api
    }
}
