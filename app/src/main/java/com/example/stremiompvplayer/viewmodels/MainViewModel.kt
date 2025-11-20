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
    val movieCatalogs: LiveData<List<UserCatalog>> = allCatalogConfigs.map { list -> list.filter { it.catalogType == "movie" && it.showInUser }.sortedBy { it.displayOrder } }
    val seriesCatalogs: LiveData<List<UserCatalog>> = allCatalogConfigs.map { list -> list.filter { it.catalogType == "series" && it.showInUser }.sortedBy { it.displayOrder } }

    // --- LIVE DATA ---
    private val _currentCatalogContent = MutableLiveData<List<MetaItem>>()
    val currentCatalogContent: LiveData<List<MetaItem>> = _currentCatalogContent
    private val _streams = MutableLiveData<List<Stream>>()
    val streams: LiveData<List<Stream>> = _streams
    private val _metaDetails = MutableLiveData<Meta?>()
    val metaDetails: LiveData<Meta?> = _metaDetails
    private val _castList = MutableLiveData<List<TMDBCast>>()
    val castList: LiveData<List<TMDBCast>> = _castList
    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    private val _requestToken = MutableLiveData<String?>()
    val requestToken: LiveData<String?> = _requestToken
    private val _sessionId = MutableLiveData<String?>()
    val sessionId: LiveData<String?> = _sessionId
    private val _searchResults = MutableLiveData<List<MetaItem>>()
    val searchResults: LiveData<List<MetaItem>> = _searchResults
    private val _isSearching = MutableLiveData<Boolean>()
    val isSearching: LiveData<Boolean> = _isSearching
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

    // --- AUTH FUNCTIONS ---

    fun fetchRequestToken() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = TMDBClient.api.createRequestToken(TMDBClient.API_KEY)
                if (response.success) _requestToken.value = response.requestToken else _error.value = "Failed to create request token"
            } catch (e: Exception) { _error.value = "Auth Error: ${e.message}" } finally { _isLoading.value = false }
        }
    }

    fun createSession(token: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val body = mapOf("request_token" to token)
                val response = TMDBClient.api.createSession(TMDBClient.API_KEY, body)
                if (response.success) {
                    prefsManager.saveTMDBSessionId(response.sessionId)
                    _sessionId.value = response.sessionId
                    fetchAccountDetails(response.sessionId)
                } else { _error.value = "Failed to create session" }
            } catch (e: Exception) { _error.value = "Session Error: ${e.message}" } finally { _isLoading.value = false }
        }
    }

    // --- NEW: Check Auth on Startup ---
    fun checkTMDBAuthAndSync() {
        val sessionId = prefsManager.getTMDBSessionId()
        if (!sessionId.isNullOrEmpty()) {
            // Valid session found, verify account & sync lists
            viewModelScope.launch {
                // We fetch account details again to ensure Account ID is fresh and sync lists
                // This acts as a refresh
                fetchAccountDetails(sessionId)
            }
        }
    }

    private suspend fun fetchAccountDetails(sessionId: String) {
        try {
            val details = TMDBClient.api.getAccountDetails(TMDBClient.API_KEY, sessionId)
            prefsManager.saveTMDBAccountId(details.id)
            addWatchlistCatalogs()
        } catch (e: Exception) {
            // If account fetch fails (e.g. session invalid), we might want to clear session
            // But for now just log error
            Log.e("MainViewModel", "Failed to fetch account: ${e.message}")
        }
    }

    private suspend fun addWatchlistCatalogs() {
        val userId = prefsManager.getCurrentUserId() ?: "default"

        // Check Movies Watchlist
        if (!catalogRepository.isCatalogAdded(userId, "watchlist_movies", "movie", "movies")) {
            val movieWatchlist = UserCatalog(
                userId = userId,
                catalogId = "watchlist_movies",
                catalogType = "movie",
                catalogName = "My Watchlist",
                customName = null,
                displayOrder = -1, // Put at very top
                pageType = "movies",
                addonUrl = "tmdb",
                manifestId = "tmdb_watchlist",
                showInDiscover = true,
                showInUser = true
            )
            catalogRepository.insertCatalog(movieWatchlist)
        }

        // Check Series Watchlist
        if (!catalogRepository.isCatalogAdded(userId, "watchlist_series", "series", "series")) {
            val seriesWatchlist = UserCatalog(
                userId = userId,
                catalogId = "watchlist_series",
                catalogType = "series",
                catalogName = "My Watchlist",
                customName = null,
                displayOrder = -1, // Put at very top
                pageType = "series",
                addonUrl = "tmdb",
                manifestId = "tmdb_watchlist",
                showInDiscover = true,
                showInUser = true
            )
            catalogRepository.insertCatalog(seriesWatchlist)
        }
    }

    // --- CATALOG LOADING ---

    fun loadContentForCatalog(catalog: UserCatalog) {
        viewModelScope.launch {
            _isLoading.value = true
            _currentCatalogContent.value = emptyList()
            try {
                val today = TMDBClient.getTodaysDate()
                val apiKey = TMDBClient.API_KEY

                val results = when(catalog.catalogId) {
                    "watchlist_movies" -> {
                        val sessionId = prefsManager.getTMDBSessionId()
                        val accountId = prefsManager.getTMDBAccountId()
                        if (sessionId != null && accountId != -1) {
                            fetchMovies { p -> TMDBClient.api.getMovieWatchlist(accountId, apiKey, sessionId, page = p) }
                        } else emptyList()
                    }
                    "watchlist_series" -> {
                        val sessionId = prefsManager.getTMDBSessionId()
                        val accountId = prefsManager.getTMDBAccountId()
                        if (sessionId != null && accountId != -1) {
                            fetchTV { p -> TMDBClient.api.getTVWatchlist(accountId, apiKey, sessionId, page = p) }
                        } else emptyList()
                    }
                    "popular", "popular_series" -> if (catalog.catalogType == "movie")
                        fetchMovies { p -> TMDBClient.api.getPopularMovies(apiKey, page=p, releaseDate=today) }
                    else fetchTV { p -> TMDBClient.api.getPopularSeries(apiKey, page=p, airDate=today) }
                    "latest", "latest_series" -> if (catalog.catalogType == "movie")
                        fetchMovies { p -> TMDBClient.api.getLatestMovies(apiKey, page=p, releaseDate=today) }
                    else fetchTV { p -> TMDBClient.api.getLatestSeries(apiKey, page=p, airDate=today) }
                    "trending", "trending_series" -> if (catalog.catalogType == "movie")
                        fetchMovies { p -> TMDBClient.api.getTrendingMovies(apiKey, page=p) }
                    else fetchTV { p -> TMDBClient.api.getTrendingSeries(apiKey, page=p) }
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
        viewModelScope.launch {
            val apiKey = TMDBClient.API_KEY
            val today = TMDBClient.getTodaysDate()
            val pop = async { fetchMovies { p -> TMDBClient.api.getPopularMovies(apiKey, page=p, releaseDate=today) } }
            val lat = async { fetchMovies { p -> TMDBClient.api.getLatestMovies(apiKey, page=p, releaseDate=today) } }
            val trd = async { fetchMovies { p -> TMDBClient.api.getTrendingMovies(apiKey, page=p) } }

            val results = awaitAll(pop, lat, trd)
            _popularMovies.value = results[0]
            _latestMovies.value = results[1]
            _trendingMovies.value = results[2]
        }
    }

    fun loadSeriesLists() {
        viewModelScope.launch {
            val apiKey = TMDBClient.API_KEY
            val today = TMDBClient.getTodaysDate()
            val pop = async { fetchTV { p -> TMDBClient.api.getPopularSeries(apiKey, page=p, airDate=today) } }
            val lat = async { fetchTV { p -> TMDBClient.api.getLatestSeries(apiKey, page=p, airDate=today) } }
            val trd = async { fetchTV { p -> TMDBClient.api.getTrendingSeries(apiKey, page=p) } }

            val results = awaitAll(pop, lat, trd)
            _popularSeries.value = results[0]
            _latestSeries.value = results[1]
            _trendingSeries.value = results[2]
        }
    }

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

    fun loadStreams(type: String, tmdbId: String) {
        val user = prefsManager.getAIOStreamsUsername() ?: return
        val pass = prefsManager.getAIOStreamsPassword() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val numId = tmdbId.removePrefix("tmdb:").toIntOrNull() ?: return@launch
                val apiKey = TMDBClient.API_KEY
                val exIds = if(type=="movie") TMDBClient.api.getMovieExternalIds(numId, apiKey)
                else TMDBClient.api.getTVExternalIds(numId, apiKey)
                val imdb = exIds.imdbId ?: return@launch
                val api = AIOStreamsClient.getApi(user, pass)
                val res = api.searchStreams(type, imdb)
                _streams.value = res.data?.results ?: emptyList()
            } catch (e: Exception) { _streams.value = emptyList() }
            finally { _isLoading.value = false }
        }
    }

    fun loadEpisodeStreams(tmdbId: String, season: Int, episode: Int) {
        val user = prefsManager.getAIOStreamsUsername() ?: return
        val pass = prefsManager.getAIOStreamsPassword() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val numId = tmdbId.removePrefix("tmdb:").toIntOrNull() ?: return@launch
                val apiKey = TMDBClient.API_KEY
                val exIds = TMDBClient.api.getTVExternalIds(numId, apiKey)
                val imdb = exIds.imdbId ?: return@launch
                val api = AIOStreamsClient.getApi(user, pass)
                val res = api.searchStreams("series", "$imdb:$season:$episode")
                _streams.value = res.data?.results ?: emptyList()
            } catch (e: Exception) { _streams.value = emptyList() }
            finally { _isLoading.value = false }
        }
    }

    fun loadSeriesMeta(tmdbId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val numId = tmdbId.removePrefix("tmdb:").toIntOrNull() ?: return@launch
                val apiKey = TMDBClient.API_KEY
                val res = TMDBClient.api.getSeriesDetail(numId, apiKey)
                val videos = res.seasons.flatMap { s -> (1..s.episodeCount).map { e ->
                    Video("tmdb:$numId:${s.seasonNumber}:$e", "Episode $e", s.airDate, s.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }, e, s.seasonNumber)
                }}
                val meta = Meta(tmdbId, "series", res.name, res.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }, res.backdropPath?.let { "https://image.tmdb.org/t/p/original$it" }, res.overview, videos)
                _metaDetails.value = meta
            } catch (e: Exception) {}
            finally { _isLoading.value = false }
        }
    }

    fun clearStreams() { _streams.value = emptyList() }

    fun fetchCast(tmdbId: String, type: String) {
        val id = tmdbId.removePrefix("tmdb:").toIntOrNull() ?: return
        viewModelScope.launch {
            try {
                val response = if (type == "movie") {
                    TMDBClient.api.getMovieCredits(id, TMDBClient.API_KEY)
                } else {
                    TMDBClient.api.getTVCredits(id, TMDBClient.API_KEY)
                }
                _castList.value = response.cast.take(5)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error fetching cast: ${e.message}")
                _castList.value = emptyList()
            }
        }
    }
    fun clearCast() { _castList.value = emptyList() }

    fun loadPersonCredits(personId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _currentCatalogContent.value = emptyList()
            try {
                val response = TMDBClient.api.getPersonCombinedCredits(personId, TMDBClient.API_KEY)
                val credits = response.cast.map { it.toMetaItem() }.filter { it.poster != null }
                _currentCatalogContent.value = credits
            } catch (e: Exception) { _error.value = "Failed to load person credits: ${e.message}" }
            finally { _isLoading.value = false }
        }
    }

    fun toggleWatchlist(meta: MetaItem, addToWatchlist: Boolean) {
        val sessionId = prefsManager.getTMDBSessionId()
        val accountId = prefsManager.getTMDBAccountId()
        if (sessionId == null || accountId == -1) {
            _error.value = "Please authorise TMDB in Settings first."
            return
        }
        val mediaId = meta.id.removePrefix("tmdb:").toIntOrNull() ?: return
        val mediaType = if (meta.type == "series") "tv" else "movie"

        viewModelScope.launch {
            try {
                val body = TMDBWatchlistBody(mediaType = mediaType, mediaId = mediaId, watchlist = addToWatchlist)
                val response = TMDBClient.api.addToWatchlist(accountId, TMDBClient.API_KEY, sessionId, body)
                if (response.success) {
                    val action = if (addToWatchlist) "Added to" else "Removed from"
                    _error.value = "$action Watchlist successfully"
                } else { _error.value = "Failed: ${response.statusMessage}" }
            } catch (e: Exception) { _error.value = "Watchlist Error: ${e.message}" }
        }
    }

    fun searchTMDB(query: String) {
        if (query.isBlank()) { _searchResults.value = emptyList(); return }
        viewModelScope.launch {
            _isSearching.value = true
            try {
                val allResults = mutableListOf<TMDBMultiSearchResult>()
                for (page in 1..3) {
                    try {
                        val response = TMDBClient.api.searchMulti(TMDBClient.API_KEY, query, page = page)
                        allResults.addAll(response.results)
                    } catch (e: Exception) { break }
                }
                _searchResults.value = allResults.filter { it.mediaType == "movie" || it.mediaType == "tv" }.map { it.toMetaItem() }
            } catch (e: Exception) { _error.value = "Search failed: ${e.message}" }
            finally { _isSearching.value = false }
        }
    }

    fun searchMovies(query: String) {
        if (query.isBlank()) { _searchResults.value = emptyList(); return }
        viewModelScope.launch {
            _isSearching.value = true
            try {
                val allMovies = mutableListOf<TMDBMovie>()
                for (page in 1..3) {
                    try {
                        val response = TMDBClient.api.searchMovies(TMDBClient.API_KEY, query, page = page)
                        allMovies.addAll(response.results)
                    } catch (e: Exception) { break }
                }
                _searchResults.value = allMovies.map { it.toMetaItem() }
            } catch (e: Exception) { _error.value = "Movie search failed: ${e.message}" }
            finally { _isSearching.value = false }
        }
    }

    fun searchSeries(query: String) {
        if (query.isBlank()) { _searchResults.value = emptyList(); return }
        viewModelScope.launch {
            _isSearching.value = true
            try {
                val allSeries = mutableListOf<TMDBSeries>()
                for (page in 1..3) {
                    try {
                        val response = TMDBClient.api.searchSeries(TMDBClient.API_KEY, query, page = page)
                        allSeries.addAll(response.results)
                    } catch (e: Exception) { break }
                }
                _searchResults.value = allSeries.map { it.toMetaItem() }
            } catch (e: Exception) { _error.value = "Series search failed: ${e.message}" }
            finally { _isSearching.value = false }
        }
    }

    fun clearSearchResults() { _searchResults.value = emptyList() }

    fun getDiscoverCatalogs(type: String): LiveData<List<UserCatalog>> {
        return allCatalogConfigs.map { list -> list.filter { it.catalogType == type && it.showInDiscover }.sortedBy { it.displayOrder } }
    }
    fun updateCatalogConfig(c: UserCatalog) { viewModelScope.launch { catalogRepository.updateCatalog(c) } }
    fun swapCatalogOrder(i1: UserCatalog, i2: UserCatalog) { viewModelScope.launch { catalogRepository.swapOrder(i1, i2) } }
    fun initDefaultCatalogs() { viewModelScope.launch { catalogRepository.initializeDefaultsIfNeeded() } }
}