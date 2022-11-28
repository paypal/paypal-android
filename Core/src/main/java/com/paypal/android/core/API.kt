package com.paypal.android.core

import android.content.Context
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.paypal.android.core.analytics.AnalyticsEventData
import com.paypal.android.core.analytics.DeviceInspector
import java.util.UUID

class API internal constructor(
    private val configuration: CoreConfig,
    private val sessionID: String,
    private val http: Http,
    private val httpRequestFactory: HttpRequestFactory,
    private val deviceInspector: DeviceInspector
) {

    constructor(configuration: CoreConfig, context: Context) :
            this(
                configuration,
                UUID.randomUUID().toString().replace("-", ""),
                Http(),
                HttpRequestFactory(),
                DeviceInspector(context.applicationContext)
            )

    suspend fun send(apiRequest: APIRequest): HttpResponse {
        val httpRequest =
            httpRequestFactory.createHttpRequestFromAPIRequest(apiRequest, configuration)
        return http.send(httpRequest)
    }

    @Throws(PayPalSDKError::class)
    suspend fun getClientId(): String {
        val apiRequest = APIRequest("v1/oauth2/token", HttpMethod.GET)
        val httpRequest =
            httpRequestFactory.createHttpRequestFromAPIRequest(apiRequest, configuration)
        val response = http.send(httpRequest)
        val correlationID = response.headers["Paypal-Debug-Id"]
        if (response.isSuccessful) {
            return parseClientId(response.body, correlationID)
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

    suspend fun sendAnalyticsEvent(name: String) {
        sendAnalyticsEvent(name, System.currentTimeMillis())
    }

    @VisibleForTesting
    internal suspend fun sendAnalyticsEvent(name: String, timestamp: Long) {
        val analyticsEventData = AnalyticsEventData(name, timestamp, sessionID, deviceInspector)
        val httpRequest = httpRequestFactory.createHttpRequestForAnalytics(analyticsEventData)
        val response = http.send(httpRequest)
        if (!response.isSuccessful) {
            Log.d("[PayPal SDK]", "Failed to send analytics: ${response.error?.message}")
        }
    }
}
