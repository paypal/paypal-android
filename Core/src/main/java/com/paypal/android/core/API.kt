package com.paypal.android.core

class API internal constructor(
    private val configuration: CoreConfig,
    private val http: Http,
    private val httpRequestFactory: HttpRequestFactory
) {

    constructor(configuration: CoreConfig) :
            this(configuration, Http(), HttpRequestFactory())

    suspend fun send(apiRequest: APIRequest): HttpResponse {
        val httpRequest =
            httpRequestFactory.createHttpRequestFromAPIRequest(apiRequest, configuration)
        return http.send(httpRequest)
    }

    suspend fun getClientId(): String {
        val apiRequest = APIRequest(
            path = "v1/oauth2/token",
            method = HttpMethod.GET,
            body = null
        )
        val httpRequest =
            httpRequestFactory.createHttpRequestFromAPIRequest(apiRequest, configuration)
        val response = http.send(httpRequest)
        val json = PaymentsJSON(response.body!!)
        return json.getString("client_id")
    }
}

