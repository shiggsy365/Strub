package com.example.stremiompvplayer.utils

import android.view.View
import java.lang.ref.WeakReference

/**
 * Manages focus memory across fragments and navigation for seamless UX
 * Ensures key presses follow on from previously focused items
 */
class FocusMemoryManager private constructor() {

    private val focusMemory = mutableMapOf<String, FocusState>()

    data class FocusState(
        val viewRef: WeakReference<View>?,
        val viewId: Int,
        val position: Int = -1,
        val timestamp: Long = System.currentTimeMillis()
    )

    companion object {
        @Volatile
        private var instance: FocusMemoryManager? = null

        fun getInstance(): FocusMemoryManager {
            return instance ?: synchronized(this) {
                instance ?: FocusMemoryManager().also { instance = it }
            }
        }
    }

    /**
     * Save focus state for a specific fragment/screen
     * @param key Unique identifier for the fragment (e.g., "home", "discover_movies")
     * @param view The view that currently has focus
     * @param position Optional position in a list/grid
     */
    fun saveFocus(key: String, view: View?, position: Int = -1) {
        if (view == null) return

        val focusState = FocusState(
            viewRef = WeakReference(view),
            viewId = view.id,
            position = position,
            timestamp = System.currentTimeMillis()
        )

        focusMemory[key] = focusState
    }

    /**
     * Restore focus for a specific fragment/screen
     * @param key Unique identifier for the fragment
     * @return The position that was saved, or -1 if none
     */
    fun restoreFocus(key: String): Int {
        val focusState = focusMemory[key] ?: return -1

        // Try to restore focus to the actual view
        focusState.viewRef?.get()?.let { view ->
            if (view.isFocusable && view.isAttachedToWindow) {
                view.post {
                    view.requestFocus()
                }
            }
        }

        return focusState.position
    }

    /**
     * Get the saved position without restoring focus
     */
    fun getSavedPosition(key: String): Int {
        return focusMemory[key]?.position ?: -1
    }

    /**
     * Check if focus memory exists for a key
     */
    fun hasFocusMemory(key: String): Boolean {
        return focusMemory.containsKey(key)
    }

    /**
     * Clear focus memory for a specific key
     */
    fun clearFocus(key: String) {
        focusMemory.remove(key)
    }

    /**
     * Clear all focus memory
     */
    fun clearAll() {
        focusMemory.clear()
    }

    /**
     * Clean up stale focus memory (older than 5 minutes)
     */
    fun cleanupStale() {
        val now = System.currentTimeMillis()
        val staleKeys = focusMemory.filter { (_, state) ->
            now - state.timestamp > 5 * 60 * 1000 // 5 minutes
        }.keys

        staleKeys.forEach { focusMemory.remove(it) }
    }

    /**
     * Get a unique key for a fragment with parameters
     */
    fun getFragmentKey(fragmentName: String, vararg params: Any): String {
        return if (params.isEmpty()) {
            fragmentName
        } else {
            "$fragmentName:${params.joinToString("_")}"
        }
    }
}
