package com.example.stremiompvplayer.utils

import com.example.stremiompvplayer.models.MetaItem

/**
 * In-memory session cache with TTL for frequently accessed data
 * Reduces redundant API calls and database queries
 */
class SessionCache(private val ttlMinutes: Int = 5) {

    private data class CachedItem<T>(val data: T, val timestamp: Long)

    private val continueWatchingCache = mutableMapOf<String, CachedItem<List<MetaItem>>>()
    private val nextUpCache = mutableMapOf<String, CachedItem<List<MetaItem>>>()
    private val homeContentCache = mutableMapOf<String, CachedItem<List<MetaItem>>>()

    private fun isExpired(timestamp: Long): Boolean {
        return (System.currentTimeMillis() - timestamp) > (ttlMinutes * 60 * 1000L)
    }

    // Continue Watching cache
    fun getContinueWatching(userId: String, type: String): List<MetaItem>? {
        val key = "${userId}_${type}"
        val cached = continueWatchingCache[key]
        return if (cached != null && !isExpired(cached.timestamp)) {
            cached.data
        } else {
            continueWatchingCache.remove(key)
            null
        }
    }

    fun putContinueWatching(userId: String, type: String, data: List<MetaItem>) {
        val key = "${userId}_${type}"
        continueWatchingCache[key] = CachedItem(data, System.currentTimeMillis())
    }

    // Next Up cache
    fun getNextUp(userId: String): List<MetaItem>? {
        val cached = nextUpCache[userId]
        return if (cached != null && !isExpired(cached.timestamp)) {
            cached.data
        } else {
            nextUpCache.remove(userId)
            null
        }
    }

    fun putNextUp(userId: String, data: List<MetaItem>) {
        nextUpCache[userId] = CachedItem(data, System.currentTimeMillis())
    }

    // Home content cache
    fun getHomeContent(userId: String, catalogId: String): List<MetaItem>? {
        val key = "${userId}_${catalogId}"
        val cached = homeContentCache[key]
        return if (cached != null && !isExpired(cached.timestamp)) {
            cached.data
        } else {
            homeContentCache.remove(key)
            null
        }
    }

    fun putHomeContent(userId: String, catalogId: String, data: List<MetaItem>) {
        val key = "${userId}_${catalogId}"
        homeContentCache[key] = CachedItem(data, System.currentTimeMillis())
    }

    // Invalidation methods
    fun invalidateContinueWatching(userId: String) {
        continueWatchingCache.keys.removeAll { it.startsWith(userId) }
    }

    fun invalidateNextUp(userId: String) {
        nextUpCache.remove(userId)
    }

    fun invalidateAll(userId: String) {
        continueWatchingCache.keys.removeAll { it.startsWith(userId) }
        nextUpCache.remove(userId)
        homeContentCache.keys.removeAll { it.startsWith(userId) }
    }

    fun clearAll() {
        continueWatchingCache.clear()
        nextUpCache.clear()
        homeContentCache.clear()
    }

    companion object {
        @Volatile
        private var instance: SessionCache? = null

        fun getInstance(ttlMinutes: Int = 5): SessionCache {
            return instance ?: synchronized(this) {
                instance ?: SessionCache(ttlMinutes).also { instance = it }
            }
        }
    }
}
