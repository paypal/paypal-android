package com.paypal.android.core.analytics

import android.util.Log
import com.paypal.android.core.Http
import com.paypal.android.core.HttpRequestFactory
import java.util.*

internal class AnalyticsService(
    private val deviceInspector: DeviceInspector,
    private val http: Http,
    private val httpRequestFactory: HttpRequestFactory
) {

    internal suspend fun sendAnalyticsEvent(name: String) {
        val timestamp = System.currentTimeMillis()

        val analyticsEventData =
            AnalyticsEventData(name, timestamp, sessionId, deviceInspector.inspect())
        val httpRequest = httpRequestFactory.createHttpRequestForAnalytics(analyticsEventData)

        val response = http.send(httpRequest)
        if (!response.isSuccessful) {
            Log.d("[PayPal SDK]", "Failed to send analytics: ${response.error?.message}")
        }
    }

    companion object {
        private val sessionId = UUID.randomUUID().toString()
    }
}
