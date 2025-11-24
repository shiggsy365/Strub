package com.example.stremiompvplayer.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SharedPreferencesManager private constructor(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("stremio_mpv_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        // [CHANGE] Hardcoded TMDB Key - Replace this with your actual key
        private const val TMDB_API_KEY = "YOUR_TMDB_API_KEY_HERE"

        @Volatile
        private var instance: SharedPreferencesManager? = null

        fun getInstance(context: Context): SharedPreferencesManager {
            return instance ?: synchronized(this) {
                instance ?: SharedPreferencesManager(context.applicationContext).also { instance = it }
            }
        }
    }

    fun saveTMDBAccountId(accountId: Int) {
        prefs.edit().putInt("tmdb_account_id", accountId).apply()
    }

    fun getTMDBAccountId(): Int {
        return prefs.getInt("tmdb_account_id", -1)
    }

    // --- TMDB API KEY ---
    fun saveTMDBApiKey(key: String) {
        // No-op
    }

    fun getTMDBApiKey(): String {
        // [CHANGE] Read from the external Secrets object
        return Secrets.TMDB_API_KEY
    }

    fun hasTMDBApiKey(): Boolean {
        return getTMDBApiKey().isNotEmpty()
    }

    // --- TMDB SESSION ---
    fun saveTMDBSessionId(sessionId: String) {
        prefs.edit().putString("tmdb_session_id", sessionId).apply()
    }

    fun getTMDBSessionId(): String? {
        return prefs.getString("tmdb_session_id", null)
    }
    fun saveTraktTokens(accessToken: String, refreshToken: String) {
        prefs.edit()
            .putString("trakt_access_token", accessToken)
            .putString("trakt_refresh_token", refreshToken)
            .putBoolean("trakt_enabled", true)
            .apply()
    }

    fun getTraktAccessToken(): String? = prefs.getString("trakt_access_token", null)

    fun isTraktEnabled(): Boolean = prefs.getBoolean("trakt_enabled", false)

    fun setTraktEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("trakt_enabled", enabled).apply()
    }

    fun clearTraktData() {
        prefs.edit()
            .remove("trakt_access_token")
            .remove("trakt_refresh_token")
            .remove("trakt_enabled")
            .apply()
    }

    // --- AIOStreams ---
    fun saveAIOStreamsUsername(username: String) {
        prefs.edit().putString("aiostreams_username", username).apply()
    }

    fun getAIOStreamsUsername(): String? {
        return prefs.getString("aiostreams_username", null)
    }

    fun saveAIOStreamsPassword(password: String) {
        prefs.edit().putString("aiostreams_password", password).apply()
    }

    fun getAIOStreamsPassword(): String? {
        return prefs.getString("aiostreams_password", null)
    }

    fun saveAIOStreamsUrl(url: String) {
        prefs.edit().putString("aiostreams_url", url).apply()
    }

    fun getAIOStreamsUrl(): String? {
        // Default to Viren's server if not set
        return prefs.getString("aiostreams_url", "https://aiostreams.viren070.me")
    }

    fun clearAIOStreamsCredentials() {
        prefs.edit()
            .remove("aiostreams_username")
            .remove("aiostreams_password")
            .apply()
    }

    fun getTMDBAccessToken(): String? { return prefs.getString("tmdb_access_token", null) }
    fun saveTMDBAccessToken(token: String) { prefs.edit().putString("tmdb_access_token", token).apply() }

    fun getAllUsers(): List<User> {
        val json = prefs.getString("users_list", "[]")
        val type = object : TypeToken<List<User>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    fun createUser(name: String, avatarColor: Int, isKidsProfile: Boolean = false): User {
        val users = getAllUsers().toMutableList()
        val newUser = User(
            id = System.currentTimeMillis().toString(),
            name = name,
            avatarColor = avatarColor,
            isKidsProfile = isKidsProfile
        )
        users.add(newUser)
        saveUsers(users)
        return newUser
    }


    fun saveUsers(users: List<User>) {
        val json = gson.toJson(users)
        prefs.edit().putString("users_list", json).apply()
    }

    fun deleteUser(userId: String) {
        val users = getAllUsers().toMutableList()
        val iterator = users.iterator()
        var removed = false
        while (iterator.hasNext()) {
            if (iterator.next().id == userId) {
                iterator.remove()
                removed = true
            }
        }
        if (removed) {
            saveUsers(users)
            if (getCurrentUserId() == userId) {
                prefs.edit().remove("current_user_id").apply()
            }
        }
    }

    fun getCurrentUserId(): String? { return prefs.getString("current_user_id", null) }
    fun setCurrentUser(id: String) { prefs.edit().putString("current_user_id", id).apply() }
    fun getUser(id: String): User? { return getAllUsers().find { it.id == id } }
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
    fun saveUserSettings(settings: UserSettings) {
        val json = gson.toJson(settings)
        prefs.edit().putString("user_settings", json).apply()
    }
    fun getUserSettings(): UserSettings {
        val json = prefs.getString("user_settings", null)
        return if (json != null) {
            gson.fromJson(json, UserSettings::class.java)
        } else {
            UserSettings()
        }
    }
}

data class User(val id: String, val name: String, val avatarColor: Int, val isKidsProfile: Boolean = false)
data class UserSettings(var autoPlayFirstStream: Boolean = false, var subtitlesEnabled: Boolean = true, var subtitleSize: Int = 20, var subtitleColor: Int = android.graphics.Color.WHITE)