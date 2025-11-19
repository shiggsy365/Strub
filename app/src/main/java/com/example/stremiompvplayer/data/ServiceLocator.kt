package com.example.stremiompvplayer.data

import android.content.Context
import com.example.stremiompvplayer.CatalogRepository
import com.example.stremiompvplayer.CatalogRepository_Factory

// Simple ServiceLocator for dependency injection
class ServiceLocator(context: Context) {
    
    val database = AppDatabase.getDatabase(context)
    
    // Repositories
    val catalogRepository = CatalogRepository(database.userCatalogDao())

    companion object {
        @Volatile
        private var INSTANCE: ServiceLocator? = null

        fun getInstance(context: Context): ServiceLocator {
            return INSTANCE ?: synchronized(this) {
                val instance = ServiceLocator(context)
                INSTANCE = instance
                instance
            }
        }
    }
}
