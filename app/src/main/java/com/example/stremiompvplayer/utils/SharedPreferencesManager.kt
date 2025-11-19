package com.example.stremiompvplayer.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SharedPreferencesManager private constructor(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("stremio_mpv_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        @Volatile
        private var instance: SharedPreferencesManager? = null

        fun getInstance(context: Context): SharedPreferencesManager {
            return instance ?: synchronized(this) {
                instance ?: SharedPreferencesManager(context.applicationContext).also { instance = it }
            }
        }
    }

    fun saveActiveManifestUrl(url: String) {
        prefs.edit().putString("active_manifest_url", url).apply()
    }

    fun getActiveManifestUrl(): String? {
        return prefs.getString("active_manifest_url", null)
    }

    // TMDB Access Token Management
    fun saveTMDBAccessToken(token: String) {
        prefs.edit().putString("tmdb_access_token", token).apply()
    }

    fun getTMDBAccessToken(): String? {
        return prefs.getString("tmdb_access_token", null)
    }

    fun hasTMDBAccessToken(): Boolean {
        return !getTMDBAccessToken().isNullOrEmpty()
    }

    fun saveString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    fun getString(key: String, defaultValue: String?): String? {
        return prefs.getString(key, defaultValue)
    }

    // User Management
    fun getAllUsers(): List<User> {
        val json = prefs.getString("users_list", "[]")
        val type = object : TypeToken<List<User>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    fun createUser(name: String, avatarColor: Int): User {
        val users = getAllUsers().toMutableList()
        val newUser = User(
            id = System.currentTimeMillis().toString(),
            name = name,
            avatarColor = avatarColor
        )
        users.add(newUser)
        saveUsers(users)
        return newUser
    }

    fun saveUsers(users: List<User>) {
        val json = gson.toJson(users)
        prefs.edit().putString("users_list", json).apply()
    }

    fun getCurrentUserId(): String? {
        return prefs.getString("current_user_id", null)
    }

    fun setCurrentUser(id: String) {
        prefs.edit().putString("current_user_id", id).apply()
    }

    fun getUser(id: String): User? {
        return getAllUsers().find { it.id == id }
    }

    // Addon Management
    fun getUserAddonUrls(): List<String> {
        val json = prefs.getString("addon_urls", "[]")
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    fun addAddonUrl(url: String) {
        val urls = getUserAddonUrls().toMutableList()
        if (!urls.contains(url)) {
            urls.add(url)
            val json = gson.toJson(urls)
            prefs.edit().putString("addon_urls", json).apply()
        }
    }

    fun removeAddonUrl(url: String) {
        val urls = getUserAddonUrls().toMutableList()
        urls.remove(url)
        val json = gson.toJson(urls)
        prefs.edit().putString("addon_urls", json).apply()
    }

    // Settings
    fun saveUserSettings(settings: UserSettings) {
        val json = gson.toJson(settings)
        prefs.edit().putString("user_settings", json).apply()
    }

    fun getUserSettings(): UserSettings {
        val json = prefs.getString("user_settings", null)
        return if (json != null) {
            gson.fromJson(json, UserSettings::class.java)
        } else {
            UserSettings() // Return defaults
        }
    }
}

// Data classes
data class User(
    val id: String,
    val name: String,
    val avatarColor: Int
)

data class UserSettings(
    var autoPlayFirstStream: Boolean = false,
    var subtitlesEnabled: Boolean = true,
    var subtitleSize: Int = 20,
    var subtitleColor: Int = android.graphics.Color.WHITE
)
