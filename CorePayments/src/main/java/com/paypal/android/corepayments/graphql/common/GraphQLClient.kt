package com.paypal.android.corepayments.graphql.common

import com.paypal.android.corepayments.APIClientError
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.Http
import com.paypal.android.corepayments.HttpMethod
import com.paypal.android.corepayments.HttpRequest
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class GraphQLClient internal constructor(
    coreConfig: CoreConfig,
    private val http: Http = Http(),
) {

    companion object {
        const val PAYPAL_DEBUG_ID = "Paypal-Debug-Id"
    }

    constructor(coreConfig: CoreConfig) : this(coreConfig, Http())

    private val graphQLEndpoint = coreConfig.environment.graphQLEndpoint
    private val graphQLURL = URL("$graphQLEndpoint/graphql")

    private val httpRequestHeaders = mutableMapOf(
        "Content-Type" to "application/json",
        "Accept" to "application/json",
        "x-app-name" to "nativecheckout",
        "Origin" to coreConfig.environment.graphQLEndpoint
    )

    suspend fun send(graphQLRequestBody: JSONObject, queryName: String? = null): GraphQLResponse {
        val body = graphQLRequestBody.toString()
        val httpRequest = HttpRequest(graphQLURL, HttpMethod.POST, body, httpRequestHeaders)
        val httpResponse = http.send(httpRequest)
        val correlationId: String? = httpResponse.headers[PAYPAL_DEBUG_ID]
        val status = httpResponse.status
        return if (status == HttpURLConnection.HTTP_OK) {
            if (httpResponse.body.isNullOrBlank()) {
                throw APIClientError.noResponseData(correlationId)
            } else {
                val responseAsJSON = JSONObject(httpResponse.body)
                GraphQLResponse(responseAsJSON.getJSONObject("data"), correlationId = correlationId)
            }
        } else {
            GraphQLResponse(null, correlationId = correlationId)
        }
    }
}
