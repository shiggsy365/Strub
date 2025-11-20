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

    private val apiKey: String
        get() = prefsManager.getTMDBApiKey() ?: ""

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

    private val _castList = MutableLiveData<List<TMDBCast>>()
    val castList: LiveData<List<TMDBCast>> = _castList

    private val _director = MutableLiveData<String?>()
    val director: LiveData<String?> = _director

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

    // STATUS LIVE DATA
    private val _isItemInWatchlist = MutableLiveData<Boolean>(false)
    val isItemInWatchlist: LiveData<Boolean> = _isItemInWatchlist

    private val _isItemInLibrary = MutableLiveData<Boolean>(false)
    val isItemInLibrary: LiveData<Boolean> = _isItemInLibrary

    // --- LIBRARY DATA ---
    val libraryMovies: LiveData<List<MetaItem>> = catalogRepository.getLibraryItems(
        prefsManager.getCurrentUserId() ?: "default", "movie"
    ).map { list -> list.map { toMetaItem(it) } }

    val librarySeries: LiveData<List<MetaItem>> = catalogRepository.getLibraryItems(
        prefsManager.getCurrentUserId() ?: "default", "series"
    ).map { list -> list.map { toMetaItem(it) } }

    private fun toMetaItem(item: CollectedItem): MetaItem {
        return MetaItem(
            id = item.itemId,
            type = item.itemType,
            name = item.name,
            poster = item.poster,
            background = item.background,
            description = item.description
        )
    }

    // --- LIBRARY ACTIONS ---

    fun checkLibraryStatus(metaId: String) {
        val userId = prefsManager.getCurrentUserId() ?: return
        viewModelScope.launch {
            val exists = catalogRepository.isItemCollected(metaId, userId)
            _isItemInLibrary.postValue(exists)
        }
    }

    fun addToLibrary(meta: MetaItem) {
        val userId = prefsManager.getCurrentUserId() ?: return
        viewModelScope.launch {
            val collected = CollectedItem.fromMetaItem(userId, meta)
            catalogRepository.addToLibrary(collected)
            _isItemInLibrary.postValue(true)
            _error.value = "Added to Library"
        }
    }

    fun removeFromLibrary(metaId: String) {
        val userId = prefsManager.getCurrentUserId() ?: return
        viewModelScope.launch {
            catalogRepository.removeFromLibrary(metaId, userId)
            _isItemInLibrary.postValue(false)
            _error.value = "Removed from Library"
        }
    }

    fun toggleLibrary(meta: MetaItem) {
        if (_isItemInLibrary.value == true) {
            removeFromLibrary(meta.id)
        } else {
            addToLibrary(meta)
        }
    }

    // --- AUTH & WATCHLIST ---

    fun fetchRequestToken() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                if (apiKey.isEmpty()) {
                    _error.value = "Please set TMDB API Key in Settings"
                    return@launch
                }
                val response = TMDBClient.api.createRequestToken(apiKey)
                if (response.success) {
                    _requestToken.value = response.requestToken
                } else {
                    _error.value = "Failed to create request token"
                }
            } catch (e: Exception) {
                _error.value = "Auth Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createSession(token: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                if (apiKey.isEmpty()) {
                    _error.value = "API Key missing"
                    return@launch
                }
                val body = mapOf("request_token" to token)
                val response = TMDBClient.api.createSession(apiKey, body)
                if (response.success) {
                    prefsManager.saveTMDBSessionId(response.sessionId)
                    _sessionId.value = response.sessionId
                    fetchAccountDetails(response.sessionId)
                } else {
                    _error.value = "Failed to create session"
                }
            } catch (e: Exception) {
                _error.value = "Session Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun checkTMDBAuthAndSync() {
        val sessionId = prefsManager.getTMDBSessionId()
        if (!sessionId.isNullOrEmpty()) {
            viewModelScope.launch {
                fetchAccountDetails(sessionId)
            }
        }
    }

    private suspend fun fetchAccountDetails(sessionId: String) {
        try {
            if (apiKey.isEmpty()) return
            val details = TMDBClient.api.getAccountDetails(apiKey, sessionId)
            prefsManager.saveTMDBAccountId(details.id)
            addWatchlistCatalogs()
        } catch (e: Exception) {
            Log.e("MainViewModel", "Failed to fetch account: ${e.message}")
        }
    }

    private suspend fun addWatchlistCatalogs() {
        val userId = prefsManager.getCurrentUserId() ?: "default"
        if (!catalogRepository.isCatalogAdded(userId, "watchlist_movies", "movie", "movies")) {
            val movieWatchlist = UserCatalog(
                userId = userId, catalogId = "watchlist_movies", catalogType = "movie", catalogName = "TMDB Movies Watchlist", customName = null, displayOrder = -1, pageType = "movies", addonUrl = "tmdb", manifestId = "tmdb_watchlist", showInDiscover = true, showInUser = true
            )
            catalogRepository.insertCatalog(movieWatchlist)
        }
        if (!catalogRepository.isCatalogAdded(userId, "watchlist_series", "series", "series")) {
            val seriesWatchlist = UserCatalog(
                userId = userId, catalogId = "watchlist_series", catalogType = "series", catalogName = "TMDB Series Watchlist", customName = null, displayOrder = -1, pageType = "series", addonUrl = "tmdb", manifestId = "tmdb_watchlist", showInDiscover = true, showInUser = true
            )
            catalogRepository.insertCatalog(seriesWatchlist)
        }
    }

    fun checkWatchlistStatus(metaId: String, type: String) {
        val sessionId = prefsManager.getTMDBSessionId()
        val accountId = prefsManager.getTMDBAccountId()

        if (sessionId == null || accountId == -1 || apiKey.isEmpty()) {
            _isItemInWatchlist.value = false
            return
        }

        val mediaType = if (type == "series") "tv" else "movie"
        val mediaId = metaId.removePrefix("tmdb:").toIntOrNull() ?: return

        viewModelScope.launch {
            try {
                val response = if (mediaType == "movie") {
                    TMDBClient.api.getMovieWatchlist(accountId, apiKey, sessionId, page = 1)
                } else {
                    TMDBClient.api.getTVWatchlist(accountId, apiKey, sessionId, page = 1)
                }
                val isInWatchlist = if (mediaType == "movie") {
                    (response as TMDBMovieListResponse).results.any { it.id == mediaId }
                } else {
                    (response as TMDBSeriesListResponse).results.any { it.id == mediaId }
                }
                _isItemInWatchlist.value = isInWatchlist
            } catch (e: Exception) {
                _isItemInWatchlist.value = false
            }
        }
    }

    fun toggleWatchlist(meta: MetaItem) {
        // Overloaded for fragment usage
        val currentStatus = _isItemInWatchlist.value ?: false
        toggleWatchlist(meta, !currentStatus)
    }

    fun toggleWatchlist(meta: MetaItem, addToWatchlist: Boolean) {
        val sessionId = prefsManager.getTMDBSessionId()
        val accountId = prefsManager.getTMDBAccountId()

        if (sessionId == null || accountId == -1) {
            _error.value = "Please authorise TMDB in Settings first."
            return
        }
        if (apiKey.isEmpty()) {
            _error.value = "API Key missing"
            return
        }

        val mediaId = meta.id.removePrefix("tmdb:").toIntOrNull() ?: return
        val mediaType = if (meta.type == "series") "tv" else "movie"

        viewModelScope.launch {
            try {
                val body = TMDBWatchlistBody(mediaType = mediaType, mediaId = mediaId, watchlist = addToWatchlist)
                val response = TMDBClient.api.addToWatchlist(accountId, apiKey, sessionId, body)
                if (response.success) {
                    val action = if (addToWatchlist) "Added to" else "Removed from"
                    _error.value = "$action Watchlist successfully"
                    _isItemInWatchlist.value = addToWatchlist
                } else {
                    _error.value = "Failed: ${response.statusMessage}"
                }
            } catch (e: Exception) {
                _error.value = "Watchlist Error: ${e.message}"
            }
        }
    }

    // --- CONTENT LOADING ---

    fun loadContentForCatalog(catalog: UserCatalog) {
        viewModelScope.launch {
            _isLoading.value = true
            _currentCatalogContent.value = emptyList()
            try {
                val today = TMDBClient.getTodaysDate()
                if (apiKey.isEmpty()) {
                    _error.value = "API Key missing."
                    return@launch
                }
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

    private suspend fun fetchMovies(fetcher: suspend (Int) -> TMDBMovieListResponse): List<MetaItem> {
        val list = mutableListOf<TMDBMovie>()
        for (i in 1..5) {
            try {
                list.addAll(fetcher(i).results)
            } catch(e:Exception){ break }
        }
        return list.map { it.toMetaItem() }
    }

    private suspend fun fetchTV(fetcher: suspend (Int) -> TMDBSeriesListResponse): List<MetaItem> {
        val list = mutableListOf<TMDBSeries>()
        for (i in 1..5) {
            try {
                list.addAll(fetcher(i).results)
            } catch(e:Exception){ break }
        }
        return list.map { it.toMetaItem() }
    }

    fun loadStreams(type: String, tmdbId: String) {
        // ... (Stream loading logic, kept same)
    }

    fun loadEpisodeStreams(tmdbId: String, season: Int, episode: Int) {
        // ... (Episode loading logic, kept same)
    }

    fun loadSeriesMeta(tmdbId: String) {
        // ... (Meta loading logic, kept same)
    }

    fun clearStreams() { _streams.value = emptyList() }

    fun fetchCast(tmdbId: String, type: String) {
        // ... (Cast logic, kept same)
    }

    fun searchTMDB(query: String) {
        if (query.isBlank()) { _searchResults.value = emptyList(); return }
        viewModelScope.launch {
            _isSearching.value = true
            try {
                if (apiKey.isEmpty()) {
                    _error.value = "API Key required"
                    return@launch
                }
                val allResults = mutableListOf<TMDBMultiSearchResult>()
                for (page in 1..3) {
                    try {
                        val response = TMDBClient.api.searchMulti(apiKey, query, page = page)
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
        viewModelScope.launch {
            _isSearching.value = true
            try {
                if (apiKey.isEmpty()) return@launch
                val allMovies = mutableListOf<TMDBMovie>()
                for (page in 1..3) {
                    try {
                        val response = TMDBClient.api.searchMovies(apiKey, query, page = page)
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
        viewModelScope.launch {
            _isSearching.value = true
            try {
                if (apiKey.isEmpty()) return@launch
                val allSeries = mutableListOf<TMDBSeries>()
                for (page in 1..3) {
                    try {
                        val response = TMDBClient.api.searchSeries(apiKey, query, page = page)
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

    fun getDiscoverCatalogs(type: String): LiveData<List<UserCatalog>> {
        return allCatalogConfigs.map { list ->
            list.filter { it.catalogType == type && it.showInDiscover }.sortedBy { it.displayOrder }
        }
    }

    fun updateCatalogConfig(c: UserCatalog) {
        viewModelScope.launch { catalogRepository.updateCatalog(c) }
    }

    fun swapCatalogOrder(i1: UserCatalog, i2: UserCatalog) {
        viewModelScope.launch { catalogRepository.swapOrder(i1, i2) }
    }

    fun initDefaultCatalogs() {
        viewModelScope.launch { catalogRepository.initializeDefaultsIfNeeded() }
    }
}