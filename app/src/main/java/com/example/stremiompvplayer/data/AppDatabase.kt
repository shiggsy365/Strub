package com.example.stremiompvplayer.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.stremiompvplayer.models.*

@Database(
    entities = [
        LibraryItem::class,
        WatchProgress::class,
        HubSlot::class,
        FeedList::class,
        NextUpItem::class,
        UserCatalog::class,
        CollectedItem::class // Added this to ensure CollectedItemDao works if it wasn't registered
    ],
    version = 3, // Incremented version
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun libraryItemDao(): LibraryItemDao
    abstract fun watchProgressDao(): WatchProgressDao
    abstract fun hubSlotDao(): HubSlotDao
    abstract fun feedListDao(): FeedListDao
    abstract fun nextUpItemDao(): NextUpItemDao
    abstract fun userCatalogDao(): UserCatalogDao
    abstract fun collectedItemDao(): CollectedItemDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "stremio_player_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        // Legacy method for compatibility
        fun getDatabase(context: Context): AppDatabase = getInstance(context)
    }

    // Helper method to get library items for a user
    suspend fun getLibraryItems(userId: String): List<LibraryItem> {
        // Since we don't have a userId field in LibraryItem,
        // we'll just return all items for now
        // You can add userId field to LibraryItem entity if needed
        return libraryItemDao().getAllItems()
    }

    // NEW: Helper to delete all data for a user
    suspend fun deleteUserData(userId: String) {
        hubSlotDao().deleteByUser(userId)
        feedListDao().deleteByUser(userId)
        userCatalogDao().deleteByUser(userId)
        collectedItemDao().deleteByUser(userId)
    }
}

// Add DAO interfaces that were missing
@Dao
interface LibraryItemDao {
    @Query("SELECT * FROM LibraryItem")
    suspend fun getAllItems(): List<LibraryItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: LibraryItem)

    @Delete
    suspend fun delete(item: LibraryItem)
}

@Dao
interface WatchProgressDao {
    @Query("SELECT * FROM WatchProgress")
    suspend fun getAllProgress(): List<WatchProgress>
}

@Dao
interface HubSlotDao {
    @Query("SELECT * FROM HubSlot WHERE userId = :userId")
    suspend fun getSlotsByUser(userId: String): List<HubSlot>

    @Query("DELETE FROM HubSlot WHERE userId = :userId")
    suspend fun deleteByUser(userId: String)
}

@Dao
interface FeedListDao {
    @Query("SELECT * FROM FeedList WHERE userId = :userId")
    suspend fun getFeedsByUser(userId: String): List<FeedList>

    @Query("DELETE FROM FeedList WHERE userId = :userId")
    suspend fun deleteByUser(userId: String)
}

@Dao
interface NextUpItemDao {
    @Query("SELECT * FROM NextUpItem")
    suspend fun getAllNextUpItems(): List<NextUpItem>
}