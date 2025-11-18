package com.example.stremiompvplayer.network

import com.example.stremiompvplayer.models.CatalogResponse
import com.example.stremiompvplayer.models.FeedList
import com.example.stremiompvplayer.models.Manifest
import com.example.stremiompvplayer.models.MetaResponse
import com.example.stremiompvplayer.models.StreamResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Url

object StremioClient {

    private const val BASE_URL = "https://api.strem.io"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val api: StremioApi = retrofit.create(StremioApi::class.java)

    interface StremioApi {

        // Existing functions
        @GET("/api/feed?authKey={authKey}")
        suspend fun getFeed(@Path("authKey") authKey: String): List<FeedList>

        @GET("/api/meta?authKey={authKey}&type={type}&id={id}")
        suspend fun getMeta(
            @Path("authKey") authKey: String,
            @Path("type") type: String,
            @Path("id") id: String
        ): MetaResponse

        @GET("/api/streams?authKey={authKey}&type={type}&id={id}")
        suspend fun getStreams(
            @Path("authKey") authKey: String,
            @Path("type") type: String,
            @Path("id") id: String
        ): StreamResponse

        @GET("/api/search?authKey={authKey}&query={query}")
        suspend fun search(
            @Path("authKey") authKey: String,
            @Path("query") query: String
        ): List<FeedList>


        // --- NEW FUNCTIONS ---
        // Based on the logic from index.html to fetch directly from an add-on URL

        /**
         * Fetches the manifest.json from a specific add-on.
         * @param url The full URL to the add-on's manifest (e.g., "http://127.0.0.1:7878/manifest.json")
         */
        @GET
        suspend fun getManifest(@Url url: String): Manifest

        /**
         * Fetches a specific catalog from an add-on.
         * @param url The full URL to the catalog (e.g., "http://127.0.0.1:7878/catalog/movie/top.json")
         */
        @GET
        suspend fun getCatalog(@Url url: String): CatalogResponse
    }
}