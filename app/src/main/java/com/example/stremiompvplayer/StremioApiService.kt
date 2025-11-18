package com.example.stremiompvplayer.network

import com.example.stremiompvplayer.models.CatalogResponse
import com.example.stremiompvplayer.models.Manifest
import com.example.stremiompvplayer.models.StreamResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Url

interface StremioApiService {

    /**
     * Fetches the primary manifest (to get catalog IDs and types)
     * Example URL: http://127.0.0.1:7878/manifest.json
     */
    @GET
    suspend fun getManifest(@Url manifestUrl: String): Response<Manifest>

    /**
     * Fetches the catalog content (the list of posters)
     * Example URL: http://127.0.0.1:7878/catalog/movie/top.json
     */
    @GET
    suspend fun getCatalogItems(@Url catalogUrl: String): Response<CatalogResponse>

    /**
     * Fetches available streams for a specific content item
     * Example URL: http://127.0.0.1:7878/stream/movie/tt1234567.json
     *
     * @param streamUrl The full URL to the stream endpoint
     * @return Response containing StreamResponse with list of available streams
     */
    @GET
    suspend fun getStreams(@Url streamUrl: String): Response<StreamResponse>
}