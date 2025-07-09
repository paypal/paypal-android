package com.paypal.android.corepayments.api

import android.content.Context
import com.paypal.android.corepayments.APIClientError
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.corepayments.RestClient
import com.paypal.android.corepayments.common.Headers
import com.paypal.android.corepayments.graphql.GraphQLClient
import com.paypal.android.corepayments.graphql.GraphQLResult
import com.paypal.android.corepayments.model.APIResult
import com.paypal.android.corepayments.model.ExperimentationContext
import com.paypal.android.corepayments.model.ExternalResponse
import com.paypal.android.corepayments.model.PatchCcoWithAppSwitchEligibilityRequest
import com.paypal.android.corepayments.model.PatchCcoWithAppSwitchEligibilityResponse
import com.paypal.android.corepayments.model.TokenType
import com.paypal.android.corepayments.model.Variables

class PatchCCOWithAppSwitchEligibility internal constructor(
    private val authenticationSecureTokenServiceAPI: AuthenticationSecureTokenServiceAPI,
    private val graphQLClient: GraphQLClient,
) {

    constructor(coreConfig: CoreConfig) : this(
        authenticationSecureTokenServiceAPI = AuthenticationSecureTokenServiceAPI(
            coreConfig,
            RestClient(coreConfig)
        ),
        graphQLClient = GraphQLClient(coreConfig)
    )

    suspend operator fun invoke(
        context: Context,
        orderId: String,
        tokenType: TokenType,
        merchantOptInForAppSwitch: Boolean,
        paypalNativeAppInstalled: Boolean
    ): APIResult<PatchCcoWithAppSwitchEligibilityResponse> {
        return runCatching {
            val token =
                when (val tokenResult = authenticationSecureTokenServiceAPI.createLowScopedAccessToken()) {
                    is APIResult.Success -> tokenResult.data
                    is APIResult.Failure -> return APIResult.Failure(tokenResult.error)
                }

            val variables = Variables(
                experimentationContext = ExperimentationContext(),
                tokenType = tokenType.name,
                contextId = orderId,
                token = orderId,
                merchantOptInForAppSwitch = merchantOptInForAppSwitch,
                paypalNativeAppInstalled = paypalNativeAppInstalled
            )

            val patchCcoWithAppSwitchEligibilityRequestBody =
                PatchCcoWithAppSwitchEligibilityRequest(variables).create(context)

            when (val result = graphQLClient.send(
                graphQLRequestBody = patchCcoWithAppSwitchEligibilityRequestBody,
                headers = mapOf(Headers.AUTHORIZATION to "Bearer $token")
            )) {
                is GraphQLResult.Success -> {
                    val data = result.data ?: throw APIClientError.dataParsingError(result.correlationId)
                    val patchCcoResponse = PatchCcoWithAppSwitchEligibilityResponse.parse(data, result.correlationId)
                    APIResult.Success(PatchCcoWithAppSwitchEligibilityResponse(ExternalResponse(patchCcoResponse)))
                }
                is GraphQLResult.Failure -> APIResult.Failure(result.error)
            }
        }.getOrElse { throwable ->
            when (throwable) {
                is PayPalSDKError -> APIResult.Failure(throwable)
                else -> APIResult.Failure(APIClientError.unknownError(throwable = throwable))
            }
        }
    }
}
