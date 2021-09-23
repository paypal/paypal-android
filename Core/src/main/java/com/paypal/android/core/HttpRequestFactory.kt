package com.paypal.android.core

import android.util.Base64
import java.net.URL

class HttpRequestFactory {

    fun createHttpRequestFromAPIRequest(apiRequest: APIRequest, configuration: PaymentsConfiguration): HttpRequest {
        val path = apiRequest.path
        val baseUrl = configuration.environment.url

        val url = URL("${baseUrl}/${path}")
        val method = apiRequest.method
        val body = apiRequest.body

        val httpRequest = HttpRequest(url, method, body)
        if (method == HttpMethod.POST) {
            httpRequest.contentType = HttpContentType.JSON
        }

        httpRequest.headers["Authorization"] = "Bearer ${configuration.authToken}"
        return httpRequest
    }
}