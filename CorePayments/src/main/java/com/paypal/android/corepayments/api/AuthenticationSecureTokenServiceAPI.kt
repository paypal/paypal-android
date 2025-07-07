package com.paypal.android.corepayments.api

import androidx.annotation.RestrictTo
import com.paypal.android.corepayments.APIClientError
import com.paypal.android.corepayments.APIRequest
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.HttpMethod
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.corepayments.PayPalSDKErrorCode
import com.paypal.android.corepayments.RestClient
import com.paypal.android.corepayments.base64encoded
import org.json.JSONObject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class AuthenticationSecureTokenServiceAPI(
    private val coreConfig: CoreConfig,
    private val restClient: RestClient = RestClient(configuration = coreConfig),
) {
    suspend fun createLowScopedAccessToken(): CreateLowScopedAccessTokenResult {
        val requestBody = "grant_type=client_credentials&response_type=token"

        val headers = mutableMapOf(
            "Authorization" to "Basic ${coreConfig.clientId.base64encoded()}:",
            "Content-Type" to "application/x-www-form-urlencoded"
        )

        val apiRequest = APIRequest(
            path = "v1/oauth2/token",
            method = HttpMethod.POST,
            body = requestBody,
            headers = headers
        )

        return runCatching {
            val httpResponse = restClient.send(apiRequest)
            val correlationId = httpResponse.headers["paypal-debug-id"]
            if (httpResponse.isSuccessful && httpResponse.body != null) {
                val jsonObject = JSONObject(httpResponse.body)
                val token = jsonObject.optString("access_token")
                if (token.isNotEmpty()) {
                    CreateLowScopedAccessTokenResult.Success(token)
                } else {
                    val error = PayPalSDKError(
                        code = PayPalSDKErrorCode.NO_ACCESS_TOKEN_ERROR.ordinal,
                        errorDescription = "Missing access_token in response",
                        correlationId = correlationId
                    )
                    CreateLowScopedAccessTokenResult.Failure(error)
                }
            } else {
                val error = httpResponse.run {
                    PayPalSDKError(
                        code = status,
                        errorDescription = body ?: "Unknown error",
                        correlationId = correlationId
                    )
                }
                CreateLowScopedAccessTokenResult.Failure(error)
            }
        }.getOrElse { throwable ->
            val error = APIClientError.unknownError(throwable = throwable)
            CreateLowScopedAccessTokenResult.Failure(error)
        }
    }
}
