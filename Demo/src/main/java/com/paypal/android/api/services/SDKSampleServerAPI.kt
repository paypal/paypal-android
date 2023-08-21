package com.paypal.android.api.services

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.paypal.android.api.model.ClientId
import com.paypal.android.api.model.CreateOrderRequest
import com.paypal.android.api.model.Order
import com.paypal.android.cardpayments.OrderIntent
import com.paypal.android.usecase.UpdateOrderUseCase
import com.paypal.checkout.order.OrderRequest
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
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

// TODO: consider refactoring each method into a "use case"
// Ref: https://developer.android.com/topic/architecture/domain-layer#use-cases-kotlin
@Suppress("TooManyFunctions")
class SDKSampleServerAPI {

    companion object {
        // TODO: - require Merchant enum to be specified via UI layer
        val SELECTED_MERCHANT_INTEGRATION = MerchantIntegration.DEFAULT

        private fun optNonEmptyString(json: JSONObject?, key: String): String? = json?.let {
            it.optString(key).ifEmpty {
                null
            }
        }
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

        @GET("/orders/{orderId}")
        suspend fun getOrder(@Path("orderId") orderId: String): ResponseBody

        @PATCH("/orders/{orderId}")
        suspend fun patchOrder(
            @Path("orderId") orderId: String,
            @Body body: List<UpdateOrderUseCase.PatchRequestBody>
        ): ResponseBody

        @POST("/orders/{orderId}/capture")
        suspend fun captureOrder(
            @Path("orderId") orderId: String,
            @Header("PayPal-Client-Metadata-Id") payPalClientMetadataId: String?
        ): ResponseBody

        @POST("/orders/{orderId}/authorize")
        suspend fun authorizeOrder(
            @Path("orderId") orderId: String,
            @Header("PayPal-Client-Metadata-Id") payPalClientMetadataId: String?
        ): ResponseBody

        @POST("/setup_tokens")
        suspend fun createSetupToken(@Body jsonObject: JsonObject): ResponseBody

        @POST("/payment_tokens")
        suspend fun createPaymentToken(@Body jsonObject: JsonObject): ResponseBody
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

    private fun findService(merchantIntegration: MerchantIntegration) =
        serviceMap[merchantIntegration]
            ?: throw AssertionError("Couldn't find retrofit service for ${merchantIntegration.name}")

    suspend fun fetchClientId(merchantIntegration: MerchantIntegration = SELECTED_MERCHANT_INTEGRATION) =
        DEFAULT_CLIENT_ID ?: findService(merchantIntegration).fetchClientId().value

    suspend fun createOrder(
        orderRequest: CreateOrderRequest,
        merchantIntegration: MerchantIntegration = SELECTED_MERCHANT_INTEGRATION
    ): Order = DEFAULT_ORDER_ID?.let {
        Order(it, "CREATED")
    } ?: findService(merchantIntegration).createOrder(orderRequest)

    suspend fun createOrder(
        orderRequest: JSONObject,
        merchantIntegration: MerchantIntegration = SELECTED_MERCHANT_INTEGRATION
    ): Order {
        if (DEFAULT_ORDER_ID != null) {
            return Order(DEFAULT_ORDER_ID, "CREATED")
        }

        val body = JsonParser.parseString(orderRequest.toString()) as JsonObject
        return findService(merchantIntegration).createOrder(body)
    }

    suspend fun createOrder(
        orderRequest: OrderRequest,
        merchantIntegration: MerchantIntegration = SELECTED_MERCHANT_INTEGRATION
    ): Order = DEFAULT_ORDER_ID?.let {
        Order(it, "CREATED")
    } ?: findService(merchantIntegration).createOrder(orderRequest)

    suspend fun createOrder(
        orderIntent: OrderIntent,
        shouldVault: Boolean,
        vaultCustomerId: String,
        merchantIntegration: MerchantIntegration = SELECTED_MERCHANT_INTEGRATION
    ): Order {
        val orderRequest = parseOrderRequestJSON(orderIntent, shouldVault, vaultCustomerId)
        val body = JsonParser.parseString(orderRequest.toString()) as JsonObject
        return findService(merchantIntegration).createOrder(body)
    }

    suspend fun patchOrder(
        orderId: String,
        body: List<UpdateOrderUseCase.PatchRequestBody>,
        merchantIntegration: MerchantIntegration = SELECTED_MERCHANT_INTEGRATION
    ) = findService(merchantIntegration).patchOrder(orderId, body)

    suspend fun captureOrder(
        orderId: String,
        payPalClientMetadataId: String? = null,
        merchantIntegration: MerchantIntegration = SELECTED_MERCHANT_INTEGRATION
    ): Order {
        val response =
            findService(merchantIntegration).captureOrder(orderId, payPalClientMetadataId)
        return parseOrder(JSONObject(response.string()))
    }

    suspend fun authorizeOrder(
        orderId: String,
        payPalClientMetadataId: String? = null,
        merchantIntegration: MerchantIntegration = SELECTED_MERCHANT_INTEGRATION
    ): Order {
        val response =
            findService(merchantIntegration).authorizeOrder(orderId, payPalClientMetadataId)
        return parseOrder(JSONObject(response.string()))
    }

    suspend fun getOrder(
        orderId: String,
        merchantIntegration: MerchantIntegration = SELECTED_MERCHANT_INTEGRATION
    ): Order {
        val response = findService(merchantIntegration).getOrder(orderId)
        return parseOrder(JSONObject(response.string()))
    }

    suspend fun createSetupToken(
        jsonObject: JsonObject,
        merchantIntegration: MerchantIntegration = SELECTED_MERCHANT_INTEGRATION
    ) = findService(merchantIntegration).createSetupToken(jsonObject)

    suspend fun createPaymentToken(
        jsonObject: JsonObject,
        merchantIntegration: MerchantIntegration = SELECTED_MERCHANT_INTEGRATION
    ) = findService(merchantIntegration).createPaymentToken(jsonObject)

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

    private fun parseOrderRequestJSON(
        orderIntent: OrderIntent,
        shouldVault: Boolean,
        vaultCustomerId: String
    ): JSONObject {
        val amountJSON = JSONObject()
            .put("currency_code", "USD")
            .put("value", "10.99")

        val purchaseUnitJSON = JSONObject()
            .put("amount", amountJSON)

        val orderRequest = JSONObject()
            .put("intent", orderIntent)
            .put("purchase_units", JSONArray().put(purchaseUnitJSON))

        if (shouldVault) {
            val vaultJSON = JSONObject()
                .put("store_in_vault", "ON_SUCCESS")

            val cardAttributesJSON = JSONObject()
                .put("vault", vaultJSON)

            if (vaultCustomerId.isNotEmpty()) {
                val customerJSON = JSONObject()
                    .put("id", vaultCustomerId)
                cardAttributesJSON.put("customer", customerJSON)
            }

            val cardJSON = JSONObject()
                .put("attributes", cardAttributesJSON)

            val paymentSourceJSON = JSONObject()
                .put("card", cardJSON)

            orderRequest.put("payment_source", paymentSourceJSON)
        }
        return orderRequest
    }
}
