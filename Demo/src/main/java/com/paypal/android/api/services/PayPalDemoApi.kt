package com.paypal.android.api.services

import com.paypal.android.api.model.CreateOrderRequest
import com.paypal.android.api.model.Order
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

interface PayPalDemoApi {

    @POST("/order")
    suspend fun fetchOrderId(
        @Query("countryCode") countryCode: String,
        @Body orderRequest: CreateOrderRequest?
    ): Order
}
