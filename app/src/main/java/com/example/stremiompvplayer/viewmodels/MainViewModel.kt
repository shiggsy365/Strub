package com.example.stremiompvplayer.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.example.stremiompvplayer.CatalogRepository
import com.example.stremiompvplayer.models.*
import com.example.stremiompvplayer.network.*
import com.example.stremiompvplayer.utils.Secrets
import com.example.stremiompvplayer.utils.SessionCache
import com.example.stremiompvplayer.utils.SharedPreferencesManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.stremiompvplayer.network.TraktIds
import com.example.stremiompvplayer.network.TraktMovie
import com.example.stremiompvplayer.models.CollectedItem
import com.example.stremiompvplayer.models.TMDBMovieListResponse
import com.example.stremiompvplayer.models.TMDBSeriesListResponse
import com.example.stremiompvplayer.models.WatchProgress
import com.example.stremiompvplayer.network.TraktHistoryBody
import com.example.stremiompvplayer.network.TraktShow
import com.example.stremiompvplayer.network.TraktScrobbleBody
import com.example.stremiompvplayer.network.TraktEpisode
import com.example.stremiompvplayer.network.TraktSeason
import com.example.stremiompvplayer.models.Video
import com.example.stremiompvplayer.models.TMDBWatchlistBody
import com.example.stremiompvplayer.network.AIOStreamsClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainViewModel(
    private val catalogRepository: CatalogRepository,
    private val prefsManager: SharedPreferencesManager
) : ViewModel() {

    // --- Core LiveData ---
    private val _currentCatalogContent = MutableLiveData<List<MetaItem>>()
    val currentCatalogContent: LiveData<List<MetaItem>> = _currentCatalogContent

    private val _currentLogo = MutableLiveData<String?>()
    val currentLogo: LiveData<String?> = _currentLogo
    private var logoFetchJob: Job? = null

    // --- Genre Selection ---
    private val _movieGenres = MutableLiveData<List<TMDBGenre>>()
    val movieGenres: LiveData<List<TMDBGenre>> = _movieGenres

    private val _tvGenres = MutableLiveData<List<TMDBGenre>>()
    val tvGenres: LiveData<List<TMDBGenre>> = _tvGenres

    private val _selectedGenre = MutableLiveData<TMDBGenre?>()
    val selectedGenre: LiveData<TMDBGenre?> = _selectedGenre

    private val apiKey: String get() = prefsManager.getTMDBApiKey() ?: ""

    // Session cache for performance optimization (5-min TTL)
    private val sessionCache = SessionCache.getInstance()

    // Track current catalog to allow refreshing
    private var lastRequestedCatalog: UserCatalog? = null
    private var loadedContentCache: MutableList<MetaItem> = mutableListOf()

    // === CATALOG CONFIGURATION ===
    // Raw source from DB
    private val _allCatalogsRaw: LiveData<List<UserCatalog>> = catalogRepository.allCatalogs

    // FILTERED Config List: Used by Settings & Discover
    // Removes "Next Up" and "Continue Watching" so they only appear on Home
    val allCatalogConfigs: LiveData<List<UserCatalog>> = _allCatalogsRaw.map { list ->
        list.filter { cat ->
            val id = cat.catalogId
            // Exclude these IDs from the configurable list
            id != "next_up" &&
                    id != "continue_movies" &&
                    id != "continue_episodes" &&
                    id != "trakt_next_up" &&
                    id != "trakt_continue_movies" &&
                    id != "trakt_continue_shows"
        }
    }

    val movieCatalogs = allCatalogConfigs.map { it.filter { c -> c.catalogType == "movie" && c.showInUser }.sortedBy { c -> c.displayOrder } }
    val seriesCatalogs = allCatalogConfigs.map { it.filter { c -> c.catalogType == "series" && c.showInUser }.sortedBy { c -> c.displayOrder } }

    // === HOME SCREEN LIVE DATA ===
    private val _homeNextUp = MutableLiveData<List<MetaItem>>()
    val homeNextUp: LiveData<List<MetaItem>> = _homeNextUp

    private val _homeContinueEpisodes = MutableLiveData<List<MetaItem>>()
    val homeContinueEpisodes: LiveData<List<MetaItem>> = _homeContinueEpisodes

    private val _homeContinueMovies = MutableLiveData<List<MetaItem>>()
    val homeContinueMovies: LiveData<List<MetaItem>> = _homeContinueMovies

    // UI State
    private val _streams = MutableLiveData<List<Stream>>()
    val streams: LiveData<List<Stream>> = _streams
    private val _metaDetails = MutableLiveData<Meta?>()
    val metaDetails: LiveData<Meta?> = _metaDetails
    private val _seasonEpisodes = MutableLiveData<List<MetaItem>>()
    val seasonEpisodes: LiveData<List<MetaItem>> = _seasonEpisodes
    private val _castList = MutableLiveData<List<MetaItem>>()
    val castList: LiveData<List<MetaItem>> = _castList
    private val _director = MutableLiveData<MetaItem?>()
    val director: LiveData<MetaItem?> = _director
    private val _isCastLoading = MutableLiveData<Boolean>(false)
    val isCastLoading: LiveData<Boolean> = _isCastLoading
    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    private val _actionResult = MutableLiveData<ActionResult>()
    val actionResult: LiveData<ActionResult> = _actionResult

    // Trakt State
    private val _traktDeviceCode = MutableLiveData<TraktDeviceCodeResponse?>()
    val traktDeviceCode: LiveData<TraktDeviceCodeResponse?> = _traktDeviceCode
    private val _isTraktEnabled = MutableLiveData<Boolean>(prefsManager.isTraktEnabled())
    val isTraktEnabled: LiveData<Boolean> = _isTraktEnabled
    private val _traktSyncProgress = MutableLiveData<String>()
    val traktSyncProgress: LiveData<String> = _traktSyncProgress
    private val _traktSyncStatus = MutableLiveData<TraktSyncStatus>()
    val traktSyncStatus: LiveData<TraktSyncStatus> = _traktSyncStatus

    // EPG/TV Guide State
    private val _epgParsingStatus = MutableLiveData<String>()
    val epgParsingStatus: LiveData<String> = _epgParsingStatus

    // Library Sources (Local + Trakt)
    val libraryMovies = MediatorLiveData<List<MetaItem>>()
    val librarySeries = MediatorLiveData<List<MetaItem>>()

    private val _localMoviesRaw = catalogRepository.getLibraryItems(prefsManager.getCurrentUserId() ?: "default", "movie").map { items ->
        items.map { toMetaItem(it) }
    }
    private val _localSeriesRaw = catalogRepository.getLibraryItems(prefsManager.getCurrentUserId() ?: "default", "series").map { items ->
        items.map { toMetaItem(it) }
    }

    private val _traktMoviesRaw = MutableLiveData<List<MetaItem>>()
    private val _traktSeriesRaw = MutableLiveData<List<MetaItem>>()

    // Filtered library for UI
    private val _filteredLibraryMovies = MutableLiveData<List<MetaItem>>()
    val filteredLibraryMovies: LiveData<List<MetaItem>> = _filteredLibraryMovies

    private val _filteredLibrarySeries = MutableLiveData<List<MetaItem>>()
    val filteredLibrarySeries: LiveData<List<MetaItem>> = _filteredLibrarySeries

    // Library Status Checks
    private val _isItemInWatchlist = MutableLiveData<Boolean>(false)
    val isItemInWatchlist: LiveData<Boolean> = _isItemInWatchlist
    private val _isItemInLibrary = MutableLiveData<Boolean>(false)
    val isItemInLibrary: LiveData<Boolean> = _isItemInLibrary
    private val _isItemWatched = MutableLiveData<Boolean>(false)
    val isItemWatched: LiveData<Boolean> = _isItemWatched

    // Search & Auth
    private val _searchResults = MutableLiveData<List<MetaItem>>()
    val searchResults: LiveData<List<MetaItem>> = _searchResults
    private val _isSearching = MutableLiveData<Boolean>()
    val isSearching: LiveData<Boolean> = _isSearching
    private val _requestToken = MutableLiveData<String?>()
    val requestToken: LiveData<String?> = _requestToken
    private val _sessionId = MutableLiveData<String?>()
    val sessionId: LiveData<String?> = _sessionId

    init {
        setupLibrarySources()
        initDefaultCatalogs()
        initUserLists()
        if (prefsManager.isTraktEnabled()) {
            syncTraktLibrary()
            startPeriodicTraktSync()
        }
    }

    // === HELPER FUNCTIONS ===
    private fun toMetaItem(item: CollectedItem): MetaItem {
        return MetaItem(item.itemId, item.itemType, item.name, item.poster, item.background, item.description)
    }

    fun getHomeCatalogs(): List<UserCatalog> {
        val userId = prefsManager.getCurrentUserId() ?: "default"
        val isTrakt = prefsManager.isTraktEnabled()

        return listOf(
            // Next Up
            if (isTrakt) UserCatalog(0, userId, "trakt_next_up", "series", "Next Up", null, 0, "series", "trakt", "trakt_official", true, true)
            else UserCatalog(0, userId, "next_up", "series", "Next Up", null, 0, "series", "local", "local", true, true),

            // Continue Movies
            if (isTrakt) UserCatalog(0, userId, "trakt_continue_movies", "movie", "Continue Movies", null, 1, "movies", "trakt", "trakt_official", true, true)
            else UserCatalog(0, userId, "continue_movies", "movie", "Continue Movies", null, 1, "movies", "local", "local", true, true),

            // Continue Episodes
            if (isTrakt) UserCatalog(0, userId, "trakt_continue_shows", "series", "Continue Series", null, 2, "series", "trakt", "trakt_official", true, true)
            else UserCatalog(0, userId, "continue_episodes", "series", "Continue Series", null, 2, "series", "local", "local", true, true)
        )
    }

    suspend fun isItemInLibrarySync(itemId: String): Boolean {
        val currentUserId = prefsManager.getCurrentUserId() ?: return false
        return catalogRepository.isItemCollected(itemId, currentUserId)
    }

    // === LIBRARY SOURCE MANAGEMENT ===
    private fun setupLibrarySources() {
        libraryMovies.addSource(_isTraktEnabled) { updateMovieSource() }
        libraryMovies.addSource(_localMoviesRaw) { updateMovieSource() }
        libraryMovies.addSource(_traktMoviesRaw) { updateMovieSource() }

        librarySeries.addSource(_isTraktEnabled) { updateSeriesSource() }
        librarySeries.addSource(_localSeriesRaw) { updateSeriesSource() }
        librarySeries.addSource(_traktSeriesRaw) { updateSeriesSource() }
    }

    private fun updateMovieSource() {
        val localItems = _localMoviesRaw.value ?: emptyList()
        val traktItems = if (_isTraktEnabled.value == true) _traktMoviesRaw.value ?: emptyList() else emptyList()
        val combined = (localItems + traktItems).distinctBy { it.id }
        libraryMovies.value = combined
        filterAndSortLibrary("movie")
    }

    private fun updateSeriesSource() {
        val localItems = _localSeriesRaw.value ?: emptyList()
        val traktItems = if (_isTraktEnabled.value == true) _traktSeriesRaw.value ?: emptyList() else emptyList()
        val combined = (localItems + traktItems).distinctBy { it.id }
        librarySeries.value = combined
        filterAndSortLibrary("series")
    }

    // === TRAKT AUTHENTICATION ===
    fun startTraktAuth() {
        viewModelScope.launch {
            try {
                val body = mapOf("client_id" to Secrets.TRAKT_CLIENT_ID)
                val codeResponse = TraktClient.api.getDeviceCode(body)
                _traktDeviceCode.postValue(codeResponse)
                pollTraktToken(codeResponse)
            } catch (e: Exception) {
                _error.postValue("Trakt Auth Error: ${e.message}")
            }
        }
    }

    private fun pollTraktToken(codeData: TraktDeviceCodeResponse) {
        viewModelScope.launch {
            var attempts = 0
            val interval = if (codeData.interval > 0) codeData.interval else 5
            val maxAttempts = codeData.expires_in / interval

            while (attempts < maxAttempts) {
                delay(interval * 1000L)
                try {
                    val body = mapOf(
                        "code" to codeData.device_code,
                        "client_id" to Secrets.TRAKT_CLIENT_ID,
                        "client_secret" to Secrets.TRAKT_CLIENT_SECRET
                    )
                    val token = TraktClient.api.getDeviceToken(body)

                    prefsManager.saveTraktTokens(token.access_token, token.refresh_token)
                    _isTraktEnabled.postValue(true)
                    _traktDeviceCode.postValue(null)

                    // Auto-sync on successful auth
                    syncTraktLibrary()

                    // Trigger full setup including lists with metadata
                    performTraktSync(syncHistory = true, syncNextUp = true, syncLists = true, fetchMetadata = true)

                    startPeriodicTraktSync()

                    _actionResult.postValue(ActionResult.Success("Trakt connected successfully!"))
                    return@launch
                } catch (e: Exception) {
                    attempts++
                }
            }
            _error.postValue("Trakt Auth Timed Out")
        }
    }

    fun logoutTrakt() {
        prefsManager.clearTraktData()
        _isTraktEnabled.postValue(false)
        _traktMoviesRaw.postValue(emptyList())
        _traktSeriesRaw.postValue(emptyList())
        _actionResult.postValue(ActionResult.Success("Trakt disconnected"))
    }

    // === ENHANCED TRAKT SYNC ===
    fun syncTraktLibrary() {
        val token = prefsManager.getTraktAccessToken() ?: return
        val bearer = "Bearer $token"
        val clientId = Secrets.TRAKT_CLIENT_ID

        _isLoading.postValue(true)
        viewModelScope.launch {
            try {
                // Fetch Movies
                val movies = TraktClient.api.getMovieCollection(bearer, clientId)
                val metaMovies = movies.mapNotNull { it.movie }.map { movie ->
                    MetaItem(
                        id = "tmdb:${movie.ids.tmdb}",
                        type = "movie",
                        name = movie.title,
                        poster = null,
                        background = null,
                        description = null,
                        releaseDate = movie.year?.toString()
                    )
                }
                _traktMoviesRaw.postValue(metaMovies)

                // Fetch Shows
                val shows = TraktClient.api.getShowCollection(bearer, clientId)
                val metaShows = shows.mapNotNull { it.show }.map { show ->
                    MetaItem(
                        id = "tmdb:${show.ids.tmdb}",
                        type = "series",
                        name = show.title,
                        poster = null,
                        background = null,
                        description = null,
                        releaseDate = show.year?.toString()
                    )
                }
                _traktSeriesRaw.postValue(metaShows)

            } catch (e: Exception) {
                Log.e("Trakt", "Sync failed", e)
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    // === GENRE MANAGEMENT ===

    fun fetchGenres(catalogType: String) {
        if (apiKey.isEmpty()) {
            Log.e("GenreFetch", "API key is empty!")
            return
        }

        viewModelScope.launch {
            try {
                val response = if (catalogType == "movie") {
                    TMDBClient.api.getMovieGenres(apiKey)
                } else {
                    TMDBClient.api.getTVGenres(apiKey)
                }

                if (catalogType == "movie") {
                    _movieGenres.postValue(response.genres)
                    Log.d("GenreFetch", "Loaded ${response.genres.size} movie genres")
                } else {
                    _tvGenres.postValue(response.genres)
                    Log.d("GenreFetch", "Loaded ${response.genres.size} TV genres")
                }
            } catch (e: Exception) {
                Log.e("GenreFetch", "Error fetching $catalogType genres", e)
            }
        }
    }

    fun selectGenre(genre: com.example.stremiompvplayer.models.TMDBGenre?) {
        _selectedGenre.postValue(genre)
        Log.d("GenreSelection", "Selected genre: ${genre?.name ?: "None"}")
    }

    fun clearGenreSelection() {
        _selectedGenre.postValue(null)
        Log.d("GenreSelection", "Cleared genre selection")
    }

    // === CONTENT LOADING (Refactored) ===

    // Original method used by DiscoverFragment
    fun loadContentForCatalog(catalog: UserCatalog, isInitialLoad: Boolean = true) {
        if (apiKey.isEmpty()) {
            Log.e("CatalogLoad", "API key is empty! Catalog: ${catalog.catalogId}")
            return
        }

        Log.d("CatalogLoad", "Loading catalog: ${catalog.catalogId} (${catalog.displayName})")
        _isLoading.postValue(true)
        if (isInitialLoad) {
            loadedContentCache.clear()
            lastRequestedCatalog = catalog
        }

        viewModelScope.launch {
            try {
                val items = fetchCatalogItems(catalog)
                Log.d("CatalogLoad", "Fetched ${items.size} items for ${catalog.catalogId}")

                if (isInitialLoad) {
                    loadedContentCache.addAll(items)
                    _currentCatalogContent.postValue(loadedContentCache)
                } else {
                    _currentCatalogContent.postValue(items)
                }
            } catch (e: Exception) {
                Log.e("CatalogLoad", "Error loading ${catalog.catalogId}", e)
                _error.postValue("Load failed: ${e.message}")
                if (isInitialLoad) {
                    _currentCatalogContent.postValue(emptyList())
                }
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    // Load all Home sections in parallel (Called by HomeFragment)
    fun loadHomeContent() {
        if (apiKey.isEmpty()) {
            Log.e("HomeLoad", "API key is empty! Cannot load home content")
            return
        }

        viewModelScope.launch {
            _isLoading.postValue(true)

            val userId = prefsManager.getCurrentUserId() ?: "default"
            val isTrakt = prefsManager.isTraktEnabled()
            Log.d("HomeLoad", "Loading home content - userId: $userId, isTrakt: $isTrakt")

            // Create catalogs on the fly for Home (skipping the filtered DB list)
            val nextUpCat = if (isTrakt) UserCatalog(0, userId, "trakt_next_up", "series", "Next Up", null, 0, "series", "trakt", "trakt_official", true, true)
            else UserCatalog(0, userId, "next_up", "series", "Next Up", null, 0, "series", "local", "local", true, true)

            val continueShowCat = if (isTrakt) UserCatalog(0, userId, "trakt_continue_shows", "series", "Continue Watching", null, 0, "series", "trakt", "trakt_official", true, true)
            else UserCatalog(0, userId, "continue_episodes", "series", "Continue Watching", null, 0, "series", "local", "local", true, true)

            val continueMovieCat = if (isTrakt) UserCatalog(0, userId, "trakt_continue_movies", "movie", "Continue Watching", null, 0, "movies", "trakt", "trakt_official", true, true)
            else UserCatalog(0, userId, "continue_movies", "movie", "Continue Watching", null, 0, "movies", "local", "local", true, true)

            val nextUpJob = async { fetchCatalogItems(nextUpCat) }
            val showsJob = async { fetchCatalogItems(continueShowCat) }
            val moviesJob = async { fetchCatalogItems(continueMovieCat) }

            try {
                val nextUp = nextUpJob.await()
                val shows = showsJob.await()
                val movies = moviesJob.await()

                Log.d("HomeLoad", "Loaded - NextUp: ${nextUp.size}, Shows: ${shows.size}, Movies: ${movies.size}")

                _homeNextUp.postValue(nextUp)
                _homeContinueEpisodes.postValue(shows)
                _homeContinueMovies.postValue(movies)
            } catch (e: Exception) {
                Log.e("HomeLoad", "Error loading home content", e)
                _error.postValue("Failed to load home content: ${e.message}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    // Reusable suspended function
    private suspend fun fetchCatalogItems(catalog: UserCatalog): List<MetaItem> {
        // Check for popular/latest/trending FIRST (before checking addonUrl)
        if (catalog.catalogId in listOf("popular", "latest", "trending")) {
            Log.d("CatalogLoad", "Fetching TMDB ${catalog.catalogId} ${catalog.catalogType}")
            val pagesToLoad = listOf(1, 2)
            val allItems = mutableListOf<MetaItem>()

            // Get current user's age rating
            val currentUserId = prefsManager.getCurrentUserId()
            val currentUser = currentUserId?.let { prefsManager.getUser(it) }
            val ageRating = currentUser?.ageRating ?: "18" // Default to 18 (no filtering) if not set
            Log.d("CatalogLoad", "Age Rating: $ageRating, userId: $currentUserId")

            // Get selected genre ID if any
            val selectedGenreId = _selectedGenre.value?.id?.toString()
            if (selectedGenreId != null) {
                Log.d("CatalogLoad", "Using genre filter: $selectedGenreId (${_selectedGenre.value?.name})")
            }

            val deferredResults = pagesToLoad.map { page ->
                viewModelScope.async {
                    try {
                        Log.d("CatalogLoad", "Loading page $page for ${catalog.catalogId}")
                        val response = when (ageRating) {
                            "U", "PG", "12", "15" -> {
                                // Use discover endpoints with age rating filtering
                                if (catalog.catalogType == "movie") {
                                    TMDBClient.api.discoverMovies(
                                        apiKey = apiKey,
                                        page = page,
                                        sortBy = "popularity.desc",
                                        certificationCountry = "GB",
                                        certificationLte = ageRating,
                                        withGenres = selectedGenreId,
                                        includeAdult = false
                                    )
                                } else {
                                    // For TV, use genre filtering based on age rating or user selection
                                    val genres = selectedGenreId ?: when (ageRating) {
                                        "U", "PG" -> "10762" // Kids genre
                                        else -> null // No genre filter for 12, 15
                                    }
                                    TMDBClient.api.discoverTV(
                                        apiKey = apiKey,
                                        page = page,
                                        sortBy = "popularity.desc",
                                        withGenres = genres,
                                        includeAdult = false
                                    )
                                }
                            }
                            else -> {
                                // Age rating 18 or not set - check if genre filter is applied
                                if (selectedGenreId != null) {
                                    // Use discover endpoints with genre filtering
                                    if (catalog.catalogType == "movie") {
                                        TMDBClient.api.discoverMovies(
                                            apiKey = apiKey,
                                            page = page,
                                            sortBy = "popularity.desc",
                                            withGenres = selectedGenreId,
                                            includeAdult = false
                                        )
                                    } else {
                                        TMDBClient.api.discoverTV(
                                            apiKey = apiKey,
                                            page = page,
                                            sortBy = "popularity.desc",
                                            withGenres = selectedGenreId,
                                            includeAdult = false
                                        )
                                    }
                                } else {
                                    // No genre filter - use standard endpoints
                                    if (catalog.catalogType == "movie") {
                                        when (catalog.catalogId) {
                                            "popular" -> TMDBClient.api.getPopularMovies(apiKey, page = page)
                                            "latest" -> TMDBClient.api.getLatestMovies(apiKey, page = page)
                                            else -> TMDBClient.api.getTrendingMovies(apiKey, page = page)
                                        }
                                    } else {
                                        when (catalog.catalogId) {
                                            "popular" -> TMDBClient.api.getPopularSeries(apiKey, page = page)
                                            "latest" -> TMDBClient.api.getLatestSeries(apiKey, page = page)
                                            else -> TMDBClient.api.getTrendingSeries(apiKey, page = page)
                                        }
                                    }
                                }
                            }
                        }
                        Log.d("CatalogLoad", "Page $page loaded successfully: ${response}")
                        response
                    } catch (e: Exception) {
                        Log.e("CatalogLoad", "Error fetching TMDB ${catalog.catalogId} ${catalog.catalogType} page $page", e)
                        _error.postValue("Failed to load ${catalog.catalogId} ${catalog.catalogType}: ${e.message}")
                        null
                    }
                }
            }
            val results = deferredResults.awaitAll()
            Log.d("CatalogLoad", "Got ${results.filterNotNull().size} non-null responses")
            results.filterNotNull().forEach { response ->
                val fetchedItems = if (response is TMDBMovieListResponse) {
                    Log.d("CatalogLoad", "MovieListResponse has ${response.results.size} results")
                    response.results.map { it.toMetaItem() }
                } else if (response is TMDBSeriesListResponse) {
                    Log.d("CatalogLoad", "SeriesListResponse has ${response.results.size} results")
                    response.results.map { it.toMetaItem() }
                } else {
                    Log.w("CatalogLoad", "Unknown response type: ${response::class.simpleName}")
                    emptyList()
                }
                Log.d("CatalogLoad", "Adding ${fetchedItems.size} items to allItems")
                allItems.addAll(fetchedItems)
            }
            Log.d("CatalogLoad", "Returning ${allItems.size} total items")
            return allItems

        } else if (catalog.addonUrl == "trakt") {
            val token = prefsManager.getTraktAccessToken()
            val clientId = Secrets.TRAKT_CLIENT_ID

            if (token != null) {
                val bearer = "Bearer $token"
                val items: List<MetaItem> = when (catalog.catalogId) {
                    "trakt_next_up" -> generateNextUpList()

                    "trakt_continue_movies" -> {
                        // PERFORMANCE: Fetch all progress once instead of querying in loop
                        val userId = prefsManager.getCurrentUserId() ?: "default"
                        val allProgress = catalogRepository.getAllWatchProgress(userId)
                        val progressMap = allProgress.associateBy { it.itemId }

                        val list = TraktClient.api.getPausedMovies(bearer, clientId)
                        list.mapNotNull { it.movie }
                            // [FIX] Filter out items that are watched OR have progress > 90% in LOCAL DB
                            .filter { movie ->
                                val tmdbId = movie.ids.tmdb
                                if (tmdbId == null) return@filter true

                                val progress = progressMap["tmdb:$tmdbId"]
                                val isLocallyWatched = progress?.isWatched == true
                                val isLocallyFinished = progress?.let { p -> p.duration > 0 && (p.progress.toFloat() / p.duration.toFloat() > 0.9f) } == true

                                !isLocallyWatched && !isLocallyFinished
                            }
                            .map { movie ->
                                MetaItem(id = "tmdb:${movie.ids.tmdb}", type = "movie", name = movie.title, description = "Paused", poster = null, background = null)
                            }
                    }

                    "trakt_continue_shows" -> {
                        // PERFORMANCE: Fetch all progress once instead of querying in loop
                        val userId = prefsManager.getCurrentUserId() ?: "default"
                        val allProgress = catalogRepository.getAllWatchProgress(userId)
                        val progressMap = allProgress.associateBy { it.itemId }

                        val list = TraktClient.api.getPausedEpisodes(bearer, clientId)
                        list.mapNotNull { item ->
                            val show = item.show
                            val ep = item.episode
                            if (show != null && ep != null && ep.ids?.tmdb != null) {
                                val showTmdbId = show.ids.tmdb
                                val epTmdbId = ep.ids.tmdb
                                val season = ep.season ?: 1
                                val episode = ep.number ?: 1
                                val epId = "tmdb:$showTmdbId:$season:$episode" // Construct standard local ID

                                // CHECK LOCAL DB using map lookup
                                val progress = progressMap[epId]
                                val isLocallyWatched = progress?.isWatched == true
                                val isLocallyFinished = progress?.let { p -> p.duration > 0 && (p.progress.toFloat() / p.duration.toFloat() > 0.9f) } == true

                                if (!isLocallyWatched && !isLocallyFinished) {
                                    MetaItem(
                                        id = epId,
                                        type = "episode",
                                        name = "${show.title} - S${season.toString().padStart(2, '0')}E${episode.toString().padStart(2, '0')}",
                                        description = "Paused at ${(item.progress ?: 0f).toInt()}%",
                                        poster = null, background = null
                                    )
                                } else null
                            } else null
                        }
                    }

                    "trakt_watchlist_movies" -> {
                        TraktClient.api.getWatchlist(bearer, clientId, type = "movies")
                            .mapNotNull { it.movie }
                            .map { MetaItem("tmdb:${it.ids.tmdb}", "movie", it.title, null, null, null) }
                    }
                    "trakt_watchlist_shows" -> {
                        TraktClient.api.getWatchlist(bearer, clientId, type = "shows")
                            .mapNotNull { it.show }
                            .map { MetaItem("tmdb:${it.ids.tmdb}", "series", it.title, null, null, null) }
                    }

                    "trakt_popular_movies" -> TraktClient.api.getPopularMovies(clientId).map { MetaItem("tmdb:${it.ids.tmdb}", "movie", it.title, null, null, null) }
                    "trakt_popular_shows" -> TraktClient.api.getPopularShows(clientId).map { MetaItem("tmdb:${it.ids.tmdb}", "series", it.title, null, null, null) }
                    "trakt_trending_movies" -> TraktClient.api.getTrendingMovies(clientId).mapNotNull { it.movie }.map { MetaItem("tmdb:${it.ids.tmdb}", "movie", it.title, null, null, null) }
                    "trakt_trending_shows" -> TraktClient.api.getTrendingShows(clientId).mapNotNull { it.show }.map { MetaItem("tmdb:${it.ids.tmdb}", "series", it.title, null, null, null) }

                    else -> {
                        // Custom Trakt list: format is "username/listname" or full URL
                        val listPath = catalog.manifestId
                        try {
                            // Parse username and list name from the path
                            val cleanPath = listPath.replace("https://trakt.tv/users/", "")
                                .replace("http://trakt.tv/users/", "")
                                .replace("trakt.tv/users/", "")
                                .replace("/lists/", "/")
                                .trim('/')

                            val parts = cleanPath.split("/")
                            if (parts.size >= 2) {
                                val username = parts[0]
                                val listName = parts[1]

                                val listItems = TraktClient.api.getUserListItems(clientId, username, listName)
                                listItems.mapNotNull { item ->
                                    when (item.type) {
                                        "movie" -> item.movie?.let {
                                            MetaItem("tmdb:${it.ids.tmdb}", "movie", it.title, null, null, null)
                                        }
                                        "show" -> item.show?.let {
                                            MetaItem("tmdb:${it.ids.tmdb}", "series", it.title, null, null, null)
                                        }
                                        else -> null
                                    }
                                }
                            } else {
                                emptyList()
                            }
                        } catch (e: Exception) {
                            emptyList()
                        }
                    }
                }

                return enrichWithTmdbMetadata(items, catalog.catalogId)
            } else {
                return emptyList()
            }

        } else if (catalog.addonUrl == "tmdb") {
            // Custom TMDB list
            val listId = catalog.manifestId
            try {
                val response = TMDBClient.api.getList(listId, apiKey)
                return response.items.map { it.toMetaItem() }
            } catch (e: Exception) {
                return emptyList()
            }

        } else if (catalog.addonUrl == "mdblist") {
            // mdblist integration - Placeholder
            return emptyList()

        } else if (catalog.addonUrl == "imdb") {
            // IMDB list integration - Placeholder
            return emptyList()

        } else {
            // Local Logic (App Next Up, App Continue)
            return when (catalog.catalogId) {
                "continue_movies" -> {
                    val currentUserId = prefsManager.getCurrentUserId() ?: "default"
                    catalogRepository.getContinueWatching(currentUserId, "movie")
                        .filter { !it.isWatched && (it.duration == 0L || (it.progress.toFloat() / it.duration.toFloat()) < 0.9f) }
                        .map { progress ->
                            MetaItem(id = progress.itemId, type = progress.type, name = progress.name ?: "Unknown", poster = progress.poster, background = progress.background, description = null, isWatched = progress.isWatched, progress = progress.progress, duration = progress.duration)
                        }
                }
                "continue_episodes" -> {
                    val currentUserId = prefsManager.getCurrentUserId() ?: "default"
                    val episodes = catalogRepository.getContinueWatching(currentUserId, "episode")
                        .filter { !it.isWatched && (it.duration == 0L || (it.progress.toFloat() / it.duration.toFloat()) < 0.9f) }

                    // Enrich with TMDB episode details
                    episodes.mapNotNull { progress ->
                        try {
                            val parts = progress.itemId.split(":")
                            if (parts.size >= 4 && apiKey.isNotEmpty()) {
                                val showId = parts[1].toIntOrNull()
                                val seasonNum = parts[2].toIntOrNull()
                                val episodeNum = parts[3].toIntOrNull()

                                if (showId != null && seasonNum != null && episodeNum != null) {
                                    val showDetails = TMDBClient.api.getTVDetails(showId, apiKey)
                                    val seasonDetails = TMDBClient.api.getTVSeasonDetails(showId, seasonNum, apiKey)
                                    val episode = seasonDetails.episodes.find { it.episode_number == episodeNum }

                                    if (episode != null) {
                                        MetaItem(
                                            id = progress.itemId,
                                            type = progress.type,
                                            name = "${showDetails.name} - ${episode.name}",
                                            poster = progress.poster ?: episode.still_path?.let { "https://image.tmdb.org/t/p/w500$it" },
                                            background = progress.background,
                                            description = episode.overview,
                                            isWatched = progress.isWatched,
                                            progress = progress.progress,
                                            duration = progress.duration
                                        )
                                    } else {
                                        // Fallback if episode not found
                                        MetaItem(id = progress.itemId, type = progress.type, name = progress.name ?: "Unknown", poster = progress.poster, background = progress.background, description = null, isWatched = progress.isWatched, progress = progress.progress, duration = progress.duration)
                                    }
                                } else {
                                    // Fallback if parsing fails
                                    MetaItem(id = progress.itemId, type = progress.type, name = progress.name ?: "Unknown", poster = progress.poster, background = progress.background, description = null, isWatched = progress.isWatched, progress = progress.progress, duration = progress.duration)
                                }
                            } else {
                                // Fallback if no API key or wrong ID format
                                MetaItem(id = progress.itemId, type = progress.type, name = progress.name ?: "Unknown", poster = progress.poster, background = progress.background, description = null, isWatched = progress.isWatched, progress = progress.progress, duration = progress.duration)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            // Fallback on error
                            MetaItem(id = progress.itemId, type = progress.type, name = progress.name ?: "Unknown", poster = progress.poster, background = progress.background, description = null, isWatched = progress.isWatched, progress = progress.progress, duration = progress.duration)
                        }
                    }
                }
                "next_up" -> generateNextUpList()
                else -> emptyList()
            }
        }
    }

    private suspend fun enrichWithTmdbMetadata(items: List<MetaItem>, catalogId: String): List<MetaItem> {
        return items.map { item ->
            viewModelScope.async {
                try {
                    if (item.id.startsWith("trakt_ep:")) {
                        val parts = item.id.split(":")
                        val showTmdbId = parts[1].toIntOrNull()

                        if (showTmdbId != null && apiKey.isNotEmpty()) {
                            val details = TMDBClient.api.getTVDetails(showTmdbId, apiKey)
                            val poster = details.poster_path?.let { "https://image.tmdb.org/t/p/w500$it" }
                            val background = details.backdrop_path?.let { "https://image.tmdb.org/t/p/original$it" }

                            val newId = "tmdb:$showTmdbId:1:1"

                            item.copy(
                                id = newId,
                                poster = poster,
                                background = background,
                                name = "${details.name} - ${item.name}",
                                description = "${item.description}\n\n(Resume info limited)"
                            )
                        } else item
                    } else {
                        val parts = item.id.removePrefix("tmdb:").split(":")
                        val tmdbId = parts[0].toIntOrNull()

                        if (tmdbId != null && apiKey.isNotEmpty()) {
                            if (item.type == "movie") {
                                val details = TMDBClient.api.getMovieDetails(tmdbId, apiKey)
                                val poster = details.poster_path?.let { "https://image.tmdb.org/t/p/w500$it" }
                                val background = details.backdrop_path?.let { "https://image.tmdb.org/t/p/original$it" }
                                item.copy(poster = poster, background = background, description = details.overview)
                            } else if (item.type == "episode") {
                                // For episodes, preserve episode description and use show poster/background
                                val details = TMDBClient.api.getTVDetails(tmdbId, apiKey)
                                val poster = details.poster_path?.let { "https://image.tmdb.org/t/p/w500$it" }
                                val background = details.backdrop_path?.let { "https://image.tmdb.org/t/p/original$it" }
                                item.copy(poster = poster, background = background)
                            } else {
                                // For series, use show description
                                val details = TMDBClient.api.getTVDetails(tmdbId, apiKey)
                                val poster = details.poster_path?.let { "https://image.tmdb.org/t/p/w500$it" }
                                val background = details.backdrop_path?.let { "https://image.tmdb.org/t/p/original$it" }
                                item.copy(poster = poster, background = background, description = details.overview)
                            }
                        } else item
                    }
                } catch (e: Exception) { item }
            }
        }.awaitAll()
    }

    // === NEXT UP GENERATION ===
    private suspend fun generateNextUpList(): List<MetaItem> = withContext(Dispatchers.IO) {
        val currentUserId = prefsManager.getCurrentUserId() ?: return@withContext emptyList()

        // PERFORMANCE: Check cache first (5-min TTL)
        val cached = sessionCache.getNextUp(currentUserId)
        if (cached != null) {
            Log.d("NextUp", "Using cached Next Up list")
            return@withContext cached
        }

        val watchedEpisodes = catalogRepository.getNextUpCandidates(currentUserId)
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val today = dateFormat.format(java.util.Date())

        // PERFORMANCE: Fetch all progress once to avoid queries in async loop
        val allProgress = catalogRepository.getAllWatchProgress(currentUserId)
        val progressMap = allProgress.associateBy { it.itemId }

        val latestEpisodesByShow = watchedEpisodes
            .filter { it.parentId != null && it.season != null && it.episode != null }
            .groupBy { it.parentId!! }
            .mapValues { (_, episodes) ->
                episodes.maxWithOrNull(compareBy<WatchProgress> { it.season!! }.thenBy { it.episode!! })
            }

        // Process all shows in parallel
        val nextUpItems = latestEpisodesByShow.mapNotNull { (showId, latestEpisode) ->
            async {
                if (latestEpisode == null) return@async null

                val tmdbId = showId.removePrefix("tmdb:").toIntOrNull() ?: return@async null
                val currentSeason = latestEpisode.season!!
                val currentEpisode = latestEpisode.episode!!

                try {
                    val showDetails = TMDBClient.api.getTVDetails(tmdbId, apiKey)
                    var nextSeasonNum = currentSeason
                    var nextEpisodeNum = currentEpisode + 1
                    var potentialNextFound = false

                    val currentSeasonSpec = showDetails.seasons?.find { it.season_number == currentSeason }
                    if (currentSeasonSpec != null && currentEpisode < currentSeasonSpec.episode_count) {
                        potentialNextFound = true
                    } else {
                        val nextSeasonSpec = showDetails.seasons?.find { it.season_number == currentSeason + 1 }
                        if (nextSeasonSpec != null && nextSeasonSpec.episode_count > 0) {
                            nextSeasonNum = currentSeason + 1
                            nextEpisodeNum = 1
                            potentialNextFound = true
                        }
                    }

                    if (potentialNextFound) {
                        try {
                            val seasonDetails = TMDBClient.api.getTVSeasonDetails(tmdbId, nextSeasonNum, apiKey)
                            val nextEpDetails = seasonDetails.episodes.find { it.episode_number == nextEpisodeNum }

                            if (nextEpDetails != null) {
                                val isReleased = nextEpDetails.airDate == null || nextEpDetails.airDate <= today

                                if (isReleased) {
                                    val nextEpisodeId = "$showId:$nextSeasonNum:$nextEpisodeNum"
                                    val nextEpisodeProgress = progressMap[nextEpisodeId]

                                    if (nextEpisodeProgress == null || !nextEpisodeProgress.isWatched) {
                                        val poster = nextEpDetails.still_path?.let { "https://image.tmdb.org/t/p/w500$it" }
                                            ?: showDetails.poster_path?.let { "https://image.tmdb.org/t/p/w500$it" }

                                        val background = nextEpDetails.still_path?.let { "https://image.tmdb.org/t/p/original$it" }
                                            ?: showDetails.backdrop_path?.let { "https://image.tmdb.org/t/p/original$it" }

                                        val metaItem = MetaItem(
                                            id = nextEpisodeId,
                                            type = "episode",
                                            name = nextEpDetails.name,
                                            poster = poster,
                                            background = background,
                                            description = nextEpDetails.overview ?: "Next episode"
                                        )
                                        return@async Pair(metaItem, latestEpisode.lastUpdated)
                                    }
                                }
                            }
                        } catch (e: Exception) { Log.e("NextUp", "Failed season details", e) }
                    }
                } catch (e: Exception) { Log.e("NextUp", "Error processing next episode", e) }
                null
            }
        }.awaitAll().filterNotNull()

        // Sort by lastUpdated (most recent first) and return only the MetaItems
        val result = nextUpItems.sortedByDescending { it.second }.map { it.first }

        // PERFORMANCE: Cache the result
        sessionCache.putNextUp(currentUserId, result)

        result
    }

    // === TRAKT SYNC ===
    fun performTraktSync(syncHistory: Boolean, syncNextUp: Boolean, syncLists: Boolean, fetchMetadata: Boolean = true) {
        if (!prefsManager.isTraktEnabled()) return

        _traktSyncStatus.postValue(TraktSyncStatus.Syncing("Syncing..."))
        viewModelScope.launch {
            val userId = prefsManager.getCurrentUserId() ?: "default"
            val token = prefsManager.getTraktAccessToken() ?: return@launch
            val bearer = "Bearer $token"
            val clientId = Secrets.TRAKT_CLIENT_ID

            try {
                if (syncHistory) {
                    _traktSyncStatus.postValue(TraktSyncStatus.Syncing("Fetching watched history..."))

                    val watchedMovies = TraktClient.api.getWatchedMovies(bearer, clientId)
                    val importedMovieIds = mutableListOf<Int>()

                    watchedMovies.forEach { item ->
                        item.movie?.ids?.tmdb?.let { tmdbId ->
                            try {
                                // Fetch full metadata from TMDB
                                val details = TMDBClient.api.getMovieDetails(tmdbId, apiKey)
                                val posterUrl = details.poster_path?.let { "https://image.tmdb.org/t/p/w500$it" }
                                val backgroundUrl = details.backdrop_path?.let { "https://image.tmdb.org/t/p/original$it" }

                                val meta = MetaItem(
                                    id = "tmdb:$tmdbId",
                                    type = "movie",
                                    name = details.title ?: item.movie.title,
                                    poster = posterUrl,
                                    background = backgroundUrl,
                                    description = details.overview,
                                    releaseDate = details.release_date,
                                    rating = details.vote_average?.toString()
                                )
                                catalogRepository.addToLibrary(CollectedItem.fromMetaItem(userId, meta))
                                catalogRepository.saveWatchProgress(WatchProgress(userId, meta.id, "movie", 0, 0, true, System.currentTimeMillis(), meta.name, posterUrl, null, null, null, null))
                                importedMovieIds.add(tmdbId)
                            } catch (e: Exception) {
                                // Fallback to minimal metadata if TMDB fetch fails
                                val meta = MetaItem("tmdb:$tmdbId", "movie", item.movie.title, null, null, "Trakt Import")
                                catalogRepository.addToLibrary(CollectedItem.fromMetaItem(userId, meta))
                                catalogRepository.saveWatchProgress(WatchProgress(userId, meta.id, "movie", 0, 0, true, System.currentTimeMillis(), meta.name, null, null, null, null, null))
                                importedMovieIds.add(tmdbId)
                            }
                        }
                    }

                    val watchedShows = TraktClient.api.getWatchedShows(bearer, clientId)
                    val importedShowIds = mutableListOf<Int>()

                    watchedShows.forEach { item ->
                        item.show?.ids?.tmdb?.let { showTmdbId ->
                            try {
                                // Fetch full metadata from TMDB
                                val details = TMDBClient.api.getTVDetails(showTmdbId, apiKey)
                                val posterUrl = details.poster_path?.let { "https://image.tmdb.org/t/p/w500$it" }
                                val backgroundUrl = details.backdrop_path?.let { "https://image.tmdb.org/t/p/original$it" }

                                val seriesMeta = MetaItem(
                                    id = "tmdb:$showTmdbId",
                                    type = "series",
                                    name = details.name ?: item.show.title,
                                    poster = posterUrl,
                                    background = backgroundUrl,
                                    description = details.overview,
                                    releaseDate = details.first_air_date,
                                    rating = details.vote_average?.toString()
                                )
                                catalogRepository.addToLibrary(CollectedItem.fromMetaItem(userId, seriesMeta))
                                importedShowIds.add(showTmdbId)

                                item.seasons?.forEach { season ->
                                    season.episodes.forEach { ep ->
                                        val epId = "tmdb:$showTmdbId:${season.number}:${ep.number}"
                                        catalogRepository.saveWatchProgress(
                                            WatchProgress(userId, epId, "episode", 0, 0, true, System.currentTimeMillis(),
                                                "${seriesMeta.name} S${season.number}E${ep.number}", posterUrl, null, "tmdb:$showTmdbId", season.number, ep.number)
                                        )
                                    }
                                }
                            } catch (e: Exception) {
                                // Fallback to minimal metadata if TMDB fetch fails
                                val seriesMeta = MetaItem("tmdb:$showTmdbId", "series", item.show.title, null, null, "Trakt Import")
                                catalogRepository.addToLibrary(CollectedItem.fromMetaItem(userId, seriesMeta))
                                importedShowIds.add(showTmdbId)

                                item.seasons?.forEach { season ->
                                    season.episodes.forEach { ep ->
                                        val epId = "tmdb:$showTmdbId:${season.number}:${ep.number}"
                                        catalogRepository.saveWatchProgress(
                                            WatchProgress(userId, epId, "episode", 0, 0, true, System.currentTimeMillis(),
                                                "${item.show.title} S${season.number}E${ep.number}", null, null, "tmdb:$showTmdbId", season.number, ep.number)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // NEW: Sync Paused Content to Local DB
                    _traktSyncStatus.postValue(TraktSyncStatus.Syncing("Syncing playback progress..."))
                    try {
                        val pausedMovies = TraktClient.api.getPausedMovies(bearer, clientId)
                        pausedMovies.forEach { item ->
                            item.movie?.ids?.tmdb?.let { tmdbId ->
                                val progressVal = ((item.progress ?: 0f) / 100f * 10000).toLong()
                                catalogRepository.saveWatchProgress(
                                    WatchProgress(userId, "tmdb:$tmdbId", "movie",
                                        progress = progressVal,
                                        duration = 10000,
                                        isWatched = false,
                                        lastUpdated = System.currentTimeMillis(),
                                        name = item.movie.title, poster = null, background = null, parentId = null, season = null, episode = null)
                                )
                            }
                        }
                    } catch (e: Exception) { Log.e("TraktSync", "Error syncing paused content", e) }

                    if (fetchMetadata && apiKey.isNotEmpty()) {
                        _traktSyncStatus.postValue(TraktSyncStatus.Syncing("Refreshing metadata from TMDB..."))
                        var successCount = 0
                        var failCount = 0

                        importedMovieIds.forEach { tmdbId ->
                            try {
                                val details = TMDBClient.api.getMovieDetails(tmdbId, apiKey)
                                val poster = details.poster_path?.let { "https://image.tmdb.org/t/p/w500$it" }
                                val background = details.backdrop_path?.let { "https://image.tmdb.org/t/p/original$it" }
                                val enrichedMeta = MetaItem("tmdb:$tmdbId", "movie", details.title, poster, background, details.overview, details.release_date)
                                catalogRepository.addToLibrary(CollectedItem.fromMetaItem(userId, enrichedMeta))
                                successCount++
                            } catch (e: Exception) {
                                Log.e("TraktSync", "Error fetching metadata for movie tmdb:$tmdbId", e)
                                failCount++
                            }
                        }

                        importedShowIds.forEach { tmdbId ->
                            try {
                                val details = TMDBClient.api.getTVDetails(tmdbId, apiKey)
                                val poster = details.poster_path?.let { "https://image.tmdb.org/t/p/w500$it" }
                                val background = details.backdrop_path?.let { "https://image.tmdb.org/t/p/original$it" }
                                val enrichedMeta = MetaItem("tmdb:$tmdbId", "series", details.name, poster, background, details.overview, null)
                                catalogRepository.addToLibrary(CollectedItem.fromMetaItem(userId, enrichedMeta))
                                successCount++
                            } catch (e: Exception) {
                                Log.e("TraktSync", "Error fetching metadata for series tmdb:$tmdbId", e)
                                failCount++
                            }
                        }

                        Log.i("TraktSync", "Metadata enrichment: $successCount succeeded, $failCount failed")
                    } else if (fetchMetadata && apiKey.isEmpty()) {
                        Log.w("TraktSync", "Cannot fetch metadata: TMDB API key is not configured")
                    }
                }

                if (syncLists) {
                    val catalogs = listOf(
                        UserCatalog(0, userId, "trakt_watchlist_movies", "movie", "Trakt Watchlist", null, 1, "movies", "trakt", "trakt_official", true, false),
                        UserCatalog(0, userId, "trakt_watchlist_shows", "series", "Trakt Watchlist", null, 1, "series", "trakt", "trakt_official", true, false),
                        UserCatalog(0, userId, "trakt_trending_movies", "movie", "Trakt Trending", null, 3, "movies", "trakt", "trakt_official", true, true),
                        UserCatalog(0, userId, "trakt_trending_shows", "series", "Trakt Trending", null, 3, "series", "trakt", "trakt_official", true, true)
                    )
                    catalogs.forEach {
                        if (!catalogRepository.isCatalogAdded(userId, it.catalogId, it.catalogType, it.pageType)) {
                            catalogRepository.insertCatalog(it)
                        }
                    }
                }

                if (fetchMetadata && apiKey.isNotEmpty()) {
                    _traktSyncStatus.postValue(TraktSyncStatus.Syncing("Checking library for missing metadata..."))
                    ensureAllLibraryItemsHaveMetadata()
                }

                _traktSyncStatus.postValue(TraktSyncStatus.Success("Sync Complete"))

            } catch (e: Exception) {
                _traktSyncStatus.postValue(TraktSyncStatus.Error(e.message ?: "Error"))
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    // Public wrapper for metadata check - can be called from MainActivity on startup
    fun ensureLibraryMetadata() {
        viewModelScope.launch {
            ensureAllLibraryItemsHaveMetadata()
        }
    }

    private suspend fun ensureAllLibraryItemsHaveMetadata() {
        val userId = prefsManager.getCurrentUserId() ?: return
        if (apiKey.isEmpty()) {
            Log.w("TraktSync", "Cannot ensure metadata: TMDB API key is not configured")
            return
        }
        withContext(Dispatchers.IO) {
            try {
                val libraryItems = catalogRepository.getAllLibraryItems(userId)
                var checkedCount = 0
                var updatedCount = 0
                var errorCount = 0

                libraryItems.forEach { item ->
                    // Check if item is missing any metadata (poster, background, or description)
                    val needsMetadata = item.poster.isNullOrEmpty() ||
                                       item.background.isNullOrEmpty() ||
                                       item.description.isNullOrEmpty() ||
                                       item.name.isNullOrEmpty()

                    if (needsMetadata) {
                        checkedCount++
                        val tmdbId = item.itemId.removePrefix("tmdb:").split(":").firstOrNull()?.toIntOrNull()
                        if (tmdbId != null) {
                            try {
                                when (item.itemType) {
                                    "movie" -> {
                                        val details = TMDBClient.api.getMovieDetails(tmdbId, apiKey)
                                        val poster = details.poster_path?.let { "https://image.tmdb.org/t/p/w500$it" }
                                        val background = details.backdrop_path?.let { "https://image.tmdb.org/t/p/original$it" }
                                        val enrichedMeta = MetaItem(
                                            id = "tmdb:$tmdbId",
                                            type = "movie",
                                            name = details.title,
                                            poster = poster,
                                            background = background,
                                            description = details.overview,
                                            releaseDate = details.release_date,
                                            rating = details.vote_average?.toString()
                                        )
                                        catalogRepository.addToLibrary(CollectedItem.fromMetaItem(userId, enrichedMeta))
                                        updatedCount++
                                    }
                                    "series" -> {
                                        val details = TMDBClient.api.getTVDetails(tmdbId, apiKey)
                                        val poster = details.poster_path?.let { "https://image.tmdb.org/t/p/w500$it" }
                                        val background = details.backdrop_path?.let { "https://image.tmdb.org/t/p/original$it" }
                                        val enrichedMeta = MetaItem(
                                            id = "tmdb:$tmdbId",
                                            type = "series",
                                            name = details.name,
                                            poster = poster,
                                            background = background,
                                            description = details.overview,
                                            releaseDate = details.first_air_date,
                                            rating = details.vote_average?.toString()
                                        )
                                        catalogRepository.addToLibrary(CollectedItem.fromMetaItem(userId, enrichedMeta))
                                        updatedCount++
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("TraktSync", "Error fetching metadata for ${item.itemType} ${item.itemId}", e)
                                errorCount++
                            }
                        }
                    }
                }
                Log.i("TraktSync", "Metadata backfill: checked $checkedCount items, updated $updatedCount, errors $errorCount")
            } catch (e: Exception) {
                Log.e("TraktSync", "Error in ensureAllLibraryItemsHaveMetadata", e)
            }
        }
    }

    private fun startPeriodicTraktSync() {
        viewModelScope.launch {
            while (prefsManager.isTraktEnabled()) {
                delay(30 * 60 * 1000L)
                syncTraktLibrary()
            }
        }
    }

    // === WATCHED ACTION & REFRESH ===
    fun markAsWatched(item: MetaItem, syncToTrakt: Boolean = true) {
        val currentUserId = prefsManager.getCurrentUserId() ?: return
        val idStr = item.id.removePrefix("tmdb:")
        val tmdbId = idStr.toIntOrNull()

        viewModelScope.launch {
            // 1. Scrobble to Trakt (Isolated in try-catch)
            if (syncToTrakt && prefsManager.isTraktEnabled()) {
                try {
                    val token = prefsManager.getTraktAccessToken()
                    if (token != null) {
                        val bearer = "Bearer $token"
                        val body: TraktHistoryBody? = if (item.type == "movie") {
                            val id = item.id.removePrefix("tmdb:").toIntOrNull()
                            if (id != null) TraktHistoryBody(movies = listOf(TraktMovie("", null, TraktIds(0, id, null, null)))) else null
                        } else if (item.type == "episode") {
                            val parts = item.id.split(":")
                            if (parts.size >= 4) {
                                // Format: tmdb:SHOW_ID:SEASON:EPISODE
                                val showId = parts[1].toIntOrNull()
                                val s = parts[2].toIntOrNull()
                                val e = parts[3].toIntOrNull()

                                if (showId != null && s != null && e != null) {
                                    // [FIX] Use nested Show -> Season -> Episode structure
                                    // This gives Trakt the context it needs (which show this episode belongs to)
                                    TraktHistoryBody(
                                        shows = listOf(
                                            TraktShow(
                                                title = item.name, // Optional
                                                year = null,
                                                ids = TraktIds(0, showId, null, null),
                                                seasons = listOf(
                                                    TraktSeason(
                                                        number = s,
                                                        episodes = listOf(TraktEpisode(season = s, number = e))
                                                    )
                                                )
                                            )
                                        )
                                    )
                                } else null
                            } else null
                        } else null

                        if (body != null) {
                            TraktClient.api.addToHistory(bearer, Secrets.TRAKT_CLIENT_ID, body)

                            // Remove from playback progress (continue watching)
                            try {
                                val playbackItems = if (item.type == "movie") {
                                    TraktClient.api.getPausedMovies(bearer, Secrets.TRAKT_CLIENT_ID)
                                } else {
                                    TraktClient.api.getPausedEpisodes(bearer, Secrets.TRAKT_CLIENT_ID)
                                }

                                // Find matching playback item
                                val matchingPlayback = playbackItems.find { playbackItem ->
                                    when (item.type) {
                                        "movie" -> playbackItem.movie?.ids?.tmdb == tmdbId
                                        "episode" -> {
                                            val parts = item.id.split(":")
                                            if (parts.size >= 4) {
                                                val showId = parts[1].toIntOrNull()
                                                val s = parts[2].toIntOrNull()
                                                val e = parts[3].toIntOrNull()
                                                playbackItem.show?.ids?.tmdb == showId &&
                                                        playbackItem.episode?.season == s &&
                                                        playbackItem.episode?.number == e
                                            } else false
                                        }
                                        else -> false
                                    }
                                }

                                // Remove from playback if found
                                matchingPlayback?.id?.let { playbackId ->
                                    TraktClient.api.removePlaybackProgress(bearer, Secrets.TRAKT_CLIENT_ID, playbackId)
                                }
                            } catch (e: Exception) {
                                // Ignore playback removal errors
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MainViewModel", "Trakt sync failed in markAsWatched: ${e.message}")
                    // Don't block local update if Trakt fails
                }
            }

            // 2. Update Local DB (Always runs)
            try {
                if (item.type == "series" && tmdbId != null && apiKey.isNotEmpty()) {
                    val details = TMDBClient.api.getTVDetails(tmdbId, apiKey)
                    details.seasons?.forEach { season ->
                        if (season.episode_count > 0) {
                            val seasonDetails = TMDBClient.api.getTVSeasonDetails(tmdbId, season.season_number, apiKey)
                            seasonDetails.episodes.forEach { ep ->
                                val epId = "tmdb:$tmdbId:${ep.season_number}:${ep.episode_number}"
                                catalogRepository.saveWatchProgress(WatchProgress(currentUserId, epId, "episode", 0, 0, true, System.currentTimeMillis(), "${details.name} S${season.season_number}E${ep.episode_number}", null, null, "tmdb:$tmdbId", season.season_number, ep.episode_number))
                            }
                        }
                    }
                } else if (item.type == "episode") {
                    val parts = item.id.split(":")
                    if (parts.size >= 4) {
                        val parentId = "${parts[0]}:${parts[1]}"
                        val s = parts[2].toIntOrNull()
                        val e = parts[3].toIntOrNull()
                        catalogRepository.saveWatchProgress(WatchProgress(currentUserId, item.id, "episode", 0, 0, true, System.currentTimeMillis(), item.name, item.poster, item.background, parentId, s, e))
                    }
                } else {
                    catalogRepository.saveWatchProgress(WatchProgress(currentUserId, item.id, item.type, 0, 0, true, System.currentTimeMillis(), item.name, item.poster, item.background, null, null, null))
                }

                // 3. REFRESH UI
                loadedContentCache.clear()
                sessionCache.invalidateAll(currentUserId)
                lastRequestedCatalog?.let {
                    loadContentForCatalog(it, isInitialLoad = true)
                }
                loadHomeContent()

                _isItemWatched.postValue(true)
                _actionResult.postValue(ActionResult.Success("Marked as watched"))

            } catch (e: Exception) {
                _actionResult.postValue(ActionResult.Error("Failed: ${e.message}"))
            }
        }
    }

    // === NEW: NOT WATCHING (Replaces Clear Progress in Context Menu) ===
    fun markAsNotWatching(item: MetaItem) {
        val currentUserId = prefsManager.getCurrentUserId() ?: return
        val tmdbId = item.id.removePrefix("tmdb:").split(":")[0].toIntOrNull()

        viewModelScope.launch {
            try {
                if (prefsManager.isTraktEnabled() && tmdbId != null) {
                    val token = prefsManager.getTraktAccessToken()
                    if (token != null) {
                        val bearer = "Bearer $token"
                        val clientId = Secrets.TRAKT_CLIENT_ID

                        // 1. Remove from Trakt History (Clears from "Next Up")
                        val historyBody = if (item.type == "movie") {
                            TraktHistoryBody(movies = listOf(TraktMovie("", null, TraktIds(0, tmdbId, null, null))))
                        } else {
                            // For series/episodes, removing the SHOW ID from history clears all progress for that show
                            TraktHistoryBody(shows = listOf(TraktShow("", null, TraktIds(0, tmdbId, null, null))))
                        }
                        TraktClient.api.removeFromHistory(bearer, clientId, historyBody)

                        // 2. Remove from Trakt Playback (Clears from "Continue Watching")
                        try {
                            // We need to find the playback ID first
                            val playbackItems = if (item.type == "movie") {
                                TraktClient.api.getPausedMovies(bearer, clientId)
                            } else {
                                TraktClient.api.getPausedEpisodes(bearer, clientId)
                            }

                            // Find item matching TMDB ID
                            val matchingItem = playbackItems.find { playback ->
                                val pTmdb = if (item.type == "movie") playback.movie?.ids?.tmdb else playback.show?.ids?.tmdb
                                pTmdb == tmdbId
                            }

                            matchingItem?.id?.let { playbackId ->
                                TraktClient.api.removePlaybackProgress(bearer, clientId, playbackId)
                            }
                        } catch (e: Exception) {
                            Log.e("MainViewModel", "Failed to remove playback progress: ${e.message}")
                        }
                    }
                }

                // 3. Clear Local Watch Progress
                if (item.type == "episode" || item.type == "series") {
                    // For episodes/series, remove ALL episodes of the show from database
                    val showId = "tmdb:" + tmdbId.toString()
                    catalogRepository.deleteAllEpisodesOfShow(currentUserId, showId)
                } else {
                    // For movies, just remove the single item
                    catalogRepository.updateWatchedStatus(currentUserId, item.id, false)
                }

                // Refresh UI
                loadedContentCache.clear()
                sessionCache.invalidateAll(currentUserId)
                lastRequestedCatalog?.let { loadContentForCatalog(it, isInitialLoad = true) }
                loadHomeContent()

                _isItemWatched.postValue(false)
                _actionResult.postValue(ActionResult.Success("Removed from Watching lists"))

            } catch (e: Exception) {
                _actionResult.postValue(ActionResult.Error("Failed: ${e.message}"))
            }
        }
    }

    // Keeping clearWatchedStatus for backwards compatibility if needed, but markAsNotWatching is superior
    fun clearWatchedStatus(item: MetaItem, syncToTrakt: Boolean = true) {
        markAsNotWatching(item)
    }

    // === STANDARD METHODS ===
    fun loadStreams(type: String, itemId: String) {
        _isLoading.postValue(true)
        // OPTIMIZATION: Run on IO dispatcher
        viewModelScope.launch(Dispatchers.IO) {
            val allStreams = mutableListOf<Stream>()
            val manifestUrl = prefsManager.getAIOStreamsManifestUrl()

            val startTime = System.currentTimeMillis() // Timing debug

            if (!manifestUrl.isNullOrEmpty()) {
                try {
                    // This now returns the singleton API, very fast
                    val aioApi = AIOStreamsClient.getApi(manifestUrl)

                    val streamUrl = when (type) {
                        "movie" -> {
                            val imdbId = getImdbIdForMovie(itemId)
                            if (imdbId != null) {
                                Log.d("StreamDebug", "Movie Fetch: $itemId -> $imdbId")
                                AIOStreamsClient.buildMovieStreamUrl(manifestUrl, imdbId)
                            } else {
                                Log.e("MainViewModel", "Could not get IMDb ID for movie: $itemId")
                                null
                            }
                        }
                        "series", "episode" -> {
                            // itemId format: "tmdb:12345:season:episode"
                            val parts = itemId.split(":")
                            // Be flexible: handle "tmdb:ID:S:E"
                            if (parts.size >= 4 && parts[0] == "tmdb") {
                                val tmdbId = parts[1].toIntOrNull()
                                val season = parts[2].toIntOrNull() ?: 1
                                val episode = parts[3].toIntOrNull() ?: 1

                                val imdbId = getImdbIdForSeries(tmdbId)
                                if (imdbId != null) {
                                    // AIOStreams expects: showImdbId:season:episode
                                    // e.g. tt049406:2:1
                                    val url = AIOStreamsClient.buildSeriesStreamUrl(manifestUrl, imdbId, season, episode)
                                    Log.d("StreamDebug", "Series Fetch: $itemId -> $imdbId S$season E$episode -> URL: $url")
                                    url
                                } else {
                                    Log.e("MainViewModel", "Could not get IMDb ID for series: $itemId")
                                    null
                                }
                            } else {
                                Log.e("MainViewModel", "Invalid series itemId format: $itemId")
                                null
                            }
                        }
                        else -> null
                    }

                    if (streamUrl != null) {
                        val aioResponse = aioApi.getStreams(streamUrl)
                        allStreams.addAll(aioResponse.streams)
                        Log.d("StreamDebug", "Fetched ${aioResponse.streams.size} streams in ${System.currentTimeMillis() - startTime}ms")
                    }
                } catch (e: Exception) {
                    Log.e("MainViewModel", "AIOStreams error: ${e.message}", e)
                }
            }
            _streams.postValue(allStreams)
            _isLoading.postValue(false)
        }
    }

    fun clearStreams() {
        _streams.value = emptyList()
        Log.d("MainViewModel", "Streams cleared from memory")
    }

    private suspend fun getImdbIdForMovie(itemId: String): String? {
        return try {
            if (apiKey.isEmpty()) return null
            val tmdbId = itemId.removePrefix("tmdb:").toIntOrNull() ?: return null
            val externalIds = TMDBClient.api.getMovieExternalIds(tmdbId, apiKey)
            externalIds.imdbId
        } catch (e: Exception) {
            Log.e("MainViewModel", "Error getting IMDb ID for movie $itemId", e)
            null
        }
    }

    private suspend fun getImdbIdForSeries(tmdbId: Int?): String? {
        return try {
            if (apiKey.isEmpty() || tmdbId == null) return null
            val externalIds = TMDBClient.api.getTVExternalIds(tmdbId, apiKey)
            externalIds.imdbId
        } catch (e: Exception) {
            Log.e("MainViewModel", "Error getting IMDb ID for series $tmdbId", e)
            null
        }
    }

    fun loadEpisodeStreams(seriesId: String, season: Int, episode: Int) {
        val episodeId = "$seriesId:$season:$episode"
        loadStreams("series", episodeId)
    }

    /**
     * Fetch English subtitles from AIOStreams for the given meta item
     */
    suspend fun fetchSubtitles(meta: MetaItem): List<com.example.stremiompvplayer.models.Subtitle> {
        return withContext(Dispatchers.IO) {
            try {
                val manifestUrl = prefsManager.getAIOStreamsManifestUrl()
                if (manifestUrl.isNullOrEmpty()) {
                    Log.d("MainViewModel", "No AIOStreams manifest URL configured")
                    return@withContext emptyList()
                }

                val aioApi = AIOStreamsClient.getApi(manifestUrl)
                val subtitleUrl = when (meta.type) {
                    "movie" -> {
                        val imdbId = getImdbIdForMovie(meta.id)
                        if (imdbId != null) {
                            AIOStreamsClient.buildMovieSubtitleUrl(manifestUrl, imdbId)
                        } else {
                            Log.e("MainViewModel", "Could not get IMDb ID for movie: ${meta.id}")
                            null
                        }
                    }
                    "episode" -> {
                        // Parse episode ID: tmdb:12345:season:episode
                        val parts = meta.id.split(":")
                        if (parts.size >= 4) {
                            val tmdbId = parts[1].toIntOrNull()
                            val season = parts[2].toIntOrNull() ?: 1
                            val episode = parts[3].toIntOrNull() ?: 1
                            val imdbId = getImdbIdForSeries(tmdbId)
                            if (imdbId != null) {
                                AIOStreamsClient.buildSeriesSubtitleUrl(manifestUrl, imdbId, season, episode)
                            } else {
                                Log.e("MainViewModel", "Could not get IMDb ID for series: ${meta.id}")
                                null
                            }
                        } else {
                            Log.e("MainViewModel", "Invalid episode ID format: ${meta.id}")
                            null
                        }
                    }
                    else -> {
                        Log.d("MainViewModel", "Subtitles not supported for type: ${meta.type}")
                        null
                    }
                }

                if (subtitleUrl != null) {
                    val response = aioApi.getSubtitles(subtitleUrl)
                    // Filter for English subtitles only
                    val englishSubtitles = response.subtitles.filter { it.lang == "eng" }
                    Log.d("MainViewModel", "Found ${englishSubtitles.size} English subtitles")
                    englishSubtitles
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error fetching subtitles", e)
                emptyList()
            }
        }
    }

    fun loadSeriesMeta(itemId: String) {
        if (apiKey.isEmpty()) return
        viewModelScope.launch {
            try {
                val tmdbId = itemId.removePrefix("tmdb:").toIntOrNull() ?: return@launch
                val details = TMDBClient.api.getTVDetails(tmdbId, apiKey)
                val meta = Meta(
                    id = itemId,
                    type = "series",
                    name = details.name,
                    poster = details.poster_path?.let { "https://image.tmdb.org/t/p/w500$it" },
                    background = details.backdrop_path?.let { "https://image.tmdb.org/t/p/original$it" },
                    description = details.overview,
                    videos = details.seasons?.map { season ->
                        Video(season.id.toString(), season.name, null, season.poster_path?.let { "https://image.tmdb.org/t/p/w500$it" }, null, season.season_number)
                    }
                )
                _metaDetails.postValue(meta)
            } catch (e: Exception) { _error.postValue("Failed to load series details: ${e.message}") }
        }
    }

    fun loadSeasonEpisodes(seriesId: String, seasonNumber: Int) {
        if (apiKey.isEmpty()) return
        viewModelScope.launch {
            try {
                val tmdbId = seriesId.removePrefix("tmdb:").toIntOrNull() ?: return@launch
                val seasonDetails = TMDBClient.api.getTVSeasonDetails(tmdbId, seasonNumber, apiKey)
                val episodes = seasonDetails.episodes.map { episode ->
                    MetaItem(
                        id = "tmdb:$tmdbId:${episode.season_number}:${episode.episode_number}",
                        type = "episode",
                        name = episode.name,
                        poster = episode.still_path?.let { "https://image.tmdb.org/t/p/w500$it" },
                        background = null,
                        description = episode.overview
                    )
                }
                _seasonEpisodes.postValue(episodes)
            } catch (e: Exception) { _error.postValue("Failed to load season episodes: ${e.message}") }
        }
    }

    fun fetchCast(itemId: String, type: String) {
        if (apiKey.isEmpty()) return
        viewModelScope.launch {
            try {
                _isCastLoading.postValue(true)
                val tmdbId = itemId.removePrefix("tmdb:").toIntOrNull() ?: return@launch
                val credits = if (type == "movie") TMDBClient.api.getMovieCredits(tmdbId, apiKey) else TMDBClient.api.getTVCredits(tmdbId, apiKey)
                val cast = credits.cast.take(10).map { member ->
                    MetaItem("tmdb:${member.id}", "person", member.name, member.profile_path?.let { "https://image.tmdb.org/t/p/w500$it" }, null, member.character)
                }
                val director = credits.crew.find { it.job == "Director" }?.let { member ->
                    MetaItem("tmdb:${member.id}", "person", member.name, null, null, "Director")
                }
                _castList.postValue(cast)
                _director.postValue(director)
                _isCastLoading.postValue(false)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to fetch cast", e)
                _isCastLoading.postValue(false)
            }
        }
    }

    fun checkWatchedStatus(itemId: String) {
        val currentUserId = prefsManager.getCurrentUserId() ?: return
        viewModelScope.launch {
            val progress = catalogRepository.getWatchProgress(currentUserId, itemId)
            _isItemWatched.postValue(progress?.isWatched ?: false)
        }
    }

    suspend fun getWatchProgressSync(itemId: String): WatchProgress? {
        val currentUserId = prefsManager.getCurrentUserId() ?: return null
        return catalogRepository.getWatchProgress(currentUserId, itemId)
    }

    /**
     * Get the next episode for a given episode ID
     * Returns MetaItem for the next episode if it exists and is released
     */
    suspend fun getNextEpisode(currentEpisodeId: String): MetaItem? = withContext(Dispatchers.IO) {
        val parts = currentEpisodeId.split(":")
        if (parts.size < 4 || apiKey.isEmpty()) return@withContext null

        val showId = "${parts[0]}:${parts[1]}"
        val tmdbId = parts[1].toIntOrNull() ?: return@withContext null
        val currentSeason = parts[2].toIntOrNull() ?: return@withContext null
        val currentEpisode = parts[3].toIntOrNull() ?: return@withContext null

        try {
            val showDetails = TMDBClient.api.getTVDetails(tmdbId, apiKey)
            var nextSeasonNum = currentSeason
            var nextEpisodeNum = currentEpisode + 1
            var potentialNextFound = false

            // Check if next episode is in current season
            val currentSeasonSpec = showDetails.seasons?.find { it.season_number == currentSeason }
            if (currentSeasonSpec != null && currentEpisode < currentSeasonSpec.episode_count) {
                potentialNextFound = true
            } else {
                // Check if there's a next season
                val nextSeasonSpec = showDetails.seasons?.find { it.season_number == currentSeason + 1 }
                if (nextSeasonSpec != null && nextSeasonSpec.episode_count > 0) {
                    nextSeasonNum = currentSeason + 1
                    nextEpisodeNum = 1
                    potentialNextFound = true
                }
            }

            if (potentialNextFound) {
                val seasonDetails = TMDBClient.api.getTVSeasonDetails(tmdbId, nextSeasonNum, apiKey)
                val nextEpDetails = seasonDetails.episodes.find { it.episode_number == nextEpisodeNum }

                if (nextEpDetails != null) {
                    val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                    val today = dateFormat.format(java.util.Date())
                    val isReleased = nextEpDetails.airDate == null || nextEpDetails.airDate <= today

                    if (isReleased) {
                        val nextEpisodeId = "$showId:$nextSeasonNum:$nextEpisodeNum"
                        val poster = nextEpDetails.still_path?.let { "https://image.tmdb.org/t/p/w500$it" }
                            ?: showDetails.poster_path?.let { "https://image.tmdb.org/t/p/w500$it" }
                        val background = nextEpDetails.still_path?.let { "https://image.tmdb.org/t/p/original$it" }
                            ?: showDetails.backdrop_path?.let { "https://image.tmdb.org/t/p/original$it" }

                        return@withContext MetaItem(
                            id = nextEpisodeId,
                            type = "episode",
                            name = "S${nextSeasonNum}E${nextEpisodeNum}: ${nextEpDetails.name}",
                            poster = poster,
                            background = background,
                            description = nextEpDetails.overview ?: "Next episode"
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("NextEpisode", "Error getting next episode for $currentEpisodeId", e)
        }
        return@withContext null
    }

    fun saveWatchProgress(meta: MetaItem, currentPos: Long, duration: Long) {
        val currentUserId = prefsManager.getCurrentUserId() ?: return
        viewModelScope.launch {
            val parts = meta.id.split(":")
            val parentId = if (parts.size >= 3) "${parts[0]}:${parts[1]}" else null
            val season = if (parts.size >= 3) parts[2].toIntOrNull() else null
            val episode = if (parts.size >= 4) parts[3].toIntOrNull() else null

            // For episodes, try to fetch proper episode details for better name storage
            var displayName = meta.name
            if (meta.type == "episode" && parts.size >= 4 && apiKey.isNotEmpty()) {
                try {
                    val showId = parts[1].toIntOrNull()
                    val seasonNum = parts[2].toIntOrNull()
                    val episodeNum = parts[3].toIntOrNull()

                    if (showId != null && seasonNum != null && episodeNum != null) {
                        val showDetails = TMDBClient.api.getTVDetails(showId, apiKey)
                        val seasonDetails = TMDBClient.api.getTVSeasonDetails(showId, seasonNum, apiKey)
                        val episodeDetails = seasonDetails.episodes.find { it.episode_number == episodeNum }

                        if (episodeDetails != null) {
                            displayName = "${showDetails.name} - ${episodeDetails.name}"
                        }
                    }
                } catch (e: Exception) {
                    // Fallback to original name on error
                    e.printStackTrace()
                }
            }

            val progress = WatchProgress(
                userId = currentUserId,
                itemId = meta.id,
                type = meta.type,
                progress = currentPos,
                duration = duration,
                isWatched = currentPos >= duration * 0.9,
                lastUpdated = System.currentTimeMillis(),
                name = displayName,
                poster = meta.poster,
                background = meta.background,
                parentId = parentId,
                season = season,
                episode = episode
            )
            catalogRepository.saveWatchProgress(progress)
        }
    }

    fun searchTMDB(query: String) {
        if (apiKey.isEmpty() || query.isBlank()) return
        _isSearching.postValue(true)
        viewModelScope.launch {
            try {
                val response = TMDBClient.api.searchMulti(apiKey, query)
                var results = response.results.filter { it.media_type == "movie" || it.media_type == "tv" }

                // Filter for kids profiles
                val currentUserId = prefsManager.getCurrentUserId()
                val currentUser = currentUserId?.let { prefsManager.getUser(it) }
                if (currentUser?.isKidsProfile == true) {
                    val safeGenres = listOf(16, 10751, 10762) // Animation, Family, Kids
                    results = results.filter { result ->
                        result.genre_ids?.any { it in safeGenres } == true
                    }
                }

                _searchResults.postValue(results.map { it.toMetaItem() })
            } catch (e: Exception) {
                _error.postValue("Search failed: ${e.message}")
                _searchResults.postValue(emptyList())
            } finally { _isSearching.postValue(false) }
        }
    }

    fun searchMovies(query: String) {
        if (apiKey.isEmpty() || query.isBlank()) return
        _isSearching.postValue(true)
        viewModelScope.launch {
            try {
                val response = TMDBClient.api.searchMovies(apiKey, query)
                var results = response.results

                // Filter for kids profiles
                val currentUserId = prefsManager.getCurrentUserId()
                val currentUser = currentUserId?.let { prefsManager.getUser(it) }
                if (currentUser?.isKidsProfile == true) {
                    val safeGenres = listOf(16, 10751, 10762) // Animation, Family, Kids
                    results = results.filter { movie ->
                        movie.genre_ids?.any { it in safeGenres } == true
                    }
                }

                _searchResults.postValue(results.map { it.toMetaItem() })
            } catch (e: Exception) {
                _error.postValue("Movie search failed: ${e.message}")
                _searchResults.postValue(emptyList())
            } finally { _isSearching.postValue(false) }
        }
    }

    fun searchSeries(query: String) {
        if (apiKey.isEmpty() || query.isBlank()) return
        _isSearching.postValue(true)
        viewModelScope.launch {
            try {
                val response = TMDBClient.api.searchSeries(apiKey, query)
                var results = response.results

                // Filter for kids profiles
                val currentUserId = prefsManager.getCurrentUserId()
                val currentUser = currentUserId?.let { prefsManager.getUser(it) }
                if (currentUser?.isKidsProfile == true) {
                    val safeGenres = listOf(16, 10751, 10762) // Animation, Family, Kids
                    results = results.filter { series ->
                        series.genre_ids?.any { it in safeGenres } == true
                    }
                }

                _searchResults.postValue(results.map { it.toMetaItem() })
            } catch (e: Exception) {
                _error.postValue("Series search failed: ${e.message}")
                _searchResults.postValue(emptyList())
            } finally { _isSearching.postValue(false) }
        }
    }

    fun loadPersonCredits(personId: Int) {
        if (apiKey.isEmpty()) return
        _isSearching.postValue(true)
        viewModelScope.launch {
            try {
                val response = TMDBClient.api.getPersonCombinedCredits(personId, apiKey)
                // Separate movies and series, then interleave them
                val allCredits = response.cast.mapIndexed { index, it ->
                    it.toMetaItem().copy(popularity = index.toDouble())
                }
                val movies = allCredits.filter { it.type == "movie" }
                val series = allCredits.filter { it.type == "series" || it.type == "tv" }

                // Interleave results: movie 0, series 0, movie 1, series 1, etc.
                val interleaved = mutableListOf<MetaItem>()
                val maxSize = maxOf(movies.size, series.size)
                for (i in 0 until maxSize) {
                    if (i < movies.size) interleaved.add(movies[i])
                    if (i < series.size) interleaved.add(series[i])
                }
                _searchResults.postValue(interleaved)
            } catch (e: Exception) {
                _error.postValue("Person credits failed: ${e.message}")
                _searchResults.postValue(emptyList())
            } finally { _isSearching.postValue(false) }
        }
    }

    fun clearSearchResults() { _searchResults.postValue(emptyList()) }

    fun toggleWatchlist(meta: MetaItem, force: Boolean = false) {
        if (apiKey.isEmpty()) return
        val sessionId = prefsManager.getTMDBSessionId()
        val accountId = prefsManager.getTMDBAccountId()
        if (sessionId.isNullOrEmpty() || accountId == -1) return

        viewModelScope.launch {
            try {
                val tmdbId = meta.id.removePrefix("tmdb:").toIntOrNull() ?: return@launch
                val mediaType = if (meta.type == "series") "tv" else "movie"
                val isCurrentlyInWatchlist = _isItemInWatchlist.value ?: false
                val body = TMDBWatchlistBody(mediaType, tmdbId, if (force) true else !isCurrentlyInWatchlist)
                TMDBClient.api.addToWatchlist(accountId, apiKey, sessionId, body)
                _isItemInWatchlist.postValue(if (force) true else !isCurrentlyInWatchlist)
                _actionResult.postValue(ActionResult.Success("Watchlist updated"))
            } catch (e: Exception) { _actionResult.postValue(ActionResult.Error("Watchlist operation failed: ${e.message}")) }
        }
    }

    fun checkLibraryStatus(itemId: String) {
        val currentUserId = prefsManager.getCurrentUserId() ?: return
        viewModelScope.launch {
            val isCollected = catalogRepository.isItemCollected(itemId, currentUserId)
            _isItemInLibrary.postValue(isCollected)
        }
    }

    fun checkWatchlistStatus(itemId: String, type: String) { _isItemInWatchlist.postValue(false) }

    fun fetchRequestToken() {
        if (apiKey.isEmpty()) return
        viewModelScope.launch {
            try {
                val response = TMDBClient.api.createRequestToken(apiKey)
                if (response.success) _requestToken.postValue(response.requestToken)
                else _error.postValue("Failed to create TMDB request token")
            } catch (e: Exception) { _error.postValue("TMDB Auth error: ${e.message}") }
        }
    }

    fun createSession(requestToken: String) {
        if (apiKey.isEmpty()) return
        viewModelScope.launch {
            try {
                val body = mapOf("request_token" to requestToken)
                val response = TMDBClient.api.createSession(apiKey, body)
                if (response.success) {
                    prefsManager.saveTMDBSessionId(response.sessionId)
                    _sessionId.postValue(response.sessionId)
                    val account = TMDBClient.api.getAccountDetails(apiKey, response.sessionId)
                    prefsManager.saveTMDBAccountId(account.id)
                    _actionResult.postValue(ActionResult.Success("TMDB connected successfully"))
                } else { _error.postValue("Failed to create TMDB session") }
            } catch (e: Exception) { _error.postValue("TMDB Session error: ${e.message}") }
        }
    }

    fun checkTMDBAuthAndSync() {
        val sessionId = prefsManager.getTMDBSessionId()
        if (!sessionId.isNullOrEmpty() && apiKey.isNotEmpty()) _sessionId.postValue(sessionId)
    }

    fun initUserLists() {
        viewModelScope.launch { catalogRepository.ensureUserListCatalogs(prefsManager.getCurrentUserId() ?: "default") }
    }

    fun initDefaultCatalogs() {
        viewModelScope.launch { catalogRepository.initializeDefaultsIfNeeded() }
    }

    fun updateCatalogConfig(catalog: UserCatalog) {
        viewModelScope.launch { catalogRepository.updateCatalog(catalog) }
    }

    fun swapCatalogOrder(catalog1: UserCatalog, catalog2: UserCatalog) {
        viewModelScope.launch { catalogRepository.swapOrder(catalog1, catalog2) }
    }

    fun deleteCatalog(catalog: UserCatalog) {
        viewModelScope.launch { catalogRepository.deleteCatalog(catalog) }
    }

    fun addCustomList(listType: String, urlOrId: String, customName: String, pageType: String) {
        viewModelScope.launch {
            val userId = prefsManager.getCurrentUserId() ?: "default"
            val catalogType = if (pageType == "movies") "movie" else "series"
            val catalogId = "${listType}_${urlOrId.hashCode()}"
            val maxOrder = catalogRepository.getMaxDisplayOrderForPage(userId, pageType) ?: 0

            val catalog = UserCatalog(
                id = 0,
                userId = userId,
                catalogId = catalogId,
                catalogType = catalogType,
                catalogName = customName.ifEmpty { "$listType List - ${urlOrId.take(20)}" },
                customName = customName.ifEmpty { null },
                displayOrder = maxOrder + 1,
                pageType = pageType,
                addonUrl = listType,
                manifestId = urlOrId, // Store the actual URL/ID here
                showInDiscover = true,
                showInUser = true
            )
            catalogRepository.insertCatalog(catalog)
        }
    }

    fun getDiscoverCatalogs(type: String): LiveData<List<UserCatalog>> {
        return allCatalogConfigs.map { list ->
            list.filter { cat ->
                cat.catalogType == type && cat.showInDiscover
            }.sortedBy { it.displayOrder }
        }
    }

    fun filterAndSortLibrary(type: String, genre: String? = null, sortBy: String = "dateAdded", ascending: Boolean = false) {
        viewModelScope.launch {
            val rawItems = if (type == "movie") libraryMovies.value else librarySeries.value
            if (rawItems == null) return@launch
            val filtered = rawItems
            val sorted = when (sortBy) {
                "dateAdded" -> if (ascending) filtered else filtered.reversed()
                "releaseDate" -> if (ascending) filtered.sortedBy { it.releaseDate ?: "" } else filtered.sortedByDescending { it.releaseDate ?: "" }
                "title" -> if (ascending) filtered.sortedBy { it.name } else filtered.sortedByDescending { it.name }
                else -> filtered
            }
            if (type == "movie") _filteredLibraryMovies.postValue(sorted) else _filteredLibrarySeries.postValue(sorted)
        }
    }

    fun addToLibrary(meta: MetaItem, syncToTrakt: Boolean = true) {
        val currentUserId = prefsManager.getCurrentUserId() ?: return
        viewModelScope.launch {
            try {
                val item = CollectedItem.fromMetaItem(currentUserId, meta)
                catalogRepository.addToLibrary(item)
                _isItemInLibrary.postValue(true)
                _actionResult.postValue(ActionResult.Success("Added to library"))
            } catch (e: Exception) { _actionResult.postValue(ActionResult.Error("Failed to add to library: ${e.message}")) }
        }
    }

    fun removeFromLibrary(itemId: String, syncToTrakt: Boolean = true) {
        val currentUserId = prefsManager.getCurrentUserId() ?: return
        viewModelScope.launch {
            try {
                catalogRepository.removeFromLibrary(itemId, currentUserId)
                _isItemInLibrary.postValue(false)
                _actionResult.postValue(ActionResult.Success("Removed from library"))
            } catch (e: Exception) { _actionResult.postValue(ActionResult.Error("Failed to remove from library: ${e.message}")) }
        }
    }

    fun toggleLibrary(meta: MetaItem) {
        val currentUserId = prefsManager.getCurrentUserId() ?: return
        viewModelScope.launch {
            if (catalogRepository.isItemCollected(meta.id, currentUserId)) removeFromLibrary(meta.id) else addToLibrary(meta)
        }
    }

    // PERFORMANCE FIX: Use viewModelScope instead of GlobalScope to prevent leaks
    fun scrobble(action: String, meta: MetaItem, progress: Float) {
        if (!prefsManager.isTraktEnabled()) return
        val token = prefsManager.getTraktAccessToken() ?: return
        val clientId = Secrets.TRAKT_CLIENT_ID
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val body: TraktScrobbleBody? = if (meta.type == "movie") {
                    val idStr = meta.id.removePrefix("tmdb:")
                    val tmdbId = idStr.toIntOrNull()
                    if (tmdbId != null && tmdbId > 0) {
                        TraktScrobbleBody(progress, TraktMovie(meta.name, null, TraktIds(0, tmdbId, null, null)))
                    } else {
                        Log.e("Trakt", "Invalid movie ID: ${meta.id}")
                        null
                    }
                } else if (meta.type == "episode") {
                    val parts = meta.id.split(":")
                    if (parts.size >= 4) {
                        val showId = parts[1].toIntOrNull()
                        val s = parts[2].toIntOrNull()
                        val e = parts[3].toIntOrNull()
                        if (showId != null && showId > 0 && s != null && e != null) {
                            TraktScrobbleBody(progress, episode = TraktEpisode(s, e), show = TraktShow(meta.name, null, TraktIds(0, showId, null, null)))
                        } else {
                            Log.e("Trakt", "Invalid episode ID: ${meta.id}")
                            null
                        }
                    } else {
                        Log.e("Trakt", "Episode ID format invalid: ${meta.id}")
                        null
                    }
                } else {
                    Log.e("Trakt", "Unsupported scrobble type: ${meta.type}")
                    null
                }

                if (body != null) {
                    val bearer = "Bearer $token"
                    val response = when (action) {
                        "start" -> TraktClient.api.startScrobble(bearer, clientId, body = body)
                        "pause" -> TraktClient.api.pauseScrobble(bearer, clientId, body = body)
                        "stop" -> TraktClient.api.stopScrobble(bearer, clientId, body = body)
                        else -> null
                    }
                    if (response != null) {
                        Log.d("Trakt", "Scrobble $action successful: ${meta.name} at ${progress.toInt()}%")
                    }
                } else {
                    Log.e("Trakt", "Scrobble body is null, cannot send $action scrobble")
                }
            } catch (e: Exception) {
                Log.e("Trakt", "Scrobble $action failed for ${meta.name}", e)
            }
        }
    }

    fun fetchItemLogo(meta: MetaItem) {
        logoFetchJob?.cancel()
        _currentLogo.value = ""
        logoFetchJob = viewModelScope.launch {
            try {
                val idParts = meta.id.removePrefix("tmdb:").split(":")
                val tmdbId = idParts[0].toIntOrNull()
                Log.d("LogoFetch", "Fetching logo for ${meta.name} (${meta.type}) - TMDB ID: $tmdbId, apiKey present: ${apiKey.isNotEmpty()}")

                if (tmdbId != null && apiKey.isNotEmpty()) {
                    // For episodes, use TV images API; for movies use movie images; for series use TV images
                    val images = if (meta.type == "movie") {
                        TMDBClient.api.getMovieImages(tmdbId, apiKey)
                    } else {
                        // For both "series" and "episode", use TV images with the show ID
                        TMDBClient.api.getTVImages(tmdbId, apiKey)
                    }

                    Log.d("LogoFetch", "Total logos received: ${images.logos.size}")
                    images.logos.forEach {
                        Log.d("LogoFetch", "Logo: ${it.file_path}, language: ${it.iso_639_1}")
                    }

                    // Filter for English logos (iso_639_1 == "en" or null for language-neutral logos)
                    val enLogos = images.logos.filter { it.iso_639_1 == "en" || it.iso_639_1 == null }
                    Log.d("LogoFetch", "English/neutral logos: ${enLogos.size}")

                    val logo = enLogos.firstOrNull()
                    val logoUrl = logo?.let { "https://image.tmdb.org/t/p/w300${it.file_path}" }
                    Log.d("LogoFetch", "Final logo URL: $logoUrl")
                    _currentLogo.postValue(logoUrl)
                } else {
                    Log.w("LogoFetch", "Cannot fetch logo: tmdbId=$tmdbId, apiKey present=${apiKey.isNotEmpty()}")
                    _currentLogo.postValue(null)
                }
            } catch (e: Exception) {
                Log.e("LogoFetch", "Error fetching logo for ${meta.name}", e)
                _currentLogo.postValue(null)
            }
        }
    }

    // Fetch trailer for a movie or series
    suspend fun fetchTrailer(itemId: String, type: String): String? {
        if (apiKey.isEmpty()) return null
        return try {
            val tmdbId = itemId.removePrefix("tmdb:").toIntOrNull() ?: return null
            val videos = if (type == "movie") {
                TMDBClient.api.getMovieVideos(tmdbId, apiKey)
            } else {
                TMDBClient.api.getTVVideos(tmdbId, apiKey)
            }
            // Find first video with type "Trailer" and site "YouTube"
            val trailer = videos.results.firstOrNull { it.type == "Trailer" && it.site == "YouTube" }
            trailer?.let { "https://www.youtube.com/watch?v=${it.key}" }
        } catch (e: Exception) {
            Log.e("MainViewModel", "Failed to fetch trailer", e)
            null
        }
    }

    // Fetch similar content for a movie or series - returns same type only
    suspend fun fetchSimilarContent(itemId: String, type: String): List<MetaItem> {
        if (apiKey.isEmpty()) return emptyList()
        return try {
            val tmdbId = itemId.removePrefix("tmdb:").toIntOrNull() ?: return emptyList()

            if (type == "movie") {
                val response = TMDBClient.api.getSimilarMovies(tmdbId, apiKey)
                response.results.map { it.toMetaItem() }
            } else {
                val response = TMDBClient.api.getSimilarTV(tmdbId, apiKey)
                response.results.map { it.toMetaItem() }
            }
        } catch (e: Exception) {
            Log.e("MainViewModel", "Failed to fetch similar content", e)
            emptyList()
        }
    }

    // Fetch popular content filtered by genre
    suspend fun fetchPopularByGenre(type: String, genreId: Int): List<MetaItem> {
        if (apiKey.isEmpty()) return emptyList()
        return try {
            val results = if (type == "movie") {
                val response = TMDBClient.api.discoverMovies(
                    apiKey = apiKey,
                    withGenres = genreId.toString(),
                    sortBy = "popularity.desc",
                    page = 1
                )
                response.results.map { it.toMetaItem() }
            } else {
                val response = TMDBClient.api.discoverTV(
                    apiKey = apiKey,
                    withGenres = genreId.toString(),
                    sortBy = "popularity.desc",
                    page = 1
                )
                response.results.map { it.toMetaItem() }
            }
            results
        } catch (e: Exception) {
            Log.e("MainViewModel", "Failed to fetch content by genre", e)
            emptyList()
        }
    }

    fun refreshTVGuide() {
        viewModelScope.launch {
            val epgUrl = prefsManager.getLiveTVEPGUrl()
            if (epgUrl.isNullOrEmpty()) {
                _epgParsingStatus.postValue("Error: No EPG URL configured")
                return@launch
            }

            try {
                _epgParsingStatus.postValue("Starting EPG refresh...")
                val programs = com.example.stremiompvplayer.utils.EPGParser.parseEPG(epgUrl) { status ->
                    _epgParsingStatus.postValue(status)
                }
                _epgParsingStatus.postValue("Success: Loaded ${programs.size} programs")
            } catch (e: Exception) {
                _epgParsingStatus.postValue("Error: ${e.message}")
            }
        }
    }

    sealed class TraktSyncStatus {
        object Idle : TraktSyncStatus()
        data class Syncing(val progress: String) : TraktSyncStatus()
        data class Success(val message: String) : TraktSyncStatus()
        data class Error(val message: String) : TraktSyncStatus()
    }

    sealed class ActionResult {
        data class Success(val message: String) : ActionResult()
        data class Error(val message: String) : ActionResult()
    }
}