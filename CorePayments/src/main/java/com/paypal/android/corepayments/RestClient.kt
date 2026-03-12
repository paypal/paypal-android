package com.paypal.android.corepayments

import androidx.annotation.RestrictTo
import com.paypal.android.corepayments.common.Headers
import java.net.URL
import java.util.Locale

/**
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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
    ): HttpRequest {
        val path = apiRequest.path
        val baseUrl = configuration.environment.url

        val url = URL("$baseUrl/$path")
        val method = apiRequest.method
        val body = apiRequest.body

        // default headers
        val headers: MutableMap<String, String> =
            apiRequest.headers?.toMutableMap() ?: mutableMapOf()
        headers.putAll(
            from = mapOf("Accept-Encoding" to "gzip", "Accept-Language" to language)
        )

        if (!headers.containsKey(Headers.AUTHORIZATION)) {
            // override auth header with client-id-only Basic authentication
            val credentials = "${configuration.clientId}:"
            headers[Headers.AUTHORIZATION] = "Basic ${credentials.base64encoded()}"
        }

        if (method == HttpMethod.POST) {
            headers["Content-Type"] = "application/json"
        }
        return HttpRequest(url, method, body, headers)
    }
}
