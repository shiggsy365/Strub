package com.example.stremiompvplayer.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
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

    // --- CONFIG & CATALOGS ---
    val allCatalogConfigs: LiveData<List<UserCatalog>> = catalogRepository.allCatalogs

    val movieCatalogs: LiveData<List<UserCatalog>> = allCatalogConfigs.map { list ->
        list.filter { it.catalogType == "movie" && it.showInUser }.sortedBy { it.displayOrder }
    }

    val seriesCatalogs: LiveData<List<UserCatalog>> = allCatalogConfigs.map { list ->
        list.filter { it.catalogType == "series" && it.showInUser }.sortedBy { it.displayOrder }
    }

    // --- LIVE DATA ---
    private val _currentCatalogContent = MutableLiveData<List<MetaItem>>()
    val currentCatalogContent: LiveData<List<MetaItem>> = _currentCatalogContent

    private val _streams = MutableLiveData<List<Stream>>()
    val streams: LiveData<List<Stream>> = _streams

    private val _metaDetails = MutableLiveData<Meta?>()
    val metaDetails: LiveData<Meta?> = _metaDetails

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // Legacy / Standard Lists
    private val _popularMovies = MutableLiveData<List<MetaItem>>()
    val popularMovies: LiveData<List<MetaItem>> = _popularMovies
    private val _latestMovies = MutableLiveData<List<MetaItem>>()
    val latestMovies: LiveData<List<MetaItem>> = _latestMovies
    private val _trendingMovies = MutableLiveData<List<MetaItem>>()
    val trendingMovies: LiveData<List<MetaItem>> = _trendingMovies

    private val _popularSeries = MutableLiveData<List<MetaItem>>()
    val popularSeries: LiveData<List<MetaItem>> = _popularSeries
    private val _latestSeries = MutableLiveData<List<MetaItem>>()
    val latestSeries: LiveData<List<MetaItem>> = _latestSeries
    private val _trendingSeries = MutableLiveData<List<MetaItem>>()
    val trendingSeries: LiveData<List<MetaItem>> = _trendingSeries

    // Search
    private val _searchResults = MutableLiveData<List<MetaItem>>()
    val searchResults: LiveData<List<MetaItem>> = _searchResults
    private val _isSearching = MutableLiveData<Boolean>()
    val isSearching: LiveData<Boolean> = _isSearching

    // --- SEARCH FUNCTIONS ---

    fun searchTMDB(query: String) {
        if (query.isBlank()) { _searchResults.value = emptyList(); return }
        val token = prefsManager.getTMDBAccessToken() ?: return

        viewModelScope.launch {
            _isSearching.value = true
            try {
                val bearerToken = TMDBClient.getBearerToken(token)
                val allResults = mutableListOf<TMDBMultiSearchResult>()
                for (page in 1..3) {
                    try {
                        val response = TMDBClient.api.searchMulti(bearerToken, query, page = page)
                        allResults.addAll(response.results)
                    } catch (e: Exception) { break }
                }
                _searchResults.value = allResults
                    .filter { it.mediaType == "movie" || it.mediaType == "tv" }
                    .map { it.toMetaItem() }
            } catch (e: Exception) {
                _error.value = "Search failed: ${e.message}"
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun searchMovies(query: String) {
        if (query.isBlank()) { _searchResults.value = emptyList(); return }
        val token = prefsManager.getTMDBAccessToken() ?: return

        viewModelScope.launch {
            _isSearching.value = true
            try {
                val bearerToken = TMDBClient.getBearerToken(token)
                val allMovies = mutableListOf<TMDBMovie>()
                for (page in 1..3) {
                    try {
                        val response = TMDBClient.api.searchMovies(bearerToken, query, page = page)
                        allMovies.addAll(response.results)
                    } catch (e: Exception) { break }
                }
                _searchResults.value = allMovies.map { it.toMetaItem() }
            } catch (e: Exception) {
                _error.value = "Movie search failed: ${e.message}"
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun searchSeries(query: String) {
        if (query.isBlank()) { _searchResults.value = emptyList(); return }
        val token = prefsManager.getTMDBAccessToken() ?: return

        viewModelScope.launch {
            _isSearching.value = true
            try {
                val bearerToken = TMDBClient.getBearerToken(token)
                val allSeries = mutableListOf<TMDBSeries>()
                for (page in 1..3) {
                    try {
                        val response = TMDBClient.api.searchSeries(bearerToken, query, page = page)
                        allSeries.addAll(response.results)
                    } catch (e: Exception) { break }
                }
                _searchResults.value = allSeries.map { it.toMetaItem() }
            } catch (e: Exception) {
                _error.value = "Series search failed: ${e.message}"
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun clearSearchResults() { _searchResults.value = emptyList() }

    // --- CATALOG LOADING ---

    fun loadContentForCatalog(catalog: UserCatalog) {
        val token = prefsManager.getTMDBAccessToken() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _currentCatalogContent.value = emptyList()
            try {
                val bearerToken = TMDBClient.getBearerToken(token)
                val today = TMDBClient.getTodaysDate()

                val results = when(catalog.catalogId) {
                    "popular", "popular_series" -> if (catalog.catalogType == "movie")
                        fetchMovies { p -> TMDBClient.api.getPopularMovies(bearerToken, page=p, releaseDate=today) }
                    else fetchTV { p -> TMDBClient.api.getPopularSeries(bearerToken, page=p, airDate=today) }
                    "latest", "latest_series" -> if (catalog.catalogType == "movie")
                        fetchMovies { p -> TMDBClient.api.getLatestMovies(bearerToken, page=p, releaseDate=today) }
                    else fetchTV { p -> TMDBClient.api.getLatestSeries(bearerToken, page=p, airDate=today) }
                    "trending", "trending_series" -> if (catalog.catalogType == "movie")
                        fetchMovies { p -> TMDBClient.api.getTrendingMovies(bearerToken, page=p) }
                    else fetchTV { p -> TMDBClient.api.getTrendingSeries(bearerToken, page=p) }
                    else -> emptyList()
                }
                _currentCatalogContent.value = results
            } catch (e: Exception) {
                _error.value = "Load failed: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Legacy Loaders
    fun loadMovieLists() {
        val token = prefsManager.getTMDBAccessToken() ?: return
        viewModelScope.launch {
            val bearer = TMDBClient.getBearerToken(token)
            val today = TMDBClient.getTodaysDate()
            val pop = async { fetchMovies { p -> TMDBClient.api.getPopularMovies(bearer, page=p, releaseDate=today) } }
            val lat = async { fetchMovies { p -> TMDBClient.api.getLatestMovies(bearer, page=p, releaseDate=today) } }
            val trd = async { fetchMovies { p -> TMDBClient.api.getTrendingMovies(bearer, page=p) } }

            val results = awaitAll(pop, lat, trd)
            _popularMovies.value = results[0]
            _latestMovies.value = results[1]
            _trendingMovies.value = results[2]
        }
    }

    fun loadSeriesLists() {
        val token = prefsManager.getTMDBAccessToken() ?: return
        viewModelScope.launch {
            val bearer = TMDBClient.getBearerToken(token)
            val today = TMDBClient.getTodaysDate()
            val pop = async { fetchTV { p -> TMDBClient.api.getPopularSeries(bearer, page=p, airDate=today) } }
            val lat = async { fetchTV { p -> TMDBClient.api.getLatestSeries(bearer, page=p, airDate=today) } }
            val trd = async { fetchTV { p -> TMDBClient.api.getTrendingSeries(bearer, page=p) } }

            val results = awaitAll(pop, lat, trd)
            _popularSeries.value = results[0]
            _latestSeries.value = results[1]
            _trendingSeries.value = results[2]
        }
    }

    // Helpers
    private suspend fun fetchMovies(fetcher: suspend (Int) -> TMDBMovieListResponse): List<MetaItem> {
        val list = mutableListOf<TMDBMovie>()
        for (i in 1..5) try { list.addAll(fetcher(i).results) } catch(e:Exception){ break }
        return list.map { it.toMetaItem() }
    }

    private suspend fun fetchTV(fetcher: suspend (Int) -> TMDBSeriesListResponse): List<MetaItem> {
        val list = mutableListOf<TMDBSeries>()
        for (i in 1..5) try { list.addAll(fetcher(i).results) } catch(e:Exception){ break }
        return list.map { it.toMetaItem() }
    }

    // --- STREAMS ---
    fun loadStreams(type: String, tmdbId: String) {
        val token = prefsManager.getTMDBAccessToken() ?: return
        val user = prefsManager.getAIOStreamsUsername() ?: return
        val pass = prefsManager.getAIOStreamsPassword() ?: return

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val numId = tmdbId.removePrefix("tmdb:").toIntOrNull() ?: return@launch
                val tokenBear = TMDBClient.getBearerToken(token)
                val exIds = if(type=="movie") TMDBClient.api.getMovieExternalIds(numId, tokenBear)
                else TMDBClient.api.getTVExternalIds(numId, tokenBear)
                val imdb = exIds.imdbId ?: return@launch

                val api = AIOStreamsClient.getApi(user, pass)
                val res = api.searchStreams(type, imdb)

                _streams.value = res.data?.results ?: emptyList()
            } catch (e: Exception) { _streams.value = emptyList() }
            finally { _isLoading.value = false }
        }
    }

    fun loadEpisodeStreams(tmdbId: String, season: Int, episode: Int) {
        val token = prefsManager.getTMDBAccessToken() ?: return
        val user = prefsManager.getAIOStreamsUsername() ?: return
        val pass = prefsManager.getAIOStreamsPassword() ?: return

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val numId = tmdbId.removePrefix("tmdb:").toIntOrNull() ?: return@launch
                val tokenBear = TMDBClient.getBearerToken(token)
                val exIds = TMDBClient.api.getTVExternalIds(numId, tokenBear)
                val imdb = exIds.imdbId ?: return@launch

                val api = AIOStreamsClient.getApi(user, pass)
                val res = api.searchStreams("series", "$imdb:$season:$episode")

                _streams.value = res.data?.results ?: emptyList()
            } catch (e: Exception) { _streams.value = emptyList() }
            finally { _isLoading.value = false }
        }
    }

    private fun handleStreamError(e: Exception) {
        val errorMsg = when {
            e.message?.contains("Required value") == true -> "No streams found."
            e.message?.contains("401") == true -> "Authentication failed."
            e.message?.contains("404") == true -> "Content not found."
            else -> "Failed: ${e.message}"
        }
        _error.value = errorMsg
        _streams.value = emptyList()
    }

    fun loadSeriesMeta(tmdbId: String) {
        val token = prefsManager.getTMDBAccessToken() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val numId = tmdbId.removePrefix("tmdb:").toIntOrNull() ?: return@launch
                val tokenBear = TMDBClient.getBearerToken(token)
                val res = TMDBClient.api.getSeriesDetail(numId, tokenBear)

                val videos = res.seasons.flatMap { s -> (1..s.episodeCount).map { e ->
                    Video("tmdb:$numId:${s.seasonNumber}:$e", "Episode $e", s.airDate, s.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }, e, s.seasonNumber)
                }}
                val meta = Meta(tmdbId, "series", res.name, res.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }, res.backdropPath?.let { "https://image.tmdb.org/t/p/original$it" }, res.overview, videos)
                _metaDetails.value = meta
            } catch (e: Exception) {}
            finally { _isLoading.value = false }
        }
    }

    // --- THIS IS THE MISSING FUNCTION ---
    fun clearStreams() {
        _streams.value = emptyList()
    }

    // Config
    fun updateCatalogConfig(c: UserCatalog) { viewModelScope.launch { catalogRepository.updateCatalog(c) } }
    fun swapCatalogOrder(i1: UserCatalog, i2: UserCatalog) { viewModelScope.launch { catalogRepository.swapOrder(i1, i2) } }
    fun initDefaultCatalogs() { viewModelScope.launch { catalogRepository.initializeDefaultsIfNeeded() } }
}