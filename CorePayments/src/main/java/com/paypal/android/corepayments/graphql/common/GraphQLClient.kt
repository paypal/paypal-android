package com.paypal.android.corepayments.graphql.common

import com.paypal.android.corepayments.APIClientError
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.GraphQLRequestFactory
import com.paypal.android.corepayments.Http
import com.paypal.android.corepayments.HttpMethod
import com.paypal.android.corepayments.HttpRequest
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

internal interface GraphQLClient {
    suspend fun send(graphQLRequestBody: JSONObject): GraphQLQueryResponse<JSONObject>
    suspend fun <T> executeQuery(query: Query<T>): GraphQLQueryResponse<T>
}

internal class GraphQLClientImpl(
    private val coreConfig: CoreConfig,
    private val http: Http = Http(),
    private val graphQlRequestFactory: GraphQLRequestFactory = GraphQLRequestFactory(coreConfig)
) : GraphQLClient {

    override suspend fun send(graphQLRequestBody: JSONObject): GraphQLQueryResponse<JSONObject> {
        val baseUrl = coreConfig.environment.graphQLEndpoint
        val url = URL("$baseUrl/graphql")
        val body = graphQLRequestBody.toString()

        // default headers
        val headers = mutableMapOf(
            "Content-Type" to "application/json",
            "Accept" to "application/json",
            "x-app-name" to "nativecheckout",
            "Origin" to coreConfig.environment.graphQLEndpoint
        )

        val httpRequest = HttpRequest(url, HttpMethod.POST, body, headers)
        val httpResponse = http.send(httpRequest)
        val correlationID: String? = httpResponse.headers[PAYPAL_DEBUG_ID]
        val status = httpResponse.status
        return if (status == HttpURLConnection.HTTP_OK) {
            if (httpResponse.body.isNullOrBlank()) {
                throw APIClientError.noResponseData(correlationID)
            } else {
                val responseAsJSON = JSONObject(httpResponse.body)
                GraphQLQueryResponse(responseAsJSON.getJSONObject("data"), correlationId = correlationID)
            }
        } else {
            // TODO: GraphQL error handling logic still needs requirements and unit testing
            GraphQLQueryResponse(JSONObject(), correlationId = correlationID)
        }
    }

    override suspend fun <T> executeQuery(query: Query<T>): GraphQLQueryResponse<T> {
        val httpRequest = graphQlRequestFactory.createHttpRequestFromQuery(
            query.requestBody()
        )
        val httpResponse = http.send(httpRequest)
        val bodyResponse = httpResponse.body
        val correlationID: String? = httpResponse.headers[PAYPAL_DEBUG_ID]
        if (bodyResponse.isNullOrBlank()) {
            throw APIClientError.noResponseData(correlationID)
        }
        val status = httpResponse.status
        return if (status == HttpURLConnection.HTTP_OK && !bodyResponse.isNullOrBlank()) {
            val data = query.parse(JSONObject(bodyResponse).getJSONObject("data"))
            GraphQLQueryResponse(
                data = data,
                correlationId = correlationID
            )
        } else {
            GraphQLQueryResponse()
        }
    }

    companion object {
        const val PAYPAL_DEBUG_ID = "Paypal-Debug-Id"
    }
}
