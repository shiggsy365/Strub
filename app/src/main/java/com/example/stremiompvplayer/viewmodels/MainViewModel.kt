package com.example.stremiompvplayer.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stremiompvplayer.CatalogRepository
import com.example.stremiompvplayer.models.*
import com.example.stremiompvplayer.network.AIOStreamsClient
import com.example.stremiompvplayer.network.TMDBClient
import com.example.stremiompvplayer.utils.SharedPreferencesManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll

class MainViewModel(
    private val catalogRepository: CatalogRepository,
    private val prefsManager: SharedPreferencesManager
) : ViewModel() {
    
    private val _catalogs = MutableLiveData<List<MetaItem>>()
    val catalogs: LiveData<List<MetaItem>> = _catalogs

    private val _streams = MutableLiveData<List<Stream>>()
    val streams: LiveData<List<Stream>> = _streams

    private val _metaDetails = MutableLiveData<Meta?>()
    val metaDetails: LiveData<Meta?> = _metaDetails

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // Standard lists for movies
    private val _popularMovies = MutableLiveData<List<MetaItem>>()
    val popularMovies: LiveData<List<MetaItem>> = _popularMovies

    private val _latestMovies = MutableLiveData<List<MetaItem>>()
    val latestMovies: LiveData<List<MetaItem>> = _latestMovies

    private val _trendingMovies = MutableLiveData<List<MetaItem>>()
    val trendingMovies: LiveData<List<MetaItem>> = _trendingMovies

    // Standard lists for series
    private val _popularSeries = MutableLiveData<List<MetaItem>>()
    val popularSeries: LiveData<List<MetaItem>> = _popularSeries

    private val _latestSeries = MutableLiveData<List<MetaItem>>()
    val latestSeries: LiveData<List<MetaItem>> = _latestSeries

    private val _trendingSeries = MutableLiveData<List<MetaItem>>()
    val trendingSeries: LiveData<List<MetaItem>> = _trendingSeries

    // Search results
    private val _searchResults = MutableLiveData<List<MetaItem>>()
    val searchResults: LiveData<List<MetaItem>> = _searchResults

    private val _isSearching = MutableLiveData<Boolean>()
    val isSearching: LiveData<Boolean> = _isSearching

    /**
     * Load all standard movie lists (Popular, Latest, Trending)
     * Fetches 100 items per list (5 pages of 20 results)
     */
    fun loadMovieLists() {
        val token = prefsManager.getTMDBAccessToken()
        if (token.isNullOrEmpty()) {
            _error.value = "TMDB Access Token not configured. Please add it in Settings."
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val bearerToken = TMDBClient.getBearerToken(token)
                val todayDate = TMDBClient.getTodaysDate()

                // Fetch all three lists in parallel
                val popularDeferred = async { fetchMultiplePages { page ->
                    TMDBClient.api.getPopularMovies(bearerToken, page = page, releaseDate = todayDate)
                }}
                
                val latestDeferred = async { fetchMultiplePages { page ->
                    TMDBClient.api.getLatestMovies(bearerToken, page = page, releaseDate = todayDate)
                }}
                
                val trendingDeferred = async { fetchMultiplePages { page ->
                    TMDBClient.api.getTrendingMovies(bearerToken, page = page)
                }}

                val results = awaitAll(popularDeferred, latestDeferred, trendingDeferred)
                
                _popularMovies.value = results[0]
                _latestMovies.value = results[1]
                _trendingMovies.value = results[2]
                
                // Also update the main catalogs list with popular movies by default
                _catalogs.value = results[0]

            } catch (e: Exception) {
                _error.value = "Failed to load movie lists: ${e.message}"
                Log.e("MainViewModel", "Error loading movie lists", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Load all standard series lists (Popular, Latest, Trending)
     * Fetches 100 items per list (5 pages of 20 results)
     */
    fun loadSeriesLists() {
        val token = prefsManager.getTMDBAccessToken()
        if (token.isNullOrEmpty()) {
            _error.value = "TMDB Access Token not configured. Please add it in Settings."
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val bearerToken = TMDBClient.getBearerToken(token)
                val todayDate = TMDBClient.getTodaysDate()

                // Fetch all three lists in parallel
                val popularDeferred = async { fetchMultiplePagesTV { page ->
                    TMDBClient.api.getPopularSeries(bearerToken, page = page, airDate = todayDate)
                }}
                
                val latestDeferred = async { fetchMultiplePagesTV { page ->
                    TMDBClient.api.getLatestSeries(bearerToken, page = page, airDate = todayDate)
                }}
                
                val trendingDeferred = async { fetchMultiplePagesTV { page ->
                    TMDBClient.api.getTrendingSeries(bearerToken, page = page)
                }}

                val results = awaitAll(popularDeferred, latestDeferred, trendingDeferred)
                
                _popularSeries.value = results[0]
                _latestSeries.value = results[1]
                _trendingSeries.value = results[2]
                
                // Also update the main catalogs list with popular series by default
                _catalogs.value = results[0]

            } catch (e: Exception) {
                _error.value = "Failed to load series lists: ${e.message}"
                Log.e("MainViewModel", "Error loading series lists", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Helper function to fetch multiple pages (up to 5 pages = 100 items)
     */
    private suspend fun fetchMultiplePages(
        fetcher: suspend (Int) -> TMDBMovieListResponse
    ): List<MetaItem> {
        val allMovies = mutableListOf<TMDBMovie>()
        
        for (page in 1..5) {
            try {
                val response = fetcher(page)
                allMovies.addAll(response.results)
                
                // Stop if we've reached the last page
                if (page >= response.totalPages) break
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error fetching page $page", e)
                break
            }
        }
        
        return allMovies.map { it.toMetaItem() }
    }

    /**
     * Helper function to fetch multiple pages for TV shows
     */
    private suspend fun fetchMultiplePagesTV(
        fetcher: suspend (Int) -> TMDBSeriesListResponse
    ): List<MetaItem> {
        val allSeries = mutableListOf<TMDBSeries>()
        
        for (page in 1..5) {
            try {
                val response = fetcher(page)
                allSeries.addAll(response.results)
                
                // Stop if we've reached the last page
                if (page >= response.totalPages) break
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error fetching page $page", e)
                break
            }
        }
        
        return allSeries.map { it.toMetaItem() }
    }

    /**
     * Load streams from AIOStreams for a movie
     * @param tmdbId The TMDB ID with 'tmdb:' prefix (e.g., 'tmdb:12345')
     */
    fun loadStreams(type: String, tmdbId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = if (type == "movie") {
                    AIOStreamsClient.api.searchMovieStreams(id = tmdbId)
                } else {
                    AIOStreamsClient.api.searchSeriesStreams(id = tmdbId)
                }
                _streams.value = response.streams
            } catch (e: Exception) {
                _error.value = "Failed to load streams: ${e.message}"
                Log.e("MainViewModel", "Error loading streams", e)
                _streams.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Load series metadata including seasons and episodes
     */
    fun loadSeriesMeta(tmdbId: String) {
        val token = prefsManager.getTMDBAccessToken()
        if (token.isNullOrEmpty()) {
            _error.value = "TMDB Access Token not configured."
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Extract numeric ID from tmdb:12345 format
                val numericId = tmdbId.removePrefix("tmdb:").toIntOrNull()
                if (numericId == null) {
                    _error.value = "Invalid TMDB ID format"
                    return@launch
                }

                val bearerToken = TMDBClient.getBearerToken(token)
                val response = TMDBClient.api.getSeriesDetail(numericId, bearerToken)
                
                // Convert TMDB response to Meta format
                val videos = response.seasons.flatMap { season ->
                    (1..season.episodeCount).map { episodeNum ->
                        Video(
                            id = "tmdb:$numericId:${season.seasonNumber}:$episodeNum",
                            title = "Episode $episodeNum",
                            released = season.airDate,
                            thumbnail = season.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" },
                            number = episodeNum,
                            season = season.seasonNumber
                        )
                    }
                }

                val meta = Meta(
                    id = tmdbId,
                    type = "series",
                    name = response.name,
                    poster = response.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" },
                    background = response.backdropPath?.let { "https://image.tmdb.org/t/p/original$it" },
                    description = response.overview,
                    videos = videos
                )

                _metaDetails.value = meta

            } catch (e: Exception) {
                _error.value = "Failed to load series metadata: ${e.message}"
                Log.e("MainViewModel", "Error loading series meta", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearStreams() {
        _streams.value = emptyList()
    }

    /**
     * Search TMDB for movies and TV shows
     * Uses multi-search to get both types
     */
    fun searchTMDB(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }

        val token = prefsManager.getTMDBAccessToken()
        if (token.isNullOrEmpty()) {
            _error.value = "TMDB Access Token not configured. Please add it in Settings."
            return
        }

        viewModelScope.launch {
            _isSearching.value = true
            _isLoading.value = true
            try {
                val bearerToken = TMDBClient.getBearerToken(token)
                val allResults = mutableListOf<TMDBMultiSearchResult>()
                
                // Fetch up to 3 pages for search (60 results)
                for (page in 1..3) {
                    try {
                        val response = TMDBClient.api.searchMulti(bearerToken, query, page = page)
                        allResults.addAll(response.results)
                        
                        // Stop if we've reached the last page
                        if (page >= response.totalPages) break
                    } catch (e: Exception) {
                        Log.e("MainViewModel", "Error fetching search page $page", e)
                        break
                    }
                }
                
                // Convert to MetaItem and filter out non-movie/tv results
                val metaItems = allResults
                    .filter { it.mediaType == "movie" || it.mediaType == "tv" }
                    .map { it.toMetaItem() }
                
                _searchResults.value = metaItems

            } catch (e: Exception) {
                _error.value = "Search failed: ${e.message}"
                Log.e("MainViewModel", "Error searching TMDB", e)
                _searchResults.value = emptyList()
            } finally {
                _isSearching.value = false
                _isLoading.value = false
            }
        }
    }

    /**
     * Search only movies
     */
    fun searchMovies(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }

        val token = prefsManager.getTMDBAccessToken()
        if (token.isNullOrEmpty()) {
            _error.value = "TMDB Access Token not configured."
            return
        }

        viewModelScope.launch {
            _isSearching.value = true
            _isLoading.value = true
            try {
                val bearerToken = TMDBClient.getBearerToken(token)
                val allMovies = mutableListOf<TMDBMovie>()
                
                for (page in 1..3) {
                    try {
                        val response = TMDBClient.api.searchMovies(bearerToken, query, page = page)
                        allMovies.addAll(response.results)
                        if (page >= response.totalPages) break
                    } catch (e: Exception) {
                        break
                    }
                }
                
                _searchResults.value = allMovies.map { it.toMetaItem() }

            } catch (e: Exception) {
                _error.value = "Movie search failed: ${e.message}"
                _searchResults.value = emptyList()
            } finally {
                _isSearching.value = false
                _isLoading.value = false
            }
        }
    }

    /**
     * Search only TV series
     */
    fun searchSeries(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }

        val token = prefsManager.getTMDBAccessToken()
        if (token.isNullOrEmpty()) {
            _error.value = "TMDB Access Token not configured."
            return
        }

        viewModelScope.launch {
            _isSearching.value = true
            _isLoading.value = true
            try {
                val bearerToken = TMDBClient.getBearerToken(token)
                val allSeries = mutableListOf<TMDBSeries>()
                
                for (page in 1..3) {
                    try {
                        val response = TMDBClient.api.searchSeries(bearerToken, query, page = page)
                        allSeries.addAll(response.results)
                        if (page >= response.totalPages) break
                    } catch (e: Exception) {
                        break
                    }
                }
                
                _searchResults.value = allSeries.map { it.toMetaItem() }

            } catch (e: Exception) {
                _error.value = "Series search failed: ${e.message}"
                _searchResults.value = emptyList()
            } finally {
                _isSearching.value = false
                _isLoading.value = false
            }
        }
    }

    /**
     * Clear search results
     */
    fun clearSearchResults() {
        _searchResults.value = emptyList()
    }

    // Legacy catalog functions for compatibility
    fun loadUserEnabledCatalogs(type: String) {
        if (type == "movie") {
            loadMovieLists()
        } else {
            loadSeriesLists()
        }
    }

    fun loadMeta(type: String, id: String) {
        if (type == "series") {
            loadSeriesMeta(id)
        }
    }

    // Catalog config functions (now mostly placeholder since we're using TMDB)
    fun updateCatalogConfig(catalog: UserCatalog) {
        viewModelScope.launch {
            catalogRepository.updateCatalog(catalog)
        }
    }

    fun swapCatalogOrder(item1: UserCatalog, item2: UserCatalog) {
        viewModelScope.launch {
            catalogRepository.swapOrder(item1, item2)
        }
    }

    val allCatalogConfigs: LiveData<List<UserCatalog>> = catalogRepository.allCatalogs

    fun initDefaultCatalogs() {
        viewModelScope.launch {
            catalogRepository.initializeDefaultsIfNeeded()
        }
    }
}
