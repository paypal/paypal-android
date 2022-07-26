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

    @Throws(PayPalSDKError::class)
    suspend fun getClientId(): String {
        val apiRequest = APIRequest("v1/oauth2/token", HttpMethod.GET)
        val httpRequest =
            httpRequestFactory.createHttpRequestFromAPIRequest(apiRequest, configuration)
        val response = http.send(httpRequest)
        return response.run {
            val correlationID = headers["Paypal-Debug-Id"]
            if (isSuccessful) {
                if (body.isNullOrBlank()) {
                    throw APIClientError.noResponseData(correlationID)
                } else {
                    val json = PaymentsJSON(body)
                    val clientID = json.optString("client_id")
                    if (clientID.isNullOrBlank()) {
                        throw APIClientError.dataParsingError(correlationID)
                    } else {
                        clientID
                    }
                }
            } else {
                throw APIClientError.serverResponseError(correlationID)
            }
        }
    }
}
