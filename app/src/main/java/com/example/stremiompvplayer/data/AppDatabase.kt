package com.example.stremiompvplayer.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.stremiompvplayer.models.*

@Database(
    entities = [
        LibraryItem::class, 
        WatchProgress::class, 
        User::class, 
        UserSettings::class, 
        HubSlot::class, 
        FeedList::class, 
        NextUpItem::class,
        UserCatalog::class // Added
    ], 
    version = 2, // Incremented version
    exportSchema = false
)
@TypeConverters(com.example.stremiompvplayer.models.Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun libraryItemDao(): LibraryItemDao
    abstract fun watchProgressDao(): WatchProgressDao
    abstract fun userDao(): UserDao
    abstract fun userSettingsDao(): UserSettingsDao
    abstract fun hubSlotDao(): HubSlotDao
    abstract fun feedListDao(): FeedListDao
    abstract fun nextUpItemDao(): NextUpItemDao
    abstract fun userCatalogDao(): UserCatalogDao // Added

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "stremio_player_database"
                )
                .fallbackToDestructiveMigration() // Simplified for this update
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
