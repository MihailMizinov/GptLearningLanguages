package com.example.gpt_messenger

import com.example.gpt_messenger.api.ApiClient
import com.example.gpt_messenger.api.ChatApi
import com.example.gpt_messenger.api.ChatRequest
import com.example.gpt_messenger.api.ChatResponse
import com.example.gpt_messenger.uzi.sendMessageToApi
import org.junit.After
import org.junit.Test
import org.junit.Assert.*
import org.mockito.kotlin.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ChatApiUnitTest {

    @After
    fun tearDown() {

        ApiClient.resetToRealApi()
    }

    @Test
    fun testSendMessageToApi_Success() {

        val mockApi = mock<ChatApi>()
        val mockCall = mock<Call<ChatResponse>>()


        whenever(mockApi.sendMessage(any())).thenReturn(mockCall)

        doAnswer { invocation ->
            val callback = invocation.getArgument<Callback<ChatResponse>>(0)
            callback.onResponse(mockCall, Response.success(ChatResponse("Test response")))
            null
        }.whenever(mockCall).enqueue(any())


        ApiClient.setTestApi(mockApi)


        var result = ""
        sendMessageToApi("Hello") { response ->
            result = response
        }


        assertTrue(result.isNotEmpty())
        assertEquals("Test response", result)


        verify(mockApi).sendMessage(ChatRequest(message = "Hello", user_id = "mobile_user"))
    }

    @Test
    fun testSendMessageToApi_AnyResponseReturnsTrue() {

        val mockApi = mock<ChatApi>()
        val mockCall = mock<Call<ChatResponse>>()


        whenever(mockApi.sendMessage(any())).thenReturn(mockCall)

        doAnswer { invocation ->
            val callback = invocation.getArgument<Callback<ChatResponse>>(0)
            callback.onResponse(mockCall, Response.success(ChatResponse("Any response")))
            null
        }.whenever(mockCall).enqueue(any())


        ApiClient.setTestApi(mockApi)


        var receivedResponse = false
        sendMessageToApi("Any message") { response ->
            receivedResponse = response.isNotEmpty()
        }


        assertTrue(receivedResponse)
    }
}