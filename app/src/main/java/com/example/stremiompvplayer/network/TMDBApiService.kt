package com.example.stremiompvplayer.network

import com.example.stremiompvplayer.models.TMDBExternalIdsResponse
import com.example.stremiompvplayer.models.TMDBMovieListResponse
import com.example.stremiompvplayer.models.TMDBMultiSearchResponse
import com.example.stremiompvplayer.models.TMDBSeriesListResponse
import com.example.stremiompvplayer.models.TMDBSeriesDetailResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface TMDBApiService {

    // Most Popular Movies
    @GET("discover/movie")
    suspend fun getPopularMovies(
        @Header("Authorization") authorization: String,
        @Query("include_adult") includeAdult: Boolean = false,
        @Query("include_video") includeVideo: Boolean = false,
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1,
        @Query("release_date.lte") releaseDate: String,
        @Query("vote_count.gte") voteCount: Int = 300,
        @Query("sort_by") sortBy: String = "popularity.desc"
    ): TMDBMovieListResponse

    // Latest Release Movies
    @GET("discover/movie")
    suspend fun getLatestMovies(
        @Header("Authorization") authorization: String,
        @Query("include_adult") includeAdult: Boolean = false,
        @Query("include_video") includeVideo: Boolean = false,
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1,
        @Query("release_date.lte") releaseDate: String,
        @Query("vote_count.gte") voteCount: Int = 50,
        @Query("sort_by") sortBy: String = "primary_release_date.desc"
    ): TMDBMovieListResponse

    // Trending Movies
    @GET("trending/movie/week")
    suspend fun getTrendingMovies(
        @Header("Authorization") authorization: String,
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1
    ): TMDBMovieListResponse

    // Most Popular Series
    @GET("discover/tv")
    suspend fun getPopularSeries(
        @Header("Authorization") authorization: String,
        @Query("include_adult") includeAdult: Boolean = false,
        @Query("include_video") includeVideo: Boolean = false,
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1,
        @Query("air_date.lte") airDate: String,
        @Query("vote_count.gte") voteCount: Int = 100,
        @Query("sort_by") sortBy: String = "popularity.desc"
    ): TMDBSeriesListResponse

    // Latest Release Series
    @GET("discover/tv")
    suspend fun getLatestSeries(
        @Header("Authorization") authorization: String,
        @Query("include_adult") includeAdult: Boolean = false,
        @Query("include_video") includeVideo: Boolean = false,
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1,
        @Query("air_date.lte") airDate: String,
        @Query("vote_count.gte") voteCount: Int = 10,
        @Query("sort_by") sortBy: String = "primary_release_date.desc"
    ): TMDBSeriesListResponse

    // Trending Series
    @GET("trending/tv/week")
    suspend fun getTrendingSeries(
        @Header("Authorization") authorization: String,
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1
    ): TMDBSeriesListResponse

    // Get Series Detail (for episodes)
    @GET("tv/{series_id}")
    suspend fun getSeriesDetail(
        @Path("series_id") seriesId: Int,
        @Header("Authorization") authorization: String,
        @Query("language") language: String = "en-US"
    ): TMDBSeriesDetailResponse

    // Search Movies
    @GET("search/movie")
    suspend fun searchMovies(
        @Header("Authorization") authorization: String,
        @Query("query") query: String,
        @Query("include_adult") includeAdult: Boolean = false,
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1
    ): TMDBMovieListResponse

    // Search TV Series
    @GET("search/tv")
    suspend fun searchSeries(
        @Header("Authorization") authorization: String,
        @Query("query") query: String,
        @Query("include_adult") includeAdult: Boolean = false,
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1
    ): TMDBSeriesListResponse

    // Multi Search (searches both movies and TV shows)
    @GET("search/multi")
    suspend fun searchMulti(
        @Header("Authorization") authorization: String,
        @Query("query") query: String,
        @Query("include_adult") includeAdult: Boolean = false,
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1
    ): TMDBMultiSearchResponse

    // Get External IDs for Movie (includes IMDB ID)
    @GET("movie/{movie_id}/external_ids")
    suspend fun getMovieExternalIds(
        @Path("movie_id") movieId: Int,
        @Header("Authorization") authorization: String,
        @Query("language") language: String = "en-US"
    ): TMDBExternalIdsResponse

    // Get External IDs for TV Show (includes IMDB ID)
    @GET("tv/{tv_id}/external_ids")
    suspend fun getTVExternalIds(
        @Path("tv_id") tvId: Int,
        @Header("Authorization") authorization: String,
        @Query("language") language: String = "en-US"
    ): TMDBExternalIdsResponse
}