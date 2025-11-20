package com.example.stremiompvplayer.network

import com.example.stremiompvplayer.models.TMDBExternalIdsResponse
import com.example.stremiompvplayer.models.TMDBMovieListResponse
import com.example.stremiompvplayer.models.TMDBMultiSearchResponse
import com.example.stremiompvplayer.models.TMDBSeriesDetailResponse
import com.example.stremiompvplayer.models.TMDBSeriesListResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface TMDBApiService {

    // --- EXISTING LISTS ---
    @GET("movie/popular")
    suspend fun getPopularMovies(
        @Header("Authorization") token: String,
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1,
        @Query("primary_release_date.lte") releaseDate: String? = null
    ): TMDBMovieListResponse

    @GET("movie/now_playing")
    suspend fun getLatestMovies(
        @Header("Authorization") token: String,
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1,
        @Query("primary_release_date.lte") releaseDate: String? = null
    ): TMDBMovieListResponse

    @GET("trending/movie/week")
    suspend fun getTrendingMovies(
        @Header("Authorization") token: String,
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1
    ): TMDBMovieListResponse

    // --- TV SHOWS ---
    @GET("tv/popular")
    suspend fun getPopularSeries(
        @Header("Authorization") token: String,
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1,
        @Query("first_air_date.lte") airDate: String? = null
    ): TMDBSeriesListResponse

    @GET("tv/on_the_air")
    suspend fun getLatestSeries(
        @Header("Authorization") token: String,
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1,
        @Query("first_air_date.lte") airDate: String? = null
    ): TMDBSeriesListResponse

    @GET("trending/tv/week")
    suspend fun getTrendingSeries(
        @Header("Authorization") token: String,
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1
    ): TMDBSeriesListResponse

    // --- SEARCH (Ensure these exist!) ---
    @GET("search/multi")
    suspend fun searchMulti(
        @Header("Authorization") token: String,
        @Query("query") query: String,
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1,
        @Query("include_adult") includeAdult: Boolean = false
    ): TMDBMultiSearchResponse

    @GET("search/movie")
    suspend fun searchMovies(
        @Header("Authorization") token: String,
        @Query("query") query: String,
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1,
        @Query("include_adult") includeAdult: Boolean = false
    ): TMDBMovieListResponse

    @GET("search/tv")
    suspend fun searchSeries(
        @Header("Authorization") token: String,
        @Query("query") query: String,
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1,
        @Query("include_adult") includeAdult: Boolean = false
    ): TMDBSeriesListResponse

    // --- DETAILS ---
    @GET("tv/{series_id}")
    suspend fun getSeriesDetail(
        @Path("series_id") seriesId: Int,
        @Header("Authorization") token: String,
        @Query("language") language: String = "en-US",
        @Query("append_to_response") appendToResponse: String = "external_ids"
    ): TMDBSeriesDetailResponse

    @GET("movie/{movie_id}/external_ids")
    suspend fun getMovieExternalIds(
        @Path("movie_id") movieId: Int,
        @Header("Authorization") token: String
    ): TMDBExternalIdsResponse

    @GET("tv/{tv_id}/external_ids")
    suspend fun getTVExternalIds(
        @Path("tv_id") tvId: Int,
        @Header("Authorization") token: String
    ): TMDBExternalIdsResponse
}