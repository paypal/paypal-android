package com.paypal.android.corepayments

import org.json.JSONObject
import java.net.URL

internal class GraphQLRequestFactory(
    private val coreConfig: CoreConfig
) {

    fun createHttpRequestFromQuery(requestBody: JSONObject): HttpRequest {
        return HttpRequest(
            url = URL(coreConfig.environment.grqphQlUrl),
            method = HttpMethod.POST,
            body = requestBody.toString().trimIndent(),
            headers = headers()
        )
    }

    private fun headers() = mutableMapOf(
        "Content-type" to "application/json",
        "Accept" to "application/json",
        "x-app-name" to "nativecheckout",
        "Origin" to Environment.SANDBOX.grqphQlUrl
    )
}
