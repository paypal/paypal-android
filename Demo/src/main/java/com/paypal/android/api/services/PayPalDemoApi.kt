package com.paypal.android.api.services

import com.google.gson.JsonObject
import com.paypal.android.api.model.AccessToken
import com.paypal.android.api.model.CreateOrderRequest
import com.paypal.android.api.model.Order
import retrofit2.http.Body
import retrofit2.http.POST

interface PayPalDemoApi {

    @POST("/orders")
    suspend fun createOrder(@Body orderRequest: CreateOrderRequest?): Order

    @POST("/orders")
    suspend fun createOrder(@Body jsonObject: JsonObject): Order

    @POST("/access_tokens")
    suspend fun fetchAccessToken(): AccessToken
}
