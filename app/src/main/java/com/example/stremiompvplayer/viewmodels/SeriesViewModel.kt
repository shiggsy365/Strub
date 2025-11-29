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

/**
 * ViewModel for the Series page.
 * Fetches and manages series rows based on the configuration from Settings.
 */
class SeriesViewModel(
    private val repository: CatalogRepository,
    private val prefsManager: SharedPreferencesManager
) : ViewModel() {

    private val _seriesRows = MutableLiveData<List<HomeRow>>()
    val seriesRows: LiveData<List<HomeRow>> = _seriesRows

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _tvGenres = MutableLiveData<List<TMDBGenre>>()
    val tvGenres: LiveData<List<TMDBGenre>> = _tvGenres

    private val apiKey: String get() = Secrets.TMDB_API_KEY

    /**
     * Load all series rows based on the configuration from Settings.
     */
    fun loadSeriesContent() {
        if (apiKey.isEmpty()) {
            Log.e("SeriesViewModel", "API key is empty!")
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            val rowConfigs = prefsManager.getSeriesRowConfigs().sortedBy { it.order }
            val rows = mutableListOf<HomeRow>()

            // Fetch genres first for the Genres row
            fetchTVGenres()

            val deferredRows = rowConfigs.map { config ->
                async {
                    fetchRowContent(config)
                }
            }

            rows.addAll(deferredRows.awaitAll().filterNotNull())
            
            _seriesRows.postValue(rows)
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
                Log.e("SeriesViewModel", "Unknown source type: ${config.sourceType}")
                return null
            }

            val items = when (sourceType) {
                RowSourceType.TMDB_TRENDING_TV -> fetchTrendingSeries()
                RowSourceType.TMDB_LATEST_TV -> fetchLatestSeries()
                RowSourceType.TMDB_POPULAR_TV -> fetchPopularSeries()
                RowSourceType.TRAKT_WATCHLIST, RowSourceType.LOCAL_WATCHLIST -> fetchSeriesWatchlist()
                RowSourceType.GENRES -> fetchSeriesGenreItems()
                else -> emptyList()
            }

            if (items.isNotEmpty()) {
                HomeRow(config.id, config.label, items)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("SeriesViewModel", "Error fetching row ${config.id}", e)
            null
        }
    }

    private suspend fun fetchTrendingSeries(): List<MetaItem> {
        return try {
            val response = TMDBClient.api.getTrendingSeries(apiKey)
            response.results.map { it.toMetaItem() }
        } catch (e: Exception) {
            Log.e("SeriesViewModel", "Error fetching trending series", e)
            emptyList()
        }
    }

    private suspend fun fetchLatestSeries(): List<MetaItem> {
        return try {
            val response = TMDBClient.api.getLatestSeries(apiKey)
            response.results.map { it.toMetaItem() }
        } catch (e: Exception) {
            Log.e("SeriesViewModel", "Error fetching latest series", e)
            emptyList()
        }
    }

    private suspend fun fetchPopularSeries(): List<MetaItem> {
        return try {
            val response = TMDBClient.api.getPopularSeries(apiKey)
            response.results.map { it.toMetaItem() }
        } catch (e: Exception) {
            Log.e("SeriesViewModel", "Error fetching popular series", e)
            emptyList()
        }
    }

    private suspend fun fetchSeriesWatchlist(): List<MetaItem> {
        // Check if Trakt is synced
        if (prefsManager.isTraktEnabled()) {
            return fetchTraktSeriesWatchlist()
        } else {
            return fetchLocalSeriesWatchlist()
        }
    }

    private suspend fun fetchTraktSeriesWatchlist(): List<MetaItem> {
        return try {
            val token = prefsManager.getTraktAccessToken() ?: return emptyList()
            val bearer = "Bearer $token"
            val clientId = Secrets.TRAKT_CLIENT_ID

            val watchlistItems = TraktClient.api.getWatchlist(bearer, clientId, type = "shows")
            watchlistItems.mapNotNull { item ->
                item.show?.let { show ->
                    val tmdbId = show.ids.tmdb ?: return@let null
                    try {
                        val details = TMDBClient.api.getTVDetails(tmdbId, apiKey)
                        MetaItem(
                            id = "tmdb:$tmdbId",
                            type = "series",
                            name = details.name ?: show.title,
                            poster = details.poster_path?.let { "https://image.tmdb.org/t/p/w500$it" },
                            background = details.backdrop_path?.let { "https://image.tmdb.org/t/p/original$it" },
                            description = details.overview,
                            releaseDate = details.first_air_date,
                            rating = details.vote_average?.toString()
                        )
                    } catch (e: Exception) {
                        MetaItem(
                            id = "tmdb:$tmdbId",
                            type = "series",
                            name = show.title,
                            poster = null,
                            background = null,
                            description = null
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SeriesViewModel", "Error fetching Trakt series watchlist", e)
            emptyList()
        }
    }

    private suspend fun fetchLocalSeriesWatchlist(): List<MetaItem> {
        // TODO: Implement local watchlist fetching from the database
        // For now, return empty list as local watchlist feature may not be fully implemented
        return emptyList()
    }

    private fun fetchTVGenres() {
        viewModelScope.launch {
            try {
                val response = TMDBClient.api.getTVGenres(apiKey)
                _tvGenres.postValue(response.genres)
            } catch (e: Exception) {
                Log.e("SeriesViewModel", "Error fetching TV genres", e)
            }
        }
    }

    private fun fetchSeriesGenreItems(): List<MetaItem> {
        // Convert genres to MetaItems for display in the genre row
        val genres = _tvGenres.value ?: return emptyList()
        return genres.map { genre ->
            MetaItem(
                id = "genre_${genre.id}_series",
                type = "genre",
                name = genre.name,
                poster = null,
                background = null,
                description = "${genre.name} Series",
                genreId = genre.id,
                genreType = "series"
            )
        }
    }

    /**
     * Load series by genre.
     */
    suspend fun fetchSeriesByGenre(genreId: Int): List<MetaItem> {
        return try {
            val response = TMDBClient.api.discoverTV(
                apiKey = apiKey,
                withGenres = genreId.toString(),
                sortBy = "popularity.desc",
                page = 1
            )
            response.results.map { it.toMetaItem() }
        } catch (e: Exception) {
            Log.e("SeriesViewModel", "Error fetching series by genre", e)
            emptyList()
        }
    }
}

class SeriesViewModelFactory(
    private val serviceLocator: ServiceLocator,
    private val prefsManager: SharedPreferencesManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SeriesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SeriesViewModel(serviceLocator.catalogRepository, prefsManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
