package com.paypal.android.corepayments.api

import androidx.annotation.RestrictTo
import com.paypal.android.corepayments.APIClientError
import com.paypal.android.corepayments.APIClientError.payPalSDKError
import com.paypal.android.corepayments.APIRequest
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.HttpMethod
import com.paypal.android.corepayments.PayPalSDKErrorCode
import com.paypal.android.corepayments.RestClient
import com.paypal.android.corepayments.base64encoded
import com.paypal.android.corepayments.common.Headers
import com.paypal.android.corepayments.model.APIResult
import org.json.JSONObject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class AuthenticationSecureTokenServiceAPI(
    private val coreConfig: CoreConfig,
    private val restClient: RestClient = RestClient(configuration = coreConfig),
) {
    suspend fun getClientToken(): APIResult<String> {
        val requestBody = "grant_type=client_credentials&response_type=token"

        val headers = mutableMapOf(
            Headers.AUTHORIZATION to "Basic ${"${coreConfig.clientId}:".base64encoded()}",
            Headers.CONTENT_TYPE to "application/x-www-form-urlencoded"
        )

        val apiRequest = APIRequest(
            path = "v1/oauth2/token",
            method = HttpMethod.POST,
            body = requestBody,
            headers = headers
        )

        return runCatching {
            val httpResponse = restClient.send(apiRequest)
            val correlationId = httpResponse.headers[Headers.PAYPAL_DEBUG_ID]
            if (httpResponse.isSuccessful && httpResponse.body != null) {
                val jsonObject = JSONObject(httpResponse.body)
                val token = jsonObject.optString("access_token")
                if (token.isNotEmpty()) {
                    APIResult.Success(token)
                } else {
                    val error = payPalSDKError(
                        code = PayPalSDKErrorCode.NO_ACCESS_TOKEN_ERROR.ordinal,
                        errorDescription = "Missing access_token in response",
                        correlationId = correlationId
                    )
                    APIResult.Failure(error)
                }
            } else {
                val error = httpResponse.run {
                    payPalSDKError(
                        code = status,
                        errorDescription = body,
                        correlationId = correlationId
                    )
                }
                APIResult.Failure(error)
            }
        }.getOrElse { throwable ->
            val error = APIClientError.unknownError(throwable = throwable)
            APIResult.Failure(error)
        }
    }
}
