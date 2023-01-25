package com.paypal.android.corepayments

import com.paypal.android.corepayments.analytics.AnalyticsEventData
import java.net.URL
import java.util.Locale

internal class HttpRequestFactory(private val language: String = Locale.getDefault().language) {

    fun createHttpRequestFromAPIRequest(
        apiRequest: APIRequest,
        configuration: CoreConfig,
    ): HttpRequest =
        configuration.run { createHttpRequestFromAPIRequest(apiRequest, environment, accessToken) }

    fun createHttpRequestForAnalytics(analyticsEventData: AnalyticsEventData): HttpRequest {
        val body = analyticsEventData.toJSON().toString()
        val apiRequest = APIRequest("v1/tracking/events", HttpMethod.POST, body)
        return createHttpRequestFromAPIRequest(apiRequest, Environment.LIVE)
    }

    private fun createHttpRequestFromAPIRequest(
        apiRequest: APIRequest,
        environment: Environment,
        accessToken: String? = null
    ): HttpRequest {
        val path = apiRequest.path
        val baseUrl = environment.url

        val url = URL("$baseUrl/$path")
        val method = apiRequest.method
        val body = apiRequest.body

        // default headers
        val headers: MutableMap<String, String> = mutableMapOf(
            "Accept-Encoding" to "gzip",
            "Accept-Language" to language
        )

        accessToken?.let { token ->
            headers["Authorization"] = "Bearer $token"
        }

        if (method == HttpMethod.POST) {
            headers["Content-Type"] = "application/json"
        }
        return HttpRequest(url, method, body, headers)
    }
}
