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
        CollectedItem::class
    ],
    version = 4, // Incremented version for schema change
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
                    .fallbackToDestructiveMigration() // Handle schema changes simply for now
                    .build()
                INSTANCE = instance
                instance
            }
        }

        fun getDatabase(context: Context): AppDatabase = getInstance(context)
    }

    suspend fun getLibraryItems(userId: String): List<LibraryItem> {
        return libraryItemDao().getAllItems()
    }

    suspend fun deleteUserData(userId: String) {
        hubSlotDao().deleteByUser(userId)
        feedListDao().deleteByUser(userId)
        userCatalogDao().deleteByUser(userId)
        collectedItemDao().deleteByUser(userId)
        watchProgressDao().deleteByUser(userId)
    }
}

@Dao
interface WatchProgressDao {
    @Query("SELECT * FROM watch_progress WHERE userId = :userId")
    fun getProgressForUser(userId: String): androidx.lifecycle.LiveData<List<WatchProgress>>

    @Query("SELECT * FROM watch_progress WHERE userId = :userId AND itemId = :itemId")
    suspend fun getItemProgress(userId: String, itemId: String): WatchProgress?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProgress(progress: WatchProgress)

    @Query("DELETE FROM watch_progress WHERE userId = :userId")
    suspend fun deleteByUser(userId: String)

    @Query("DELETE FROM watch_progress WHERE userId = :userId AND itemId = :itemId")
    suspend fun deleteItemProgress(userId: String, itemId: String)

    @Query("UPDATE watch_progress SET isWatched = :isWatched, progress = :progress WHERE userId = :userId AND itemId = :itemId")
    suspend fun updateWatchedStatus(userId: String, itemId: String, isWatched: Boolean, progress: Long = 0)

    // Continue Watching Lists
    @Query("SELECT * FROM watch_progress WHERE userId = :userId AND type = 'movie' AND isWatched = 0 AND progress > 0 ORDER BY lastUpdated DESC")
    suspend fun getContinueWatchingMovies(userId: String): List<WatchProgress>

    @Query("SELECT * FROM watch_progress WHERE userId = :userId AND type = 'episode' AND isWatched = 0 AND progress > 0 ORDER BY lastUpdated DESC")
    suspend fun getContinueWatchingEpisodes(userId: String): List<WatchProgress>

    // For Next Up: Get all watched episodes to calculate next
    @Query("SELECT * FROM watch_progress WHERE userId = :userId AND type = 'episode' AND isWatched = 1 ORDER BY lastUpdated DESC")
    suspend fun getWatchedEpisodes(userId: String): List<WatchProgress>
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