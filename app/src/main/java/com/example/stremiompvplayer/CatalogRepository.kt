package com.example.stremiompvplayer

import androidx.lifecycle.LiveData
import com.example.stremiompvplayer.data.UserCatalogDao
import com.example.stremiompvplayer.models.UserCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CatalogRepository(private val userCatalogDao: UserCatalogDao) {

    val allCatalogs: LiveData<List<UserCatalog>> = userCatalogDao.getAllCatalogs()

    suspend fun initializeDefaultsIfNeeded() {
        withContext(Dispatchers.IO) {
            if (userCatalogDao.getCount() == 0) {
                val defaults = listOf(
                    UserCatalog(0, "com.linvo.cinemeta", "top", "movie", "Popular Movies", true, true, 0),
                    UserCatalog(0, "com.linvo.cinemeta", "top", "series", "Popular Series", true, true, 1),
                    UserCatalog(0, "com.linvo.cinemeta", "imdbRating", "movie", "Top Rated Movies", true, true, 2),
                    UserCatalog(0, "com.linvo.cinemeta", "imdbRating", "series", "Top Rated Series", true, true, 3)
                )
                userCatalogDao.insertAll(defaults)
            }
        }
    }

    suspend fun getCatalogsForUser(type: String): List<UserCatalog> {
        return withContext(Dispatchers.IO) {
            userCatalogDao.getEnabledUserCatalogs(type)
        }
    }

    suspend fun updateCatalog(catalog: UserCatalog) {
        withContext(Dispatchers.IO) {
            userCatalogDao.update(catalog)
        }
    }

    suspend fun swapOrder(item1: UserCatalog, item2: UserCatalog) {
        withContext(Dispatchers.IO) {
            userCatalogDao.swapSortOrder(item1.dbId, item1.sortOrder, item2.dbId, item2.sortOrder)
        }
    }
}
