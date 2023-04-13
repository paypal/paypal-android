package com.paypal.android.api.services

import com.google.gson.JsonObject
import com.paypal.android.api.model.AccessToken
import com.paypal.android.api.model.CreateOrderRequest
import com.paypal.android.api.model.Order
import com.paypal.android.usecase.UpdateOrderUseCase
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

@JvmSuppressWildcards
interface SDKSampleServerApi {

    @POST("/orders")
    suspend fun createOrder(@Body orderRequest: CreateOrderRequest?): Order

    @POST("/orders")
    suspend fun createOrder(@Body jsonObject: JsonObject): Order

    @POST("/orders")
    suspend fun createOrder(@Body order: com.paypal.checkout.order.Order): Order

    @POST("/access_tokens")
    suspend fun fetchAccessToken(): AccessToken

    @PATCH("/orders/{orderID}")
    suspend fun patchOrder(@Path("orderID") orderId: String, @Body body: List<UpdateOrderUseCase.PatchRequestBody>): ResponseBody
}
