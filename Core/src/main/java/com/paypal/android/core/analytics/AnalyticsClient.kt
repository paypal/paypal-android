package com.paypal.android.core.analytics

import android.util.Log
import com.paypal.android.core.Http
import com.paypal.android.core.HttpRequestFactory
import java.util.UUID

internal class AnalyticsClient(
    private val deviceInspector: DeviceInspector,
    private val http: Http,
    private val httpRequestFactory: HttpRequestFactory
) {

    internal suspend fun sendAnalyticsEvent(name: String, timestamp: Long) {
        val analyticsEventData =
            AnalyticsEventData(name, timestamp, sessionId, deviceInspector.inspect())
        val httpRequest = httpRequestFactory.createHttpRequestForAnalytics(analyticsEventData)
        val response = http.send(httpRequest)
        if (!response.isSuccessful) {
            Log.d("[PayPal SDK]", "Failed to send analytics: ${response.error?.message}")
        }
    }

    suspend fun sendAnalyticsEvent(name: String) {
        sendAnalyticsEvent(name, System.currentTimeMillis())
    }

    companion object {
        private val sessionId = UUID.randomUUID().toString()
    }
}
