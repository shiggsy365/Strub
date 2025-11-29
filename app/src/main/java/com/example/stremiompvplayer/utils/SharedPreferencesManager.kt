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

    // --- TMDB SESSION (Per User) ---
    fun saveTMDBSessionId(sessionId: String) {
        val userId = getCurrentUserId() ?: "default"
        prefs.edit().putString("tmdb_session_id_$userId", sessionId).apply()
    }

    fun getTMDBSessionId(): String? {
        val userId = getCurrentUserId() ?: "default"
        return prefs.getString("tmdb_session_id_$userId", null)
    }

    // --- TRAKT (Per User) ---
    fun saveTraktTokens(accessToken: String, refreshToken: String) {
        val userId = getCurrentUserId() ?: "default"
        prefs.edit()
            .putString("trakt_access_token_$userId", accessToken)
            .putString("trakt_refresh_token_$userId", refreshToken)
            .putBoolean("trakt_enabled_$userId", true)
            .apply()
    }

    fun getTraktAccessToken(): String? {
        val userId = getCurrentUserId() ?: "default"
        return prefs.getString("trakt_access_token_$userId", null)
    }

    fun isTraktEnabled(): Boolean {
        val userId = getCurrentUserId() ?: "default"
        return prefs.getBoolean("trakt_enabled_$userId", false)
    }

    fun setTraktEnabled(enabled: Boolean) {
        val userId = getCurrentUserId() ?: "default"
        prefs.edit().putBoolean("trakt_enabled_$userId", enabled).apply()
    }

    fun clearTraktData() {
        val userId = getCurrentUserId() ?: "default"
        prefs.edit()
            .remove("trakt_access_token_$userId")
            .remove("trakt_refresh_token_$userId")
            .remove("trakt_enabled_$userId")
            .apply()
    }

    fun getTraktClientId(): String {
        return Secrets.TRAKT_CLIENT_ID
    }

    // --- TRAKT SYNC SETTINGS ---
    fun isAutoSyncOnStartup(): Boolean {
        val userId = getCurrentUserId() ?: "default"
        return prefs.getBoolean("trakt_auto_sync_startup_$userId", true) // Default: enabled
    }

    fun setAutoSyncOnStartup(enabled: Boolean) {
        val userId = getCurrentUserId() ?: "default"
        prefs.edit().putBoolean("trakt_auto_sync_startup_$userId", enabled).apply()
    }

    fun isBackgroundSyncEnabled(): Boolean {
        val userId = getCurrentUserId() ?: "default"
        return prefs.getBoolean("trakt_background_sync_$userId", false) // Default: disabled
    }

    fun setBackgroundSyncEnabled(enabled: Boolean) {
        val userId = getCurrentUserId() ?: "default"
        prefs.edit().putBoolean("trakt_background_sync_$userId", enabled).apply()
    }

    fun getBackgroundSyncInterval(): Long {
        val userId = getCurrentUserId() ?: "default"
        // Default: 12 hours in milliseconds
        return prefs.getLong("trakt_sync_interval_$userId", 12 * 60 * 60 * 1000L)
    }

    fun setBackgroundSyncInterval(intervalMillis: Long) {
        val userId = getCurrentUserId() ?: "default"
        prefs.edit().putLong("trakt_sync_interval_$userId", intervalMillis).apply()
    }

    fun getLastTraktSyncTime(): Long {
        val userId = getCurrentUserId() ?: "default"
        return prefs.getLong("last_trakt_sync_$userId", 0L)
    }

    fun setLastTraktSyncTime(time: Long) {
        val userId = getCurrentUserId() ?: "default"
        prefs.edit().putLong("last_trakt_sync_$userId", time).apply()
    }

    fun shouldPerformBackgroundSync(): Boolean {
        if (!isBackgroundSyncEnabled() || !isTraktEnabled()) return false
        val lastSync = getLastTraktSyncTime()
        val currentTime = System.currentTimeMillis()
        val interval = getBackgroundSyncInterval()
        return (currentTime - lastSync) >= interval
    }

    fun isSyncOnWifiOnly(): Boolean {
        val userId = getCurrentUserId() ?: "default"
        return prefs.getBoolean("trakt_sync_wifi_only_$userId", true) // Default: Wi-Fi only
    }

    fun setSyncOnWifiOnly(wifiOnly: Boolean) {
        val userId = getCurrentUserId() ?: "default"
        prefs.edit().putBoolean("trakt_sync_wifi_only_$userId", wifiOnly).apply()
    }

    // --- AIOStreams ---
    fun saveAIOStreamsManifestUrl(manifestUrl: String) {
        prefs.edit().putString("aiostreams_manifest_url", manifestUrl).apply()
    }

    fun getAIOStreamsManifestUrl(): String? {
        return prefs.getString("aiostreams_manifest_url", null)
    }

    fun clearAIOStreamsCredentials() {
        prefs.edit()
            .remove("aiostreams_manifest_url")
            // Also remove old keys for migration
            .remove("aiostreams_username")
            .remove("aiostreams_password")
            .remove("aiostreams_url")
            .apply()
    }

    // --- Live TV (Per User) ---
    fun saveLiveTVM3UUrl(m3uUrl: String) {
        val userId = getCurrentUserId() ?: "default"
        prefs.edit().putString("livetv_m3u_url_$userId", m3uUrl).apply()
    }

    fun getLiveTVM3UUrl(): String? {
        val userId = getCurrentUserId() ?: "default"
        return prefs.getString("livetv_m3u_url_$userId", null)
    }

    fun saveLiveTVEPGUrl(epgUrl: String) {
        val userId = getCurrentUserId() ?: "default"
        prefs.edit().putString("livetv_epg_url_$userId", epgUrl).apply()
    }

    fun getLiveTVEPGUrl(): String? {
        val userId = getCurrentUserId() ?: "default"
        return prefs.getString("livetv_epg_url_$userId", null)
    }

    fun clearLiveTVCredentials() {
        val userId = getCurrentUserId() ?: "default"
        prefs.edit()
            .remove("livetv_m3u_url_$userId")
            .remove("livetv_epg_url_$userId")
            .apply()
    }

    fun getTMDBAccessToken(): String? { return prefs.getString("tmdb_access_token", null) }
    fun saveTMDBAccessToken(token: String) { prefs.edit().putString("tmdb_access_token", token).apply() }

    // TV Channels refresh tracking (24-hour cycle)
    fun getLastTVRefreshTime(): Long {
        val userId = getCurrentUserId() ?: "default"
        return prefs.getLong("last_tv_refresh_$userId", 0L)
    }

    fun setLastTVRefreshTime(time: Long) {
        val userId = getCurrentUserId() ?: "default"
        prefs.edit().putLong("last_tv_refresh_$userId", time).apply()
    }

    fun shouldRefreshTV(): Boolean {
        val lastRefresh = getLastTVRefreshTime()
        val currentTime = System.currentTimeMillis()
        val twentyFourHours = 24 * 60 * 60 * 1000L // 24 hours in milliseconds
        return (currentTime - lastRefresh) >= twentyFourHours
    }

    fun getAllUsers(): List<User> {
        val json = prefs.getString("users_list", "[]")
        val type = object : TypeToken<List<User>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    fun createUser(
        name: String,
        avatarColor: Int,
        isKidsProfile: Boolean = false,
        ageRating: String = "PG",
        passcode: String? = null
    ): User {
        val users = getAllUsers().toMutableList()
        val newUser = User(
            id = System.currentTimeMillis().toString(),
            name = name,
            avatarColor = avatarColor,
            isKidsProfile = isKidsProfile,
            ageRating = ageRating,
            passcode = passcode,
            isNewUser = true // Mark as new user
        )
        users.add(newUser)
        saveUsers(users)
        return newUser
    }

    fun markUserAsExisting(userId: String) {
        val users = getAllUsers().toMutableList()
        val userIndex = users.indexOfFirst { it.id == userId }
        if (userIndex != -1) {
            users[userIndex] = users[userIndex].copy(isNewUser = false)
            saveUsers(users)
        }
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

    // --- SUBTITLE STYLING PREFERENCES ---
    fun getSubtitleTextSize(): Float {
        val userId = getCurrentUserId() ?: "default"
        return prefs.getFloat("subtitle_text_size_$userId", 1.0f) // Default: medium (1.0)
    }

    fun setSubtitleTextSize(size: Float) {
        val userId = getCurrentUserId() ?: "default"
        prefs.edit().putFloat("subtitle_text_size_$userId", size).apply()
    }

    fun getSubtitleTextColor(): Int {
        val userId = getCurrentUserId() ?: "default"
        return prefs.getInt("subtitle_text_color_$userId", android.graphics.Color.WHITE)
    }

    fun setSubtitleTextColor(color: Int) {
        val userId = getCurrentUserId() ?: "default"
        prefs.edit().putInt("subtitle_text_color_$userId", color).apply()
    }

    fun getSubtitleBackgroundColor(): Int {
        val userId = getCurrentUserId() ?: "default"
        return prefs.getInt("subtitle_background_color_$userId", android.graphics.Color.TRANSPARENT)
    }

    fun setSubtitleBackgroundColor(color: Int) {
        val userId = getCurrentUserId() ?: "default"
        prefs.edit().putInt("subtitle_background_color_$userId", color).apply()
    }

    fun getSubtitleWindowColor(): Int {
        val userId = getCurrentUserId() ?: "default"
        return prefs.getInt("subtitle_window_color_$userId", android.graphics.Color.TRANSPARENT)
    }

    fun setSubtitleWindowColor(color: Int) {
        val userId = getCurrentUserId() ?: "default"
        prefs.edit().putInt("subtitle_window_color_$userId", color).apply()
    }

    fun getSubtitleEdgeType(): Int {
        val userId = getCurrentUserId() ?: "default"
        // Default: EDGE_TYPE_OUTLINE (1)
        return prefs.getInt("subtitle_edge_type_$userId", 1)
    }

    fun setSubtitleEdgeType(edgeType: Int) {
        val userId = getCurrentUserId() ?: "default"
        prefs.edit().putInt("subtitle_edge_type_$userId", edgeType).apply()
    }

    fun getSubtitleEdgeColor(): Int {
        val userId = getCurrentUserId() ?: "default"
        return prefs.getInt("subtitle_edge_color_$userId", android.graphics.Color.BLACK)
    }

    fun setSubtitleEdgeColor(color: Int) {
        val userId = getCurrentUserId() ?: "default"
        prefs.edit().putInt("subtitle_edge_color_$userId", color).apply()
    }

    // Export all profile settings to JSON
    fun exportProfileSettings(): String {
        val tmdbAccountId = getTMDBAccountId()
        val tmdbSettings = TMDBSettings(
            accountId = if (tmdbAccountId != -1) tmdbAccountId else null,
            sessionId = getTMDBSessionId(),
            accessToken = getTMDBAccessToken()
        )

        val traktAccessToken = getTraktAccessToken()
        val traktSettings = if (traktAccessToken != null) {
            TraktSettings(
                accessToken = traktAccessToken,
                refreshToken = prefs.getString("trakt_refresh_token", null),
                enabled = isTraktEnabled()
            )
        } else null

        val liveTVSettings = LiveTVSettings(
            m3uUrl = getLiveTVM3UUrl(),
            epgUrl = getLiveTVEPGUrl()
        )

        val integrations = IntegrationSettings(
            tmdb = tmdbSettings,
            trakt = traktSettings,
            aioStreams = getAIOStreamsManifestUrl(),
            liveTV = liveTVSettings
        )

        val exportData = ProfileExportData(
            users = getAllUsers(),
            currentUserId = getCurrentUserId(),
            userSettings = getUserSettings(),
            addonUrls = getUserAddonUrls(),
            integrations = integrations
        )

        return gson.toJson(exportData)
    }

    // Import profile settings from JSON
    fun importProfileSettings(jsonString: String): Boolean {
        return try {
            val exportData = gson.fromJson(jsonString, ProfileExportData::class.java)

            // Import users
            saveUsers(exportData.users)

            // Import current user
            exportData.currentUserId?.let { setCurrentUser(it) }

            // Import user settings
            saveUserSettings(exportData.userSettings)

            // Import addon URLs
            prefs.edit().putString("addon_urls", gson.toJson(exportData.addonUrls)).apply()

            // Import integrations
            val editor = prefs.edit()

            // TMDB
            exportData.integrations.tmdb?.let { tmdb ->
                tmdb.accountId?.let { if (it != -1) editor.putInt("tmdb_account_id", it) }
                tmdb.sessionId?.let { editor.putString("tmdb_session_id", it) }
                tmdb.accessToken?.let { editor.putString("tmdb_access_token", it) }
            }

            // Trakt
            exportData.integrations.trakt?.let { trakt ->
                trakt.accessToken?.let { editor.putString("trakt_access_token", it) }
                trakt.refreshToken?.let { editor.putString("trakt_refresh_token", it) }
                editor.putBoolean("trakt_enabled", trakt.enabled)
            }

            // AIOStreams
            exportData.integrations.aioStreams?.let {
                editor.putString("aiostreams_manifest_url", it)
            }

            // Live TV
            exportData.integrations.liveTV?.let { liveTV ->
                liveTV.m3uUrl?.let { editor.putString("livetv_m3u_url", it) }
                liveTV.epgUrl?.let { editor.putString("livetv_epg_url", it) }
            }

            editor.apply()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // --- PAGE ROW CONFIGURATION (Per User) ---
    
    /**
     * Get the movie page row configurations for the current user.
     * Returns default configurations if none are saved.
     */
    fun getMovieRowConfigs(): List<PageRowConfigData> {
        val userId = getCurrentUserId() ?: "default"
        val json = prefs.getString("movie_row_configs_$userId", null)
        return if (json != null) {
            try {
                val type = object : TypeToken<List<PageRowConfigData>>() {}.type
                gson.fromJson(json, type) ?: getDefaultMovieRowConfigs()
            } catch (e: Exception) {
                getDefaultMovieRowConfigs()
            }
        } else {
            getDefaultMovieRowConfigs()
        }
    }
    
    /**
     * Save the movie page row configurations for the current user.
     */
    fun saveMovieRowConfigs(configs: List<PageRowConfigData>) {
        val userId = getCurrentUserId() ?: "default"
        val json = gson.toJson(configs)
        prefs.edit().putString("movie_row_configs_$userId", json).apply()
    }
    
    /**
     * Get the series page row configurations for the current user.
     * Returns default configurations if none are saved.
     */
    fun getSeriesRowConfigs(): List<PageRowConfigData> {
        val userId = getCurrentUserId() ?: "default"
        val json = prefs.getString("series_row_configs_$userId", null)
        return if (json != null) {
            try {
                val type = object : TypeToken<List<PageRowConfigData>>() {}.type
                gson.fromJson(json, type) ?: getDefaultSeriesRowConfigs()
            } catch (e: Exception) {
                getDefaultSeriesRowConfigs()
            }
        } else {
            getDefaultSeriesRowConfigs()
        }
    }
    
    /**
     * Save the series page row configurations for the current user.
     */
    fun saveSeriesRowConfigs(configs: List<PageRowConfigData>) {
        val userId = getCurrentUserId() ?: "default"
        val json = gson.toJson(configs)
        prefs.edit().putString("series_row_configs_$userId", json).apply()
    }
    
    /**
     * Get default movie row configurations.
     */
    private fun getDefaultMovieRowConfigs(): List<PageRowConfigData> = listOf(
        PageRowConfigData("movies_trending", "Trending", "TMDB_TRENDING_MOVIES", true, 0),
        PageRowConfigData("movies_latest", "Latest", "TMDB_LATEST_MOVIES", true, 1),
        PageRowConfigData("movies_popular", "Popular", "TMDB_POPULAR_MOVIES", true, 2),
        PageRowConfigData("movies_watchlist", "Watchlist", "TRAKT_WATCHLIST", true, 3),
        PageRowConfigData("movies_genres", "Genres", "GENRES", true, 4)
    )
    
    /**
     * Get default series row configurations.
     */
    private fun getDefaultSeriesRowConfigs(): List<PageRowConfigData> = listOf(
        PageRowConfigData("series_trending", "Trending", "TMDB_TRENDING_TV", true, 0),
        PageRowConfigData("series_latest", "Latest", "TMDB_LATEST_TV", true, 1),
        PageRowConfigData("series_popular", "Popular", "TMDB_POPULAR_TV", true, 2),
        PageRowConfigData("series_watchlist", "Watchlist", "TRAKT_WATCHLIST", true, 3),
        PageRowConfigData("series_genres", "Genres", "GENRES", true, 4)
    )
}

enum class AgeRating(val displayName: String, val value: String) {
    U("U", "U"),
    PG("PG", "PG"),
    TWELVE("12", "12"),
    FIFTEEN("15", "15"),
    EIGHTEEN("18", "18");

    companion object {
        fun fromString(value: String): AgeRating {
            return values().find { it.value == value } ?: PG
        }
    }
}

data class User(
    val id: String,
    val name: String,
    val avatarColor: Int,
    val isKidsProfile: Boolean = false, // Deprecated, kept for backwards compatibility
    val ageRating: String = "PG", // U, PG, 12, 15, 18
    val passcode: String? = null, // Optional 4-digit passcode
    val isNewUser: Boolean = false // Flag for first-time user
)
data class UserSettings(var autoPlayFirstStream: Boolean = false, var subtitlesEnabled: Boolean = true, var subtitleSize: Int = 20, var subtitleColor: Int = android.graphics.Color.WHITE)

// Data model for exporting/importing profile settings
data class ProfileExportData(
    val version: Int = 1,
    val exportDate: Long = System.currentTimeMillis(),
    val users: List<User>,
    val currentUserId: String?,
    val userSettings: UserSettings,
    val addonUrls: List<String>,
    val integrations: IntegrationSettings
)

data class IntegrationSettings(
    val tmdb: TMDBSettings?,
    val trakt: TraktSettings?,
    val aioStreams: String?,
    val liveTV: LiveTVSettings?
)

data class TMDBSettings(
    val accountId: Int?,
    val sessionId: String?,
    val accessToken: String?
)

data class TraktSettings(
    val accessToken: String?,
    val refreshToken: String?,
    val enabled: Boolean
)

data class LiveTVSettings(
    val m3uUrl: String?,
    val epgUrl: String?
)

// Data models for page row configuration
data class PageRowConfigData(
    val id: String,
    val label: String,
    val sourceType: String,
    val isProtected: Boolean,
    val order: Int
)