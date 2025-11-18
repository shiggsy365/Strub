package com.example.stremiompvplayer.data

import com.example.stremiompvplayer.models.Manifest
import com.example.stremiompvplayer.models.CatalogResponse
import com.example.stremiompvplayer.network.StremioApiService

class CatalogRepository(private val apiService: StremioApiService) {

    // IMPORTANT: REPLACE THIS URL with a working add-on URL from your setup
    private val MANIFEST_URL = "YOUR_BASE_ADDON_URL/manifest.json"

    // Base URL is derived by removing the last segment /manifest.json
    private val ADDON_BASE_URL = MANIFEST_URL.substring(0, MANIFEST_URL.lastIndexOf('/'))

    suspend fun fetchManifest(): Result<Manifest> {
        return try {
            val response = apiService.getManifest(MANIFEST_URL)
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
        val catalogURL = "$ADDON_BASE_URL/catalog/$type/$id.json"

        return try {
            val response = apiService.getCatalogItems(catalogURL)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(RuntimeException("Catalog fetch failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}