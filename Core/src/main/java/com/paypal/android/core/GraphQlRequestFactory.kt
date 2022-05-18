package com.paypal.android.core

import org.json.JSONObject
import java.net.URL

class GraphQlRequestFactory {

    fun createHttpRequestFromQuery(requestBody: JSONObject): HttpRequest {
        return HttpRequest(
            url = URL(Environment.SANDBOX.grqphQlUrl),
            method = HttpMethod.POST,
            body = requestBody.toString().trimIndent(),
            headers = headers()
        )
    }

    fun headers() = mutableMapOf(
        "Content-type" to "application/json",
        "Accept" to "application/json",
        "x-app-name" to "nativecheckout",
        "Origin" to Environment.SANDBOX.grqphQlUrl
    )
}
