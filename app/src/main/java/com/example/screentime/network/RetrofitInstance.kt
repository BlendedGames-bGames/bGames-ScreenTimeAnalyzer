package com.example.screentime.network

import com.example.screentime.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {
    private val retrofit1 by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL_USER_API)
            .addConverterFactory(GsonConverterFactory.create())
            .client(OkHttpClient.Builder().build())
            .build()
    }
    private val retrofit2 by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL_GET_API)
            .addConverterFactory(GsonConverterFactory.create())
            .client(OkHttpClient.Builder().build())
            .build()
    }
    private val retrofit3 by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL_POST_API)
            .addConverterFactory(GsonConverterFactory.create())
            .client(OkHttpClient.Builder().build())
            .build()
    }

    fun userApi(): ApiService = retrofit1.create(ApiService::class.java)
    fun getApi(): ApiService = retrofit2.create(ApiService::class.java)
    fun postApi(): ApiService = retrofit3.create(ApiService::class.java)
}