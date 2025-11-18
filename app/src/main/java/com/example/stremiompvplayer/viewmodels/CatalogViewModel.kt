package com.example.stremiompvplayer.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stremiompvplayer.data.CatalogRepository
import com.example.stremiompvplayer.models.Catalog
import com.example.stremiompvplayer.models.MetaItem
import com.example.stremiompvplayer.models.Stream
import kotlinx.coroutines.launch

sealed class CatalogUiState {
    data object Loading : CatalogUiState()
    data class Success(val catalogs: List<Catalog>, val items: List<MetaItem>) : CatalogUiState()
    data class Error(val message: String) : CatalogUiState()
}

class CatalogViewModel(private val repository: CatalogRepository) : ViewModel() {

    private val _uiState = MutableLiveData<CatalogUiState>(CatalogUiState.Loading)
    val uiState: LiveData<CatalogUiState> = _uiState

    private val _streams = MutableLiveData<List<Stream>>()
    val streams: LiveData<List<Stream>> = _streams

    private var allManifestCatalogs: List<Catalog> = emptyList()

    init {
        fetchManifestAndDefaultCatalog()
    }

    // ADD THIS: LiveData to expose available catalogs once manifest is loaded
    private val _loadedCatalogs = MutableLiveData<List<Catalog>>()
    val loadedCatalogs: LiveData<List<Catalog>> = _loadedCatalogs
    private fun fetchManifestAndDefaultCatalog() {
        Log.d("CatalogViewModel", "Fetching manifest...")
        viewModelScope.launch {
            repository.fetchManifest().onSuccess { manifest ->
                Log.d("CatalogViewModel", "Manifest fetched: ${manifest.name}")

                allManifestCatalogs = manifest.catalogs.filter {
                    // Filter out search catalogs and catalogs with no type
                    !(it.name ?: "").contains("search", ignoreCase = true) && it.type.isNotEmpty()
                }

                _loadedCatalogs.postValue(allManifestCatalogs)

                Log.d("CatalogViewModel", "Found ${allManifestCatalogs.size} total catalogs")

                // Load the first available catalog by default
                val firstCatalog = allManifestCatalogs.firstOrNull()
                if (firstCatalog != null) {
                    Log.d("CatalogViewModel", "Loading first catalog: ${firstCatalog.name} (${firstCatalog.type}/${firstCatalog.id})")
                    fetchCatalog(firstCatalog.type, firstCatalog.id)
                } else {
                    _uiState.value = CatalogUiState.Error("No content catalogs found in manifest.")
                }
            }.onFailure { error ->
                Log.e("CatalogViewModel", "Failed to load manifest: ${error.message}")
                _uiState.value = CatalogUiState.Error("Failed to load manifest: ${error.message}")
            }
        }
    }

    fun getCatalogsByType(type: String): List<Catalog> {
        return allManifestCatalogs.filter { it.type == type }
    }

    fun fetchCatalog(type: String, id: String) {
        Log.d("CatalogViewModel", "Fetching catalog: $type/$id")
        _uiState.value = CatalogUiState.Loading

        viewModelScope.launch {
            repository.fetchCatalogItems(type, id).onSuccess { catalogResponse ->
                Log.d("CatalogViewModel", "Catalog fetched successfully: ${catalogResponse.metas.size} items")
                _uiState.value = CatalogUiState.Success(
                    catalogs = getCatalogsByType(type),
                    // FIX: Limit items to 100
                    items = catalogResponse.metas.take(100)
                )
            }.onFailure { error ->
                Log.e("CatalogViewModel", "Failed to load catalog '$id': ${error.message}")
                _uiState.value = CatalogUiState.Error("Failed to load catalog '$id': ${error.message}")
            }
        }
    }

    fun fetchStreams(type: String, id: String) {
        Log.d("CatalogViewModel", "Fetching streams for: $type/$id")
        viewModelScope.launch {
            repository.fetchStreams(type, id).onSuccess { streamResponse ->
                Log.d("CatalogViewModel", "Streams fetched successfully: ${streamResponse.streams.size} streams")
                _streams.postValue(streamResponse.streams)
            }.onFailure { error ->
                Log.e("CatalogViewModel", "Failed to fetch streams: ${error.message}")
                // Post empty list on error
                _streams.postValue(emptyList())
            }
        }
    }
}