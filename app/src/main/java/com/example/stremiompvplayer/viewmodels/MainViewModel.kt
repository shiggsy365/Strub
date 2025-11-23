package com.example.stremiompvplayer.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.example.stremiompvplayer.CatalogRepository
import com.example.stremiompvplayer.models.Meta
import com.example.stremiompvplayer.models.MetaItem
import com.example.stremiompvplayer.models.Stream
import com.example.stremiompvplayer.models.UserCatalog
import com.example.stremiompvplayer.network.TMDBClient
import com.example.stremiompvplayer.network.TraktClient
import com.example.stremiompvplayer.network.TraktDeviceCodeResponse
import com.example.stremiompvplayer.utils.Secrets
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
import com.example.stremiompvplayer.models.Video
import com.example.stremiompvplayer.models.TMDBWatchlistBody
import com.example.stremiompvplayer.network.AIOStreamsClient


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

    private val apiKey: String get() = prefsManager.getTMDBApiKey() ?: ""

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

                    // Trigger full setup including lists
                    performTraktSync(syncHistory = true, syncNextUp = true, syncLists = true, fetchMetadata = false)

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

    // === CONTENT LOADING (Refactored) ===

    // Original method used by DiscoverFragment
    fun loadContentForCatalog(catalog: UserCatalog, isInitialLoad: Boolean = true) {
        if (apiKey.isEmpty()) return

        _isLoading.postValue(true)
        if (isInitialLoad) {
            loadedContentCache.clear()
            lastRequestedCatalog = catalog
        }

        viewModelScope.launch {
            try {
                val items = fetchCatalogItems(catalog)

                if (isInitialLoad) {
                    loadedContentCache.addAll(items)
                    _currentCatalogContent.postValue(loadedContentCache)
                } else {
                    _currentCatalogContent.postValue(items)
                }
            } catch (e: Exception) {
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
        if (apiKey.isEmpty()) return

        viewModelScope.launch {
            val userId = prefsManager.getCurrentUserId() ?: "default"
            val isTrakt = prefsManager.isTraktEnabled()

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
                _homeNextUp.postValue(nextUpJob.await())
                _homeContinueEpisodes.postValue(showsJob.await())
                _homeContinueMovies.postValue(moviesJob.await())
            } catch (e: Exception) {
                Log.e("HomeLoad", "Error loading home content", e)
            }
        }
    }

    // Reusable suspended function
    private suspend fun fetchCatalogItems(catalog: UserCatalog): List<MetaItem> {
        if (catalog.addonUrl == "trakt") {
            val token = prefsManager.getTraktAccessToken()
            val clientId = Secrets.TRAKT_CLIENT_ID

            if (token != null) {
                val bearer = "Bearer $token"
                val items: List<MetaItem> = when (catalog.catalogId) {
                    "trakt_next_up" -> generateNextUpList()

                    "trakt_continue_movies" -> {
                        val list = TraktClient.api.getPausedMovies(bearer, clientId)
                        list.mapNotNull { it.movie }.map { movie ->
                            MetaItem(id = "tmdb:${movie.ids.tmdb}", type = "movie", name = movie.title, description = "Paused", poster = null, background = null)
                        }
                    }

                    "trakt_continue_shows" -> {
                        val list = TraktClient.api.getPausedEpisodes(bearer, clientId)
                        list.mapNotNull { item ->
                            val show = item.show
                            val ep = item.episode
                            if (show != null && ep != null) {
                                val epTmdbId = ep.ids?.tmdb
                                if (epTmdbId != null) {
                                    MetaItem(
                                        id = "trakt_ep:${show.ids.tmdb}:$epTmdbId",
                                        type = "episode",
                                        name = "${show.title} - ${ep.number}",
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

                    else -> emptyList()
                }

                return enrichWithTmdbMetadata(items, catalog.catalogId)
            } else {
                return emptyList()
            }

        } else if (catalog.catalogId in listOf("popular", "latest", "trending")) {
            val pagesToLoad = listOf(1, 2)
            val allItems = mutableListOf<MetaItem>()

            val deferredResults = pagesToLoad.map { page ->
                viewModelScope.async {
                    try {
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
                    } catch (e: Exception) { null }
                }
            }
            val results = deferredResults.awaitAll()
            results.filterNotNull().forEach { response ->
                val fetchedItems = if (response is TMDBMovieListResponse) {
                    response.results.map { it.toMetaItem() }
                } else if (response is TMDBSeriesListResponse) {
                    response.results.map { it.toMetaItem() }
                } else { emptyList() }
                allItems.addAll(fetchedItems)
            }
            return allItems

        } else {
            return when (catalog.catalogId) {
                "continue_movies" -> {
                    val currentUserId = prefsManager.getCurrentUserId() ?: "default"
                    catalogRepository.getContinueWatching(currentUserId, "movie").map { progress ->
                        MetaItem(id = progress.itemId, type = progress.type, name = progress.name ?: "Unknown", poster = progress.poster, background = progress.background, description = null, isWatched = progress.isWatched, progress = progress.progress, duration = progress.duration)
                    }
                }
                "continue_episodes" -> {
                    val currentUserId = prefsManager.getCurrentUserId() ?: "default"
                    catalogRepository.getContinueWatching(currentUserId, "episode").map { progress ->
                        MetaItem(id = progress.itemId, type = progress.type, name = progress.name ?: "Unknown", poster = progress.poster, background = progress.background, description = null, isWatched = progress.isWatched, progress = progress.progress, duration = progress.duration)
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
                            } else {
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
    private suspend fun generateNextUpList(): List<MetaItem> {
        val currentUserId = prefsManager.getCurrentUserId() ?: return emptyList()
        val watchedEpisodes = catalogRepository.getNextUpCandidates(currentUserId)
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val today = dateFormat.format(java.util.Date())

        val latestEpisodesByShow = watchedEpisodes
            .filter { it.parentId != null && it.season != null && it.episode != null }
            .groupBy { it.parentId!! }
            .mapValues { (_, episodes) ->
                episodes.maxWithOrNull(compareBy<WatchProgress> { it.season!! }.thenBy { it.episode!! })
            }

        val nextUpItems = mutableListOf<MetaItem>()

        for ((showId, latestEpisode) in latestEpisodesByShow) {
            if (latestEpisode == null) continue

            val tmdbId = showId.removePrefix("tmdb:").toIntOrNull() ?: continue
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
                                val nextEpisodeProgress = catalogRepository.getWatchProgress(currentUserId, nextEpisodeId)

                                if (nextEpisodeProgress == null || !nextEpisodeProgress.isWatched) {
                                    val poster = nextEpDetails.still_path?.let { "https://image.tmdb.org/t/p/w500$it" }
                                        ?: showDetails.poster_path?.let { "https://image.tmdb.org/t/p/w500$it" }

                                    val background = nextEpDetails.still_path?.let { "https://image.tmdb.org/t/p/original$it" }
                                        ?: showDetails.backdrop_path?.let { "https://image.tmdb.org/t/p/original$it" }

                                    nextUpItems.add(
                                        MetaItem(
                                            id = nextEpisodeId,
                                            type = "episode",
                                            name = nextEpDetails.name,
                                            poster = poster,
                                            background = background,
                                            description = nextEpDetails.overview ?: "Next episode"
                                        )
                                    )
                                }
                            }
                        }
                    } catch (e: Exception) { Log.e("NextUp", "Failed season details", e) }
                }
            } catch (e: Exception) { Log.e("NextUp", "Error processing next episode", e) }
        }
        return nextUpItems
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
                            val meta = MetaItem("tmdb:$tmdbId", "movie", item.movie.title, null, null, "Trakt Import")
                            catalogRepository.addToLibrary(CollectedItem.fromMetaItem(userId, meta))
                            catalogRepository.saveWatchProgress(WatchProgress(userId, meta.id, "movie", 0, 0, true, System.currentTimeMillis(), meta.name, null, null, null, null, null))
                            importedMovieIds.add(tmdbId)
                        }
                    }

                    val watchedShows = TraktClient.api.getWatchedShows(bearer, clientId)
                    val importedShowIds = mutableListOf<Int>()

                    watchedShows.forEach { item ->
                        item.show?.ids?.tmdb?.let { showTmdbId ->
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

                    // Refresh TMDB metadata for all imported items
                    if (fetchMetadata && apiKey.isNotEmpty()) {
                        _traktSyncStatus.postValue(TraktSyncStatus.Syncing("Refreshing metadata from TMDB..."))

                        // Refresh movie metadata
                        importedMovieIds.forEach { tmdbId ->
                            try {
                                val details = TMDBClient.api.getMovieDetails(tmdbId, apiKey)
                                val poster = details.poster_path?.let { "https://image.tmdb.org/t/p/w500$it" }
                                val background = details.backdrop_path?.let { "https://image.tmdb.org/t/p/original$it" }

                                // Update the library item with enriched metadata
                                val enrichedMeta = MetaItem(
                                    id = "tmdb:$tmdbId",
                                    type = "movie",
                                    name = details.title,
                                    poster = poster,
                                    background = background,
                                    description = details.overview,
                                    releaseDate = details.release_date
                                )
                                catalogRepository.addToLibrary(CollectedItem.fromMetaItem(userId, enrichedMeta))
                            } catch (e: Exception) {
                                Log.e("TraktSync", "Failed to refresh movie $tmdbId: ${e.message}")
                            }
                        }

                        // Refresh show metadata
                        importedShowIds.forEach { tmdbId ->
                            try {
                                val details = TMDBClient.api.getTVDetails(tmdbId, apiKey)
                                val poster = details.poster_path?.let { "https://image.tmdb.org/t/p/w500$it" }
                                val background = details.backdrop_path?.let { "https://image.tmdb.org/t/p/original$it" }

                                // Update the library item with enriched metadata
                                val enrichedMeta = MetaItem(
                                    id = "tmdb:$tmdbId",
                                    type = "series",
                                    name = details.name,
                                    poster = poster,
                                    background = background,
                                    description = details.overview,
                                    releaseDate = details.first_air_date
                                )
                                catalogRepository.addToLibrary(CollectedItem.fromMetaItem(userId, enrichedMeta))
                            } catch (e: Exception) {
                                Log.e("TraktSync", "Failed to refresh show $tmdbId: ${e.message}")
                            }
                        }
                    }
                }

                // No need to insert "Next Up" or "Continue Watching" to DB anymore
                // They are handled dynamically by HomeFragment.
                // We only sync lists like Watchlists/Popular to DB for Discover page.

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

                _traktSyncStatus.postValue(TraktSyncStatus.Success("Sync Complete"))

            } catch (e: Exception) {
                _traktSyncStatus.postValue(TraktSyncStatus.Error(e.message ?: "Error"))
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    private fun startPeriodicTraktSync() {
        viewModelScope.launch {
            while (prefsManager.isTraktEnabled()) {
                delay(30 * 60 * 1000L) // Every 30 minutes
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
            try {
                if (syncToTrakt && prefsManager.isTraktEnabled()) {
                    val token = prefsManager.getTraktAccessToken()
                    if (token != null) {
                        val bearer = "Bearer $token"
                        val body = if (item.type == "movie") {
                            val id = item.id.removePrefix("tmdb:").toIntOrNull()
                            if (id != null) TraktHistoryBody(movies = listOf(TraktMovie("", null, TraktIds(0, id, null, null)))) else null
                        } else if (item.type == "episode") {
                            val parts = item.id.split(":")
                            if (parts.size >= 4) {
                                val e = parts[3].toIntOrNull()
                                if (e != null) TraktHistoryBody(episodes = listOf(TraktEpisode(e, 1, null, null))) else null
                            } else null
                        } else null

                        if (body != null) {
                            TraktClient.api.addToHistory(bearer, Secrets.TRAKT_CLIENT_ID, body)
                        }
                    }
                }

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

                // Refresh
                lastRequestedCatalog?.let {
                    loadContentForCatalog(it, isInitialLoad = false)
                }
                // Also reload home if needed
                loadHomeContent()

                _isItemWatched.postValue(true)
                _actionResult.postValue(ActionResult.Success("Marked as watched"))

            } catch (e: Exception) {
                _actionResult.postValue(ActionResult.Error("Failed: ${e.message}"))
            }
        }
    }

    fun clearWatchedStatus(item: MetaItem, syncToTrakt: Boolean = true) {
        val currentUserId = prefsManager.getCurrentUserId() ?: return
        val tmdbId = item.id.removePrefix("tmdb:").split(":")[0].toIntOrNull()

        viewModelScope.launch {
            try {
                if (syncToTrakt && prefsManager.isTraktEnabled() && tmdbId != null) {
                    val token = prefsManager.getTraktAccessToken()
                    if (token != null) {
                        val bearer = "Bearer $token"
                        val body = if (item.type == "movie") {
                            TraktHistoryBody(movies = listOf(TraktMovie("", null, TraktIds(0, tmdbId, null, null))))
                        } else null

                        if (body != null) TraktClient.api.removeFromHistory(bearer, Secrets.TRAKT_CLIENT_ID, body)
                    }
                }

                catalogRepository.updateWatchedStatus(currentUserId, item.id, false)
                lastRequestedCatalog?.let { loadContentForCatalog(it, isInitialLoad = false) }
                loadHomeContent()

                _isItemWatched.postValue(false)
                _actionResult.postValue(ActionResult.Success("Cleared watched status"))
            } catch (e: Exception) {
                _actionResult.postValue(ActionResult.Error("Failed: ${e.message}"))
            }
        }
    }

    // === STANDARD METHODS ===
    fun loadStreams(type: String, itemId: String) {
        _isLoading.postValue(true)
        viewModelScope.launch {
            val allStreams = mutableListOf<Stream>()
            val aioUsername = prefsManager.getAIOStreamsUsername()
            val aioPassword = prefsManager.getAIOStreamsPassword()
            val aioUrl = prefsManager.getAIOStreamsUrl() ?: "https://aiostreams.shiggsy.co.uk"

            if (!aioUsername.isNullOrEmpty() && !aioPassword.isNullOrEmpty()) {
                try {
                    val aioApi = AIOStreamsClient.getApi(aioUrl, aioUsername, aioPassword)
                    val aioResponse = aioApi.searchStreams(type, itemId)
                    if (aioResponse.success && aioResponse.data != null) {
                        allStreams.addAll(aioResponse.data.results)
                    }
                } catch (e: Exception) { Log.e("MainViewModel", "AIOStreams error", e) }
            }
            _streams.postValue(allStreams)
            _isLoading.postValue(false)
        }
    }

    fun loadEpisodeStreams(seriesId: String, season: Int, episode: Int) {
        val episodeId = "$seriesId:$season:$episode"
        loadStreams("series", episodeId)
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
            } catch (e: Exception) { Log.e("MainViewModel", "Failed to fetch cast", e) }
        }
    }

    fun checkWatchedStatus(itemId: String) {
        val currentUserId = prefsManager.getCurrentUserId() ?: return
        viewModelScope.launch {
            val progress = catalogRepository.getWatchProgress(currentUserId, itemId)
            _isItemWatched.postValue(progress?.isWatched ?: false)
        }
    }

    fun saveWatchProgress(meta: MetaItem, currentPos: Long, duration: Long) {
        val currentUserId = prefsManager.getCurrentUserId() ?: return
        viewModelScope.launch {
            val parts = meta.id.split(":")
            val parentId = if (parts.size >= 3) "${parts[0]}:${parts[1]}" else null
            val season = if (parts.size >= 3) parts[2].toIntOrNull() else null
            val episode = if (parts.size >= 4) parts[3].toIntOrNull() else null

            val progress = WatchProgress(
                userId = currentUserId,
                itemId = meta.id,
                type = meta.type,
                progress = currentPos,
                duration = duration,
                isWatched = currentPos >= duration * 0.9,
                lastUpdated = System.currentTimeMillis(),
                name = meta.name,
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
                val results = response.results.filter { it.media_type == "movie" || it.media_type == "tv" }.map { it.toMetaItem() }
                _searchResults.postValue(results)
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
                val results = response.results.map { it.toMetaItem() }
                _searchResults.postValue(results)
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
                val results = response.results.map { it.toMetaItem() }
                _searchResults.postValue(results)
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
                val results = response.cast.map { it.toMetaItem() }
                _searchResults.postValue(results)
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

    fun getDiscoverCatalogs(type: String): LiveData<List<UserCatalog>> {
        // The 'allCatalogConfigs' source is already filtered to exclude Home items
        // So we just need to check the type and 'showInDiscover'
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
                if (syncToTrakt && prefsManager.isTraktEnabled()) {
                    // Sync logic omitted
                }
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

    fun scrobble(action: String, meta: MetaItem, progress: Float) {
        if (!prefsManager.isTraktEnabled()) return
        val token = prefsManager.getTraktAccessToken() ?: return
        val clientId = Secrets.TRAKT_CLIENT_ID
        viewModelScope.launch {
            try {
                val idStr = meta.id.removePrefix("tmdb:")
                val tmdbId = if (meta.type == "movie" || meta.type == "series") idStr.toIntOrNull() else 0
                val body: TraktScrobbleBody? = if (meta.type == "movie" && tmdbId != null) {
                    TraktScrobbleBody(progress, TraktMovie(meta.name, null, TraktIds(0, tmdbId, null, null)))
                } else if (meta.type == "episode") {
                    val parts = meta.id.split(":")
                    if (parts.size >= 4) {
                        val showId = parts[1].toIntOrNull()
                        val s = parts[2].toIntOrNull()
                        val e = parts[3].toIntOrNull()
                        if (showId != null && s != null && e != null) {
                            TraktScrobbleBody(progress, episode = TraktEpisode(s, e), show = TraktShow(meta.name, null, TraktIds(0, showId, null, null)))
                        } else null
                    } else null
                } else null

                if (body != null) {
                    val bearer = "Bearer $token"
                    when (action) {
                        "start" -> TraktClient.api.startScrobble(bearer, clientId, body = body)
                        "pause" -> TraktClient.api.pauseScrobble(bearer, clientId, body = body)
                        "stop" -> TraktClient.api.stopScrobble(bearer, clientId, body = body)
                    }
                }
            } catch (e: Exception) { Log.e("Trakt", "Scrobble failed", e) }
        }
    }

    fun fetchItemLogo(meta: MetaItem) {
        logoFetchJob?.cancel()
        _currentLogo.value = ""
        logoFetchJob = viewModelScope.launch {
            try {
                val idParts = meta.id.removePrefix("tmdb:").split(":")
                val tmdbId = idParts[0].toIntOrNull()
                if (tmdbId != null && apiKey.isNotEmpty()) {
                    val images = if (meta.type == "movie") TMDBClient.api.getMovieImages(tmdbId, apiKey) else TMDBClient.api.getTVImages(tmdbId, apiKey)
                    val logo = images.logos.firstOrNull()
                    _currentLogo.postValue(logo?.let { "https://image.tmdb.org/t/p/w300${it.file_path}" })
                } else { _currentLogo.postValue(null) }
            } catch (e: Exception) { _currentLogo.postValue(null) }
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