package com.example.stremiompvplayer.data

import android.content.Context
import com.example.stremiompvplayer.network.StremioApiService
import com.example.stremiompvplayer.viewmodels.CatalogViewModelFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import com.example.stremiompvplayer.viewmodels.CatalogViewModel
import retrofit2.converter.moshi.MoshiConverterFactory


object ServiceLocator {
    // This URL will be used to initialize Retrofit, though the manifest call overrides it.
    private const val BASE_URL = "https://api.strem.io/"

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory()) // Essential for Kotlin data classes
            .build()
    }

    private val retrofit by lazy {
        Retrofit.Builder()
            // Using a placeholder BASE_URL. The @GET @Url in the service will override this.
            .baseUrl(BASE_URL)

            // 2. Use the Moshi instance with the Converter Factory
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    private val apiService by lazy {
        retrofit.create(StremioApiService::class.java)
    }

    private val repository by lazy {
        CatalogRepository(apiService)
    }

    fun provideCatalogViewModelFactory(): CatalogViewModelFactory {
        return CatalogViewModelFactory(repository)
    }
}