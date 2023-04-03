package com.paypal.android.corepayments

class APIClient internal constructor(
    private val configuration: CoreConfig,
    private val http: Http = Http(),
    private val httpRequestFactory: HttpRequestFactory = HttpRequestFactory(),
) {

    constructor(configuration: CoreConfig) : this(configuration, Http(), HttpRequestFactory())

    suspend fun send(apiRequest: APIRequest): HttpResponse {
        val httpRequest =
            httpRequestFactory.createHttpRequestFromAPIRequest(apiRequest, configuration)
        return http.send(httpRequest)
    }
}