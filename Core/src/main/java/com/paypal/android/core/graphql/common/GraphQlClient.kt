package com.paypal.android.core.graphql.common

import android.util.Log
import com.paypal.android.core.APIClientError
import com.paypal.android.core.GraphQlRequestFactory
import com.paypal.android.core.Http
import org.json.JSONObject
import java.net.HttpURLConnection

interface GraphQlClient {
    suspend fun <T> executeQuery(query: Query<T>): GraphQlQueryResponse<T>
}

class GraphQlClientImpl : GraphQlClient {
    private val http = Http()
    private val graphQlRequestFactory = GraphQlRequestFactory()

    override suspend fun <T> executeQuery(query: Query<T>): GraphQlQueryResponse<T> {
        try {
            val httpRequest = graphQlRequestFactory.createHttpRequestFromQuery(
                query.requestBody()
            )
            val httpResponse = http.send(httpRequest)
            val bodyResponse = httpResponse.body
            val correlationID = httpResponse.headers["Paypal-Debug-Id"]
            if (bodyResponse.isNullOrBlank()) {
                throw APIClientError.noResponseData(correlationID)
            }
            val status = httpResponse.status
            return if (status == HttpURLConnection.HTTP_OK) {
                val data = query.parse(JSONObject(bodyResponse).getJSONObject("data"))
                GraphQlQueryResponse(data)
            } else {
                throw Exception("error in getting graphQL response")
            }
        } catch (e: Exception) {
            Log.d(TAG, "error in executing query")
            return GraphQlQueryResponse()
        }
    }

    companion object {
        const val TAG = "GraphQl Client"
    }

}
