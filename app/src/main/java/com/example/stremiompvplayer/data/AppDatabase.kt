package com.example.stremiompvplayer.data

import android.content.Context
import android.content.SharedPreferences
import com.example.stremiompvplayer.models.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID

class AppDatabase private constructor(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("stremio_mpv_db", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    companion object {
        @Volatile
        private var instance: AppDatabase? = null
        
        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: AppDatabase(context.applicationContext).also { instance = it }
            }
        }
    }
    
    // User Management
    fun getAllUsers(): List<User> {
        val json = prefs.getString("users", "[]") ?: "[]"
        val type = object : TypeToken<List<User>>() {}.type
        return gson.fromJson(json, type)
    }
    
    fun getUser(userId: String): User? {
        return getAllUsers().find { it.id == userId }
    }
    
    fun createUser(name: String, avatarColor: Int): User {
        val user = User(
            id = UUID.randomUUID().toString(),
            name = name,
            avatarColor = avatarColor
        )
        val users = getAllUsers().toMutableList()
        users.add(user)
        saveUsers(users)
        return user
    }
    
    fun updateUser(user: User) {
        val users = getAllUsers().toMutableList()
        val index = users.indexOfFirst { it.id == user.id }
        if (index != -1) {
            users[index] = user
            saveUsers(users)
        }
    }
    
    fun deleteUser(userId: String) {
        val users = getAllUsers().toMutableList()
        users.removeAll { it.id == userId }
        saveUsers(users)
        
        // Clean up user data
        deleteAllLibraryItems(userId)
        deleteAllWatchProgress(userId)
        deleteUserSettings(userId)
    }
    
    private fun saveUsers(users: List<User>) {
        prefs.edit().putString("users", gson.toJson(users)).apply()
    }
    
    // Current User
    fun getCurrentUserId(): String? {
        return prefs.getString("current_user_id", null)
    }
    
    fun setCurrentUser(userId: String) {
        prefs.edit().putString("current_user_id", userId).apply()
        
        // Update last active
        getUser(userId)?.let { user ->
            updateUser(user.copy(lastActive = System.currentTimeMillis()))
        }
    }
    
    // Library Management
    fun getLibraryItems(userId: String): List<LibraryItem> {
        val json = prefs.getString("library_$userId", "[]") ?: "[]"
        val type = object : TypeToken<List<LibraryItem>>() {}.type
        return gson.fromJson(json, type)
    }
    
    fun addToLibrary(userId: String, item: LibraryItem) {
        val items = getLibraryItems(userId).toMutableList()
        if (!items.any { it.metaId == item.metaId }) {
            items.add(item)
            saveLibraryItems(userId, items)
        }
    }
    
    fun removeFromLibrary(userId: String, metaId: String) {
        val items = getLibraryItems(userId).toMutableList()
        items.removeAll { it.metaId == metaId }
        saveLibraryItems(userId, items)
    }
    
    fun isInLibrary(userId: String, metaId: String): Boolean {
        return getLibraryItems(userId).any { it.metaId == metaId }
    }
    
    fun getLibraryItemsByType(userId: String, type: String): List<LibraryItem> {
        return getLibraryItems(userId).filter { it.type == type }
    }
    
    private fun saveLibraryItems(userId: String, items: List<LibraryItem>) {
        prefs.edit().putString("library_$userId", gson.toJson(items)).apply()
    }
    
    private fun deleteAllLibraryItems(userId: String) {
        prefs.edit().remove("library_$userId").apply()
    }
    
    // Watch Progress Management
    fun getAllWatchProgress(userId: String): List<WatchProgress> {
        val json = prefs.getString("watch_progress_$userId", "[]") ?: "[]"
        val type = object : TypeToken<List<WatchProgress>>() {}.type
        return gson.fromJson(json, type)
    }
    
    fun getWatchProgress(userId: String, metaId: String, videoId: String? = null): WatchProgress? {
        return getAllWatchProgress(userId).find { 
            it.metaId == metaId && (videoId == null || it.videoId == videoId)
        }
    }
    
    fun saveWatchProgress(progress: WatchProgress) {
        val allProgress = getAllWatchProgress(progress.userId).toMutableList()
        val index = allProgress.indexOfFirst { 
            it.metaId == progress.metaId && it.videoId == progress.videoId 
        }
        
        if (index != -1) {
            allProgress[index] = progress
        } else {
            allProgress.add(progress)
        }
        
        saveAllWatchProgress(progress.userId, allProgress)
    }
    
    fun getContinueWatchingItems(userId: String, limit: Int = 20): List<NextUpItem> {
        val progress = getAllWatchProgress(userId)
            .filter { !it.completed && it.position > 0 }
            .sortedByDescending { it.lastWatched }
            .take(limit)
        
        return progress.mapNotNull { wp ->
            val libraryItem = getLibraryItems(userId).find { it.metaId == wp.metaId }
            libraryItem?.let {
                NextUpItem(
                    metaId = wp.metaId,
                    type = wp.type,
                    name = it.name,
                    poster = it.poster,
                    background = it.background,
                    videoId = wp.videoId,
                    videoTitle = if (wp.episodeNumber != null) "S${wp.seasonNumber}E${wp.episodeNumber}" else null,
                    seasonNumber = wp.seasonNumber,
                    episodeNumber = wp.episodeNumber,
                    progress = if (wp.duration > 0) wp.position.toFloat() / wp.duration else 0f,
                    lastWatched = wp.lastWatched
                )
            }
        }
    }
    
    private fun saveAllWatchProgress(userId: String, progress: List<WatchProgress>) {
        prefs.edit().putString("watch_progress_$userId", gson.toJson(progress)).apply()
    }
    
    private fun deleteAllWatchProgress(userId: String) {
        prefs.edit().remove("watch_progress_$userId").apply()
    }
    
    // User Settings
    fun getUserSettings(userId: String): UserSettings {
        val json = prefs.getString("settings_$userId", null)
        return if (json != null) {
            gson.fromJson(json, UserSettings::class.java)
        } else {
            UserSettings(userId = userId)
        }
    }
    
    fun saveUserSettings(settings: UserSettings) {
        prefs.edit().putString("settings_${settings.userId}", gson.toJson(settings)).apply()
    }
    
    private fun deleteUserSettings(userId: String) {
        prefs.edit().remove("settings_$userId").apply()
    }
    
    // Addon URLs per user
    fun getUserAddonUrls(userId: String): List<String> {
        return getUser(userId)?.addonUrls ?: emptyList()
    }
    
    fun addAddonUrl(userId: String, url: String) {
        getUser(userId)?.let { user ->
            val urls = user.addonUrls.toMutableList()
            if (!urls.contains(url)) {
                urls.add(url)
                updateUser(user.copy(addonUrls = urls))
            }
        }
    }
    
    fun removeAddonUrl(userId: String, url: String) {
        getUser(userId)?.let { user ->
            val urls = user.addonUrls.toMutableList()
            urls.remove(url)
            updateUser(user.copy(addonUrls = urls))
        }
    }
}
