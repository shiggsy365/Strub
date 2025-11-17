package com.example.stremiompvplayer.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stremiompvplayer.models.AddonManifest
import com.example.stremiompvplayer.models.MetaPreview
import com.example.stremiompvplayer.models.Stream
import com.example.stremiompvplayer.network.StremioClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    
    private val stremioClient = StremioClient()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private val _catalogItems = MutableStateFlow<List<MetaPreview>>(emptyList())
    val catalogItems: StateFlow<List<MetaPreview>> = _catalogItems
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    
    private var currentAddonUrl: String? = null
    private var currentManifest: AddonManifest? = null
    
    suspend fun loadAddon(addonUrl: String): Boolean {
        _isLoading.value = true
        _error.value = null
        
        return try {
            val manifest = stremioClient.getManifest(addonUrl)
            if (manifest != null) {
                currentAddonUrl = addonUrl
                currentManifest = manifest
                true
            } else {
                _error.value = "Failed to load addon manifest"
                false
            }
        } catch (e: Exception) {
            _error.value = "Error: ${e.message}"
            false
        } finally {
            _isLoading.value = false
        }
    }
    
    fun loadCatalog(type: String, catalogId: String, extra: Map<String, String> = emptyMap()) {
        val addonUrl = currentAddonUrl ?: return
        
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                val catalogResponse = stremioClient.getCatalog(addonUrl, type, catalogId, extra)
                if (catalogResponse != null) {
                    _catalogItems.value = catalogResponse.metas
                } else {
                    _error.value = "Failed to load catalog"
                    _catalogItems.value = emptyList()
                }
            } catch (e: Exception) {
                _error.value = "Error loading catalog: ${e.message}"
                _catalogItems.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    suspend fun getStreams(type: String, id: String): List<Stream> {
        val addonUrl = currentAddonUrl ?: return emptyList()
        
        return try {
            stremioClient.getStreams(addonUrl, type, id)
        } catch (e: Exception) {
            _error.value = "Error loading streams: ${e.message}"
            emptyList()
        }
    }
}
