package com.example.stremiompvplayer.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.stremiompvplayer.CatalogRepository

// This factory class tells Android how to create the ViewModel with the repository dependency.
class CatalogViewModelFactory(
    private val repository: CatalogRepository,
    private val context: Context // New parameter
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CatalogViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            // FIX: Pass the repository instance to the ViewModel
            return CatalogViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}