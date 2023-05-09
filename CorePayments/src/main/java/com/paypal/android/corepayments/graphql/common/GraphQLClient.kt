package com.paypal.android.corepayments.graphql.common

import com.paypal.android.corepayments.APIClientError
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.Http
import com.paypal.android.corepayments.HttpMethod
import com.paypal.android.corepayments.HttpRequest
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

internal class GraphQLClient(
    coreConfig: CoreConfig,
    private val http: Http = Http(),
) {

    companion object {
        const val PAYPAL_DEBUG_ID = "Paypal-Debug-Id"
    }

    private val graphQLEndpoint = coreConfig.environment.graphQLEndpoint
    private val graphQLURL = URL("$graphQLEndpoint/graphql")

    private val httpRequestHeaders = mutableMapOf(
        "Content-Type" to "application/json",
        "Accept" to "application/json",
        "x-app-name" to "nativecheckout",
        "Origin" to coreConfig.environment.graphQLEndpoint
    )

    suspend fun send(graphQLRequestBody: JSONObject): GraphQLResponse {
        val body = graphQLRequestBody.toString()
        val httpRequest = HttpRequest(graphQLURL, HttpMethod.POST, body, httpRequestHeaders)
        val httpResponse = http.send(httpRequest)
        val correlationID: String? = httpResponse.headers[PAYPAL_DEBUG_ID]
        val status = httpResponse.status
        return if (status == HttpURLConnection.HTTP_OK) {
            if (httpResponse.body.isNullOrBlank()) {
                throw APIClientError.noResponseData(correlationID)
            } else {
                val responseAsJSON = JSONObject(httpResponse.body)
                GraphQLResponse(responseAsJSON.getJSONObject("data"), correlationId = correlationID)
            }
        } else {
            GraphQLResponse(null, correlationId = correlationID)
        }
    }
}
