package com.example.stremiompvplayer.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
// FIX: Import stub models
import com.example.stremiompvplayer.models.LibraryItem
import com.example.stremiompvplayer.models.NextUpItem
import com.example.stremiompvplayer.models.User
import com.example.stremiompvplayer.models.UserSettings
import com.example.stremiompvplayer.models.WatchProgress
// NEW: Add imports for FeedList and HubSlot
import com.example.stremiompvplayer.models.FeedList
import com.example.stremiompvplayer.models.HubSlot
import kotlinx_coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Database(
    entities = [
        FeedList::class,
        HubSlot::class,
        // FIX: Use stub models
        User::class,
        LibraryItem::class,
        WatchProgress::class,
        NextUpItem::class,
        UserSettings::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    // FIX: Add DAOs for stub models
    abstract fun userDao(): UserDao
    abstract fun libraryItemDao(): LibraryItemDao
    abstract fun watchProgressDao(): WatchProgressDao
    abstract fun nextUpItemDao(): NextUpItemDao
    abstract fun userSettingsDao(): UserSettingsDao

    abstract fun feedListDao(): FeedListDao
    abstract fun hubSlotDao(): HubSlotDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "stremio_database"
                )
                    .addCallback(AppDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

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