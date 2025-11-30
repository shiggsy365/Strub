package com.example.stremiompvplayer.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.stremiompvplayer.CatalogRepository
import com.example.stremiompvplayer.data.ServiceLocator
import com.example.stremiompvplayer.models.CollectedItem
import com.example.stremiompvplayer.models.MetaItem
import com.example.stremiompvplayer.models.TMDBGenre
import com.example.stremiompvplayer.network.TMDBClient
import com.example.stremiompvplayer.utils.Secrets
import com.example.stremiompvplayer.utils.SharedPreferencesManager
import kotlinx.coroutines.launch

/**
 * Data class representing library filter configuration.
 */
data class LibraryFilterConfig(
    val sortBy: SortType = SortType.ADDED_DATE,
    val sortAscending: Boolean = false,  // false = newest first / Z-A
    val movieGenreFilter: String? = null,  // null = all movie genres
    val tvGenreFilter: String? = null      // null = all TV genres
)

/**
 * Enum for sort types.
 */
enum class SortType {
    ADDED_DATE,    // When added to library
    RATING,        // By rating (highest first)
    RELEASE_DATE,  // Original release date
    TITLE          // Alphabetical
}

/**
 * ViewModel for the Library page.
 * Handles loading, filtering, and sorting of library content.
 */
class LibraryViewModel(
    private val repository: CatalogRepository,
    private val prefsManager: SharedPreferencesManager
) : ViewModel() {

    private val _libraryRows = MutableLiveData<List<HomeRow>>()
    val libraryRows: LiveData<List<HomeRow>> = _libraryRows

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _filterConfig = MutableLiveData(loadFilterConfigFromPrefs())
    val filterConfig: LiveData<LibraryFilterConfig> = _filterConfig

    private val _movieGenres = MutableLiveData<List<TMDBGenre>>()
    val movieGenres: LiveData<List<TMDBGenre>> = _movieGenres

    private val _tvGenres = MutableLiveData<List<TMDBGenre>>()
    val tvGenres: LiveData<List<TMDBGenre>> = _tvGenres

    // Combined genres for backwards compatibility
    private val _availableGenres = MutableLiveData<List<TMDBGenre>>()
    val availableGenres: LiveData<List<TMDBGenre>> = _availableGenres

    private val apiKey: String get() = Secrets.TMDB_API_KEY

    // Cached library items for filtering
    private var cachedMovies: List<MetaItem> = emptyList()
    private var cachedSeries: List<MetaItem> = emptyList()

    /**
     * Load all library content as two rows: Movies and Series.
     */
    fun loadLibraryContent() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val userId = prefsManager.getCurrentUserId() ?: "default"
                
                // Load library items from database
                val allItems = repository.getAllLibraryItems(userId)
                
                // Convert to MetaItems and cache
                cachedMovies = allItems.filter { it.itemType == "movie" }.map { toMetaItem(it) }
                cachedSeries = allItems.filter { it.itemType == "series" }.map { toMetaItem(it) }
                
                // Load available genres from TMDB
                loadAvailableGenres()
                
                // Apply current filter and update rows
                applyFilterAndUpdateRows()
                
            } catch (e: Exception) {
                Log.e("LibraryViewModel", "Error loading library content", e)
                _libraryRows.postValue(emptyList())
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    /**
     * Update filter configuration and reapply to library content.
     */
    fun updateFilterConfig(newConfig: LibraryFilterConfig) {
        _filterConfig.value = newConfig
        saveFilterConfigToPrefs(newConfig)
        applyFilterAndUpdateRows()
    }

    /**
     * Toggle sort direction for the current sort type.
     */
    fun toggleSortDirection() {
        val current = _filterConfig.value ?: LibraryFilterConfig()
        updateFilterConfig(current.copy(sortAscending = !current.sortAscending))
    }

    /**
     * Set sort type and reset direction to default.
     */
    fun setSortType(sortType: SortType) {
        val current = _filterConfig.value ?: LibraryFilterConfig()
        val defaultAscending = when (sortType) {
            SortType.TITLE -> true  // A-Z by default
            else -> false  // Newest first by default
        }
        updateFilterConfig(current.copy(sortBy = sortType, sortAscending = defaultAscending))
    }

    /**
     * Set genre filter.
     */
    fun setGenreFilter(genreId: String?) {
        val current = _filterConfig.value ?: LibraryFilterConfig()
        updateFilterConfig(current.copy(genreFilter = genreId))
    }

    /**
     * Clear all sorts and filters, returning to default state.
     */
    fun clearFilters() {
        updateFilterConfig(LibraryFilterConfig())
    }

    /**
     * Apply current filter configuration and update the rows.
     */
    private fun applyFilterAndUpdateRows() {
        val config = _filterConfig.value ?: LibraryFilterConfig()
        
        // Apply genre filters (separate for movies and TV)
        val filteredMovies = applyGenreFilter(cachedMovies, config.movieGenreFilter)
        val filteredSeries = applyGenreFilter(cachedSeries, config.tvGenreFilter)
        
        // Apply sorting
        val sortedMovies = applySorting(filteredMovies, config)
        val sortedSeries = applySorting(filteredSeries, config)
        
        // Create rows
        val rows = mutableListOf<HomeRow>()
        
        if (sortedMovies.isNotEmpty()) {
            rows.add(HomeRow("library_movies", "Movies", sortedMovies))
        }
        
        if (sortedSeries.isNotEmpty()) {
            rows.add(HomeRow("library_series", "Series", sortedSeries))
        }
        
        _libraryRows.postValue(rows)
    }

    /**
     * Apply genre filter to items.
     */
    private fun applyGenreFilter(items: List<MetaItem>, genreId: String?): List<MetaItem> {
        if (genreId == null) return items
        
        return items.filter { item ->
            // Check if the item's genres contain the selected genre ID
            // Genres are stored as JSON array string like ["28", "12"]
            item.genres?.contains("\"$genreId\"") == true
        }
    }

    /**
     * Apply sorting to items based on configuration.
     */
    private fun applySorting(items: List<MetaItem>, config: LibraryFilterConfig): List<MetaItem> {
        return when (config.sortBy) {
            SortType.ADDED_DATE -> {
                // Items are stored in order of addition, reverse if needed
                if (config.sortAscending) items else items.reversed()
            }
            SortType.RATING -> {
                // Sort by rating (convert string to float for numeric comparison)
                val sorted = items.sortedBy { 
                    it.rating?.toFloatOrNull() ?: 0f
                }
                if (config.sortAscending) sorted else sorted.reversed()
            }
            SortType.RELEASE_DATE -> {
                val sorted = items.sortedBy { it.releaseDate ?: "" }
                if (config.sortAscending) sorted else sorted.reversed()
            }
            SortType.TITLE -> {
                val sorted = items.sortedBy { it.name.lowercase() }
                if (config.sortAscending) sorted else sorted.reversed()
            }
        }
    }

    /**
     * Load available genres from TMDB for filtering.
     */
    private suspend fun loadAvailableGenres() {
        if (apiKey.isEmpty()) return
        
        try {
            // Load both movie and TV genres separately
            val movieGenresList = TMDBClient.api.getMovieGenres(apiKey).genres.sortedBy { it.name }
            val tvGenresList = TMDBClient.api.getTVGenres(apiKey).genres.sortedBy { it.name }
            
            _movieGenres.postValue(movieGenresList)
            _tvGenres.postValue(tvGenresList)
            
            // Combine and deduplicate for backwards compatibility
            val allGenres = (movieGenresList + tvGenresList).distinctBy { it.id }.sortedBy { it.name }
            _availableGenres.postValue(allGenres)
        } catch (e: Exception) {
            Log.e("LibraryViewModel", "Error loading genres", e)
        }
    }

    /**
     * Convert CollectedItem to MetaItem.
     */
    private fun toMetaItem(item: CollectedItem): MetaItem {
        return MetaItem(
            id = item.itemId,
            type = item.itemType,
            name = item.name,
            poster = item.poster,
            background = item.background,
            description = item.description,
            releaseDate = item.year,
            rating = item.rating,
            genres = item.genres
        )
    }

    /**
     * Load filter configuration from SharedPreferences.
     */
    private fun loadFilterConfigFromPrefs(): LibraryFilterConfig {
        val sortByName = prefsManager.getLibrarySortBy()
        val sortAscending = prefsManager.getLibrarySortAscending()
        val movieGenreFilter = prefsManager.getLibraryMovieGenreFilter()
        val tvGenreFilter = prefsManager.getLibraryTVGenreFilter()
        
        val sortBy = try {
            SortType.valueOf(sortByName)
        } catch (e: IllegalArgumentException) {
            SortType.ADDED_DATE
        }
        
        return LibraryFilterConfig(
            sortBy = sortBy,
            sortAscending = sortAscending,
            movieGenreFilter = movieGenreFilter,
            tvGenreFilter = tvGenreFilter
        )
    }

    /**
     * Save filter configuration to SharedPreferences.
     */
    private fun saveFilterConfigToPrefs(config: LibraryFilterConfig) {
        prefsManager.setLibrarySortBy(config.sortBy.name)
        prefsManager.setLibrarySortAscending(config.sortAscending)
        prefsManager.setLibraryMovieGenreFilter(config.movieGenreFilter)
        prefsManager.setLibraryTVGenreFilter(config.tvGenreFilter)
    }

    /**
     * Get the display name for a genre ID.
     */
    fun getGenreName(genreId: String): String {
        val genres = _availableGenres.value ?: return genreId
        return genres.find { it.id.toString() == genreId }?.name ?: genreId
    }
}

class LibraryViewModelFactory(
    private val serviceLocator: ServiceLocator,
    private val prefsManager: SharedPreferencesManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LibraryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LibraryViewModel(serviceLocator.catalogRepository, prefsManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
