package com.example.stremiompvplayer.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.stremiompvplayer.data.ServiceLocator
import com.example.stremiompvplayer.utils.SharedPreferencesManager

class MainViewModelFactory(
    private val serviceLocator: ServiceLocator,
    private val prefsManager: SharedPreferencesManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(serviceLocator.catalogRepository, prefsManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
