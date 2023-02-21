package com.paypal.android.corepayments

import android.content.Context
import android.util.Log
import android.util.LruCache
import com.paypal.android.corepayments.analytics.AnalyticsService
import com.paypal.android.corepayments.analytics.DeviceInspector
import kotlinx.coroutines.*

/**
 * This class is exposed for internal PayPal use only. Do not use.
 * It is not covered by Semantic Versioning and may change or be removed at any time.
 */
class API internal constructor(
    private val configuration: CoreConfig,
    private val http: Http,
    private val httpRequestFactory: HttpRequestFactory,
    private val analyticsService: AnalyticsService,
) {

    constructor(configuration: CoreConfig, context: Context) :
            this(
                configuration,
                Http(),
                HttpRequestFactory(),
                AnalyticsService(
                    deviceInspector = DeviceInspector(context),
                    environment = configuration.environment,
                    http = Http(),
                    httpRequestFactory = HttpRequestFactory()
                )
            )

    suspend fun send(apiRequest: APIRequest): HttpResponse {
        val httpRequest =
            httpRequestFactory.createHttpRequestFromAPIRequest(apiRequest, configuration)
        return http.send(httpRequest)
    }

    /**
     * Retrieves the merchant's clientID either from the local cache, or via an HTTP request if not cached.
     * @return Merchant clientID.
     */
    @Throws(PayPalSDKError::class)
    suspend fun fetchCachedOrRemoteClientID(): String {
        clientIDCache.get(configuration.accessToken)?.let { cachedClientID ->
            return cachedClientID
        }

        val apiRequest = APIRequest("v1/oauth2/token", HttpMethod.GET)
        val httpRequest =
            httpRequestFactory.createHttpRequestFromAPIRequest(apiRequest, configuration)
        val response = http.send(httpRequest)
        val correlationID = response.headers["Paypal-Debug-Id"]
        if (response.isSuccessful) {
            val clientID = parseClientId(response.body, correlationID)
            clientIDCache.put(configuration.accessToken, clientID)
            return clientID
        }

        throw APIClientError.serverResponseError(correlationID)
    }

    @Throws(PayPalSDKError::class)
    private fun parseClientId(responseBody: String?, correlationID: String?): String {
        if (responseBody.isNullOrBlank()) {
            throw APIClientError.noResponseData(correlationID)
        }
        val json = PaymentsJSON(responseBody)
        val clientID = json.optString("client_id")
        if (clientID.isNullOrBlank()) {
            throw APIClientError.dataParsingError(correlationID)
        }
        return clientID
    }

    /**
     * Sends analytics event to https://api.paypal.com/v1/tracking/events/ via a background task.
     */
    fun sendAnalyticsEvent(name: String)  {
        GlobalScope.launch {
            try {
                val clientID = fetchCachedOrRemoteClientID()
                analyticsService.sendAnalyticsEvent(name, clientID)
            } catch (e: PayPalSDKError) {
                Log.d(
                    "[PayPal SDK]",
                    "Failed to send analytics due to missing clientID: ${e.message}"
                )
            }
        }
    }

    companion object {
        val clientIDCache = LruCache<String, String>(10)
    }
}
