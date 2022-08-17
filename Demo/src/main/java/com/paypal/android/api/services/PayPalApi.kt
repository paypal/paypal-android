package com.paypal.android.api.services

import com.google.gson.JsonObject
import com.paypal.android.api.model.Order
import com.paypal.android.data.vault.VaultSessionId
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

interface PayPalApi {

    @Headers("Accept: application/json")
    @POST("/v2/checkout/orders")
    suspend fun postCheckoutOrder(
        @Body jsonObject: JsonObject
    ): Order

    @Headers("Accept: application/json")
    @POST("/v2/vault/payment-tokens")
    suspend fun postApprovalSessionId(
        @Header("Authorization") accessToken: String,
        @Body jsonObject: JsonObject
    ): VaultSessionId
}
