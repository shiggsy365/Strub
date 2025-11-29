package com.example.stremiompvplayer.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.stremiompvplayer.CatalogRepository
import com.example.stremiompvplayer.data.ServiceLocator
import com.example.stremiompvplayer.models.MetaItem
import com.example.stremiompvplayer.models.UserCatalog
import com.example.stremiompvplayer.models.WatchProgress
import com.example.stremiompvplayer.network.TMDBClient
import com.example.stremiompvplayer.utils.Secrets
import com.example.stremiompvplayer.utils.SessionCache
import com.example.stremiompvplayer.utils.SharedPreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

data class HomeRow(
    val id: String,
    val title: String,
    val items: List<MetaItem>
)

class HomeViewModel(
    private val repository: CatalogRepository,
    private val prefsManager: SharedPreferencesManager
) : ViewModel() {

    private val _homeRows = MutableLiveData<List<HomeRow>>()
    val homeRows: LiveData<List<HomeRow>> = _homeRows

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val apiKey: String get() = Secrets.TMDB_API_KEY
    private val sessionCache = SessionCache.getInstance()

    fun loadHomeContent() {
        _isLoading.value = true
        viewModelScope.launch {
            val rows = mutableListOf<HomeRow>()
            val userId = prefsManager.getCurrentUserId() ?: "default"

            // 1. Load "Next Up" (High Priority)
            val nextUpItems = generateNextUpList()
            if (nextUpItems.isNotEmpty()) {
                rows.add(HomeRow("next_up", "Next Up", nextUpItems))
            }

            // 2. Load "Continue Watching"
            val continueMovies = repository.getContinueWatching(userId, "movie").map {
                MetaItem(it.itemId, "movie", it.name ?: "Unknown", it.poster, it.background, null, isWatched = false, progress = it.progress, duration = it.duration)
            }
            val continueEpisodes = repository.getContinueWatching(userId, "episode").map {
                MetaItem(it.itemId, "episode", it.name ?: "Unknown", it.poster, it.background, null, isWatched = false, progress = it.progress, duration = it.duration)
            }

            val continueWatching = (continueMovies + continueEpisodes).sortedByDescending { it.progress } // Sort by recent
            if (continueWatching.isNotEmpty()) {
                rows.add(HomeRow("continue", "Continue Watching", continueWatching))
            }

            // 3. Load TMDB Catalogs (Parallel)
            // We manually define the rows we want for the "Browse" experience
            val deferredRows = listOf(
                async { fetchTMDBRow("trending_movie", "Trending Movies", "movie", "trending") },
                async { fetchTMDBRow("popular_series", "Popular Series", "series", "popular") },
                async { fetchTMDBRow("popular_movies", "Popular Movies", "movie", "popular") },
                async { fetchTMDBRow("top_rated_series", "Top Rated Series", "series", "top_rated") }
            )

            rows.addAll(deferredRows.awaitAll().filterNotNull())

            _homeRows.postValue(rows)
            _isLoading.postValue(false)
        }
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

        val watchedEpisodes = repository.getNextUpCandidates(currentUserId)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = dateFormat.format(java.util.Date())

        // PERFORMANCE: Fetch all progress once to avoid queries in async loop
        val allProgress = repository.getAllWatchProgress(currentUserId)
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
                                            description = nextEpDetails.overview ?: "Next episode",
                                            releaseDate = nextEpDetails.airDate,
                                            rating = nextEpDetails.voteAverage?.let { String.format("%.1f", it) }
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

        // Sort by lastUpdated (oldest first - longest ago watched) and return only the MetaItems
        val result = nextUpItems
            .sortedBy { it.second }
            .map { it.first }
            .distinctBy { it.id }  // Remove any duplicate episodes

        // PERFORMANCE: Cache the result
        sessionCache.putNextUp(currentUserId, result)

        result
    }

    private suspend fun fetchTMDBRow(id: String, title: String, type: String, category: String): HomeRow? {
        if (apiKey.isEmpty()) return null
        return try {
            val items = if (type == "movie") {
                val response = when(category) {
                    "trending" -> TMDBClient.api.getTrendingMovies(apiKey)
                    "popular" -> TMDBClient.api.getPopularMovies(apiKey)
                    "top_rated" -> TMDBClient.api.getPopularMovies(apiKey) // Fallback as top_rated endpoint wasn't in interface
                    else -> TMDBClient.api.getPopularMovies(apiKey)
                }
                response.results.map { it.toMetaItem() }
            } else {
                val response = when(category) {
                    "trending" -> TMDBClient.api.getTrendingSeries(apiKey)
                    "popular" -> TMDBClient.api.getPopularSeries(apiKey)
                    else -> TMDBClient.api.getPopularSeries(apiKey)
                }
                response.results.map { it.toMetaItem() }
            }

            if (items.isNotEmpty()) HomeRow(id, title, items) else null
        } catch (e: Exception) {
            null
        }
    }
}

class HomeViewModelFactory(
    private val serviceLocator: ServiceLocator,
    private val prefsManager: SharedPreferencesManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(serviceLocator.catalogRepository, prefsManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}