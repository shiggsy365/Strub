package com.example.stremiompvplayer.data

import android.content.Context
import com.example.stremiompvplayer.network.StremioApiService
import com.example.stremiompvplayer.viewmodels.CatalogViewModelFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.example.stremiompvplayer.viewmodels.CatalogViewModel // Note: CatalogViewModel is inferred, but import is good

object ServiceLocator {

    private const val BASE_URL = "https://api.strem.io/"

    // Moshi instance setup
    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    // Retrofit instance setup
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    // API Service instance setup
    private val apiService: StremioApiService by lazy {
        retrofit.create(StremioApiService::class.java)
    }

    /**
     * Provides the ViewModel Factory for fragments.
     * Requires Context to instantiate the CatalogRepository.
     */
    fun provideCatalogViewModelFactory(context: Context): CatalogViewModelFactory {

        // 1. Instantiate the repository using the application context
        val repository = CatalogRepository(
            apiService,
            context.applicationContext // Repository requires Context
        )

        // 2. Pass BOTH the repository AND the context to the factory constructor
        // FIX: Add the context as the second parameter here.
        return CatalogViewModelFactory(
            repository,
            context.applicationContext // <--- ADDED THE MISSING CONTEXT PARAMETER
        )
    }
}