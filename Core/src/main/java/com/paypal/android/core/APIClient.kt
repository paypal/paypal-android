package com.paypal.android.core

class APIClient(private val configuration: PaymentsConfiguration) {

    private val http = Http()
    private val httpRequestFactory = HttpRequestFactory()

    suspend fun post(path: String, body: String): HttpResponse {
        val apiRequest = APIRequest(path, HttpMethod.POST, body)
        return send(apiRequest)
    }

    private suspend fun send(apiRequest: APIRequest): HttpResponse {
        val httpRequest =
            httpRequestFactory.createHttpRequestFromAPIRequest(apiRequest, configuration)
        return http.send(httpRequest)
    }
}
