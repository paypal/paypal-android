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

class SDKSampleServerAPI {

    companion object {
        // TODO: - require Merchant enum to be specified via UI layer
        val SELECTED_MERCHANT_INTEGRATION = MerchantIntegration.DEFAULT
    }

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

        @PATCH("/orders/{orderId}")
        suspend fun patchOrder(
            @Path("orderId") orderId: String,
            @Body body: List<UpdateOrderUseCase.PatchRequestBody>
        ): ResponseBody

        @POST("/orders/{orderId}/capture")
        suspend fun captureOrder(@Path("orderId") orderId: String): Order

        @POST("/orders/{orderId}/authorize")
        suspend fun authorizeOrder(@Path("orderId") orderId: String): Order
    }

    private val serviceMap: Map<MerchantIntegration, RetrofitService>

    init {
        val serviceMap = mutableMapOf<MerchantIntegration, RetrofitService>()
        val allMerchantIntegrations = MerchantIntegration.values()
        for (merchant in allMerchantIntegrations) {
            serviceMap[merchant] = createService(merchant.baseUrl)
        }
        this.serviceMap = serviceMap
    }

    private fun createService(baseUrl: String): RetrofitService {
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
        return retrofit.create(RetrofitService::class.java)
    }

    private fun findService(merchantIntegration: MerchantIntegration) = serviceMap[merchantIntegration]
        ?: throw AssertionError("Couldn't find retrofit service for ${merchantIntegration.name}")

    suspend fun fetchClientId(merchantIntegration: MerchantIntegration = SELECTED_MERCHANT_INTEGRATION) =
        DEFAULT_CLIENT_ID ?: findService(merchantIntegration).fetchClientId().value

    suspend fun createOrder(
        orderRequest: CreateOrderRequest,
        merchantIntegration: MerchantIntegration = SELECTED_MERCHANT_INTEGRATION
    ): Order = DEFAULT_ORDER_ID?.let {
        Order(it, "CREATED")
    } ?: findService(merchantIntegration).createOrder(orderRequest)

    suspend fun createOrder(jsonObject: JsonObject, merchantIntegration: MerchantIntegration = SELECTED_MERCHANT_INTEGRATION) =
        DEFAULT_ORDER_ID?.let {
            Order(it, "CREATED")
        } ?: findService(merchantIntegration).createOrder(jsonObject)

    suspend fun createOrder(
        orderRequest: OrderRequest,
        merchantIntegration: MerchantIntegration = SELECTED_MERCHANT_INTEGRATION
    ): Order = DEFAULT_ORDER_ID?.let {
        Order(it, "CREATED")
    } ?: findService(merchantIntegration).createOrder(orderRequest)

    suspend fun patchOrder(
        orderId: String,
        body: List<UpdateOrderUseCase.PatchRequestBody>,
        merchantIntegration: MerchantIntegration = SELECTED_MERCHANT_INTEGRATION
    ) = findService(merchantIntegration).patchOrder(orderId, body)

    suspend fun captureOrder(orderId: String, merchantIntegration: MerchantIntegration = SELECTED_MERCHANT_INTEGRATION) =
        findService(merchantIntegration).captureOrder(orderId)

    suspend fun authorizeOrder(orderId: String, merchantIntegration: MerchantIntegration = SELECTED_MERCHANT_INTEGRATION) =
        findService(merchantIntegration).authorizeOrder(orderId)
}
