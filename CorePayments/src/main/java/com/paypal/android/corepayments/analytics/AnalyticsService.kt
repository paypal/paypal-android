package com.paypal.android.corepayments.analytics

import android.content.Context
import android.util.Log
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.ClientIDAPI
import com.paypal.android.corepayments.Environment
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.corepayments.APIClientError
import com.paypal.android.corepayments.Http
import com.paypal.android.corepayments.HttpRequestFactory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope

@Suppress("LongParameterList")
class AnalyticsService internal constructor(
    private val deviceInspector: DeviceInspector,
    private val clientIDAPI: ClientIDAPI,
    private val environment: Environment,
    private val http: Http,
    private val httpRequestFactory: HttpRequestFactory,
    private val orderID: String,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main
) {

    constructor(configuration: CoreConfig, context: Context, orderID: String) :
        this(
            deviceInspector = DeviceInspector(context),
            clientIDAPI = ClientIDAPI(configuration),
            environment = configuration.environment,
            http = Http(),
            httpRequestFactory = HttpRequestFactory(),
            orderID = orderID
        )

    /**
     * Sends analytics event to https://api.paypal.com/v1/tracking/events/ via a background task.
     */
    fun sendAnalyticsEvent(name: String) {
        CoroutineScope(dispatcher).launch() {
            try {
                val clientID = clientIDAPI.fetchCachedOrRemoteClientID()
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
            environment.name.lowercase(),
            name,
            timestamp,
            orderID,
            deviceInspector.inspect()
        )
        val httpRequest = httpRequestFactory.createHttpRequestForAnalytics(analyticsEventData)

        val response = http.send(httpRequest)
        if (!response.isSuccessful) {
            throw APIClientError.clientIDNotFoundError()
        }
    }
}
