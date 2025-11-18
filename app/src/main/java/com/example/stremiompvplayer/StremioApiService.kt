package com.example.stremiompvplayer.network

import com.example.stremiompvplayer.models.CatalogResponse
import com.example.stremiompvplayer.models.Manifest
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Url

interface StremioApiService {

    // 1. Fetches the primary manifest (to get catalog IDs and types)
    // NOTE: The full MANIFEST_URL is required, hence using @Url
    @GET
    suspend fun getManifest(@Url manifestUrl: String): Response<Manifest>

    // 2. Fetches the catalog content (the list of posters)
    // Example URL: http://.../catalog/movie/top.json
    // We pass the full path as the URL
    @GET
    suspend fun getCatalogItems(@Url catalogUrl: String): Response<CatalogResponse>
}