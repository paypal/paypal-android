package com.paypal.android.core.graphql.common

import android.util.Log
import com.paypal.android.core.APIClientError
import com.paypal.android.core.GraphQLRequestFactory
import com.paypal.android.core.Http
import com.paypal.android.core.PayPalSDKError
import org.json.JSONException
import org.json.JSONObject
import java.net.HttpURLConnection

interface GraphQlClient {
    suspend fun <T> executeQuery(query: Query<T>): GraphQlQueryResponse<T>
}

class GraphQlClientImpl : GraphQlClient {
    private val http = Http()
    private val graphQlRequestFactory = GraphQLRequestFactory()

    override suspend fun <T> executeQuery(query: Query<T>): GraphQlQueryResponse<T> {
        return try {
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
                throw JSONException("error in getting graphQL response")
            }
        } catch (e: JSONException) {
            Log.d(TAG, "error in parsing data: ${e.message}")
            GraphQlQueryResponse()
        } catch (e: PayPalSDKError) {
            Log.d(TAG, "payPal SDK Error: ${e.message}")
            GraphQlQueryResponse()
        }
    }

    companion object {
        const val TAG = "GraphQl Client"
    }
}
