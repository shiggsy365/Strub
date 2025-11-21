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

    // ... (Keep all existing properties: _currentCatalogContent, currentLogo, apiKey, etc.)
    private val _currentCatalogContent = MutableLiveData<List<MetaItem>>()
    val currentCatalogContent: LiveData<List<MetaItem>> = _currentCatalogContent
    private val _currentLogo = MutableLiveData<String?>()
    val currentLogo: LiveData<String?> = _currentLogo
    private var logoFetchJob: Job? = null
    private val apiKey: String get() = prefsManager.getTMDBApiKey() ?: ""
    val allCatalogConfigs: LiveData<List<UserCatalog>> = catalogRepository.allCatalogs
    val movieCatalogs = allCatalogConfigs.map { it.filter { c -> c.catalogType == "movie" && c.showInUser }.sortedBy { c -> c.displayOrder } }
    val seriesCatalogs = allCatalogConfigs.map { it.filter { c -> c.catalogType == "series" && c.showInUser }.sortedBy { c -> c.displayOrder } }
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
    private val _isItemWatched = MutableLiveData<Boolean>(false)
    val isItemWatched: LiveData<Boolean> = _isItemWatched

    val libraryMovies = catalogRepository.getLibraryItems(prefsManager.getCurrentUserId() ?: "default", "movie").map { it.map { toMetaItem(it) } }
    val librarySeries = catalogRepository.getLibraryItems(prefsManager.getCurrentUserId() ?: "default", "series").map { it.map { toMetaItem(it) } }

    init {
        initDefaultCatalogs()
        initUserLists()
    }

    private fun toMetaItem(item: CollectedItem): MetaItem {
        return MetaItem(item.itemId, item.itemType, item.name, item.poster, item.background, item.description)
    }

    // ... (fetchItemLogo, fetchAIOStreams, loadContentForCatalog, generateNextUpList, checkWatchedStatus, saveWatchProgress)
    // ASSUME THESE ARE PRESENT FROM PREVIOUS STEPS. I am focusing on the *Changed* methods.
    fun fetchItemLogo(meta: MetaItem) { /*...*/ }
    fun loadContentForCatalog(catalog: UserCatalog) { /*...*/ }
    fun checkWatchedStatus(itemId: String) { /*...*/ }
    fun saveWatchProgress(meta: MetaItem, currentPos: Long, duration: Long) { /*...*/ }

    // --- UPDATED WATCHED LOGIC ---

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
                            // Note: Doing this serially to avoid API rate limits, or could optimize with async
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
                                        poster = item.poster, // Fallback to show poster
                                        background = item.background,
                                        parentId = item.id,
                                        season = ep.season_number,
                                        episode = ep.episode_number
                                    )
                                    catalogRepository.saveWatchProgress(progress)
                                }
                            } catch (e: Exception) { Log.e("MainViewModel", "Failed to mark season ${season.season_number}", e) }
                        }
                    }
                    // Mark the show itself as watched? Usually Stremio tracks episodes.
                    // But we can mark the show ID too if needed for UI.
                    // For now, tracking episodes is key for "Next Up".

                } catch (e: Exception) { Log.e("MainViewModel", "Failed to mark series watched", e) }
            } else {
                // SINGLE ITEM (Movie or Episode)
                val parts = item.id.split(":")
                val parentId = if (parts.size >= 3) "${parts[0]}:${parts[1]}" else null
                val season = if (parts.size >= 3) parts[2].toIntOrNull() else null
                val episode = if (parts.size >= 4) parts[3].toIntOrNull() else null

                val progress = WatchProgress(userId, item.id, item.type, 0, 0, true, System.currentTimeMillis(), item.name, item.poster, item.background, parentId, season, episode)
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
                // Since we can't easily query "all episodes starting with tmdb:id:" without a LIKE query in DAO (which we didn't add),
                // we iterate again OR we rely on a new DAO method `deleteByParentId`.
                // Implementing the iterator approach for safety with current DAO.
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

    // ... (Rest of the file)
    fun initUserLists() { viewModelScope.launch { prefsManager.getCurrentUserId()?.let { catalogRepository.ensureUserListCatalogs(it) } } }
    fun initDefaultCatalogs() { viewModelScope.launch { catalogRepository.initializeDefaultsIfNeeded() } }
    fun updateCatalogConfig(c: UserCatalog) { viewModelScope.launch { catalogRepository.updateCatalog(c) } }
    fun swapCatalogOrder(a: UserCatalog, b: UserCatalog) { viewModelScope.launch { catalogRepository.swapOrder(a, b) } }
    fun getDiscoverCatalogs(type: String): LiveData<List<UserCatalog>> { return allCatalogConfigs.map { list -> list.filter { it.catalogType == type && it.showInDiscover }.sortedBy { it.displayOrder } } }
    fun checkWatchlistStatus(id: String, type: String) { /*...*/ }
    fun toggleWatchlist(meta: MetaItem, force: Boolean = false) { /*...*/ }
    fun checkLibraryStatus(id: String) { /*...*/ }
    fun toggleLibrary(meta: MetaItem) { /*...*/ }
    fun removeFromLibrary(id: String) { /*...*/ }
    fun addToLibrary(meta: MetaItem) { /*...*/ }
    fun clearSearchResults() { /*...*/ }
    fun searchTMDB(q: String) { /*...*/ }
    fun searchMovies(q: String) { /*...*/ }
    fun searchSeries(q: String) { /*...*/ }
    fun searchPeople(q: String) { /*...*/ }
    fun loadSeasonEpisodes(id: String, n: Int) { /*...*/ }
    fun loadSeriesMeta(id: String) { /*...*/ }
    fun loadEpisodeStreams(id: String, s: Int, e: Int) { /*...*/ }
    fun loadStreams(t: String, id: String) { /*...*/ }
    fun loadPersonCredits(id: Int) { /*...*/ }
    fun fetchCast(id: String, type: String) { /*...*/ }
    fun fetchRequestToken() { /*...*/ }
    fun createSession(t: String) { /*...*/ }
    fun checkTMDBAuthAndSync() { /*...*/ }
}