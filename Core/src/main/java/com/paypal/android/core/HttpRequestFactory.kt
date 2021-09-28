package com.paypal.android.core

import java.net.URL
import java.util.Locale

internal class HttpRequestFactory(private val locale: Locale = Locale.getDefault()) {

    fun createHttpRequestFromAPIRequest(
        apiRequest: APIRequest,
        configuration: PaymentsConfiguration,
    ): HttpRequest {
        val path = apiRequest.path
        val baseUrl = configuration.environment.url

        val url = URL("$baseUrl/$path")
        val method = apiRequest.method
        val body = apiRequest.body

        // default headers
        val headers: MutableMap<String, String> = mutableMapOf(
            "Accept-Encoding" to "gzip",
            "Accept-Language" to locale.language
        )

        val credentials = configuration.run { "$clientId:$clientSecret" }
        headers["Authorization"] = "Basic ${credentials.base64encoded()}"

        if (method == HttpMethod.POST) {
            headers["Content-Type"] = "application/json"
        }
        return HttpRequest(url, method, body, headers)
    }
}
