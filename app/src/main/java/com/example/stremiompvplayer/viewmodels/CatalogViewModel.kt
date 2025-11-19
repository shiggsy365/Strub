package com.example.stremiompvplayer.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stremiompvplayer.CatalogRepository
import com.example.stremiompvplayer.models.Catalog
import com.example.stremiompvplayer.models.MetaItem
import com.example.stremiompvplayer.models.Stream
import kotlinx.coroutines.launch

/**
 * Legacy CatalogViewModel - kept for compatibility
 * New code should use MainViewModel instead
 */
class CatalogViewModel(private val repository: CatalogRepository) : ViewModel() {

    private val _catalogs = MutableLiveData<List<Catalog>>()
    val catalogs: LiveData<List<Catalog>> = _catalogs

    private val _items = MutableLiveData<List<MetaItem>>()
    val items: LiveData<List<MetaItem>> = _items

    private val _streams = MutableLiveData<List<Stream>>()
    val streams: LiveData<List<Stream>> = _streams

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // Expose loaded catalogs for the UI
    private val _loadedCatalogs = MutableLiveData<List<Catalog>>()
    val loadedCatalogs: LiveData<List<Catalog>> = _loadedCatalogs

    init {
        // Initialize with default TMDB catalogs
        loadDefaultCatalogs()
    }

    private fun loadDefaultCatalogs() {
        val defaultCatalogs = listOf(
            Catalog(type = "movie", id = "popular", name = "Popular Movies", extraProps = null),
            Catalog(type = "movie", id = "latest", name = "Latest Movies", extraProps = null),
            Catalog(type = "movie", id = "trending", name = "Trending Movies", extraProps = null),
            Catalog(type = "series", id = "popular", name = "Popular Series", extraProps = null),
            Catalog(type = "series", id = "latest", name = "Latest Series", extraProps = null),
            Catalog(type = "series", id = "trending", name = "Trending Series", extraProps = null)
        )
        _loadedCatalogs.postValue(defaultCatalogs)
        _catalogs.postValue(defaultCatalogs)
    }

    fun getCatalogsByType(type: String): List<Catalog> {
        return _loadedCatalogs.value?.filter { it.type == type } ?: emptyList()
    }

    fun fetchCatalog(type: String, id: String) {
        Log.d("CatalogViewModel", "fetchCatalog called for $type/$id - This is a legacy method")
        // Legacy method - does nothing now as we use MainViewModel for TMDB
        _error.postValue("Please use MainViewModel for loading content")
    }

    fun fetchStreams(type: String, id: String) {
        Log.d("CatalogViewModel", "fetchStreams called - This is a legacy method")
        // Legacy method - does nothing now as we use MainViewModel for streams
        _error.postValue("Please use MainViewModel for loading streams")
    }
}