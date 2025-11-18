package com.example.stremiompvplayer.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.stremiompvplayer.models.LibraryItem
import com.example.stremiompvplayer.models.NextUpItem
import com.example.stremiompvplayer.models.User
import com.example.stremiompvplayer.models.UserSettings
import com.example.stremiompvplayer.models.WatchProgress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.stremiompvplayer.models.FeedList
import com.example.stremiompvplayer.models.HubSlot
import com.example.stremiompvplayer.models.UserCatalog
import com.example.stremiompvplayer.models.CollectedItem
import androidx.room.migration.Migration

@Database(
    entities = [
        FeedList::class,
        HubSlot::class,
        // FIX: Use stub models
        User::class,
        LibraryItem::class,
        WatchProgress::class,
        NextUpItem::class,
        UserSettings::class,
        UserCatalog::class,
        CollectedItem::class,
    ],
    version = 2, // INCREMENT VERSION!
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    fun getLibraryItems(userId: String): List<LibraryItem> {
        // NOTE: We ignore userId here, as the DAO doesn't filter by user, but this resolves the method signature.
        return libraryItemDao().getAll()
    }
    // FIX: Add DAOs for stub models
    abstract fun userDao(): UserDao
    abstract fun libraryItemDao(): LibraryItemDao
    abstract fun userCatalogDao(): UserCatalogDao
    abstract fun collectedItemDao(): CollectedItemDao
    abstract fun watchProgressDao(): WatchProgressDao
    abstract fun nextUpItemDao(): NextUpItemDao
    abstract fun userSettingsDao(): UserSettingsDao

    abstract fun feedListDao(): FeedListDao
    abstract fun hubSlotDao(): HubSlotDao

    // AppDatabase.kt

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // NEW: Add migration from version 1 to 2
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create user_catalogs table
                database.execSQL("""
                CREATE TABLE IF NOT EXISTS user_catalogs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    userId TEXT NOT NULL,
                    catalogId TEXT NOT NULL,
                    catalogType TEXT NOT NULL,
                    catalogName TEXT NOT NULL,
                    customName TEXT,
                    displayOrder INTEGER NOT NULL,
                    pageType TEXT NOT NULL,
                    addonUrl TEXT NOT NULL,
                    manifestId TEXT NOT NULL,
                    dateAdded INTEGER NOT NULL
                )
            """)

                // Create collected_items table
                database.execSQL("""
                CREATE TABLE IF NOT EXISTS collected_items (
                    id TEXT PRIMARY KEY NOT NULL,
                    userId TEXT NOT NULL,
                    itemId TEXT NOT NULL,
                    itemType TEXT NOT NULL,
                    name TEXT NOT NULL,
                    poster TEXT,
                    background TEXT,
                    description TEXT,
                    collectedDate INTEGER NOT NULL,
                    year TEXT,
                    genres TEXT,
                    rating TEXT
                )
            """)
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "stremio_database"
                )
                    .addMigrations(MIGRATION_1_2)  // ← ADD THIS LINE
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
// NOTE: You may need to remove the AppDatabaseCallback entire inner class as well

    private class AppDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch {
                    populateDatabase(database)
                }
            }
        }

        suspend fun populateDatabase(database: AppDatabase) {
            // You can add initial data here if needed
            // e.g., database.userDao().insert(...)
        }
    }
}

// FIX: Define placeholder DAOs for stub models. You'll need to fill these
// with your actual database logic.

@androidx.room.Dao
interface UserDao {
    @androidx.room.Query("SELECT * FROM User")
    fun getAll(): List<User>

    @androidx.room.Insert
    fun insert(user: User)
}

@androidx.room.Dao
interface LibraryItemDao {
    @androidx.room.Query("SELECT * FROM LibraryItem")
    fun getAll(): List<LibraryItem>
}

@androidx.room.Dao
interface WatchProgressDao {
    @androidx.room.Query("SELECT * FROM WatchProgress")
    fun getAll(): List<WatchProgress>
}

@androidx.room.Dao
interface NextUpItemDao {
    @androidx.room.Query("SELECT * FROM NextUpItem")
    fun getAll(): List<NextUpItem>
}

@androidx.room.Dao
interface UserSettingsDao {
    @androidx.room.Query("SELECT * FROM UserSettings")
    fun getAll(): List<UserSettings>
}

// These are DAOs from your original project that were inferred from other files
@androidx.room.Dao
interface FeedListDao {
    // Add your FeedList queries here
}

@androidx.room.Dao
interface HubSlotDao {
    // Add your HubSlot queries here
}