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

    // Helper to get API Key safely from SharedPreferences
    private val apiKey: String
        get() = prefsManager.getTMDBApiKey() ?: ""

    // --- CONFIG & CATALOGS ---
    val allCatalogConfigs: LiveData<List<UserCatalog>> = catalogRepository.allCatalogs

    // UPDATED: Ensure these filters match your requirements.
    // 'showInUser' controls visibility on the Movies/Series pages.
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

    // NEW LIVE DATA: to check initial watchlist status
    private val _isItemInWatchlist = MutableLiveData<Boolean>(false)
    val isItemInWatchlist: LiveData<Boolean> = _isItemInWatchlist

    // --- AUTH FUNCTIONS ---

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

    // Check Auth on Startup
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

        // Check Movies Watchlist
        if (!catalogRepository.isCatalogAdded(userId, "watchlist_movies", "movie", "movies")) {
            val movieWatchlist = UserCatalog(
                userId = userId,
                catalogId = "watchlist_movies",
                catalogType = "movie",
                catalogName = "TMDB Movies Watchlist",
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
                catalogName = "TMDB Series Watchlist",
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
                // Fetch first page of watchlist and check if the item is present
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
                Log.e("MainViewModel", "Failed to check watchlist status: ${e.message}")
                _isItemInWatchlist.value = false
            }
        }
    }

    // --- CATALOG LOADING ---

    fun loadContentForCatalog(catalog: UserCatalog) {
        viewModelScope.launch {
            _isLoading.value = true
            _currentCatalogContent.value = emptyList()
            try {
                val today = TMDBClient.getTodaysDate()

                if (apiKey.isEmpty()) {
                    _error.value = "API Key missing. Please configure in settings."
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

    // Legacy Loaders used by MoviesFragment/SeriesFragment directly
    fun loadMovieLists() {
        viewModelScope.launch {
            if (apiKey.isEmpty()) {
                // Optionally notify user, but these load silently on startup usually
                return@launch
            }
            val today = TMDBClient.getTodaysDate()
            try {
                val pop = async { fetchMovies { p -> TMDBClient.api.getPopularMovies(apiKey, page=p, releaseDate=today) } }
                val lat = async { fetchMovies { p -> TMDBClient.api.getLatestMovies(apiKey, page=p, releaseDate=today) } }
                val trd = async { fetchMovies { p -> TMDBClient.api.getTrendingMovies(apiKey, page=p) } }

                val results = awaitAll(pop, lat, trd)
                _popularMovies.value = results[0]
                _latestMovies.value = results[1]
                _trendingMovies.value = results[2]
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error loading movie lists: ${e.message}")
            }
        }
    }

    fun loadSeriesLists() {
        viewModelScope.launch {
            if (apiKey.isEmpty()) return@launch
            val today = TMDBClient.getTodaysDate()
            try {
                val pop = async { fetchTV { p -> TMDBClient.api.getPopularSeries(apiKey, page=p, airDate=today) } }
                val lat = async { fetchTV { p -> TMDBClient.api.getLatestSeries(apiKey, page=p, airDate=today) } }
                val trd = async { fetchTV { p -> TMDBClient.api.getTrendingSeries(apiKey, page=p) } }

                val results = awaitAll(pop, lat, trd)
                _popularSeries.value = results[0]
                _latestSeries.value = results[1]
                _trendingSeries.value = results[2]
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error loading series lists: ${e.message}")
            }
        }
    }

    private suspend fun fetchMovies(fetcher: suspend (Int) -> TMDBMovieListResponse): List<MetaItem> {
        val list = mutableListOf<TMDBMovie>()
        for (i in 1..5) {
            try {
                list.addAll(fetcher(i).results)
            } catch(e:Exception){
                break
            }
        }
        return list.map { it.toMetaItem() }
    }

    private suspend fun fetchTV(fetcher: suspend (Int) -> TMDBSeriesListResponse): List<MetaItem> {
        val list = mutableListOf<TMDBSeries>()
        for (i in 1..5) {
            try {
                list.addAll(fetcher(i).results)
            } catch(e:Exception){
                break
            }
        }
        return list.map { it.toMetaItem() }
    }

    fun loadStreams(type: String, tmdbId: String) {
        val user = prefsManager.getAIOStreamsUsername() ?: return
        val pass = prefsManager.getAIOStreamsPassword() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val numId = tmdbId.removePrefix("tmdb:").toIntOrNull() ?: return@launch
                if (apiKey.isEmpty()) {
                    _error.value = "API Key required for metadata lookup"
                    return@launch
                }

                val exIds = if(type=="movie") TMDBClient.api.getMovieExternalIds(numId, apiKey)
                else TMDBClient.api.getTVExternalIds(numId, apiKey)

                val imdb = exIds.imdbId ?: return@launch
                val api = AIOStreamsClient.getApi(user, pass)
                val res = api.searchStreams(type, imdb)
                _streams.value = res.data?.results ?: emptyList()
            } catch (e: Exception) {
                _streams.value = emptyList()
                Log.e("MainViewModel", "Error loading streams: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadEpisodeStreams(tmdbId: String, season: Int, episode: Int) {
        val user = prefsManager.getAIOStreamsUsername() ?: return
        val pass = prefsManager.getAIOStreamsPassword() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val numId = tmdbId.removePrefix("tmdb:").toIntOrNull() ?: return@launch
                if (apiKey.isEmpty()) {
                    _error.value = "API Key required"
                    return@launch
                }

                val exIds = TMDBClient.api.getTVExternalIds(numId, apiKey)
                val imdb = exIds.imdbId ?: return@launch
                val api = AIOStreamsClient.getApi(user, pass)
                val res = api.searchStreams("series", "$imdb:$season:$episode")
                _streams.value = res.data?.results ?: emptyList()
            } catch (e: Exception) {
                _streams.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadSeriesMeta(tmdbId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val numId = tmdbId.removePrefix("tmdb:").toIntOrNull() ?: return@launch
                if (apiKey.isEmpty()) return@launch

                val res = TMDBClient.api.getSeriesDetail(numId, apiKey)
                val videos = res.seasons.flatMap { s -> (1..s.episodeCount).map { e ->
                    Video(
                        id = "tmdb:$numId:${s.seasonNumber}:$e",
                        title = "Episode $e",
                        released = s.airDate,
                        thumbnail = s.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" },
                        number = e,
                        season = s.seasonNumber
                    )
                }}
                val meta = Meta(
                    id = tmdbId,
                    type = "series",
                    name = res.name,
                    poster = res.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" },
                    background = res.backdropPath?.let { "https://image.tmdb.org/t/p/original$it" },
                    description = res.overview,
                    videos = videos
                )
                _metaDetails.value = meta
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error loading series meta: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearStreams() { _streams.value = emptyList() }

    fun fetchCast(tmdbId: String, type: String) {
        val id = tmdbId.removePrefix("tmdb:").toIntOrNull() ?: return
        viewModelScope.launch {
            try {
                if (apiKey.isEmpty()) return@launch
                val response = if (type == "movie") {
                    TMDBClient.api.getMovieCredits(id, apiKey)
                } else {
                    TMDBClient.api.getTVCredits(id, apiKey)
                }

                // Top 5 Cast
                _castList.value = response.cast.take(5)

                // Find Director
                val directorItem = response.crew.find { it.job == "Director" }
                _director.value = directorItem?.name

            } catch (e: Exception) {
                Log.e("MainViewModel", "Error fetching credits: ${e.message}")
                _castList.value = emptyList()
                _director.value = null
            }
        }
    }

    fun clearCast() { _castList.value = emptyList() }

    fun loadPersonCredits(personId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _currentCatalogContent.value = emptyList()
            try {
                if (apiKey.isEmpty()) {
                    _error.value = "API Key required"
                    return@launch
                }
                val response = TMDBClient.api.getPersonCombinedCredits(personId, apiKey)
                val credits = response.cast.map { it.toMetaItem() }.filter { it.poster != null }
                _currentCatalogContent.value = credits
            } catch (e: Exception) {
                _error.value = "Failed to load person credits: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
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
                    _isItemInWatchlist.value = addToWatchlist // Update UI state
                } else {
                    _error.value = "Failed: ${response.statusMessage}"
                }
            } catch (e: Exception) {
                _error.value = "Watchlist Error: ${e.message}"
            }
        }
    }

    fun searchTMDB(query: String) {
        if (query.isBlank()) { _searchResults.value = emptyList(); return }
        viewModelScope.launch {
            _isSearching.value = true
            try {
                if (apiKey.isEmpty()) {
                    _error.value = "API Key required for search"
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
                if (apiKey.isEmpty()) {
                    _error.value = "API Key missing"
                    return@launch
                }
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