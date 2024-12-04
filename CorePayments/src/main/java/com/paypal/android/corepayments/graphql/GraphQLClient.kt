package com.paypal.android.corepayments.graphql

import androidx.annotation.RestrictTo
import com.paypal.android.corepayments.APIClientError
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.Http
import com.paypal.android.corepayments.HttpMethod
import com.paypal.android.corepayments.HttpRequest
import org.json.JSONException
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

    suspend fun send(graphQLRequestBody: JSONObject, queryName: String? = null): GraphQLResult {
        val body = graphQLRequestBody.toString()
        val urlString = if (queryName != null) "$graphQLURL?$queryName" else graphQLURL
        val httpRequest = HttpRequest(URL(urlString), HttpMethod.POST, body, httpRequestHeaders)

        val httpResponse = http.send(httpRequest)
        val correlationId: String? = httpResponse.headers[PAYPAL_DEBUG_ID]
        val status = httpResponse.status
        return if (status == HttpURLConnection.HTTP_OK) {
            if (httpResponse.body.isNullOrBlank()) {
                val error = APIClientError.noResponseData(correlationId)
                GraphQLResult.Failure(error)
            } else {
                try {
                    val responseAsJSON = JSONObject(httpResponse.body)
                    GraphQLResult.Success(responseAsJSON.getJSONObject("data"), correlationId = correlationId)
                } catch (jsonParseError: JSONException) {
                    val error = APIClientError.graphQLJSONParseError(correlationId, jsonParseError)
                    GraphQLResult.Failure(error)
                }
            }
        } else {
            GraphQLResult.Success(null, correlationId = correlationId)
        }
    }
}
