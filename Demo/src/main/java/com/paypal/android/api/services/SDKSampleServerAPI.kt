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
import com.paypal.android.customenvironment.CustomEnvironmentRepository
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Url
import java.util.concurrent.TimeUnit
import javax.inject.Inject

private const val CONNECT_TIMEOUT_IN_SEC = 20L
private const val READ_TIMEOUT_IN_SEC = 30L
private const val WRITE_TIMEOUT_IN_SEC = 30L

// To hardcode an orderId for this demo app, set the below value
private val DEFAULT_ORDER_ID: String? = null // = "your-order-id"

// TODO: consider refactoring each method into a "use case"
// Ref: https://developer.android.com/topic/architecture/domain-layer#use-cases-kotlin
@Suppress("TooManyFunctions")
class SDKSampleServerAPI @Inject constructor(
    private val customEnvironmentRepository: CustomEnvironmentRepository
) {

    @JvmSuppressWildcards
    interface RetrofitService {

        @POST("/orders")
        suspend fun createOrder(@Body orderRequestBody: OrderRequestBody): Order

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

    /**
     * Retrofit interface for endpoints with user-configured path overrides.
     *
     * Uses Retrofit's [@Url] so the full URL is constructed at call-site from
     * the merchant server base URL + the path entered in Settings. This allows
     * connecting to any merchant server (e.g. the XOSphere mock merchant at
     * `braintree.stage.paypal.com/mockmerchantnodeweb`) without hard-coding
     * environment-specific path segments in source code.
     *
     * A [JsonNamingStrategy.SnakeCase] Json instance is used for this service
     * because the XOSphere mock merchant server forwards request bodies verbatim
     * to the PayPal API, which requires snake_case field names (purchase_units,
     * currency_code, etc.).
     */
    @JvmSuppressWildcards
    interface ConfigurablePathRetrofitService {

        @POST
        suspend fun createOrder(
            @Url url: String,
            @Body orderRequestBody: OrderRequestBody
        ): Order

        @POST
        suspend fun captureOrder(
            @Url url: String,
            @Header("PayPal-Client-Metadata-Id") payPalClientMetadataId: String?
        ): OrderResponse

        @POST
        suspend fun authorizeOrder(
            @Url url: String,
            @Header("PayPal-Client-Metadata-Id") payPalClientMetadataId: String?
        ): OrderResponse

        @POST
        suspend fun createSetupToken(
            @Url url: String,
            @Body setupRequest: CardSetupRequest
        ): SetupTokenResponse

        @POST
        suspend fun createPayPalSetupToken(
            @Url url: String,
            @Body setupRequest: PayPalSetupRequestBody
        ): SetupTokenResponse

        @POST
        suspend fun createPaymentToken(
            @Url url: String,
            @Body tokenRequest: TokenRequest
        ): PaymentTokenResponse

        @GET
        suspend fun getSetupToken(@Url url: String): SetupTokenResponse
    }

    // Services for standard MerchantIntegration entries, built once at init.
    private val serviceMap: Map<MerchantIntegration, RetrofitService>

    // Cache for custom base-URL standard services; keyed by base URL.
    private val customServiceCache = mutableMapOf<String, RetrofitService>()

    // Cache for configurable-path services; keyed by merchant server base URL.
    private val configurableServiceCache = mutableMapOf<String, ConfigurablePathRetrofitService>()

    init {
        val serviceMap = mutableMapOf<MerchantIntegration, RetrofitService>()
        val allMerchantIntegrations = MerchantIntegration.entries.toTypedArray()
        for (merchant in allMerchantIntegrations) {
            serviceMap[merchant] = createService(merchant.baseUrl)
        }
        this.serviceMap = serviceMap
    }

    private fun buildRetrofit(baseUrl: String, json: Json = DEFAULT_JSON): Retrofit {
        val okHttpBuilder = OkHttpClient.Builder()
        val httpLoggingInterceptor = HttpLoggingInterceptor()
        httpLoggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
        okHttpBuilder
            .connectTimeout(CONNECT_TIMEOUT_IN_SEC, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_IN_SEC, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT_IN_SEC, TimeUnit.SECONDS)
        okHttpBuilder.addInterceptor(httpLoggingInterceptor)
        return Retrofit.Builder()
            .baseUrl(baseUrl.trimEnd('/') + "/")
            .client(okHttpBuilder.build())
            .addConverterFactory(KotlinSerializationConverterFactory.create(json))
            .build()
    }

    private fun createService(baseUrl: String): RetrofitService =
        buildRetrofit(baseUrl).create(RetrofitService::class.java)

    private fun createConfigurableService(baseUrl: String): ConfigurablePathRetrofitService =
        buildRetrofit(baseUrl, SNAKE_CASE_JSON).create(ConfigurablePathRetrofitService::class.java)

    companion object {
        // TODO: - require Merchant enum to be specified via UI layer
        val SELECTED_MERCHANT_INTEGRATION = MerchantIntegration.DEFAULT

        val clientId: String
            get() = SELECTED_MERCHANT_INTEGRATION.clientId

        /** Default JSON: camelCase, matching the standard demo-app merchant server. */
        private val DEFAULT_JSON = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }

        /**
         * Snake_case JSON for [ConfigurablePathRetrofitService].
         * The XOSphere mock merchant server passes request bodies straight to the PayPal API,
         * which requires snake_case keys (purchase_units, currency_code, etc.).
         * Explicit @SerialName annotations on response models take precedence and are unaffected.
         */
        @Suppress("OPT_IN_USAGE")
        private val SNAKE_CASE_JSON = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            namingStrategy = JsonNamingStrategy.SnakeCase
        }

        /**
         * Combines [baseUrl] and [path] into a single absolute URL, normalising slashes.
         * e.g. `("https://host/base/", "/PPCP/stage_modxo/v2/checkout/orders")`
         *   → `"https://host/base/PPCP/stage_modxo/v2/checkout/orders"`
         */
        fun resolveUrl(baseUrl: String, path: String): String =
            baseUrl.trimEnd('/') + "/" + path.trimStart('/')
    }

    /**
     * Returns the active [RetrofitService] (standard /orders paths), routing to
     * the custom merchant server URL when one is configured.
     */
    private fun getActiveService(): RetrofitService {
        val customConfig = customEnvironmentRepository.getConfig()
        return if (customConfig.isConfigured) {
            customServiceCache.getOrPut(customConfig.merchantServerUrl) {
                createService(customConfig.merchantServerUrl)
            }
        } else {
            serviceMap[SELECTED_MERCHANT_INTEGRATION]
                ?: throw AssertionError("Couldn't find retrofit service for ${SELECTED_MERCHANT_INTEGRATION.name}")
        }
    }

    /**
     * Returns the [ConfigurablePathRetrofitService] for the current merchant server URL,
     * or null when no custom merchant server is configured.
     */
    private fun getConfigurableService(): ConfigurablePathRetrofitService? {
        val customConfig = customEnvironmentRepository.getConfig()
        if (!customConfig.isConfigured) return null
        return configurableServiceCache.getOrPut(customConfig.merchantServerUrl) {
            createConfigurableService(customConfig.merchantServerUrl)
        }
    }

    suspend fun createOrder(
        orderRequestBody: OrderRequestBody,
        merchantIntegration: MerchantIntegration = SELECTED_MERCHANT_INTEGRATION
    ) = safeApiCall {
        if (DEFAULT_ORDER_ID != null) {
            Order(DEFAULT_ORDER_ID, "CREATED")
        } else {
            val config = customEnvironmentRepository.getConfig()
            val configurableSvc = getConfigurableService()
            if (configurableSvc != null && config.createOrderPath.isNotBlank()) {
                val url = resolveUrl(config.merchantServerUrl, config.createOrderPath)
                configurableSvc.createOrder(url, orderRequestBody)
            } else {
                getActiveService().createOrder(orderRequestBody)
            }
        }
    }

    suspend fun captureOrder(
        orderId: String,
        payPalClientMetadataId: String? = null,
        merchantIntegration: MerchantIntegration = SELECTED_MERCHANT_INTEGRATION
    ) = safeApiCall {
        val config = customEnvironmentRepository.getConfig()
        val configurableSvc = getConfigurableService()
        val orderResponse = if (configurableSvc != null && config.createOrderPath.isNotBlank()) {
            val url = resolveUrl(config.merchantServerUrl, config.createOrderPath) + "/$orderId/capture"
            configurableSvc.captureOrder(url, payPalClientMetadataId)
        } else {
            getActiveService().captureOrder(orderId, payPalClientMetadataId)
        }
        orderResponse.toOrder()
    }

    suspend fun authorizeOrder(
        orderId: String,
        payPalClientMetadataId: String? = null,
        merchantIntegration: MerchantIntegration = SELECTED_MERCHANT_INTEGRATION
    ) = safeApiCall {
        val config = customEnvironmentRepository.getConfig()
        val configurableSvc = getConfigurableService()
        val orderResponse = if (configurableSvc != null && config.createOrderPath.isNotBlank()) {
            val url = resolveUrl(config.merchantServerUrl, config.createOrderPath) + "/$orderId/authorize"
            configurableSvc.authorizeOrder(url, payPalClientMetadataId)
        } else {
            getActiveService().authorizeOrder(orderId, payPalClientMetadataId)
        }
        orderResponse.toOrder()
    }

    suspend fun createSetupToken(
        setupRequest: CardSetupRequest,
        merchantIntegration: MerchantIntegration = SELECTED_MERCHANT_INTEGRATION
    ) = safeApiCall {
        val config = customEnvironmentRepository.getConfig()
        val configurableSvc = getConfigurableService()
        val setupTokenResponse = if (configurableSvc != null && config.createSetupTokenPath.isNotBlank()) {
            val url = resolveUrl(config.merchantServerUrl, config.createSetupTokenPath)
            configurableSvc.createSetupToken(url, setupRequest)
        } else {
            getActiveService().createSetupToken(setupRequest)
        }
        setupTokenResponse.toCardSetupToken()
    }

    suspend fun getSetupToken(
        setupTokenId: String,
        merchantIntegration: MerchantIntegration = SELECTED_MERCHANT_INTEGRATION
    ) = safeApiCall {
        val config = customEnvironmentRepository.getConfig()
        val configurableSvc = getConfigurableService()
        val setupTokenResponse = if (configurableSvc != null && config.createSetupTokenPath.isNotBlank()) {
            val url = resolveUrl(config.merchantServerUrl, config.createSetupTokenPath) + "/$setupTokenId"
            configurableSvc.getSetupToken(url)
        } else {
            getActiveService().getSetupToken(setupTokenId)
        }
        setupTokenResponse.toCardSetupToken()
    }

    suspend fun createPaymentToken(
        tokenRequest: TokenRequest,
        merchantIntegration: MerchantIntegration = SELECTED_MERCHANT_INTEGRATION
    ) = safeApiCall {
        val config = customEnvironmentRepository.getConfig()
        val configurableSvc = getConfigurableService()
        val paymentTokenResponse = if (configurableSvc != null && config.createPaymentTokenPath.isNotBlank()) {
            val url = resolveUrl(config.merchantServerUrl, config.createPaymentTokenPath)
            configurableSvc.createPaymentToken(url, tokenRequest)
        } else {
            getActiveService().createPaymentToken(tokenRequest)
        }
        paymentTokenResponse.toCardPaymentToken()
    }

    suspend fun createPayPalPaymentToken(
        tokenRequest: TokenRequest,
        merchantIntegration: MerchantIntegration = SELECTED_MERCHANT_INTEGRATION
    ) = safeApiCall {
        val config = customEnvironmentRepository.getConfig()
        val configurableSvc = getConfigurableService()
        val paymentTokenResponse = if (configurableSvc != null && config.createPaymentTokenPath.isNotBlank()) {
            val url = resolveUrl(config.merchantServerUrl, config.createPaymentTokenPath)
            configurableSvc.createPaymentToken(url, tokenRequest)
        } else {
            getActiveService().createPaymentToken(tokenRequest)
        }
        paymentTokenResponse.toPayPalPaymentToken()
    }

    suspend fun createPayPalSetupToken(
        setupRequest: PayPalSetupRequestBody,
        merchantIntegration: MerchantIntegration = SELECTED_MERCHANT_INTEGRATION
    ) = safeApiCall {
        val config = customEnvironmentRepository.getConfig()
        val configurableSvc = getConfigurableService()
        val setupTokenResponse = if (configurableSvc != null && config.createSetupTokenPath.isNotBlank()) {
            val url = resolveUrl(config.merchantServerUrl, config.createSetupTokenPath)
            configurableSvc.createPayPalSetupToken(url, setupRequest)
        } else {
            getActiveService().createPayPalSetupToken(setupRequest)
        }
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
