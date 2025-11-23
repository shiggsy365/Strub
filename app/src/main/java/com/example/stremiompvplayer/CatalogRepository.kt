package com.example.stremiompvplayer

import androidx.lifecycle.LiveData
import com.example.stremiompvplayer.data.WatchProgressDao
import com.example.stremiompvplayer.models.CollectedItem
import com.example.stremiompvplayer.models.CollectedItemDao
import com.example.stremiompvplayer.models.UserCatalog
import com.example.stremiompvplayer.models.UserCatalogDao
import com.example.stremiompvplayer.models.WatchProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CatalogRepository(
    private val userCatalogDao: UserCatalogDao,
    private val collectedItemDao: CollectedItemDao,
    private val watchProgressDao: WatchProgressDao
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

    // NEW: Initialize User Lists (Continue Watching, Next Up)
    suspend fun ensureUserListCatalogs(userId: String) {
        withContext(Dispatchers.IO) {
            // Continue Watching Movies (Movies Page)
            if (userCatalogDao.isCatalogAdded(userId, "continue_movies", "movie", "movies") == 0) {
                userCatalogDao.insert(UserCatalog(0, userId, "continue_movies", "movie", "Continue Watching Movies", null, -2, "movies", "local", "local", true, true))
            }
            // Continue Watching Episodes (Series Page)
            if (userCatalogDao.isCatalogAdded(userId, "continue_episodes", "series", "series") == 0) {
                userCatalogDao.insert(UserCatalog(0, userId, "continue_episodes", "series", "Continue Watching", null, -2, "series", "local", "local", true, true))
            }
            // Next Up (Series Page)
            if (userCatalogDao.isCatalogAdded(userId, "next_up", "series", "series") == 0) {
                userCatalogDao.insert(UserCatalog(0, userId, "next_up", "series", "Next Up", null, -1, "series", "local", "local", true, true))
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

    suspend fun deleteCatalog(catalog: UserCatalog) {
        withContext(Dispatchers.IO) {
            userCatalogDao.delete(catalog)
        }
    }

    suspend fun getMaxDisplayOrderForPage(userId: String, pageType: String): Int? {
        return withContext(Dispatchers.IO) {
            userCatalogDao.getMaxDisplayOrder(userId, pageType)
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

    suspend fun getAllLibraryItems(userId: String): List<CollectedItem> {
        return withContext(Dispatchers.IO) {
            collectedItemDao.getAllCollectedItems(userId)
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

    // --- WATCH PROGRESS METHODS ---

    suspend fun saveWatchProgress(progress: WatchProgress) {
        withContext(Dispatchers.IO) {
            watchProgressDao.saveProgress(progress)
        }
    }

    suspend fun getWatchProgress(userId: String, itemId: String): WatchProgress? {
        return withContext(Dispatchers.IO) {
            watchProgressDao.getItemProgress(userId, itemId)
        }
    }

    suspend fun updateWatchedStatus(userId: String, itemId: String, isWatched: Boolean) {
        withContext(Dispatchers.IO) {
            if (!isWatched) {
                // "Clear Watched Status" -> Remove from progress entirely
                watchProgressDao.deleteItemProgress(userId, itemId)
            } else {
                // "Mark as Watched" -> Set progress to duration (approx) and flag true
                val existing = watchProgressDao.getItemProgress(userId, itemId)
                if (existing != null) {
                    watchProgressDao.saveProgress(existing.copy(isWatched = true, progress = existing.duration, lastUpdated = System.currentTimeMillis()))
                } else {
                    // If no progress exists, we can't easily mark as watched without metadata (poster etc).
                    // The ViewModel handles creating the full object if it doesn't exist.
                }
            }
        }
    }

    suspend fun getContinueWatching(userId: String, type: String): List<WatchProgress> {
        return withContext(Dispatchers.IO) {
            if (type == "movie") {
                watchProgressDao.getContinueWatchingMovies(userId)
            } else {
                watchProgressDao.getContinueWatchingEpisodes(userId)
            }
        }
    }

    suspend fun getNextUpCandidates(userId: String): List<WatchProgress> {
        return withContext(Dispatchers.IO) {
            watchProgressDao.getWatchedEpisodes(userId)
        }
    }
}