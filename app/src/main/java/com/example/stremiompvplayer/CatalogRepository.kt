package com.example.stremiompvplayer

import androidx.lifecycle.LiveData
import com.example.stremiompvplayer.models.CollectedItem
import com.example.stremiompvplayer.models.CollectedItemDao
import com.example.stremiompvplayer.models.UserCatalog
import com.example.stremiompvplayer.models.UserCatalogDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CatalogRepository(
    private val userCatalogDao: UserCatalogDao,
    private val collectedItemDao: CollectedItemDao
) {

    val allCatalogs: LiveData<List<UserCatalog>> = userCatalogDao.getAllCatalogs()

    suspend fun initializeDefaultsIfNeeded() {
        withContext(Dispatchers.IO) {
            if (userCatalogDao.getCount() == 0) {
                // Create default catalogs for TMDB
                val defaults = listOf(
                    UserCatalog(id = 0, userId = "default", catalogId = "popular", catalogType = "movie", catalogName = "Popular Movies", customName = null, displayOrder = 0, pageType = "movies", addonUrl = "tmdb", manifestId = "tmdb", dateAdded = System.currentTimeMillis()),
                    UserCatalog(id = 0, userId = "default", catalogId = "latest", catalogType = "movie", catalogName = "Latest Movies", customName = null, displayOrder = 1, pageType = "movies", addonUrl = "tmdb", manifestId = "tmdb", dateAdded = System.currentTimeMillis()),
                    UserCatalog(id = 0, userId = "default", catalogId = "trending", catalogType = "movie", catalogName = "Trending Movies", customName = null, displayOrder = 2, pageType = "movies", addonUrl = "tmdb", manifestId = "tmdb", dateAdded = System.currentTimeMillis()),
                    UserCatalog(id = 0, userId = "default", catalogId = "popular", catalogType = "series", catalogName = "Popular Series", customName = null, displayOrder = 0, pageType = "series", addonUrl = "tmdb", manifestId = "tmdb", dateAdded = System.currentTimeMillis()),
                    UserCatalog(id = 0, userId = "default", catalogId = "latest", catalogType = "series", catalogName = "Latest Series", customName = null, displayOrder = 1, pageType = "series", addonUrl = "tmdb", manifestId = "tmdb", dateAdded = System.currentTimeMillis()),
                    UserCatalog(id = 0, userId = "default", catalogId = "trending", catalogType = "series", catalogName = "Trending Series", customName = null, displayOrder = 2, pageType = "series", addonUrl = "tmdb", manifestId = "tmdb", dateAdded = System.currentTimeMillis())
                )

                defaults.forEach { catalog ->
                    userCatalogDao.insert(catalog)
                }
            }
        }
    }

    suspend fun getCatalogsForUser(pageType: String, userId: String = "default"): List<UserCatalog> {
        return withContext(Dispatchers.IO) {
            userCatalogDao.getCatalogsForPageSync(userId, pageType)
        }
    }

    suspend fun updateCatalog(catalog: UserCatalog) {
        withContext(Dispatchers.IO) {
            userCatalogDao.update(catalog)
        }
    }

    suspend fun insertCatalog(catalog: UserCatalog) {
        withContext(Dispatchers.IO) {
            userCatalogDao.insert(catalog)
        }
    }

    suspend fun isCatalogAdded(userId: String, catalogId: String, type: String, page: String): Boolean {
        return withContext(Dispatchers.IO) {
            userCatalogDao.isCatalogAdded(userId, catalogId, type, page) > 0
        }
    }

    suspend fun swapOrder(item1: UserCatalog, item2: UserCatalog) {
        withContext(Dispatchers.IO) {
            val tempOrder = item1.displayOrder
            userCatalogDao.updateDisplayOrder(item1.id, item2.displayOrder)
            userCatalogDao.updateDisplayOrder(item2.id, tempOrder)
        }
    }

    // --- LIBRARY METHODS ---

    fun getLibraryItems(userId: String, type: String): LiveData<List<CollectedItem>> {
        return collectedItemDao.getCollectedItems(userId, type)
    }

    suspend fun addToLibrary(item: CollectedItem) {
        withContext(Dispatchers.IO) {
            collectedItemDao.insert(item)
        }
    }

    suspend fun removeFromLibrary(itemId: String, userId: String) {
        withContext(Dispatchers.IO) {
            // Construct the composite ID used in CollectedItem
            val compositeId = "${userId}_${itemId}"
            collectedItemDao.deleteById(compositeId)
        }
    }

    suspend fun isItemCollected(itemId: String, userId: String): Boolean {
        return withContext(Dispatchers.IO) {
            val compositeId = "${userId}_${itemId}"
            collectedItemDao.isCollected(compositeId) > 0
        }
    }
}