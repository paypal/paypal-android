package com.paypal.android.corepayments.api

import androidx.annotation.RestrictTo
import com.paypal.android.corepayments.APIClientError
import com.paypal.android.corepayments.APIRequest
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.HttpMethod
import com.paypal.android.corepayments.RestClient
import com.paypal.android.corepayments.base64encoded
import org.json.JSONObject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class FetchClientToken(
    private val coreConfig: CoreConfig,
    private val restClient: RestClient = RestClient(configuration = coreConfig),
) {
    suspend operator fun invoke(): String {
        val requestBody = "grant_type=client_credentials&response_type=token"

        val credentials = "${coreConfig.clientId}:"
        val headers = mutableMapOf(
            "Authorization" to "Basic ${credentials.base64encoded()}",
            "Content-Type" to "application/x-www-form-urlencoded"
        )

        val apiRequest = APIRequest(
            path = "v1/oauth2/token",
            method = HttpMethod.POST,
            body = requestBody,
            headers = headers
        )
        val httpResponse = restClient.send(apiRequest)
        if (httpResponse.isSuccessful) {
            val jsonObject = JSONObject(httpResponse.body ?: "")
            return jsonObject.getString("access_token")
        } else {
            throw httpResponse.run { buildError(status, body) }
        }
    }
}

private fun buildError(code: Int, message: String?) = APIClientError.payPalCheckoutError(
    "Error fetching client token: code: $code, message: ${message ?: "No response body"}"
)
