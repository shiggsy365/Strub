package com.example.stremiompvplayer

import com.example.stremiompvplayer.models.*
import retrofit2.http.GET
import retrofit2.http.Path

interface StremioApiService {
    @GET("catalog/{type}/{id}.json")
    suspend fun getCatalog(
        @Path("type") type: String,
        @Path("id") id: String
    ): CatalogResponse

    @GET("stream/{type}/{id}.json")
    suspend fun getStreams(
        @Path("type") type: String,
        @Path("id") id: String
    ): StreamResponse

    // Added to support fetching Series Details (Seasons/Episodes)
    @GET("meta/{type}/{id}.json")
    suspend fun getMeta(
        @Path("type") type: String,
        @Path("id") id: String
    ): MetaResponse
}
