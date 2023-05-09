package com.paypal.android.corepayments.analytics

import android.util.Log
import com.paypal.android.corepayments.Environment
import com.paypal.android.corepayments.Http
import com.paypal.android.corepayments.HttpRequestFactory

internal class AnalyticsService(
    private val deviceInspector: DeviceInspector,
    private val environment: Environment,
    private val http: Http,
    private val httpRequestFactory: HttpRequestFactory
) {

    internal suspend fun sendAnalyticsEvent(name: String, clientID: String, orderID: String) {
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
            Log.d("[PayPal SDK]", "Failed to send analytics: ${response.error?.message}")
        }
    }
}
