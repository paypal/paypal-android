package com.paypal.android.core

class API internal constructor(
    private val configuration: PaymentsConfiguration,
    private val http: Http,
    private val httpRequestFactory: HttpRequestFactory
) {
    constructor(configuration: PaymentsConfiguration) :
            this(configuration, Http(), HttpRequestFactory())

    suspend fun send(apiRequest: APIRequest): HttpResponse {
        val httpRequest =
            httpRequestFactory.createHttpRequestFromAPIRequest(apiRequest, configuration)
        return http.send(httpRequest)
    }
}
