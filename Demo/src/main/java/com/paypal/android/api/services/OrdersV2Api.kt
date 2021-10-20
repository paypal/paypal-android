package com.paypal.android.api.services

import com.google.gson.JsonObject
import com.paypal.android.api.model.Order
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface OrdersV2Api {

    @Headers("Accept: application/json")
    @POST("/v2/checkout/orders")
    suspend fun postCheckoutOrder(
        @Body jsonObject: JsonObject
    ): Order
}