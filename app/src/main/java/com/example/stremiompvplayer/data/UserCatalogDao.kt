package com.example.stremiompvplayer.data

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.stremiompvplayer.models.UserCatalog

@Dao
interface UserCatalogDao {
    @Query("SELECT * FROM user_catalogs ORDER BY sortOrder ASC")
    fun getAllCatalogs(): LiveData<List<UserCatalog>>

    @Query("SELECT * FROM user_catalogs WHERE type = :type AND isUserEnabled = 1 ORDER BY sortOrder ASC")
    suspend fun getEnabledUserCatalogs(type: String): List<UserCatalog>

    @Query("SELECT * FROM user_catalogs WHERE type = :type AND isDiscoverEnabled = 1 ORDER BY sortOrder ASC")
    suspend fun getEnabledDiscoverCatalogs(type: String): List<UserCatalog>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(catalogs: List<UserCatalog>)

    @Update
    suspend fun update(catalog: UserCatalog)

    @Query("SELECT MAX(sortOrder) FROM user_catalogs")
    suspend fun getMaxSortOrder(): Int?

    @Transaction
    suspend fun swapSortOrder(id1: Long, order1: Int, id2: Long, order2: Int) {
        updateSortOrder(id1, order2)
        updateSortOrder(id2, order1)
    }

    @Query("UPDATE user_catalogs SET sortOrder = :newOrder WHERE dbId = :id")
    suspend fun updateSortOrder(id: Long, newOrder: Int)
    
    @Query("SELECT COUNT(*) FROM user_catalogs")
    suspend fun getCount(): Int
}
