package com.paypal.android.core

import android.content.Context
import android.util.Log
import com.paypal.android.core.analytics.AnalyticsEventData
import com.paypal.android.core.analytics.models.DeviceData
import java.util.*

class API internal constructor(
    // TODO: - Make context non-optional.
    // TODO: - Is there another way to get Context w/o passing it down from feature clients?
    private val configuration: CoreConfig,
    private val http: Http,
    private val httpRequestFactory: HttpRequestFactory,
    private val context: Context? = null
) {

    private val isSimulator: Boolean by lazy {
        false
    }
    private val merchantAppVersion: String by lazy {
        ""
    }
    private val appId: String by lazy {
        ""
    }
    private val appName: String by lazy {
        ""
    }
    private val sessionID = UUID.randomUUID().toString().replace("-", "")

    constructor(configuration: CoreConfig, context: Context? = null) :
            this(configuration, Http(), HttpRequestFactory(), context)

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
        val appMetadata = AppInspector().getAppMetadata(context!!)

        val analyticsEventData = AnalyticsEventData(
            eventName = name,
            sessionID = sessionID,
            deviceData = DeviceData(appName, appId, false, merchantAppVersion)
        ).toJSON().toString()

        val apiRequest = APIRequest("v1/tracking/events", HttpMethod.POST, analyticsEventData)
        val httpRequest = httpRequestFactory.createHttpRequestForFPTI(apiRequest)
        val response = http.send(httpRequest)
        if (!response.isSuccessful) {
            Log.d("[PayPal SDK]", "Failed to send analytics: ${response.error?.message}")
        }
    }
}
