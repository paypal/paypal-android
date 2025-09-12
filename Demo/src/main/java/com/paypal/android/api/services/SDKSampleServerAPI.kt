package com.paypal.android.api.services

import com.paypal.android.api.model.Order
import com.paypal.android.api.model.PayPalSetupToken
import com.paypal.android.api.model.serialization.CardSetupRequest
import com.paypal.android.api.model.serialization.OrderRequestBody
import com.paypal.android.api.model.serialization.OrderResponse
import com.paypal.android.api.model.serialization.PayPalSetupRequestBody
import com.paypal.android.api.model.serialization.PaymentTokenResponse
import com.paypal.android.api.model.serialization.SetupTokenResponse
import com.paypal.android.api.model.serialization.TokenRequest
import com.paypal.android.api.model.serialization.toCardPaymentToken
import com.paypal.android.api.model.serialization.toCardSetupToken
import com.paypal.android.api.model.serialization.toOrder
import com.paypal.android.api.model.serialization.toPayPalPaymentToken
import com.paypal.android.models.OrderRequest
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

private const val CONNECT_TIMEOUT_IN_SEC = 20L
private const val READ_TIMEOUT_IN_SEC = 30L
private const val WRITE_TIMEOUT_IN_SEC = 30L

// To hardcode an orderId for this demo app, set the below value
private val DEFAULT_ORDER_ID: String? = null // = "your-order-id"

// TODO: consider refactoring each method into a "use case"
// Ref: https://developer.android.com/topic/architecture/domain-layer#use-cases-kotlin
@Suppress("TooManyFunctions")
class SDKSampleServerAPI {

    companion object {
        // TODO: - require Merchant enum to be specified via UI layer
        val SELECTED_MERCHANT_INTEGRATION = MerchantIntegration.DEFAULT

        val clientId: String
            get() = SELECTED_MERCHANT_INTEGRATION.clientId
    }

    @JvmSuppressWildcards
    interface RetrofitService {

        @POST("/orders")
        suspend fun createOrder(@Body orderRequestBody: OrderRequestBody): Order

        @POST("/orders")
        suspend fun createOrder(@Body order: OrderRequest): Order

        @POST("/orders/{orderId}/capture")
        suspend fun captureOrder(
            @Path("orderId") orderId: String,
            @Header("PayPal-Client-Metadata-Id") payPalClientMetadataId: String?
        ): OrderResponse

        @POST("/orders/{orderId}/authorize")
        suspend fun authorizeOrder(
            @Path("orderId") orderId: String,
            @Header("PayPal-Client-Metadata-Id") payPalClientMetadataId: String?
        ): OrderResponse

        @POST("/setup-tokens")
        suspend fun createSetupToken(@Body setupRequest: CardSetupRequest): SetupTokenResponse

        @POST("/setup-tokens")
        suspend fun createPayPalSetupToken(@Body setupRequest: PayPalSetupRequestBody): SetupTokenResponse

        @POST("/payment-tokens")
        suspend fun createPaymentToken(@Body tokenRequest: TokenRequest): PaymentTokenResponse

        @GET("/setup-tokens/{setupTokenId}")
        suspend fun getSetupToken(
            @Path("setupTokenId") setupTokenId: String,
        ): SetupTokenResponse
    }

    private val serviceMap: Map<MerchantIntegration, RetrofitService>

    init {
        val serviceMap = mutableMapOf<MerchantIntegration, RetrofitService>()
        val allMerchantIntegrations = MerchantIntegration.entries.toTypedArray()
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
            .addConverterFactory(KotlinSerializationConverterFactory.create())
            .build()
        return retrofit.create(RetrofitService::class.java)
    }

    private fun findService(merchantIntegration: MerchantIntegration) =
        serviceMap[merchantIntegration]
            ?: throw AssertionError("Couldn't find retrofit service for ${merchantIntegration.name}")

    suspend fun createOrder(
        orderRequestBody: OrderRequestBody,
        merchantIntegration: MerchantIntegration = SELECTED_MERCHANT_INTEGRATION
    ) = safeApiCall {
        if (DEFAULT_ORDER_ID != null) {
            Order(DEFAULT_ORDER_ID, "CREATED")
        } else {
            findService(merchantIntegration).createOrder(orderRequestBody)
        }
    }

    suspend fun createOrder(
        orderRequest: OrderRequest,
        merchantIntegration: MerchantIntegration = SELECTED_MERCHANT_INTEGRATION
    ) = safeApiCall {
        DEFAULT_ORDER_ID?.let {
            Order(it, "CREATED")
        } ?: findService(merchantIntegration).createOrder(orderRequest)
    }

    suspend fun captureOrder(
        orderId: String,
        payPalClientMetadataId: String? = null,
        merchantIntegration: MerchantIntegration = SELECTED_MERCHANT_INTEGRATION
    ) = safeApiCall {
        val orderResponse =
            findService(merchantIntegration).captureOrder(orderId, payPalClientMetadataId)
        orderResponse.toOrder()
    }

    suspend fun authorizeOrder(
        orderId: String,
        payPalClientMetadataId: String? = null,
        merchantIntegration: MerchantIntegration = SELECTED_MERCHANT_INTEGRATION
    ) = safeApiCall {
        val orderResponse =
            findService(merchantIntegration).authorizeOrder(orderId, payPalClientMetadataId)
        orderResponse.toOrder()
    }

    suspend fun createSetupToken(
        setupRequest: CardSetupRequest,
        merchantIntegration: MerchantIntegration = SELECTED_MERCHANT_INTEGRATION
    ) = safeApiCall {
        val setupTokenResponse = findService(merchantIntegration).createSetupToken(setupRequest)
        setupTokenResponse.toCardSetupToken()
    }

    suspend fun getSetupToken(
        setupTokenId: String,
        merchantIntegration: MerchantIntegration = SELECTED_MERCHANT_INTEGRATION
    ) = safeApiCall {
        val setupTokenResponse = findService(merchantIntegration).getSetupToken(setupTokenId)
        setupTokenResponse.toCardSetupToken()
    }

    suspend fun createPaymentToken(
        tokenRequest: TokenRequest,
        merchantIntegration: MerchantIntegration = SELECTED_MERCHANT_INTEGRATION
    ) = safeApiCall {
        val paymentTokenResponse = findService(merchantIntegration).createPaymentToken(tokenRequest)
        paymentTokenResponse.toCardPaymentToken()
    }

    suspend fun createPayPalPaymentToken(
        tokenRequest: TokenRequest,
        merchantIntegration: MerchantIntegration = SELECTED_MERCHANT_INTEGRATION
    ) = safeApiCall {
        val paymentTokenResponse = findService(merchantIntegration).createPaymentToken(tokenRequest)
        paymentTokenResponse.toPayPalPaymentToken()
    }

    suspend fun createPayPalSetupToken(
        setupRequest: PayPalSetupRequestBody,
        merchantIntegration: MerchantIntegration = SELECTED_MERCHANT_INTEGRATION
    ) = safeApiCall {
        val setupTokenResponse =
            findService(merchantIntegration).createPayPalSetupToken(setupRequest)
        PayPalSetupToken(
            id = setupTokenResponse.id,
            customerId = setupTokenResponse.customer.id,
            status = setupTokenResponse.status
        )
    }

    // Ref: https://medium.com/@douglas.iacovelli/how-to-handle-errors-with-retrofit-and-coroutines-33e7492a912
    @Suppress("TooGenericExceptionCaught")
    private suspend fun <T> safeApiCall(
        apiCall: suspend () -> T
    ): SDKSampleServerResult<T, SDKSampleServerException> = try {
        SDKSampleServerResult.Success(apiCall.invoke())
    } catch (e: Throwable) {
        SDKSampleServerResult.Failure(SDKSampleServerException(e.message, e))
    }

}
