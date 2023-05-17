package com.paypal.android.corepayments

import android.util.LruCache

class SecureTokenServiceAPI internal constructor(
    private val restClient: RestClient,
) {
    constructor(configuration: CoreConfig) : this(RestClient(configuration))

    @Throws(PayPalSDKError::class)
    suspend fun getClientId(): String {
        val apiRequest = APIRequest("v1/oauth2/token", HttpMethod.GET)
        val response = restClient.send(apiRequest)
        val correlationID = response.headers["Paypal-Debug-Id"]
        if (response.isSuccessful) {
            return parseClientId(response.body, correlationID)
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

    companion object {
        val clientIDCache = LruCache<String, String>(10)
    }
}
