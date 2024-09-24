package com.paypal.android.corepayments.graphql

import androidx.annotation.RestrictTo
import com.paypal.android.corepayments.APIClientError
import com.paypal.android.corepayments.CoreConfig
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
    private val http: Http = Http(),
) {

    companion object {
        const val PAYPAL_DEBUG_ID = "Paypal-Debug-Id"
    }

    constructor() : this(Http())

    suspend fun send(
        graphQLRequestBody: JSONObject,
        config: CoreConfig,
        queryName: String? = null
    ): GraphQLResponse {
        val graphQLEndpoint = config.environment.graphQLEndpoint
        val graphQLURL = "$graphQLEndpoint/graphql"

        val body = graphQLRequestBody.toString()
        val urlString = if (queryName != null) "$graphQLURL?$queryName" else graphQLURL

        val httpRequestHeaders = mutableMapOf(
            "Content-Type" to "application/json",
            "Accept" to "application/json",
            "x-app-name" to "nativecheckout",
            "Origin" to config.environment.graphQLEndpoint
        )

        val httpRequest = HttpRequest(URL(urlString), HttpMethod.POST, body, httpRequestHeaders)

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
