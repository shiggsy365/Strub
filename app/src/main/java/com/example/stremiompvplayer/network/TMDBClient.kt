package com.example.stremiompvplayer.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object TMDBClient {

    // Expose the API Key publicly so ViewModels can access it
    const val API_KEY = "a77304e2ead90e385dce678b4e530a40"

    private const val BASE_URL = "https://api.themoviedb.org/3/"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val api: TMDBApiService = retrofit.create(TMDBApiService::class.java)

    fun getBearerToken(accessToken: String): String {
        return "Bearer $accessToken"
    }

    fun getTodaysDate(): String {
        val calendar = java.util.Calendar.getInstance()
        val year = calendar.get(java.util.Calendar.YEAR)
        val month = calendar.get(java.util.Calendar.MONTH) + 1
        val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)
        return String.format("%d-%02d-%02d", year, month, day)
    }
}