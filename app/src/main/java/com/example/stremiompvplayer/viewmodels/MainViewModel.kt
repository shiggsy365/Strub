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
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll

class MainViewModel(
    private val catalogRepository: CatalogRepository,
    private val prefsManager: SharedPreferencesManager
) : ViewModel() {

    // Current catalog content and UI state
    private val _currentCatalogContent = MutableLiveData<List<MetaItem>>()
    val currentCatalogContent: LiveData<List<MetaItem>> = _currentCatalogContent

    private val _currentLogo = MutableLiveData<String?>()
    val currentLogo: LiveData<String?> = _currentLogo
    private var logoFetchJob: Job? = null

    private val apiKey: String get() = prefsManager.getTMDBApiKey() ?: ""

    // Database observables
    val allCatalogConfigs: LiveData<List<UserCatalog>> = catalogRepository.allCatalogs
    val movieCatalogs = allCatalogConfigs.map { it.filter { c -> c.catalogType == "movie" && c.showInUser }.sortedBy { c -> c.displayOrder } }
    val seriesCatalogs = allCatalogConfigs.map { it.filter { c -> c.catalogType == "series" && c.showInUser }.sortedBy { c -> c.displayOrder } }

    // Streams and metadata
    private val _streams = MutableLiveData<List<Stream>>()
    val streams: LiveData<List<Stream>> = _streams

    private val _metaDetails = MutableLiveData<Meta?>()
    val metaDetails: LiveData<Meta?> = _metaDetails

    private val _seasonEpisodes = MutableLiveData<List<MetaItem>>()
    val seasonEpisodes: LiveData<List<MetaItem>> = _seasonEpisodes

    // Cast and crew
    private val _castList = MutableLiveData<List<MetaItem>>()
    val castList: LiveData<List<MetaItem>> = _castList

    private val _director = MutableLiveData<MetaItem?>()
    val director: LiveData<MetaItem?> = _director

    // UI state
    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // TMDB Authentication
    private val _requestToken = MutableLiveData<String?>()
    val requestToken: LiveData<String?> = _requestToken

    private val _sessionId = MutableLiveData<String?>()
    val sessionId: LiveData<String?> = _sessionId

    // Search
    private val _searchResults = MutableLiveData<List<MetaItem>>()
    val searchResults: LiveData<List<MetaItem>> = _searchResults

    private val _isSearching = MutableLiveData<Boolean>()
    val isSearching: LiveData<Boolean> = _isSearching

    // User library status
    private val _isItemInWatchlist = MutableLiveData<Boolean>(false)
    val isItemInWatchlist: LiveData<Boolean> = _isItemInWatchlist

    private val _isItemInLibrary = MutableLiveData<Boolean>(false)
    val isItemInLibrary: LiveData<Boolean> = _isItemInLibrary

    private val _isItemWatched = MutableLiveData<Boolean>(false)
    val isItemWatched: LiveData<Boolean> = _isItemWatched

    // Library content
    val libraryMovies = catalogRepository.getLibraryItems(prefsManager.getCurrentUserId() ?: "default", "movie").map { it.map { toMetaItem(it) } }
    val librarySeries = catalogRepository.getLibraryItems(prefsManager.getCurrentUserId() ?: "default", "series").map { it.map { toMetaItem(it) } }

    init {
        initDefaultCatalogs()
        initUserLists()
    }

    private fun toMetaItem(item: CollectedItem): MetaItem {
        return MetaItem(item.itemId, item.itemType, item.name, item.poster, item.background, item.description)
    }

    // === LOGO FETCHING ===
    fun fetchItemLogo(meta: MetaItem) {
        logoFetchJob?.cancel()
        logoFetchJob = viewModelScope.launch {
            try {
                val idStr = meta.id.removePrefix("tmdb:")
                val id = idStr.toIntOrNull()
                if (id != null && apiKey.isNotEmpty()) {
                    val images = if (meta.type == "movie") {
                        TMDBClient.api.getMovieImages(id, apiKey)
                    } else {
                        TMDBClient.api.getTVImages(id, apiKey)
                    }
                    val logo = images.logos.firstOrNull()
                    _currentLogo.postValue(logo?.let { "https://image.tmdb.org/t/p/w300${it.file_path}" })
                } else {
                    _currentLogo.postValue(null)
                }
            } catch (e: Exception) {
                _currentLogo.postValue(null)
            }
        }
    }

    // === CONTENT LOADING ===
    fun loadContentForCatalog(catalog: UserCatalog) {
        if (apiKey.isEmpty()) return

        _isLoading.postValue(true)
        viewModelScope.launch {
            try {
                val items = when (catalog.catalogId) {
                    "popular" -> {
                        if (catalog.catalogType == "movie") {
                            TMDBClient.api.getPopularMovies(apiKey, page = 1).results.map { it.toMetaItem() }
                        } else {
                            TMDBClient.api.getPopularSeries(apiKey, page = 1).results.map { it.toMetaItem() }
                        }
                    }
                    "latest" -> {
                        if (catalog.catalogType == "movie") {
                            TMDBClient.api.getLatestMovies(apiKey, page = 1).results.map { it.toMetaItem() }
                        } else {
                            TMDBClient.api.getLatestSeries(apiKey, page = 1).results.map { it.toMetaItem() }
                        }
                    }
                    "trending" -> {
                        if (catalog.catalogType == "movie") {
                            TMDBClient.api.getTrendingMovies(apiKey, page = 1).results.map { it.toMetaItem() }
                        } else {
                            TMDBClient.api.getTrendingSeries(apiKey, page = 1).results.map { it.toMetaItem() }
                        }
                    }
                    "continue_movies" -> {
                        val userId = prefsManager.getCurrentUserId() ?: "default"
                        catalogRepository.getContinueWatching(userId, "movie").map { progress ->
                            MetaItem(
                                id = progress.itemId,
                                type = progress.type,
                                name = progress.name ?: "Unknown",
                                poster = progress.poster,
                                background = progress.background,
                                description = null,
                                isWatched = progress.isWatched,
                                progress = progress.progress,
                                duration = progress.duration
                            )
                        }
                    }
                    "continue_episodes" -> {
                        val userId = prefsManager.getCurrentUserId() ?: "default"
                        catalogRepository.getContinueWatching(userId, "episode").map { progress ->
                            MetaItem(
                                id = progress.itemId,
                                type = progress.type,
                                name = progress.name ?: "Unknown",
                                poster = progress.poster,
                                background = progress.background,
                                description = null,
                                isWatched = progress.isWatched,
                                progress = progress.progress,
                                duration = progress.duration
                            )
                        }
                    }
                    "next_up" -> generateNextUpList()
                    else -> emptyList()
                }

                // Enhance with watch progress
                val userId = prefsManager.getCurrentUserId() ?: "default"
                val enhancedItems = items.map { item ->
                    val progress = catalogRepository.getWatchProgress(userId, item.id)
                    item.copy(
                        isWatched = progress?.isWatched ?: false,
                        progress = progress?.progress ?: 0,
                        duration = progress?.duration ?: 0
                    )
                }

                _currentCatalogContent.postValue(enhancedItems)
            } catch (e: Exception) {
                _error.postValue("Failed to load content: ${e.message}")
                _currentCatalogContent.postValue(emptyList())
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    private suspend fun generateNextUpList(): List<MetaItem> {
        val userId = prefsManager.getCurrentUserId() ?: return emptyList()
        val watchedEpisodes = catalogRepository.getNextUpCandidates(userId)

        val nextUpItems = mutableListOf<MetaItem>()
        val processedShows = mutableSetOf<String>()

        for (episode in watchedEpisodes) {
            val parentId = episode.parentId ?: continue
            if (processedShows.contains(parentId)) continue

            val season = episode.season ?: continue
            val episodeNum = episode.episode ?: continue
            val nextEpisodeId = "$parentId:$season:${episodeNum + 1}"

            // Check if next episode exists and isn't watched
            val nextEpisodeProgress = catalogRepository.getWatchProgress(userId, nextEpisodeId)
            if (nextEpisodeProgress == null || !nextEpisodeProgress.isWatched) {
                nextUpItems.add(MetaItem(
                    id = nextEpisodeId,
                    type = "episode",
                    name = "${episode.name?.split(" S")?.get(0) ?: "Unknown"} S$season E${episodeNum + 1}",
                    poster = episode.poster,
                    background = episode.background,
                    description = "Next episode to watch"
                ))
                processedShows.add(parentId)
            }
        }

        return nextUpItems
    }

    // === STREAMS LOADING ===
    fun loadStreams(type: String, id: String) {
        _isLoading.postValue(true)
        viewModelScope.launch {
            val allStreams = mutableListOf<Stream>()

            // Try AIOStreams if configured
            val aioUsername = prefsManager.getAIOStreamsUsername()
            val aioPassword = prefsManager.getAIOStreamsPassword()

            if (!aioUsername.isNullOrEmpty() && !aioPassword.isNullOrEmpty()) {
                try {
                    val aioApi = AIOStreamsClient.getApi(aioUsername, aioPassword)
                    val aioResponse = aioApi.searchStreams(type, id)
                    if (aioResponse.success && aioResponse.data != null) {
                        allStreams.addAll(aioResponse.data.results)
                    }
                } catch (e: Exception) {
                    Log.e("MainViewModel", "AIOStreams error", e)
                }
            }

            _streams.postValue(allStreams)
            _isLoading.postValue(false)
        }
    }

    fun loadEpisodeStreams(seriesId: String, season: Int, episode: Int) {
        val episodeId = "$seriesId:$season:$episode"
        loadStreams("series", episodeId)
    }

    // === SERIES METADATA ===
    fun loadSeriesMeta(id: String) {
        if (apiKey.isEmpty()) return

        viewModelScope.launch {
            try {
                val tmdbId = id.removePrefix("tmdb:").toIntOrNull() ?: return@launch
                val details = TMDBClient.api.getTVDetails(tmdbId, apiKey)

                val meta = Meta(
                    id = id,
                    type = "series",
                    name = details.name,
                    poster = details.poster_path?.let { "https://image.tmdb.org/t/p/w500$it" },
                    background = details.backdrop_path?.let { "https://image.tmdb.org/t/p/original$it" },
                    description = details.overview,
                    videos = details.seasons?.map { season ->
                        Video(
                            id = season.id.toString(),
                            title = season.name,
                            released = null,
                            thumbnail = season.poster_path?.let { "https://image.tmdb.org/t/p/w500$it" },
                            number = null,
                            season = season.season_number
                        )
                    }
                )
                _metaDetails.postValue(meta)
            } catch (e: Exception) {
                _error.postValue("Failed to load series details: ${e.message}")
            }
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
            } catch (e: Exception) {
                _error.postValue("Failed to load season episodes: ${e.message}")
            }
        }
    }

    // === CAST & CREW ===
    fun fetchCast(id: String, type: String) {
        if (apiKey.isEmpty()) return

        viewModelScope.launch {
            try {
                val tmdbId = id.removePrefix("tmdb:").toIntOrNull() ?: return@launch

                val credits = if (type == "movie") {
                    TMDBClient.api.getMovieCredits(tmdbId, apiKey)
                } else {
                    TMDBClient.api.getTVCredits(tmdbId, apiKey)
                }

                val cast = credits.cast.take(10).map { member ->
                    MetaItem(
                        id = "tmdb:${member.id}",
                        type = "person",
                        name = member.name,
                        poster = member.profile_path?.let { "https://image.tmdb.org/t/p/w500$it" },
                        background = null,
                        description = member.character
                    )
                }

                val director = credits.crew.find { it.job == "Director" }?.let { member ->
                    MetaItem(
                        id = "tmdb:${member.id}",
                        type = "person",
                        name = member.name,
                        poster = null,
                        background = null,
                        description = "Director"
                    )
                }

                _castList.postValue(cast)
                _director.postValue(director)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to fetch cast", e)
            }
        }
    }

    // === WATCH PROGRESS ===
    fun checkWatchedStatus(itemId: String) {
        val userId = prefsManager.getCurrentUserId() ?: return
        viewModelScope.launch {
            val progress = catalogRepository.getWatchProgress(userId, itemId)
            _isItemWatched.postValue(progress?.isWatched ?: false)
        }
    }

    fun saveWatchProgress(meta: MetaItem, currentPos: Long, duration: Long) {
        val userId = prefsManager.getCurrentUserId() ?: return
        viewModelScope.launch {
            val parts = meta.id.split(":")
            val parentId = if (parts.size >= 3) "${parts[0]}:${parts[1]}" else null
            val season = if (parts.size >= 3) parts[2].toIntOrNull() else null
            val episode = if (parts.size >= 4) parts[3].toIntOrNull() else null

            val progress = WatchProgress(
                userId = userId,
                itemId = meta.id,
                type = meta.type,
                progress = currentPos,
                duration = duration,
                isWatched = currentPos >= duration * 0.9, // Mark watched if 90% complete
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

    // === WATCHED STATUS MANAGEMENT ===
    fun markAsWatched(item: MetaItem) {
        val userId = prefsManager.getCurrentUserId() ?: return
        val idStr = item.id.removePrefix("tmdb:")
        val id = idStr.toIntOrNull()

        viewModelScope.launch {
            if (item.type == "series" && id != null && apiKey.isNotEmpty()) {
                // SERIES LEVEL: Mark ALL episodes
                try {
                    val details = TMDBClient.api.getTVDetails(id, apiKey)

                    // Iterate all seasons
                    details.seasons?.forEach { season ->
                        if (season.episode_count > 0) {
                            // Fetch episodes for this season to get IDs
                            try {
                                val seasonDetails = TMDBClient.api.getTVSeasonDetails(id, season.season_number, apiKey)
                                seasonDetails.episodes.forEach { ep ->
                                    val epId = "tmdb:$id:${ep.season_number}:${ep.episode_number}"
                                    val progress = WatchProgress(
                                        userId = userId,
                                        itemId = epId,
                                        type = "episode",
                                        progress = 0, // Done
                                        duration = 0,
                                        isWatched = true,
                                        lastUpdated = System.currentTimeMillis(),
                                        name = "${details.name} S${ep.season_number} E${ep.episode_number}",
                                        poster = item.poster,
                                        background = item.background,
                                        parentId = item.id,
                                        season = ep.season_number,
                                        episode = ep.episode_number
                                    )
                                    catalogRepository.saveWatchProgress(progress)
                                }
                            } catch (e: Exception) {
                                Log.e("MainViewModel", "Failed to mark season ${season.season_number}", e)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MainViewModel", "Failed to mark series watched", e)
                }
            } else {
                // SINGLE ITEM (Movie or Episode)
                val parts = item.id.split(":")
                val parentId = if (parts.size >= 3) "${parts[0]}:${parts[1]}" else null
                val season = if (parts.size >= 3) parts[2].toIntOrNull() else null
                val episode = if (parts.size >= 4) parts[3].toIntOrNull() else null

                val progress = WatchProgress(
                    userId, item.id, item.type, 0, 0, true,
                    System.currentTimeMillis(), item.name, item.poster, item.background,
                    parentId, season, episode
                )
                catalogRepository.saveWatchProgress(progress)
            }
            _isItemWatched.postValue(true)
        }
    }

    fun clearWatchedStatus(item: MetaItem) {
        val userId = prefsManager.getCurrentUserId() ?: return
        val idStr = item.id.removePrefix("tmdb:")
        val id = idStr.toIntOrNull()

        viewModelScope.launch {
            if (item.type == "series" && id != null && apiKey.isNotEmpty()) {
                // SERIES LEVEL: Clear ALL episodes
                try {
                    val details = TMDBClient.api.getTVDetails(id, apiKey)
                    details.seasons?.forEach { season ->
                        val seasonDetails = TMDBClient.api.getTVSeasonDetails(id, season.season_number, apiKey)
                        seasonDetails.episodes.forEach { ep ->
                            val epId = "tmdb:$id:${ep.season_number}:${ep.episode_number}"
                            catalogRepository.updateWatchedStatus(userId, epId, false)
                        }
                    }
                } catch (e: Exception) { /* error */ }
            } else {
                // Single Item
                catalogRepository.updateWatchedStatus(userId, item.id, false)
            }
            _isItemWatched.postValue(false)
        }
    }

    // === SEARCH ===
    fun searchTMDB(query: String) {
        if (apiKey.isEmpty() || query.isBlank()) return

        _isSearching.postValue(true)
        viewModelScope.launch {
            try {
                val response = TMDBClient.api.searchMulti(apiKey, query)
                val results = response.results.filter {
                    it.media_type == "movie" || it.media_type == "tv"
                }.map { it.toMetaItem() }
                _searchResults.postValue(results)
            } catch (e: Exception) {
                _error.postValue("Search failed: ${e.message}")
                _searchResults.postValue(emptyList())
            } finally {
                _isSearching.postValue(false)
            }
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
            } finally {
                _isSearching.postValue(false)
            }
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
            } finally {
                _isSearching.postValue(false)
            }
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
            } finally {
                _isSearching.postValue(false)
            }
        }
    }

    fun clearSearchResults() {
        _searchResults.postValue(emptyList())
    }

    // === LIBRARY MANAGEMENT ===
    fun addToLibrary(meta: MetaItem) {
        val userId = prefsManager.getCurrentUserId() ?: return
        viewModelScope.launch {
            val item = CollectedItem.fromMetaItem(userId, meta)
            catalogRepository.addToLibrary(item)
            _isItemInLibrary.postValue(true)
        }
    }

    fun removeFromLibrary(id: String) {
        val userId = prefsManager.getCurrentUserId() ?: return
        viewModelScope.launch {
            catalogRepository.removeFromLibrary(id, userId)
            _isItemInLibrary.postValue(false)
        }
    }

    fun toggleLibrary(meta: MetaItem) {
        val userId = prefsManager.getCurrentUserId() ?: return
        viewModelScope.launch {
            val isCollected = catalogRepository.isItemCollected(meta.id, userId)
            if (isCollected) {
                removeFromLibrary(meta.id)
            } else {
                addToLibrary(meta)
            }
        }
    }

    fun checkLibraryStatus(id: String) {
        val userId = prefsManager.getCurrentUserId() ?: return
        viewModelScope.launch {
            val isCollected = catalogRepository.isItemCollected(id, userId)
            _isItemInLibrary.postValue(isCollected)
        }
    }

    // === WATCHLIST MANAGEMENT (TMDB) ===
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

                val body = TMDBWatchlistBody(
                    mediaType = mediaType,
                    mediaId = tmdbId,
                    watchlist = if (force) true else !isCurrentlyInWatchlist
                )

                TMDBClient.api.addToWatchlist(accountId, apiKey, sessionId, body)
                _isItemInWatchlist.postValue(if (force) true else !isCurrentlyInWatchlist)
            } catch (e: Exception) {
                _error.postValue("Watchlist operation failed: ${e.message}")
            }
        }
    }

    fun checkWatchlistStatus(id: String, type: String) {
        // For now, just set false - implementing full watchlist check requires more TMDB API calls
        _isItemInWatchlist.postValue(false)
    }

    // === TMDB AUTHENTICATION ===
    fun fetchRequestToken() {
        if (apiKey.isEmpty()) return

        viewModelScope.launch {
            try {
                val response = TMDBClient.api.createRequestToken(apiKey)
                if (response.success) {
                    _requestToken.postValue(response.requestToken)
                } else {
                    _error.postValue("Failed to create TMDB request token")
                }
            } catch (e: Exception) {
                _error.postValue("TMDB Auth error: ${e.message}")
            }
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

                    // Get account details
                    val account = TMDBClient.api.getAccountDetails(apiKey, response.sessionId)
                    prefsManager.saveTMDBAccountId(account.id)
                } else {
                    _error.postValue("Failed to create TMDB session")
                }
            } catch (e: Exception) {
                _error.postValue("TMDB Session error: ${e.message}")
            }
        }
    }

    fun checkTMDBAuthAndSync() {
        val sessionId = prefsManager.getTMDBSessionId()
        if (!sessionId.isNullOrEmpty() && apiKey.isNotEmpty()) {
            _sessionId.postValue(sessionId)
        }
    }

    // === CATALOG MANAGEMENT ===
    fun initUserLists() {
        viewModelScope.launch {
            prefsManager.getCurrentUserId()?.let {
                catalogRepository.ensureUserListCatalogs(it)
            }
        }
    }

    fun initDefaultCatalogs() {
        viewModelScope.launch {
            catalogRepository.initializeDefaultsIfNeeded()
        }
    }

    fun updateCatalogConfig(catalog: UserCatalog) {
        viewModelScope.launch {
            catalogRepository.updateCatalog(catalog)
        }
    }

    fun swapCatalogOrder(catalog1: UserCatalog, catalog2: UserCatalog) {
        viewModelScope.launch {
            catalogRepository.swapOrder(catalog1, catalog2)
        }
    }

    fun getDiscoverCatalogs(type: String): LiveData<List<UserCatalog>> {
        return allCatalogConfigs.map { list ->
            list.filter { it.catalogType == type && it.showInDiscover }.sortedBy { it.displayOrder }
        }
    }
}