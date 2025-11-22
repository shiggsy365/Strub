package com.example.stremiompvplayer.network

import com.example.stremiompvplayer.models.*
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import com.example.stremiompvplayer.models.TMDBCreditsResponse
import com.example.stremiompvplayer.models.TMDBAggregateCreditsResponse

interface TMDBApiService {

    @GET("search/person")
    suspend fun searchPeople(
        @Query("api_key") apiKey: String,
        @Query("query") query: String,
        @Query("page") page: Int = 1,
        @Query("include_adult") includeAdult: Boolean = false,
        @Query("language") language: String = "en-US"
    ): TMDBPersonListResponse

    // CREDITS
    @GET("movie/{id}/credits")
    suspend fun getMovieCredits(
        @Path("id") id: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "en-US"
    ): TMDBCreditsResponse

    @GET("tv/{id}/aggregate_credits")
    suspend fun getTVAggregateCredits(
        @Path("id") id: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "en-US"
    ): TMDBAggregateCreditsResponse

    // DETAILS
    @GET("tv/{id}")
    suspend fun getTVDetails(
        @Path("id") id: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "en-US"
    ): TMDBTVDetails

    @GET("tv/{id}/season/{season_number}")
    suspend fun getTVSeasonDetails(
        @Path("id") id: Int,
        @Path("season_number") seasonNumber: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "en-US"
    ): TMDBSeasonDetails


    // --- AUTHENTICATION ---
    @GET("authentication/token/new")
    suspend fun createRequestToken(
        @Query("api_key") apiKey: String
    ): TMDBRequestTokenResponse

    @GET("tv/{id}/credits")
    suspend fun getTVCredits(
        @Path("id") id: Int,
        @Query("api_key") apiKey: String
    ): TMDBCreditsResponse

    // [FIX] Added include_image_language to filter for English/Neutral logos
    @GET("movie/{id}/images")
    suspend fun getMovieImages(
        @Path("id") id: Int,
        @Query("api_key") apiKey: String,
        @Query("include_image_language") includeImageLanguage: String = "en,null"
    ): TMDBImagesResponse

    // [FIX] Added include_image_language to filter for English/Neutral logos
    @GET("tv/{id}/images")
    suspend fun getTVImages(
        @Path("id") id: Int,
        @Query("api_key") apiKey: String,
        @Query("include_image_language") includeImageLanguage: String = "en,null"
    ): TMDBImagesResponse

    @POST("authentication/session/new")
    suspend fun createSession(
        @Query("api_key") apiKey: String,
        @Body body: Map<String, String> // {"request_token": "..."}
    ): TMDBSessionResponse

    // --- EXISTING LISTS (Updated to use api_key) ---
    @GET("movie/popular")
    suspend fun getPopularMovies(
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1,
        @Query("primary_release_date.lte") releaseDate: String? = null
    ): TMDBMovieListResponse

    @GET("movie/now_playing")
    suspend fun getLatestMovies(
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1,
        @Query("primary_release_date.lte") releaseDate: String? = null
    ): TMDBMovieListResponse

    @GET("trending/movie/week")
    suspend fun getTrendingMovies(
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1
    ): TMDBMovieListResponse

    // --- TV SHOWS (Updated to use api_key) ---
    @GET("tv/popular")
    suspend fun getPopularSeries(
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1,
        @Query("first_air_date.lte") airDate: String? = null
    ): TMDBSeriesListResponse

    @GET("tv/on_the_air")
    suspend fun getLatestSeries(
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1,
        @Query("first_air_date.lte") airDate: String? = null
    ): TMDBSeriesListResponse

    @GET("trending/tv/week")
    suspend fun getTrendingSeries(
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1
    ): TMDBSeriesListResponse

    // --- SEARCH (Updated to use api_key) ---
    @GET("search/multi")
    suspend fun searchMulti(
        @Query("api_key") apiKey: String,
        @Query("query") query: String,
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1,
        @Query("include_adult") includeAdult: Boolean = false
    ): TMDBMultiSearchResponse

    @GET("search/movie")
    suspend fun searchMovies(
        @Query("api_key") apiKey: String,
        @Query("query") query: String,
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1,
        @Query("include_adult") includeAdult: Boolean = false
    ): TMDBMovieListResponse

    @GET("search/tv")
    suspend fun searchSeries(
        @Query("api_key") apiKey: String,
        @Query("query") query: String,
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1,
        @Query("include_adult") includeAdult: Boolean = false
    ): TMDBSeriesListResponse

    // --- DETAILS (Updated to use api_key) ---
    @GET("tv/{series_id}")
    suspend fun getSeriesDetail(
        @Path("series_id") seriesId: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "en-US",
        @Query("append_to_response") appendToResponse: String = "external_ids"
    ): TMDBSeriesDetailResponse

    @GET("movie/{movie_id}/external_ids")
    suspend fun getMovieExternalIds(
        @Path("movie_id") movieId: Int,
        @Query("api_key") apiKey: String
    ): TMDBExternalIdsResponse

    @GET("tv/{tv_id}/external_ids")
    suspend fun getTVExternalIds(
        @Path("tv_id") tvId: Int,
        @Query("api_key") apiKey: String
    ): TMDBExternalIdsResponse

    // --- CREDITS (Updated to use api_key) ---

    @GET("tv/{tv_id}/credits")
    suspend fun getTVCredits(
        @Path("tv_id") tvId: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "en-US"
    ): TMDBCreditsResponse
    @GET("account")
    suspend fun getAccountDetails(
        @Query("api_key") apiKey: String,
        @Query("session_id") sessionId: String
    ): TMDBAccountDetails

    @GET("account/{account_id}/watchlist/movies")
    suspend fun getMovieWatchlist(
        @Path("account_id") accountId: Int,
        @Query("api_key") apiKey: String,
        @Query("session_id") sessionId: String,
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1,
        @Query("sort_by") sortBy: String = "created_at.desc"
    ): TMDBMovieListResponse

    @GET("account/{account_id}/watchlist/tv")
    suspend fun getTVWatchlist(
        @Path("account_id") accountId: Int,
        @Query("api_key") apiKey: String,
        @Query("session_id") sessionId: String,
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1,
        @Query("sort_by") sortBy: String = "created_at.desc"
    ): TMDBSeriesListResponse

    @POST("account/{account_id}/watchlist")
    suspend fun addToWatchlist(
        @Path("account_id") accountId: Int,
        @Query("api_key") apiKey: String,
        @Query("session_id") sessionId: String,
        @Body body: TMDBWatchlistBody
    ): TMDBPostResponse

    // --- PERSON ---
    @GET("person/{person_id}/combined_credits")
    suspend fun getPersonCombinedCredits(
        @Path("person_id") personId: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "en-US"
    ): TMDBPersonCreditsResponse
}