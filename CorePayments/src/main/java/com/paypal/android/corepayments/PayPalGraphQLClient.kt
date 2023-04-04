package com.paypal.android.corepayments

import com.paypal.android.corepayments.graphql.common.GraphQLQueryResponse2
import org.json.JSONObject
import java.net.URL

class PayPalGraphQLClient internal constructor(
    private val coreConfig: CoreConfig,
    private val http: Http = Http()
) {
    suspend fun send(request: JSONObject): GraphQLQueryResponse2 {
        val httpRequest = mapGraphQLRequestToHttpRequest(request)
        val response = http.send(httpRequest)
        return GraphQLQueryResponse2(response)
    }

    private fun mapGraphQLRequestToHttpRequest(graphQLRequest: JSONObject): HttpRequest {
        val baseUrl = coreConfig.environment.grqphQlUrl
        val url = URL("$baseUrl/graphql")
        val body = graphQLRequest.toString()

        // default headers
        val headers: MutableMap<String, String> = mutableMapOf(
            "Content-Type" to "application/json",
            "Accept" to "application/json",
            "x-app-name" to "nativecheckout",
            "Origin" to coreConfig.environment.grqphQlUrl
        )

        return HttpRequest(url, HttpMethod.POST, body, headers)
    }
}