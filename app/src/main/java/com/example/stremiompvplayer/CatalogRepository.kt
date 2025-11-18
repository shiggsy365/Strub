package com.example.stremiompvplayer.data

import android.content.Context
import com.example.stremiompvplayer.models.Manifest
import com.example.stremiompvplayer.models.CatalogResponse
import com.example.stremiompvplayer.network.StremioApiService
import com.example.stremiompvplayer.utils.SharedPreferencesManager

class CatalogRepository(
    private val apiService: StremioApiService,
    private val context: Context // Pass Context to the Repository
) {
    private val prefsManager = SharedPreferencesManager.getInstance(context)

    // NEW LOGIC: Get the Manifest URL from preferences
    private fun getManifestUrl(): String {
        return prefsManager.getActiveManifestUrl()
            ?: throw IllegalStateException("No active manifest URL configured in settings.")
    }

    // NEW LOGIC: Derive the base URL
    private fun getAddonBaseUrl(manifestUrl: String): String {
        return manifestUrl.substring(0, manifestUrl.lastIndexOf('/'))
    }

    suspend fun fetchManifest(): Result<Manifest> {
        return try {
            val manifestUrl = getManifestUrl() // Get the current URL
            val response = apiService.getManifest(manifestUrl)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(RuntimeException("Manifest fetch failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchCatalogItems(type: String, id: String): Result<CatalogResponse> {
        // FIX: The outer 'return try' was implicitly starting the function.
        // The second 'return try' was causing the syntax error.

        return try { // FIXED: Keep the outer try block
            val manifestUrl = getManifestUrl() // Get current URL
            val addonBaseUrl = getAddonBaseUrl(manifestUrl)
            val catalogURL = "$addonBaseUrl/catalog/$type/$id.json"

            val response = apiService.getCatalogItems(catalogURL)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(RuntimeException("Catalog fetch failed: ${response.code()}"))
            }
        } catch (e: Exception) { // FIXED: Use the single catch block for the function
            Result.failure(e)
        }
    }
}