package com.paypal.android.corepayments.graphql.common

import com.paypal.android.corepayments.APIClientError
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.GraphQLRequestFactory
import com.paypal.android.corepayments.Http
import org.json.JSONObject
import java.net.HttpURLConnection

internal interface GraphQLClient {
    suspend fun <T> executeQuery(query: Query<T>): GraphQlQueryResponse<T>
}

internal class GraphQLClientImpl(
    private val coreConfig: CoreConfig,
    private val http: Http = Http(),
    private val graphQlRequestFactory: GraphQLRequestFactory = GraphQLRequestFactory(coreConfig)
) : GraphQLClient {

    override suspend fun <T> executeQuery(query: Query<T>): GraphQlQueryResponse<T> {
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
                GraphQlQueryResponse(
                    data = data,
                    correlationId = correlationID
                )
            } else {
                GraphQlQueryResponse()
            }
    }

    companion object {
        const val PAYPAL_DEBUG_ID = "Paypal-Debug-Id"
    }
}
