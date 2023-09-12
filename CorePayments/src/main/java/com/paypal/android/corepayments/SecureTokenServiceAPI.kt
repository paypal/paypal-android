package com.paypal.android.corepayments

// TODO: remove since we no longer support access token integration
class SecureTokenServiceAPI internal constructor(
    private val restClient: RestClient,
) {
    constructor(configuration: CoreConfig) : this(RestClient(configuration))

    @Throws(PayPalSDKError::class)
    suspend fun getClientId(): String {
        val apiRequest = APIRequest("v1/oauth2/token", HttpMethod.GET)
        val response = restClient.send(apiRequest)
        val correlationId = response.headers["Paypal-Debug-Id"]
        if (response.isSuccessful) {
            return parseClientId(response.body, correlationId)
        }
        throw APIClientError.serverResponseError(correlationId)
    }

    @Throws(PayPalSDKError::class)
    private fun parseClientId(responseBody: String?, correlationId: String?): String {
        if (responseBody.isNullOrBlank()) {
            throw APIClientError.noResponseData(correlationId)
        }
        val json = PaymentsJSON(responseBody)
        val clientId = json.optString("client_id")
        if (clientId.isNullOrBlank()) {
            throw APIClientError.dataParsingError(correlationId)
        }
        return clientId
    }
}
