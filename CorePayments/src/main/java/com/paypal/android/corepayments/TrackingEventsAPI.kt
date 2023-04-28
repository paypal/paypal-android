package com.paypal.android.corepayments

import com.paypal.android.corepayments.analytics.AnalyticsEventData

class TrackingEventsAPI internal constructor(
    private val restClient: RestClient,
) {

    constructor(coreConfig: CoreConfig) : this(RestClient(coreConfig))

    suspend fun sendEvent(event: AnalyticsEventData): HttpResponse {
        val apiRequest = createHttpRequestForAnalytics(event)
        return restClient.send(apiRequest)
    }

    private fun createHttpRequestForAnalytics(analyticsEventData: AnalyticsEventData): APIRequest {
        val body = analyticsEventData.toJSON().toString()
        return APIRequest("v1/tracking/events", HttpMethod.POST, body)
    }
}