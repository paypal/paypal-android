package com.paypal.android.core

class APIClient(
    private val configuration: PaymentsConfiguration,
    private val http: Http = Http(),
    private val httpRequestFactory: HttpRequestFactory = HttpRequestFactory()
) {

    suspend fun send(apiRequest: APIRequest): HttpResponse {
        val httpRequest =
            httpRequestFactory.createHttpRequestFromAPIRequest(apiRequest, configuration)
        return http.send(httpRequest)
    }
}
