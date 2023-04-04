package com.paypal.android.corepayments

import android.util.LruCache

class SecureTokenServiceAPI internal constructor(
    private val configuration: CoreConfig,
    private val http: Http,
    private val httpRequestFactory: HttpRequestFactory,
) {

    constructor(configuration: CoreConfig): this(configuration, Http(), HttpRequestFactory())

    companion object {
        val clientIDCache = LruCache<String, String>(10)
    }

    /**
     * Retrieves the merchant's clientID either from the local cache, or via an HTTP request if not cached.
     * @return Merchant clientID.
     */
    @Throws(PayPalSDKError::class)
    suspend fun fetchCachedOrRemoteClientID(): String {
        clientIDCache.get(configuration.accessToken)?.let { cachedClientID ->
            return cachedClientID
        }

        val apiRequest = APIRequest("v1/oauth2/token", HttpMethod.GET)
        val httpRequest =
            httpRequestFactory.createHttpRequestFromAPIRequest(apiRequest, configuration)
        val response = http.send(httpRequest)
        val correlationID = response.headers["Paypal-Debug-Id"]
        if (response.isSuccessful) {
            val clientID = parseClientId(response.body, correlationID)
            clientIDCache.put(configuration.accessToken, clientID)
            return clientID
        }

        throw APIClientError.serverResponseError(correlationID)
    }

    @Throws(PayPalSDKError::class)
    private fun parseClientId(responseBody: String?, correlationID: String?): String {
        if (responseBody.isNullOrBlank()) {
            throw APIClientError.noResponseData(correlationID)
        }
        val json = PaymentsJSON(responseBody)
        val clientID = json.optString("client_id")
        if (clientID.isNullOrBlank()) {
            throw APIClientError.dataParsingError(correlationID)
        }
        return clientID
    }
}