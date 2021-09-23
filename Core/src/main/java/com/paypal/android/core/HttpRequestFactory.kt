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

        val credentials = "${configuration.clientId}:${configuration.secret}"
        val encodedClientId =
            Base64.encodeToString(credentials.toByteArray(Charsets.UTF_8), Base64.DEFAULT)
        httpRequest.headers["Authorization"] = "Basic $encodedClientId"

        return httpRequest
    }
}