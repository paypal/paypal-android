package com.paypal.android.core

class API internal constructor(
    private val configuration: CoreConfig,
    private val authHandler: AuthHandler,
    private val http: Http,
    private val httpRequestFactory: HttpRequestFactory
) {

    constructor(configuration: CoreConfig, authHandler: AuthHandler) :
            this(configuration, authHandler, Http(), HttpRequestFactory())

    suspend fun send(apiRequest: APIRequest): HttpResponse {
        val httpRequest =
            httpRequestFactory.createHttpRequestFromAPIRequest(apiRequest, configuration, authHandler)
        return http.send(httpRequest)
    }
}
