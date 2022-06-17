package com.paypal.android.core

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

        headers["Authorization"] = configuration.run {
            if (accessToken != null) {
                "Bearer $accessToken"
            } else {
                val credentials = "$clientId:".base64encoded()
                "Basic $credentials"
            }
        }

        if (method == HttpMethod.POST) {
            headers["Content-Type"] = "application/json"
        }
        return HttpRequest(url, method, body, headers)
    }
}
