package com.example.stremiompvplayer.data

import android.content.Context
import android.util.Log
import com.example.stremiompvplayer.models.Manifest
import com.example.stremiompvplayer.models.CatalogResponse
import com.example.stremiompvplayer.models.StreamResponse
import com.example.stremiompvplayer.network.StremioApiService
import com.example.stremiompvplayer.utils.SharedPreferencesManager

class CatalogRepository(
    private val apiService: StremioApiService,
    private val context: Context
) {
    private val prefsManager = SharedPreferencesManager.getInstance(context)

    // Get the Manifest URL from preferences
    private fun getManifestUrl(): String {
        return prefsManager.getActiveManifestUrl()
            ?: throw IllegalStateException("No active manifest URL configured in settings.")
    }

    // Derive the base URL from the manifest URL
    private fun getAddonBaseUrl(manifestUrl: String): String {
        return manifestUrl.substring(0, manifestUrl.lastIndexOf('/'))
    }

    suspend fun fetchManifest(): Result<Manifest> {
        return try {
            val manifestUrl = getManifestUrl()
            Log.d("CatalogRepository", "Fetching manifest from: $manifestUrl")

            val response = apiService.getManifest(manifestUrl)
            if (response.isSuccessful && response.body() != null) {
                Log.d("CatalogRepository", "Manifest fetched successfully")
                Result.success(response.body()!!)
            } else {
                Log.e("CatalogRepository", "Manifest fetch failed: ${response.code()}")
                Result.failure(RuntimeException("Manifest fetch failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("CatalogRepository", "Exception fetching manifest", e)
            Result.failure(e)
        }
    }

    suspend fun fetchCatalogItems(type: String, id: String): Result<CatalogResponse> {
        return try {
            val manifestUrl = getManifestUrl()
            val addonBaseUrl = getAddonBaseUrl(manifestUrl)

            // Construct catalog URL following Stremio protocol
            val catalogURL = "$addonBaseUrl/catalog/$type/$id.json"
            Log.d("CatalogRepository", "Fetching catalog from: $catalogURL")

            val response = apiService.getCatalogItems(catalogURL)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                Log.d("CatalogRepository", "Catalog fetched successfully: ${body.metas.size} items")
                Result.success(body)
            } else {
                Log.e("CatalogRepository", "Catalog fetch failed: ${response.code()}")
                Result.failure(RuntimeException("Catalog fetch failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("CatalogRepository", "Exception fetching catalog", e)
            Result.failure(e)
        }
    }

    suspend fun fetchStreams(type: String, id: String): Result<StreamResponse> {
        return try {
            val manifestUrl = getManifestUrl()
            val addonBaseUrl = getAddonBaseUrl(manifestUrl)

            // Construct stream URL following Stremio protocol
            // Pattern: {addonBaseUrl}/stream/{type}/{id}.json
            val streamURL = "$addonBaseUrl/stream/$type/$id.json"
            Log.d("CatalogRepository", "Fetching streams from: $streamURL")

            val response = apiService.getStreams(streamURL)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                Log.d("CatalogRepository", "Streams fetched successfully: ${body.streams.size} streams")
                Result.success(body)
            } else {
                Log.e("CatalogRepository", "Streams fetch failed: ${response.code()}")
                Result.failure(RuntimeException("Streams fetch failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("CatalogRepository", "Exception fetching streams", e)
            Result.failure(e)
        }
    }
}