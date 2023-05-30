package com.paypal.android.corepayments

import java.net.URL
import java.util.Locale

class RestClient internal constructor(
    private val configuration: CoreConfig,
    private val http: Http = Http(),
    private val language: String = Locale.getDefault().language
) {

    constructor(configuration: CoreConfig) : this(configuration, Http())

    suspend fun send(apiRequest: APIRequest): HttpResponse {
        val httpRequest = createHttpRequestFromAPIRequest(apiRequest, configuration)
        return http.send(httpRequest)
    }

    private fun createHttpRequestFromAPIRequest(
        apiRequest: APIRequest,
        configuration: CoreConfig,
    ): HttpRequest =
        configuration.run { createHttpRequestFromAPIRequest(apiRequest, environment, clientId) }

    private fun createHttpRequestFromAPIRequest(
        apiRequest: APIRequest,
        environment: Environment,
        clientId: String? = null
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

        if (clientId != null) {
            // use client-id-only Basic authentication
            val credentials = "$clientId:"
            headers["Authorization"] = "Basic ${credentials.base64encoded()}"
        }

        if (method == HttpMethod.POST) {
            headers["Content-Type"] = "application/json"
        }
        return HttpRequest(url, method, body, headers)
    }
}
