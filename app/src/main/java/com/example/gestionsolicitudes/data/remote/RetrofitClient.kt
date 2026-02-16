package com.example.gestionsolicitudes.data.remote

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    // Toggle de simulación de error
    // false = normal, true = baseUrl mala para forzar error
    var FORCE_ERROR: Boolean = false

    private const val BASE_URL_OK = "https://jsonplaceholder.typicode.com/"
    private const val BASE_URL_BAD = "https://jsonplaceholder.typicode.com.invalid/" // ❌ intencional

    private fun currentBaseUrl(): String = if (FORCE_ERROR) BASE_URL_BAD else BASE_URL_OK

    private val loggingInterceptor: HttpLoggingInterceptor by lazy {
        HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()
    }

    fun buildApi(): ApiService {
        return Retrofit.Builder()
            .baseUrl(currentBaseUrl())
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    // Getter (se reconstruye cuando cambias FORCE_ERROR)
    val api: ApiService
        get() = buildApi()
}