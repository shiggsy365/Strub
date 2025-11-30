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
import com.example.stremiompvplayer.models.RowSourceType
import com.example.stremiompvplayer.models.TMDBGenre
import com.example.stremiompvplayer.network.TMDBClient
import com.example.stremiompvplayer.network.TraktClient
import com.example.stremiompvplayer.utils.PageRowConfigData
import com.example.stremiompvplayer.utils.Secrets
import com.example.stremiompvplayer.utils.SharedPreferencesManager
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

// Default certification country for movie filtering
private const val DEFAULT_CERTIFICATION_COUNTRY = "GB"

/**
 * ViewModel for the Movies page.
 * Fetches and manages movie rows based on the configuration from Settings.
 */
class MoviesViewModel(
    private val repository: CatalogRepository,
    private val prefsManager: SharedPreferencesManager
) : ViewModel() {

    private val _movieRows = MutableLiveData<List<HomeRow>>()
    val movieRows: LiveData<List<HomeRow>> = _movieRows

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _movieGenres = MutableLiveData<List<TMDBGenre>>()
    val movieGenres: LiveData<List<TMDBGenre>> = _movieGenres

    private val apiKey: String get() = Secrets.TMDB_API_KEY

    /**
     * Load all movie rows based on the configuration from Settings.
     */
    fun loadMovieContent() {
        if (apiKey.isEmpty()) {
            Log.e("MoviesViewModel", "API key is empty!")
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            val rowConfigs = prefsManager.getMovieRowConfigs().sortedBy { it.order }
            val rows = mutableListOf<HomeRow>()

            // Fetch genres first for the Genres row and WAIT for completion
            fetchMovieGenresSync()

            val deferredRows = rowConfigs.map { config ->
                async {
                    fetchRowContent(config)
                }
            }

            rows.addAll(deferredRows.awaitAll().filterNotNull())
            
            _movieRows.postValue(rows)
            _isLoading.postValue(false)
        }
    }

    /**
     * Fetch content for a specific row based on its configuration.
     */
    private suspend fun fetchRowContent(config: PageRowConfigData): HomeRow? {
        return try {
            val sourceType = try {
                RowSourceType.valueOf(config.sourceType)
            } catch (e: IllegalArgumentException) {
                Log.e("MoviesViewModel", "Unknown source type: ${config.sourceType}")
                return null
            }

            val items = when (sourceType) {
                RowSourceType.TMDB_TRENDING_MOVIES -> fetchTrendingMovies()
                RowSourceType.TMDB_LATEST_MOVIES -> fetchLatestMovies()
                RowSourceType.TMDB_POPULAR_MOVIES -> fetchPopularMovies()
                RowSourceType.TRAKT_WATCHLIST, RowSourceType.LOCAL_WATCHLIST -> fetchMovieWatchlist()
                RowSourceType.GENRES -> fetchMovieGenreItems()
                else -> emptyList()
            }

            if (items.isNotEmpty()) {
                HomeRow(config.id, config.label, items)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("MoviesViewModel", "Error fetching row ${config.id}", e)
            null
        }
    }

    private suspend fun fetchTrendingMovies(): List<MetaItem> {
        return try {
            // Get current user's age rating for filtering
            val currentUserId = prefsManager.getCurrentUserId()
            val currentUser = currentUserId?.let { prefsManager.getUser(it) }
            val ageRating = currentUser?.ageRating ?: "18"
            
            val response = when (ageRating) {
                "U", "PG", "12", "15" -> {
                    TMDBClient.api.discoverMovies(
                        apiKey = apiKey,
                        sortBy = "popularity.desc",
                        certificationCountry = DEFAULT_CERTIFICATION_COUNTRY,
                        certificationLte = ageRating,
                        includeAdult = false
                    )
                }
                else -> TMDBClient.api.getTrendingMovies(apiKey)
            }
            response.results.map { it.toMetaItem() }
        } catch (e: Exception) {
            Log.e("MoviesViewModel", "Error fetching trending movies", e)
            emptyList()
        }
    }

    private suspend fun fetchLatestMovies(): List<MetaItem> {
        return try {
            // Get current user's age rating for filtering
            val currentUserId = prefsManager.getCurrentUserId()
            val currentUser = currentUserId?.let { prefsManager.getUser(it) }
            val ageRating = currentUser?.ageRating ?: "18"
            
            val response = when (ageRating) {
                "U", "PG", "12", "15" -> {
                    // Sort by recent releases (most recent first) with certification filtering
                    TMDBClient.api.discoverMovies(
                        apiKey = apiKey,
                        sortBy = "release_date.desc",
                        certificationCountry = DEFAULT_CERTIFICATION_COUNTRY,
                        certificationLte = ageRating,
                        includeAdult = false
                    )
                }
                else -> TMDBClient.api.getLatestMovies(apiKey)
            }
            response.results.map { it.toMetaItem() }
        } catch (e: Exception) {
            Log.e("MoviesViewModel", "Error fetching latest movies", e)
            emptyList()
        }
    }

    private suspend fun fetchPopularMovies(): List<MetaItem> {
        return try {
            // Get current user's age rating for filtering
            val currentUserId = prefsManager.getCurrentUserId()
            val currentUser = currentUserId?.let { prefsManager.getUser(it) }
            val ageRating = currentUser?.ageRating ?: "18"
            
            val response = when (ageRating) {
                "U", "PG", "12", "15" -> {
                    TMDBClient.api.discoverMovies(
                        apiKey = apiKey,
                        sortBy = "popularity.desc",
                        certificationCountry = DEFAULT_CERTIFICATION_COUNTRY,
                        certificationLte = ageRating,
                        includeAdult = false
                    )
                }
                else -> TMDBClient.api.getPopularMovies(apiKey)
            }
            response.results.map { it.toMetaItem() }
        } catch (e: Exception) {
            Log.e("MoviesViewModel", "Error fetching popular movies", e)
            emptyList()
        }
    }

    private suspend fun fetchMovieWatchlist(): List<MetaItem> {
        // Check if Trakt is synced
        if (prefsManager.isTraktEnabled()) {
            return fetchTraktMovieWatchlist()
        } else {
            return fetchLocalMovieWatchlist()
        }
    }

    private suspend fun fetchTraktMovieWatchlist(): List<MetaItem> {
        return try {
            val token = prefsManager.getTraktAccessToken() ?: return emptyList()
            val bearer = "Bearer $token"
            val clientId = Secrets.TRAKT_CLIENT_ID

            val watchlistItems = TraktClient.api.getWatchlist(bearer, clientId, type = "movies")
            watchlistItems.mapNotNull { item ->
                item.movie?.let { movie ->
                    val tmdbId = movie.ids.tmdb ?: return@let null
                    try {
                        val details = TMDBClient.api.getMovieDetails(tmdbId, apiKey)
                        MetaItem(
                            id = "tmdb:$tmdbId",
                            type = "movie",
                            name = details.title ?: movie.title,
                            poster = details.poster_path?.let { "https://image.tmdb.org/t/p/w500$it" },
                            background = details.backdrop_path?.let { "https://image.tmdb.org/t/p/original$it" },
                            description = details.overview,
                            releaseDate = details.release_date,
                            rating = details.vote_average?.toString()
                        )
                    } catch (e: Exception) {
                        MetaItem(
                            id = "tmdb:$tmdbId",
                            type = "movie",
                            name = movie.title,
                            poster = null,
                            background = null,
                            description = null
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MoviesViewModel", "Error fetching Trakt movie watchlist", e)
            emptyList()
        }
    }

    private suspend fun fetchLocalMovieWatchlist(): List<MetaItem> {
        // TODO: Implement local watchlist fetching from the database
        // For now, return empty list as local watchlist feature may not be fully implemented
        return emptyList()
    }

    private fun fetchMovieGenres() {
        viewModelScope.launch {
            try {
                val response = TMDBClient.api.getMovieGenres(apiKey)
                _movieGenres.postValue(response.genres)
            } catch (e: Exception) {
                Log.e("MoviesViewModel", "Error fetching movie genres", e)
            }
        }
    }

    /**
     * Synchronously fetch movie genres - waits for completion before returning.
     */
    private suspend fun fetchMovieGenresSync() {
        try {
            val response = TMDBClient.api.getMovieGenres(apiKey)
            _movieGenres.value = response.genres
            Log.d("MoviesViewModel", "Loaded ${response.genres.size} movie genres")
        } catch (e: Exception) {
            Log.e("MoviesViewModel", "Error fetching movie genres", e)
        }
    }

    private fun fetchMovieGenreItems(): List<MetaItem> {
        // Convert genres to MetaItems for display in the genre row
        val genres = _movieGenres.value ?: return emptyList()
        return genres.map { genre ->
            MetaItem(
                id = "genre_${genre.id}_movie",
                type = "genre",
                name = genre.name,
                poster = null,
                background = null,
                description = "${genre.name} Movies",
                genreId = genre.id,
                genreType = "movie"
            )
        }
    }

    /**
     * Load movies by genre.
     */
    suspend fun fetchMoviesByGenre(genreId: Int): List<MetaItem> {
        return try {
            val response = TMDBClient.api.discoverMovies(
                apiKey = apiKey,
                withGenres = genreId.toString(),
                sortBy = "popularity.desc",
                page = 1
            )
            response.results.map { it.toMetaItem() }
        } catch (e: Exception) {
            Log.e("MoviesViewModel", "Error fetching movies by genre", e)
            emptyList()
        }
    }
}

class MoviesViewModelFactory(
    private val serviceLocator: ServiceLocator,
    private val prefsManager: SharedPreferencesManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MoviesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MoviesViewModel(serviceLocator.catalogRepository, prefsManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
