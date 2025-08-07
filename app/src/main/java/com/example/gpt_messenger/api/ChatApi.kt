package com.example.gpt_messenger.api

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

data class ChatRequest(val message: String, val user_id: String = "mobile_user")
data class ChatResponse(val response: String)

interface ChatApi {
    @POST("chat")
    fun sendMessage(@Body request: ChatRequest): Call<ChatResponse>
}