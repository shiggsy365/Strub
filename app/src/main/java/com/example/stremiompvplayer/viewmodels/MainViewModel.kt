package com.example.stremiompvplayer.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stremiompvplayer.models.*
import com.example.stremiompvplayer.network.StremioClient
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    private val _catalogs = MutableLiveData<List<MetaItem>>()
    val catalogs: LiveData<List<MetaItem>> = _catalogs

    private val _streams = MutableLiveData<List<Stream>>()
    val streams: LiveData<List<Stream>> = _streams

    // LiveData for Series Metadata (Details including videos/episodes)
    private val _metaDetails = MutableLiveData<Meta?>()
    val metaDetails: LiveData<Meta?> = _metaDetails

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun loadCatalogs(type: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Defaulting to "top" or generic catalog id if specific one not provided
                // Adjust "top" to "cinemeta" or whatever your default catalog ID is
                val response = StremioClient.api.getCatalog(type, "top") 
                _catalogs.value = response.metas
            } catch (e: Exception) {
                _error.value = "Failed to load catalogs: ${e.message}"
                Log.e("MainViewModel", "Error loading catalogs", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadStreams(type: String, id: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = StremioClient.api.getStreams(type, id)
                _streams.value = response.streams
            } catch (e: Exception) {
                _error.value = "Failed to load streams: ${e.message}"
                Log.e("MainViewModel", "Error loading streams", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadMeta(type: String, id: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = StremioClient.api.getMeta(type, id)
                _metaDetails.value = response.meta
            } catch (e: Exception) {
                _error.value = "Failed to load metadata: ${e.message}"
                Log.e("MainViewModel", "Error loading meta", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Helper to clear streams when navigating between levels
    fun clearStreams() {
        _streams.value = emptyList()
    }
}
