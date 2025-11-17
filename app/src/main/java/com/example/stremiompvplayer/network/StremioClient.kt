package com.example.stremiompvplayer.network

import com.example.stremiompvplayer.models.*
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class StremioClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val gson = Gson()
    
    suspend fun getManifest(addonUrl: String): AddonManifest? = withContext(Dispatchers.IO) {
        try {
            val url = normalizeAddonUrl(addonUrl) + "/manifest.json"
            val request = Request.Builder()
                .url(url)
                .build()
            
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val json = response.body?.string()
                gson.fromJson(json, AddonManifest::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    suspend fun getCatalog(
        addonUrl: String,
        type: String,
        catalogId: String,
        extra: Map<String, String> = emptyMap()
    ): CatalogResponse? = withContext(Dispatchers.IO) {
        try {
            val extraParams = if (extra.isEmpty()) "" else {
                "/" + extra.entries.joinToString("/") { "${it.key}=${it.value}" }
            }
            val url = normalizeAddonUrl(addonUrl) + 
                      "/catalog/$type/$catalogId$extraParams.json"
            
            val request = Request.Builder()
                .url(url)
                .build()
            
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val json = response.body?.string()
                gson.fromJson(json, CatalogResponse::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    suspend fun getMeta(
        addonUrl: String,
        type: String,
        id: String
    ): MetaDetail? = withContext(Dispatchers.IO) {
        try {
            val url = normalizeAddonUrl(addonUrl) + "/meta/$type/$id.json"
            
            val request = Request.Builder()
                .url(url)
                .build()
            
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val json = response.body?.string()
                val metaResponse = gson.fromJson(json, Map::class.java)
                gson.fromJson(gson.toJson(metaResponse["meta"]), MetaDetail::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    suspend fun getStreams(
        addonUrl: String,
        type: String,
        id: String
    ): List<Stream> = withContext(Dispatchers.IO) {
        try {
            val url = normalizeAddonUrl(addonUrl) + "/stream/$type/$id.json"
            
            val request = Request.Builder()
                .url(url)
                .build()
            
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val json = response.body?.string()
                val streamsResponse = gson.fromJson(json, StreamsResponse::class.java)
                streamsResponse.streams.filter { stream ->
                    // Filter for direct URL streams that MPV can play
                    !stream.url.isNullOrEmpty() || !stream.externalUrl.isNullOrEmpty()
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    private fun normalizeAddonUrl(url: String): String {
        return url.trimEnd('/')
    }
}
