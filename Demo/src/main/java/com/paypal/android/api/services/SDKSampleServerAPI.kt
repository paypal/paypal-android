package com.paypal.android.api.services

import com.google.gson.JsonObject
import com.paypal.android.api.model.AccessToken
import com.paypal.android.api.model.CreateOrderRequest
import com.paypal.android.api.model.Order
import com.paypal.android.usecase.UpdateOrderUseCase
import com.paypal.checkout.order.OrderRequest
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

private const val CONNECT_TIMEOUT_IN_SEC = 20L
private const val READ_TIMEOUT_IN_SEC = 30L
private const val WRITE_TIMEOUT_IN_SEC = 30L

// To hardcode an access token and order ID for this demo app, set the below values:
private val DEFAULT_ACCESS_TOKEN: String? = null // = "your-token"
private val DEFAULT_ORDER_ID: String? = null // = "your-order-id"

class SDKSampleServerAPI(baseUrl: String) {

    @JvmSuppressWildcards
    interface RetrofitService {

        @POST("/orders")
        suspend fun createOrder(@Body orderRequest: CreateOrderRequest): Order

        @POST("/orders")
        suspend fun createOrder(@Body jsonObject: JsonObject): Order

        @POST("/orders")
        suspend fun createOrder(@Body order: OrderRequest): Order

        @POST("/access_tokens")
        suspend fun fetchAccessToken(): AccessToken

        @PATCH("/orders/{orderID}")
        suspend fun patchOrder(
            @Path("orderID") orderId: String,
            @Body body: List<UpdateOrderUseCase.PatchRequestBody>
        ): ResponseBody
    }

    private val service: RetrofitService

    init {

        val okHttpBuilder = OkHttpClient.Builder()
        val httpLoggingInterceptor = HttpLoggingInterceptor()
        httpLoggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
        // Timeouts
        okHttpBuilder
            .connectTimeout(CONNECT_TIMEOUT_IN_SEC, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_IN_SEC, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT_IN_SEC, TimeUnit.SECONDS)
        okHttpBuilder.addInterceptor(httpLoggingInterceptor)
        val okHttpClient = okHttpBuilder.build()

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        service = retrofit.create(RetrofitService::class.java)
    }

    suspend fun fetchAccessToken() = DEFAULT_ACCESS_TOKEN?.let {
        AccessToken(it)
    } ?: service.fetchAccessToken()

    suspend fun createOrder(orderRequest: CreateOrderRequest): Order = DEFAULT_ORDER_ID?.let {
        Order(it, "CREATED")
    } ?: service.createOrder(orderRequest)

    suspend fun createOrder(jsonObject: JsonObject) = DEFAULT_ORDER_ID?.let {
        Order(it, "CREATED")
    } ?: service.createOrder(jsonObject)

    suspend fun createOrder(orderRequest: OrderRequest): Order = DEFAULT_ORDER_ID?.let {
        Order(it, "CREATED")
    } ?: service.createOrder(orderRequest)

    suspend fun patchOrder(orderId: String, body: List<UpdateOrderUseCase.PatchRequestBody>) =
        service.patchOrder(orderId, body)
}