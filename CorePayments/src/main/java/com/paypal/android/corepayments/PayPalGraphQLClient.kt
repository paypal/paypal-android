package com.paypal.android.corepayments

import com.paypal.android.corepayments.graphql.common.GraphQLQueryResponse2
import java.net.URL
import java.util.Locale

class PayPalGraphQLClient internal constructor(
    private val coreConfig: CoreConfig,
    private val http: Http = Http()
) {

    suspend fun send(apiRequest: APIRequest): GraphQLQueryResponse2 {
        val httpRequest = createHttpRequestFromAPIRequest(apiRequest, coreConfig)
        val response = http.send(httpRequest)
        return GraphQLQueryResponse2(response)
    }

    private fun createHttpRequestFromAPIRequest(
        apiRequest: APIRequest,
        configuration: CoreConfig,
    ): HttpRequest =
        configuration.run { createHttpRequestFromAPIRequest(apiRequest, environment, accessToken) }

    private fun createHttpRequestFromAPIRequest(
        apiRequest: APIRequest,
        environment: Environment,
        accessToken: String? = null
    ): HttpRequest {
        val path = apiRequest.path
        val baseUrl = environment.grqphQlUrl

        val url = URL("$baseUrl/$path")
        val method = apiRequest.method
        val body = apiRequest.body

        // default headers
        val headers: MutableMap<String, String> = mutableMapOf(
            "Accept-Encoding" to "gzip",
            "Accept-Language" to Locale.getDefault().language
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