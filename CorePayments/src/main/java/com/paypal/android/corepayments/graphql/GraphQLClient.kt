package com.paypal.android.corepayments.graphql

import androidx.annotation.RestrictTo
import com.paypal.android.corepayments.APIClientError
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.CoreSDKResult
import com.paypal.android.corepayments.Http
import com.paypal.android.corepayments.HttpMethod
import com.paypal.android.corepayments.HttpRequest
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class GraphQLClient internal constructor(
    coreConfig: CoreConfig,
    private val http: Http = Http(),
) {

    companion object {
        const val PAYPAL_DEBUG_ID = "Paypal-Debug-Id"
    }

    constructor(coreConfig: CoreConfig) : this(coreConfig, Http())

    private val graphQLEndpoint = coreConfig.environment.graphQLEndpoint
    private val graphQLURL = "$graphQLEndpoint/graphql"

    private val httpRequestHeaders = mutableMapOf(
        "Content-Type" to "application/json",
        "Accept" to "application/json",
        "x-app-name" to "nativecheckout",
        "Origin" to coreConfig.environment.graphQLEndpoint
    )

    suspend fun send(
        graphQLRequestBody: JSONObject,
        queryName: String? = null
    ): CoreSDKResult<GraphQLResponse> {
        val body = graphQLRequestBody.toString()
        val urlString = if (queryName != null) "$graphQLURL?$queryName" else graphQLURL
        val httpRequest = HttpRequest(URL(urlString), HttpMethod.POST, body, httpRequestHeaders)
        return when (val result = http.send(httpRequest)) {
            is CoreSDKResult.Success -> {
                val httpResponse = result.value
                val correlationId: String? = httpResponse.headers[PAYPAL_DEBUG_ID]
                val status = httpResponse.status
                if (status == HttpURLConnection.HTTP_OK) {
                    return if (httpResponse.body.isNullOrBlank()) {
                        CoreSDKResult.Failure(APIClientError.noResponseData(correlationId))
                    } else {
                        val responseAsJSON = JSONObject(httpResponse.body)
                        val data = responseAsJSON.getJSONObject("data")
                        val graphQLResponse = GraphQLResponse(data, correlationId = correlationId)
                        CoreSDKResult.Success(graphQLResponse)
                    }
                } else {
                    return CoreSDKResult.Failure(APIClientError.unknownGraphQLError(correlationId))
                }
            }

            is CoreSDKResult.Failure -> {
                CoreSDKResult.Failure(APIClientError.httpError(result.value))
            }
        }
    }
}
