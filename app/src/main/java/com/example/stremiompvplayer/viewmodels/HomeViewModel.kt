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

// TMDB Kids genre ID for family-friendly content filtering
private const val TMDB_KIDS_GENRE_ID = "10762"

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

            // 2. Load "Continue Watching" with full metadata from TMDB
            // Get all continue watching items and sort by lastUpdated (most recent first)
            val movieProgress = repository.getContinueWatching(userId, "movie")
            val episodeProgress = repository.getContinueWatching(userId, "episode")
            
            // Combine and sort by lastUpdated (most recently watched first)
            val allContinueWatching = (movieProgress + episodeProgress)
                .sortedByDescending { it.lastUpdated }
            
            // Enrich with metadata while preserving the sort order
            val continueWatching = allContinueWatching.mapNotNull { progress ->
                enrichContinueWatchingItem(progress)
            }
            
            if (continueWatching.isNotEmpty()) {
                rows.add(HomeRow("continue", "Continue Watching", continueWatching))
            }

            // 3. Load TMDB Catalogs (Parallel)
            // We manually define the rows we want for the "Browse" experience
            val deferredRows = listOf(
                async { fetchTMDBRow("trending_movie", "Trending Movies", "movie", "trending") },
                async { fetchTMDBRow("trending_series", "Trending Series", "series", "popular") }
            )

            rows.addAll(deferredRows.awaitAll().filterNotNull())

            _homeRows.postValue(rows)
            _isLoading.postValue(false)
        }
    }

    /**
     * Enriches a Continue Watching item with full metadata from TMDB.
     * This ensures description, rating, and date are available for the hero banner.
     */
    private suspend fun enrichContinueWatchingItem(progress: WatchProgress): MetaItem? {
        if (apiKey.isEmpty()) {
            // Fallback to basic metadata if no API key
            return MetaItem(
                progress.itemId,
                progress.type,
                progress.name ?: "Unknown",
                progress.poster,
                progress.background,
                null,
                isWatched = false,
                progress = progress.progress,
                duration = progress.duration
            )
        }

        return try {
            when (progress.type) {
                "movie" -> {
                    val tmdbId = progress.itemId.removePrefix("tmdb:").toIntOrNull()
                    if (tmdbId != null) {
                        val details = TMDBClient.api.getMovieDetails(tmdbId, apiKey)
                        val posterUrl = details.poster_path?.let { "https://image.tmdb.org/t/p/w500$it" }
                        val backgroundUrl = details.backdrop_path?.let { "https://image.tmdb.org/t/p/original$it" }
                        MetaItem(
                            progress.itemId,
                            progress.type,
                            details.title ?: progress.name ?: "Unknown",
                            posterUrl ?: progress.poster,
                            backgroundUrl ?: progress.background,
                            details.overview,
                            releaseDate = details.release_date,
                            rating = details.vote_average?.let { String.format("%.1f", it) },
                            isWatched = false,
                            progress = progress.progress,
                            duration = progress.duration
                        )
                    } else {
                        createBasicMetaItem(progress)
                    }
                }
                "episode" -> {
                    // Parse episode ID format: tmdb:12345:1:5 (series:season:episode)
                    val parts = progress.itemId.split(":")
                    if (parts.size >= 4 && parts[0] == "tmdb") {
                        val seriesTmdbId = parts[1].toIntOrNull()
                        val seasonNum = parts[2].toIntOrNull()
                        val episodeNum = parts[3].toIntOrNull()
                        
                        if (seriesTmdbId != null && seasonNum != null && episodeNum != null) {
                            try {
                                val showDetails = TMDBClient.api.getTVDetails(seriesTmdbId, apiKey)
                                val seasonDetails = TMDBClient.api.getTVSeasonDetails(seriesTmdbId, seasonNum, apiKey)
                                val episodeDetails = seasonDetails.episodes.find { it.episode_number == episodeNum }
                                
                                // Use series poster for episode
                                val posterUrl = showDetails.poster_path?.let { "https://image.tmdb.org/t/p/w500$it" }
                                val backgroundUrl = showDetails.backdrop_path?.let { "https://image.tmdb.org/t/p/original$it" }
                                    ?: episodeDetails?.still_path?.let { "https://image.tmdb.org/t/p/original$it" }
                                
                                MetaItem(
                                    progress.itemId,
                                    progress.type,
                                    episodeDetails?.name ?: progress.name ?: "Unknown",
                                    posterUrl ?: progress.poster,
                                    backgroundUrl ?: progress.background,
                                    episodeDetails?.overview ?: showDetails.overview,
                                    releaseDate = episodeDetails?.airDate ?: showDetails.first_air_date,
                                    rating = episodeDetails?.voteAverage?.let { String.format("%.1f", it) }
                                        ?: showDetails.vote_average?.let { String.format("%.1f", it) },
                                    isWatched = false,
                                    progress = progress.progress,
                                    duration = progress.duration
                                )
                            } catch (e: Exception) {
                                Log.e("HomeViewModel", "Error fetching episode details", e)
                                createBasicMetaItem(progress)
                            }
                        } else {
                            createBasicMetaItem(progress)
                        }
                    } else {
                        createBasicMetaItem(progress)
                    }
                }
                else -> createBasicMetaItem(progress)
            }
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Error enriching Continue Watching item", e)
            createBasicMetaItem(progress)
        }
    }

    /**
     * Creates a basic MetaItem from WatchProgress without TMDB enrichment.
     */
    private suspend fun createBasicMetaItem(progress: WatchProgress): MetaItem {
        val poster = if (progress.type == "episode") {
            getSeriesPosterForEpisode(progress) ?: progress.poster
        } else {
            progress.poster
        }
        
        return MetaItem(
            progress.itemId,
            progress.type,
            progress.name ?: "Unknown",
            poster,
            progress.background,
            null,
            isWatched = false,
            progress = progress.progress,
            duration = progress.duration
        )
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
                                        // Use series poster for episodes instead of episode thumbnail
                                        val poster = showDetails.poster_path?.let { "https://image.tmdb.org/t/p/w500$it" }
                                            ?: nextEpDetails.still_path?.let { "https://image.tmdb.org/t/p/w500$it" }

                                        val background = showDetails.backdrop_path?.let { "https://image.tmdb.org/t/p/original$it" }
                                            ?: nextEpDetails.still_path?.let { "https://image.tmdb.org/t/p/original$it" }

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

        // Sort by lastUpdated (most recent first - descending) and return only the MetaItems
        val result = nextUpItems
            .sortedByDescending { it.second }
            .map { it.first }
            .distinctBy { it.id }  // Remove any duplicate episodes

        // PERFORMANCE: Cache the result
        sessionCache.putNextUp(currentUserId, result)

        result
    }

    private suspend fun fetchTMDBRow(id: String, title: String, type: String, category: String): HomeRow? {
        if (apiKey.isEmpty()) return null
        
        // Get current user's age rating for filtering
        val currentUserId = prefsManager.getCurrentUserId()
        val currentUser = currentUserId?.let { prefsManager.getUser(it) }
        val ageRating = currentUser?.ageRating ?: "18" // Default to 18 (no filtering) if not set
        
        return try {
            val items = if (type == "movie") {
                val response = when (ageRating) {
                    "U", "PG", "12", "15" -> {
                        // Use discover endpoint with certification filtering
                        TMDBClient.api.discoverMovies(
                            apiKey = apiKey,
                            sortBy = if (category == "trending") "popularity.desc" else "popularity.desc",
                            certificationCountry = "GB",
                            certificationLte = ageRating,
                            includeAdult = false
                        )
                    }
                    else -> {
                        // Age rating 18 - no certification filtering needed
                        when(category) {
                            "trending" -> TMDBClient.api.getTrendingMovies(apiKey)
                            "popular" -> TMDBClient.api.getPopularMovies(apiKey)
                            "top_rated" -> TMDBClient.api.getPopularMovies(apiKey)
                            else -> TMDBClient.api.getPopularMovies(apiKey)
                        }
                    }
                }
                response.results.map { it.toMetaItem() }
            } else {
                // For TV shows, TMDB doesn't support direct certification filtering
                // Use genre-based filtering for kids content as a workaround
                val response = when (ageRating) {
                    "U", "PG" -> {
                        // Filter to kids content using Kids genre
                        TMDBClient.api.discoverTV(
                            apiKey = apiKey,
                            sortBy = "popularity.desc",
                            withGenres = TMDB_KIDS_GENRE_ID,
                            includeAdult = false
                        )
                    }
                    else -> {
                        // For 12, 15, 18 - TMDB doesn't support direct TV certification filtering
                        // Load content but note this is a limitation of the API
                        when(category) {
                            "trending" -> TMDBClient.api.getTrendingSeries(apiKey)
                            "popular" -> TMDBClient.api.getPopularSeries(apiKey)
                            else -> TMDBClient.api.getPopularSeries(apiKey)
                        }
                    }
                }
                response.results.map { it.toMetaItem() }
            }

            if (items.isNotEmpty()) HomeRow(id, title, items) else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get the series poster for an episode from WatchProgress.
     * Episodes should display the series poster, not the episode thumbnail.
     */
    private suspend fun getSeriesPosterForEpisode(progress: WatchProgress): String? {
        // Try to get series ID from parentId or parse from itemId
        val seriesId = progress.parentId ?: run {
            // Parse from itemId format: tmdb:12345:1:5 -> tmdb:12345
            val parts = progress.itemId.split(":")
            if (parts.size >= 2 && parts[0] == "tmdb") {
                "tmdb:${parts[1]}"
            } else null
        }

        if (seriesId != null && apiKey.isNotEmpty()) {
            try {
                val tmdbId = seriesId.removePrefix("tmdb:").toIntOrNull()
                if (tmdbId != null) {
                    val details = TMDBClient.api.getTVDetails(tmdbId, apiKey)
                    return details.poster_path?.let { "https://image.tmdb.org/t/p/w500$it" }
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error fetching series poster for $seriesId", e)
            }
        }
        return null
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