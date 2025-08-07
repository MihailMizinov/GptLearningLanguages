package com.example.gpt_messenger.api

import android.util.Log
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    private const val BASE_URL = "https://f321-50-7-40-177.ngrok-free.app"


    var api: ChatApi = createApi()
        private set

    private fun createApi(): ChatApi {
        println("Initializing Retrofit with URL: $BASE_URL")
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ChatApi::class.java)
            .also {
                println("ChatApi interface created")
            }
    }

    // Метод для тестов
    fun setTestApi(testApi: ChatApi) {
        api = testApi
    }

    // Метод для тестов
    fun resetToRealApi() {
        api = createApi()
    }
}