package com.paypal.android.core

import com.paypal.android.core.analytics.AnalyticsEventData
import java.net.URL
import java.util.Locale

internal class HttpRequestFactory(private val language: String = Locale.getDefault().language) {

    fun createHttpRequestFromAPIRequest(
        apiRequest: APIRequest,
        configuration: CoreConfig,
    ): HttpRequest {
        val path = apiRequest.path
        val baseUrl = configuration.environment.url

        val url = URL("$baseUrl/$path")
        val method = apiRequest.method
        val body = apiRequest.body

        // default headers
        val headers: MutableMap<String, String> = mutableMapOf(
            "Accept-Encoding" to "gzip",
            "Accept-Language" to language
        )

        configuration.accessToken?.also { token ->
            headers["Authorization"] = "Bearer $token"
        }

        if (method == HttpMethod.POST) {
            headers["Content-Type"] = "application/json"
        }
        return HttpRequest(url, method, body, headers)
    }

    fun createHttpRequestForAnalytics(analyticsEventData: AnalyticsEventData): HttpRequest{
        val apiRequest = APIRequest("v1/tracking/events", HttpMethod.POST, analyticsEventData.toJSON().toString())
        val path = apiRequest.path
        val baseUrl = Environment.LIVE.url

        val url = URL("$baseUrl/$path")
        val method = apiRequest.method
        val body = apiRequest.body

        val headers: MutableMap<String, String> = mutableMapOf(
            "Content-Type" to "application/json"
        )

        return HttpRequest(url, method, body, headers)
    }
}
