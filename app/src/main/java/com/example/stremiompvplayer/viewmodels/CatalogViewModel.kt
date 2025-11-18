package com.example.stremiompvplayer.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stremiompvplayer.data.CatalogRepository
import com.example.stremiompvplayer.models.Catalog
import com.example.stremiompvplayer.models.MetaItem
import kotlinx.coroutines.launch

sealed class CatalogUiState {
    data object Loading : CatalogUiState()
    data class Success(val catalogs: List<Catalog>, val items: List<MetaItem>) : CatalogUiState()
    data class Error(val message: String) : CatalogUiState()
}

class CatalogViewModel(private val repository: CatalogRepository) : ViewModel() {

    private val _uiState = MutableLiveData<CatalogUiState>(CatalogUiState.Loading)
    val uiState: LiveData<CatalogUiState> = _uiState

    private var allManifestCatalogs: List<Catalog> = emptyList()

    init {
        fetchManifestAndDefaultCatalog()
    }

    private fun fetchManifestAndDefaultCatalog() {
        viewModelScope.launch {
            repository.fetchManifest().onSuccess { manifest ->
                allManifestCatalogs = manifest.catalogs.filter {
                    // Filter out search catalogs and catalogs with no type
                    !(it.name ?: "").contains("search", ignoreCase = true) && it.type.isNotEmpty()
                }

                // Load the first available catalog by default
                val firstCatalog = allManifestCatalogs.firstOrNull()
                if (firstCatalog != null) {
                    fetchCatalog(firstCatalog.type, firstCatalog.id)
                } else {
                    _uiState.value = CatalogUiState.Error("No content catalogs found in manifest.")
                }
            }.onFailure {
                _uiState.value = CatalogUiState.Error("Failed to load manifest: ${it.message}")
            }
        }
    }

    fun getCatalogsByType(type: String): List<Catalog> {
        return allManifestCatalogs.filter { it.type == type }
    }

    fun fetchCatalog(type: String, id: String) {
        _uiState.value = CatalogUiState.Loading // Set loading state before fetch

        viewModelScope.launch {
            repository.fetchCatalogItems(type, id).onSuccess { catalogResponse ->
                _uiState.value = CatalogUiState.Success(
                    catalogs = getCatalogsByType(type),
                    items = catalogResponse.metas
                )
            }.onFailure {
                _uiState.value = CatalogUiState.Error("Failed to load catalog '$id': ${it.message}")
            }
        }
    }
}