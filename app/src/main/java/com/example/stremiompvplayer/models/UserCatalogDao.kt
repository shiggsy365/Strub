package com.example.stremiompvplayer.models

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import java.io.Serializable


@Dao
interface UserCatalogDao {

    // Get all catalogs for a specific user and page type, ordered by display order
    @Query("SELECT * FROM user_catalogs WHERE userId = :userId AND pageType = :pageType ORDER BY displayOrder ASC")
    fun getCatalogsForPage(userId: String, pageType: String): LiveData<List<UserCatalog>>

    @Query("SELECT * FROM user_catalogs WHERE userId = :userId AND pageType = :pageType ORDER BY displayOrder ASC")
    suspend fun getCatalogsForPageSync(userId: String, pageType: String): List<UserCatalog>

    // Get all catalogs (for settings)
    @Query("SELECT * FROM user_catalogs ORDER BY pageType, displayOrder ASC")
    fun getAllCatalogs(): LiveData<List<UserCatalog>>

    // Get count for initialization check
    @Query("SELECT COUNT(*) FROM user_catalogs")
    suspend fun getCount(): Int

    // Get all catalogs from a specific manifest (for cleanup when manifest changes)
    @Query("SELECT * FROM user_catalogs WHERE userId = :userId AND manifestId = :manifestId")
    suspend fun getCatalogsByManifest(userId: String, manifestId: String): List<UserCatalog>

    // Check if a catalog is already added
    @Query("SELECT COUNT(*) FROM user_catalogs WHERE userId = :userId AND catalogId = :catalogId AND catalogType = :catalogType AND pageType = :pageType")
    suspend fun isCatalogAdded(userId: String, catalogId: String, catalogType: String, pageType: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(catalog: UserCatalog): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(catalogs: List<UserCatalog>)

    @Update
    suspend fun update(catalog: UserCatalog)

    @Delete
    suspend fun delete(catalog: UserCatalog)

    // NEW: Delete all catalogs for a user
    @Query("DELETE FROM user_catalogs WHERE userId = :userId")
    suspend fun deleteByUser(userId: String)

    // Update display order for a catalog
    @Query("UPDATE user_catalogs SET displayOrder = :newOrder WHERE id = :catalogId")
    suspend fun updateDisplayOrder(catalogId: Long, newOrder: Int)

    // Update custom name
    @Query("UPDATE user_catalogs SET customName = :newName WHERE id = :catalogId")
    suspend fun updateCustomName(catalogId: Long, newName: String)

    // Get the maximum display order for a page (to add new catalogs at the end)
    @Query("SELECT MAX(displayOrder) FROM user_catalogs WHERE userId = :userId AND pageType = :pageType")
    suspend fun getMaxDisplayOrder(userId: String, pageType: String): Int?

    // Delete catalogs from a specific manifest (cleanup when manifest is removed)
    @Query("DELETE FROM user_catalogs WHERE userId = :userId AND manifestId = :manifestId")
    suspend fun deleteCatalogsByManifest(userId: String, manifestId: String)

    // Swap sort orders between two catalogs
    @Transaction
    suspend fun swapSortOrder(id1: Long, order1: Int, id2: Long, order2: Int) {
        updateDisplayOrder(id1, order2)
        updateDisplayOrder(id2, order1)
    }

    // Get enabled catalogs for user
    @Query("SELECT * FROM user_catalogs WHERE userId = :userId AND pageType = :pageType ORDER BY displayOrder ASC")
    suspend fun getEnabledUserCatalogs(pageType: String, userId: String = "default"): List<UserCatalog>
}

@Dao
interface CollectedItemDao {

    // Get all collected items for a user, filtered by type
    @Query("SELECT * FROM collected_items WHERE userId = :userId AND itemType = :itemType ORDER BY collectedDate DESC")
    fun getCollectedItems(userId: String, itemType: String): LiveData<List<CollectedItem>>

    // Get collected items sorted by name
    @Query("SELECT * FROM collected_items WHERE userId = :userId AND itemType = :itemType ORDER BY name ASC")
    fun getCollectedItemsByName(userId: String, itemType: String): LiveData<List<CollectedItem>>

    // Get collected items sorted by date (oldest first)
    @Query("SELECT * FROM collected_items WHERE userId = :userId AND itemType = :itemType ORDER BY collectedDate ASC")
    fun getCollectedItemsByDateAsc(userId: String, itemType: String): LiveData<List<CollectedItem>>

    // Check if an item is collected
    @Query("SELECT COUNT(*) FROM collected_items WHERE id = :id")
    suspend fun isCollected(id: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: CollectedItem)

    @Delete
    suspend fun delete(item: CollectedItem)

    // Delete by composite ID
    @Query("DELETE FROM collected_items WHERE id = :id")
    suspend fun deleteById(id: String)

    // NEW: Delete by User ID
    @Query("DELETE FROM collected_items WHERE userId = :userId")
    suspend fun deleteByUser(userId: String)

    // Get single item
    @Query("SELECT * FROM collected_items WHERE id = :id")
    suspend fun getItemById(id: String): CollectedItem?
}