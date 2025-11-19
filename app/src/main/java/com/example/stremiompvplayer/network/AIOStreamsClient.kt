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
    private const val USERNAME = "2024e3b8-2d4b-4316-8e99-8e80278e4ec8"
    private const val PASSWORD = "L1ver9001"
    
    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val credentials = Credentials.basic(USERNAME, PASSWORD)
        
        val request = original.newBuilder()
            .header("Authorization", credentials)
            .build()
            
        chain.proceed(request)
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .build()
    
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
    
    val api: AIOStreamsApiService = retrofit.create(AIOStreamsApiService::class.java)
}
