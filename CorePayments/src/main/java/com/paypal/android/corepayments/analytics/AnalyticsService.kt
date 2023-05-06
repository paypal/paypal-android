package com.paypal.android.corepayments.analytics

import android.content.Context
import android.util.Log
import android.util.LruCache
import com.paypal.android.corepayments.*
import com.paypal.android.corepayments.Http
import com.paypal.android.corepayments.HttpRequestFactory
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*

class AnalyticsService internal constructor(
    private val deviceInspector: DeviceInspector,
    private val configuration: CoreConfig,
    private val http: Http,
    private val httpRequestFactory: HttpRequestFactory,
    private val orderID: String
) {

    constructor(configuration: CoreConfig, context: Context, orderID: String) :
        this(
            deviceInspector = DeviceInspector(context),
            configuration = configuration,
            http = Http(),
            httpRequestFactory = HttpRequestFactory(),
            orderID = orderID
        )

    /**
     * Sends analytics event to https://api.paypal.com/v1/tracking/events/ via a background task.
     */
    fun sendAnalyticsEvent(name: String) {
        GlobalScope.launch {
            try {
                val clientID = fetchCachedOrRemoteClientID()
                performEventRequest(name, clientID)
            } catch (e: PayPalSDKError) {
                Log.d(
                    "[PayPal SDK]", "Failed to send analytics: ${e.message}"
                )
            }
        }
    }

    @Throws(PayPalSDKError::class)
    internal suspend fun performEventRequest(name: String, clientID: String) {
        val timestamp = System.currentTimeMillis()

        val analyticsEventData = AnalyticsEventData(
            clientID,
            configuration.environment.name.lowercase(),
            name,
            timestamp,
            sessionId,
            deviceInspector.inspect()
        )
        val httpRequest = httpRequestFactory.createHttpRequestForAnalytics(analyticsEventData)

        val response = http.send(httpRequest)
        if (!response.isSuccessful) {
            throw APIClientError.clientIDNotFoundError(response.status, "")
        }
    }

    /**
     * Retrieves the merchant's clientID either from the local cache, or via an HTTP request if not cached.
     * @return Merchant clientID.
     */
    @Throws(PayPalSDKError::class)
    suspend fun fetchCachedOrRemoteClientID(): String {
        API.clientIDCache.get(configuration.accessToken)?.let { cachedClientID ->
            return cachedClientID
        }

        val apiRequest = APIRequest("v1/oauth2/token", HttpMethod.GET)
        val httpRequest =
            httpRequestFactory.createHttpRequestFromAPIRequest(apiRequest, configuration)
        val response = http.send(httpRequest)
        val correlationID = response.headers["Paypal-Debug-Id"]
        if (response.isSuccessful) {
            val clientID = parseClientId(response.body, correlationID)
            API.clientIDCache.put(configuration.accessToken, clientID)
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

    companion object {
        private val sessionId = UUID.randomUUID().toString()
        // val clientIDCache = LruCache<String, String>(10)
    }
}
