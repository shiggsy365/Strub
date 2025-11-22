package com.example.stremiompvplayer.network

import com.example.stremiompvplayer.models.Stream
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// Response wrappers based on AIOStreams Wiki
data class AIOStreamsResponse(
    val success: Boolean,
    val data: AIOStreamsData?
)

data class AIOStreamsData(
    val results: List<Stream>
)

interface AIOStreamsApiService {

    @GET("api/v1/search")
    suspend fun searchStreams(
        @Query("type") type: String,
        @Query("id") id: String
    ): AIOStreamsResponse
}

object AIOStreamsClient {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private fun createClient(username: String, password: String): OkHttpClient {
        val authInterceptor = Interceptor { chain ->
            val original = chain.request()
            // Wiki: "Authorization: Basic <base64(uuid:password)>"
            val credentials = Credentials.basic(username, password)

            val request = original.newBuilder()
                .header("Authorization", credentials)
                .build()

            chain.proceed(request)
        }

        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS) // Increased timeout for searches
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private fun createRetrofit(baseUrl: String, client: OkHttpClient): Retrofit {
        // Ensure base URL ends with /
        val normalizedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        
        return Retrofit.Builder()
            .baseUrl(normalizedUrl)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    fun getApi(baseUrl: String, username: String, password: String): AIOStreamsApiService {
        val client = createClient(username, password)
        val retrofit = createRetrofit(baseUrl, client)
        return retrofit.create(AIOStreamsApiService::class.java)
    }
}
