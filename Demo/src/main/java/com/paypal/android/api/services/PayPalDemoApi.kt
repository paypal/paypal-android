package com.paypal.android.api.services

import com.google.gson.JsonObject
import com.paypal.android.api.model.CreateOrderRequest
import com.paypal.android.api.model.Order
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

interface PayPalDemoApi {

    @POST("/orders")
    suspend fun fetchOrderId(@Body orderRequest: CreateOrderRequest?): Order

    @POST("/orders")
    suspend fun fetchOrderId(@Body jsonObject: JsonObject): Order
}
