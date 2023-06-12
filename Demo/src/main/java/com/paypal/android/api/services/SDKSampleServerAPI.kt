package com.paypal.android.api.services

import com.google.gson.JsonObject
import com.paypal.android.api.model.ClientId
import com.paypal.android.api.model.CreateOrderRequest
import com.paypal.android.api.model.Order
import com.paypal.android.usecase.UpdateOrderUseCase
import com.paypal.checkout.order.OrderRequest
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

private const val CONNECT_TIMEOUT_IN_SEC = 20L
private const val READ_TIMEOUT_IN_SEC = 30L
private const val WRITE_TIMEOUT_IN_SEC = 30L

// To hardcode an clientId and orderId for this demo app, set the below values:
private val DEFAULT_CLIENT_ID: String? = null // = "your-client-id"
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

        @GET("/client_id")
        suspend fun fetchClientId(): ClientId

        @GET("/orders/{orderId}")
        suspend fun getOrder(@Path("orderId") orderId: String): ResponseBody

        @PATCH("/orders/{orderId}")
        suspend fun patchOrder(
            @Path("orderId") orderId: String,
            @Body body: List<UpdateOrderUseCase.PatchRequestBody>
        ): ResponseBody

        @POST("/orders/{orderId}/capture")
        suspend fun captureOrder(@Path("orderId") orderId: String): ResponseBody

        @POST("/orders/{orderId}/authorize")
        suspend fun authorizeOrder(@Path("orderId") orderId: String): ResponseBody
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

    suspend fun fetchClientId() = DEFAULT_CLIENT_ID ?: service.fetchClientId().value

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

    suspend fun captureOrder(orderId: String): Order {
        val response = service.captureOrder(orderId)
        return parseOrder(JSONObject(response.string()))
    }

    suspend fun authorizeOrder(orderId: String): Order {
        val response = service.authorizeOrder(orderId)
        return parseOrder(JSONObject(response.string()))
    }

    suspend fun getOrder(orderId: String): Order {
        val response = service.getOrder(orderId)
        return parseOrder(JSONObject(response.string()))
    }

    private fun parseOrder(json: JSONObject): Order {
        val cardJSON = json.optJSONObject("payment_source")?.optJSONObject("card")
        val vaultJSON = cardJSON?.optJSONObject("attributes")?.optJSONObject("vault")
        val vaultCustomerJSON = vaultJSON?.optJSONObject("customer")

        return Order(
            id = optNonEmptyString(json, "id"),
            intent = optNonEmptyString(json, "intent"),
            status = optNonEmptyString(json, "status"),
            cardLast4 = optNonEmptyString(cardJSON, "last_digits"),
            cardBrand = optNonEmptyString(cardJSON, "brand"),
            vaultId = optNonEmptyString(vaultJSON, "id"),
            customerId = optNonEmptyString(vaultCustomerJSON, "id")
        )
    }

    private fun optNonEmptyString(json: JSONObject?, key: String): String? = json?.let {
        it.optString(key).ifEmpty {
            null
        }
    }
}
