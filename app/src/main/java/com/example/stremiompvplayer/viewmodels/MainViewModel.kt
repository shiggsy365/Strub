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

    // New LiveData for Season Episodes list
    private val _seasonEpisodes = MutableLiveData<List<MetaItem>>()
    val seasonEpisodes: LiveData<List<MetaItem>> = _seasonEpisodes

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

    // --- AIOStreams Helper ---
    private suspend fun fetchAIOStreams(type: String, id: String): List<Stream> {
        val username = prefsManager.getAIOStreamsUsername()
        val password = prefsManager.getAIOStreamsPassword()

        if (username.isNullOrEmpty() || password.isNullOrEmpty()) {
            return emptyList()
        }

        return try {
            val api = AIOStreamsClient.getApi(username, password)
            val response = api.searchStreams(type, id)
            if (response.success && response.data != null) {
                response.data.results
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("MainViewModel", "AIOStreams Error: ${e.message}")
            emptyList()
        }
    }

    // --- CREDITS (CAST & DIRECTOR) ---
    fun fetchCast(tmdbId: String, type: String) {
        val id = tmdbId.removePrefix("tmdb:").toIntOrNull() ?: return
        val mediaType = if (type == "series") "tv" else "movie"

        viewModelScope.launch {
            try {
                if (apiKey.isEmpty()) return@launch

                if (mediaType == "movie") {
                    // MOVIE: Standard Credits
                    val credits = TMDBClient.api.getMovieCredits(id, apiKey)

                    val directorItem = credits.crew.find { it.job == "Director" }
                    _director.postValue(directorItem?.name)

                    _castList.postValue(credits.cast.take(5))
                } else {
                    // TV: Aggregate Credits
                    val credits = TMDBClient.api.getTVAggregateCredits(id, apiKey)

                    // Find Director (or Exec Producer if Director missing)
                    val directorItem = credits.crew.find { c ->
                        c.jobs?.any { it.job == "Director" } == true
                    } ?: credits.crew.find { c ->
                        c.jobs?.any { it.job == "Executive Producer" } == true
                    }

                    _director.postValue(directorItem?.name)

                    // Map Aggregate Cast to Standard Cast model
                    val mappedCast = credits.cast.take(5).map { aggCast ->
                        // Use the role with the most episodes as the character name
                        val mainRole = aggCast.roles?.maxByOrNull { it.episode_count }?.character
                        TMDBCast(
                            id = aggCast.id,
                            name = aggCast.name,
                            character = mainRole,
                            profile_path = aggCast.profile_path
                        )
                    }
                    _castList.postValue(mappedCast)
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error fetching credits: ${e.message}")
                _director.postValue(null)
                _castList.postValue(emptyList())
            }
        }
    }

    // --- STREAMS LOADING ---

    fun loadStreams(type: String, tmdbId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _streams.value = emptyList()

            try {
                // Fetch AIOStreams in parallel
                val aioStreamsDeferred = async { fetchAIOStreams(type, tmdbId) }

                // Placeholder for other addons
                val otherStreamsDeferred = async { emptyList<Stream>() }

                val aioResults = aioStreamsDeferred.await()
                val otherResults = otherStreamsDeferred.await()

                val combinedStreams = (aioResults + otherResults).distinctBy { it.url }
                _streams.value = combinedStreams

            } catch (e: Exception) {
                _error.value = "Error loading streams: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadEpisodeStreams(tmdbId: String, season: Int, episode: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _streams.value = emptyList()

            // AIOStreams expects ID format: tmdb:12345:1:5
            val episodeId = "$tmdbId:$season:$episode"

            try {
                val aioStreamsDeferred = async { fetchAIOStreams("series", episodeId) }
                val otherStreamsDeferred = async { emptyList<Stream>() }

                val aioResults = aioStreamsDeferred.await()
                val otherResults = otherStreamsDeferred.await()

                val combinedStreams = (aioResults + otherResults).distinctBy { it.url }
                _streams.value = combinedStreams

            } catch (e: Exception) {
                _error.value = "Error loading episode streams: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearStreams() { _streams.value = emptyList() }

    // --- SERIES METADATA ---

    fun loadSeriesMeta(tmdbId: String) {
        val id = tmdbId.removePrefix("tmdb:").toIntOrNull() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                if (apiKey.isEmpty()) return@launch

                val details = TMDBClient.api.getTVDetails(id, apiKey)

                // Create "dummy" video objects for seasons to display in the list
                val seasonVideos = details.seasons?.map { season ->
                    Video(
                        id = "season_${season.season_number}",
                        title = season.name,
                        released = null,
                        thumbnail = if(season.poster_path != null) "https://image.tmdb.org/t/p/w500${season.poster_path}" else null,
                        season = season.season_number,
                        number = 0,
                        overview = null
                    )
                } ?: emptyList()

                val meta = Meta(
                    id = "tmdb:$id",
                    type = "series",
                    name = details.name,
                    poster = if(details.poster_path != null) "https://image.tmdb.org/t/p/w500${details.poster_path}" else null,
                    background = if(details.backdrop_path != null) "https://image.tmdb.org/t/p/original${details.backdrop_path}" else null,
                    description = details.overview,
                    videos = seasonVideos
                )

                _metaDetails.postValue(meta)
            } catch (e: Exception) {
                _error.value = "Failed to load series: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadSeasonEpisodes(tmdbId: String, seasonNumber: Int) {
        val id = tmdbId.removePrefix("tmdb:").toIntOrNull() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                if (apiKey.isEmpty()) return@launch

                val seasonDetails = TMDBClient.api.getTVSeasonDetails(id, seasonNumber, apiKey)

                val items = seasonDetails.episodes.map { ep ->
                    MetaItem(
                        id = "tmdb:$id:${ep.season_number}:${ep.episode_number}",
                        type = "episode",
                        name = "Ep ${ep.episode_number}: ${ep.name}",
                        poster = if(ep.still_path != null) "https://image.tmdb.org/t/p/w500${ep.still_path}" else null,
                        background = null,
                        description = ep.overview
                    )
                }
                _seasonEpisodes.postValue(items)

            } catch (e: Exception) {
                _error.value = "Failed to load episodes: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // --- SEARCH ---

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
                // Include People in the mixed search results
                _searchResults.value = allResults
                    .filter { it.mediaType == "movie" || it.mediaType == "tv" || it.mediaType == "person" }
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

    fun searchPeople(query: String) {
        if (query.isBlank()) { _searchResults.value = emptyList(); return }
        viewModelScope.launch {
            _isSearching.value = true
            try {
                if (apiKey.isEmpty()) return@launch

                val allPeople = mutableListOf<TMDBPerson>()
                for (page in 1..3) {
                    try {
                        val response = TMDBClient.api.searchPeople(apiKey, query, page = page)
                        allPeople.addAll(response.results)
                    } catch (e: Exception) { break }
                }

                _searchResults.value = allPeople.map { it.toMetaItem() }

            } catch (e: Exception) {
                _error.value = "Person search failed: ${e.message}"
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun clearSearchResults() { _searchResults.value = emptyList() }


    // --- CATALOGS & CONTENT LOADING ---
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
            try { list.addAll(fetcher(i).results) } catch(e:Exception){ break }
        }
        return list.map { it.toMetaItem() }
    }

    private suspend fun fetchTV(fetcher: suspend (Int) -> TMDBSeriesListResponse): List<MetaItem> {
        val list = mutableListOf<TMDBSeries>()
        for (i in 1..5) {
            try { list.addAll(fetcher(i).results) } catch(e:Exception){ break }
        }
        return list.map { it.toMetaItem() }
    }

    // --- AUTH & UTILS ---
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
        }
    }

    fun removeFromLibrary(metaId: String) {
        val userId = prefsManager.getCurrentUserId() ?: return
        viewModelScope.launch {
            catalogRepository.removeFromLibrary(metaId, userId)
            _isItemInLibrary.postValue(false)
        }
    }

    fun toggleLibrary(meta: MetaItem) {
        if (_isItemInLibrary.value == true) removeFromLibrary(meta.id) else addToLibrary(meta)
    }

    fun fetchRequestToken() {
        viewModelScope.launch {
            try {
                if (apiKey.isEmpty()) return@launch
                val response = TMDBClient.api.createRequestToken(apiKey)
                if (response.success) _requestToken.value = response.requestToken
            } catch (e: Exception) { _error.value = "Auth Error: ${e.message}" }
        }
    }

    fun createSession(token: String) {
        viewModelScope.launch {
            try {
                if (apiKey.isEmpty()) return@launch
                val response = TMDBClient.api.createSession(apiKey, mapOf("request_token" to token))
                if (response.success) {
                    prefsManager.saveTMDBSessionId(response.sessionId)
                    fetchAccountDetails(response.sessionId)
                }
            } catch (e: Exception) { _error.value = "Session Error: ${e.message}" }
        }
    }

    fun checkTMDBAuthAndSync() {
        val sessionId = prefsManager.getTMDBSessionId()
        if (!sessionId.isNullOrEmpty()) {
            viewModelScope.launch { fetchAccountDetails(sessionId) }
        }
    }

    private suspend fun fetchAccountDetails(sessionId: String) {
        try {
            if (apiKey.isEmpty()) return
            val details = TMDBClient.api.getAccountDetails(apiKey, sessionId)
            prefsManager.saveTMDBAccountId(details.id)
            addWatchlistCatalogs()
        } catch (e: Exception) { Log.e("MainViewModel", "Failed to fetch account") }
    }

    private suspend fun addWatchlistCatalogs() {
        val userId = prefsManager.getCurrentUserId() ?: "default"

        // Fix: Use named arguments to ensure correct data types
        if (!catalogRepository.isCatalogAdded(userId, "watchlist_movies", "movie", "movies")) {
            val movieWatchlist = UserCatalog(
                id = 0L, // Let Room auto-generate the ID
                userId = userId,
                catalogId = "watchlist_movies",
                catalogType = "movie",
                catalogName = "TMDB Movies Watchlist",
                customName = null,
                displayOrder = -1,
                pageType = "movies",
                addonUrl = "tmdb",
                manifestId = "tmdb_watchlist",
                showInDiscover = true,
                showInUser = true
            )
            catalogRepository.insertCatalog(movieWatchlist)
        }

        if (!catalogRepository.isCatalogAdded(userId, "watchlist_series", "series", "series")) {
            val seriesWatchlist = UserCatalog(
                id = 0L, // Let Room auto-generate the ID
                userId = userId,
                catalogId = "watchlist_series",
                catalogType = "series",
                catalogName = "TMDB Series Watchlist",
                customName = null,
                displayOrder = -1,
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
        if (sessionId == null || accountId == -1 || apiKey.isEmpty()) { _isItemInWatchlist.value = false; return }
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
            } catch (e: Exception) { _isItemInWatchlist.value = false }
        }
    }

    fun toggleWatchlist(meta: MetaItem) {
        val current = _isItemInWatchlist.value ?: false
        toggleWatchlist(meta, !current)
    }

    fun toggleWatchlist(meta: MetaItem, addToWatchlist: Boolean) {
        val sessionId = prefsManager.getTMDBSessionId()
        val accountId = prefsManager.getTMDBAccountId()
        if (sessionId == null || accountId == -1 || apiKey.isEmpty()) return
        val mediaId = meta.id.removePrefix("tmdb:").toIntOrNull() ?: return
        val mediaType = if (meta.type == "series") "tv" else "movie"
        viewModelScope.launch {
            try {
                val response = TMDBClient.api.addToWatchlist(accountId, apiKey, sessionId, TMDBWatchlistBody(mediaType, mediaId, addToWatchlist))
                if (response.success) _isItemInWatchlist.value = addToWatchlist
            } catch (e: Exception) { _error.value = "Watchlist Error: ${e.message}" }
        }
    }

    fun getDiscoverCatalogs(type: String): LiveData<List<UserCatalog>> {
        return allCatalogConfigs.map { list -> list.filter { it.catalogType == type && it.showInDiscover }.sortedBy { it.displayOrder } }
    }

    fun updateCatalogConfig(c: UserCatalog) { viewModelScope.launch { catalogRepository.updateCatalog(c) } }
    fun swapCatalogOrder(i1: UserCatalog, i2: UserCatalog) { viewModelScope.launch { catalogRepository.swapOrder(i1, i2) } }
    fun initDefaultCatalogs() { viewModelScope.launch { catalogRepository.initializeDefaultsIfNeeded() } }
}