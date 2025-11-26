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

    private fun createClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private fun createRetrofit(baseUrl: String, client: OkHttpClient): Retrofit {
        // Ensure base URL ends with /
        val normalizedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

        return Retrofit.Builder()
            .baseUrl(normalizedUrl)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    /**
     * Extract base URL from manifest URL
     * Example: https://aiostreams.shiggsy.co.uk/stremio/.../manifest.json
     * Returns: https://aiostreams.shiggsy.co.uk/stremio/...
     */
    private fun extractBaseUrl(manifestUrl: String): String {
        return manifestUrl.removeSuffix("/manifest.json").removeSuffix("manifest.json")
    }

    /**
     * Build stream URL for movies
     * Format: {base_url}/stream/movie/{imdb_id}.json
     */
    fun buildMovieStreamUrl(manifestUrl: String, imdbId: String): String {
        val baseUrl = extractBaseUrl(manifestUrl)
        return "$baseUrl/stream/movie/$imdbId.json"
    }

    /**
     * Build stream URL for series
     * Format: {base_url}/stream/series/{imdb_id}:{season}:{episode}.json
     */
    fun buildSeriesStreamUrl(manifestUrl: String, imdbId: String, season: Int, episode: Int): String {
        val baseUrl = extractBaseUrl(manifestUrl)
        return "$baseUrl/stream/series/$imdbId:$season:$episode.json"
    }

    /**
     * Build subtitle URL for movies
     * Format: {base_url}/subtitles/movie/{imdb_id}.json
     */
    fun buildMovieSubtitleUrl(manifestUrl: String, imdbId: String): String {
        val baseUrl = extractBaseUrl(manifestUrl)
        return "$baseUrl/subtitles/movie/$imdbId.json"
    }

    /**
     * Build subtitle URL for series
     * Format: {base_url}/subtitles/series/{imdb_id}:{season}:{episode}.json
     */
    fun buildSeriesSubtitleUrl(manifestUrl: String, imdbId: String, season: Int, episode: Int): String {
        val baseUrl = extractBaseUrl(manifestUrl)
        return "$baseUrl/subtitles/series/$imdbId:$season:$episode.json"
    }

    fun getApi(manifestUrl: String): AIOStreamsApiService {
        val client = createClient()
        // Use a dummy base URL since we'll be using full URLs with @Url
        val retrofit = createRetrofit("https://dummy.base/", client)
        return retrofit.create(AIOStreamsApiService::class.java)
    }
}
