package com.paypal.android.corepayments.api

import android.content.Context
import androidx.annotation.RestrictTo
import com.paypal.android.corepayments.APIClientError
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.RestClient
import com.paypal.android.corepayments.common.Headers
import com.paypal.android.corepayments.graphql.GraphQLClient
import com.paypal.android.corepayments.graphql.GraphQLResult
import com.paypal.android.corepayments.model.APIResult
import com.paypal.android.corepayments.model.AppSwitchEligibility
import com.paypal.android.corepayments.model.ExperimentationContext
import com.paypal.android.corepayments.model.PatchCcoWithAppSwitchEligibilityRequest
import com.paypal.android.corepayments.model.TokenType
import com.paypal.android.corepayments.model.Variables
import org.json.JSONObject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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
    ): APIResult<AppSwitchEligibility> {
        val token = when (val tokenResult =
            authenticationSecureTokenServiceAPI.createLowScopedAccessToken()) {
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

        return when (val result = graphQLClient.send(
            graphQLRequestBody = patchCcoWithAppSwitchEligibilityRequestBody,
            headers = mapOf(Headers.AUTHORIZATION to "Bearer $token")
        )) {
            is GraphQLResult.Success -> {
                parseResponse(result.data)?.let { appSwitchEligibility ->
                    APIResult.Success(data = appSwitchEligibility)
                } ?: APIResult.Failure(
                    APIClientError.dataParsingError(result.correlationId)
                )
            }

            is GraphQLResult.Failure -> APIResult.Failure(result.error)
        }
    }

    fun parseResponse(data: JSONObject?): AppSwitchEligibility? {
        val external = data?.optJSONObject("external")
        val patchCco = external?.optJSONObject("patchCcoWithAppSwitchEligibility")
        val appSwitchEligibilityJson = patchCco?.optJSONObject("appSwitchEligibility")

        return appSwitchEligibilityJson?.let {
            AppSwitchEligibility(
                appSwitchEligible = it.optBoolean("appSwitchEligible"),
                launchUrl = it.optString("redirectURL"),
                ineligibleReason = it.optString("ineligibleReason")
            )
        }
    }
}
