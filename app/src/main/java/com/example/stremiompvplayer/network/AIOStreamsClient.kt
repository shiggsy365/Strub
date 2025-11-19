package com.example.stremiompvplayer.network

import com.example.stremiompvplayer.models.StreamResponse
import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

interface AIOStreamsApiService {

    @GET("api/v1/search")
    suspend fun searchMovieStreams(
        @Query("type") type: String = "movie",
        @Query("id") id: String
    ): StreamResponse

    @GET("api/v1/search")
    suspend fun searchSeriesStreams(
        @Query("type") type: String = "series",
        @Query("id") id: String
    ): StreamResponse
}

object AIOStreamsClient {

    private const val BASE_URL = "https://aiostreams.shiggsy.co.uk/"

    // These will be set from SharedPreferences
    private var currentUsername: String? = null
    private var currentPassword: String? = null

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    // Create client dynamically based on current credentials
    private fun createClient(username: String, password: String): OkHttpClient {
        val authInterceptor = Interceptor { chain ->
            val original = chain.request()
            val credentials = Credentials.basic(username, password)

            val request = original.newBuilder()
                .header("Authorization", credentials)
                .build()

            chain.proceed(request)
        }

        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .build()
    }

    private fun createRetrofit(client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    // Get API instance with credentials
    fun getApi(username: String, password: String): AIOStreamsApiService {
        val client = createClient(username, password)
        val retrofit = createRetrofit(client)
        return retrofit.create(AIOStreamsApiService::class.java)
    }

    // Legacy support - uses hardcoded credentials
    val api: AIOStreamsApiService by lazy {
        val client = createClient(
            "2024e3b8-2d4b-4316-8e99-8e80278e4ec8",
            "L1ver9001"
        )
        val retrofit = createRetrofit(client)
        retrofit.create(AIOStreamsApiService::class.java)
    }
}