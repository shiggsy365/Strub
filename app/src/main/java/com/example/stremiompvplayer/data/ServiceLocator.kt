package com.example.stremiompvplayer.data

import android.content.Context
import com.example.stremiompvplayer.CatalogRepository

// Simple ServiceLocator for dependency injection
// This ensures we have a single instance of the Database and Repository
class ServiceLocator(context: Context) {

    val database = AppDatabase.getDatabase(context)
    val catalogRepository = CatalogRepository(
        database.userCatalogDao(),
        database.collectedItemDao(),
        database.watchProgressDao() // Pass the new DAO
    )

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