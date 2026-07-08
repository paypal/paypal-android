package com.paypal.android.corepayments.api

import android.content.Context
import android.os.Build
import androidx.annotation.RestrictTo
import com.paypal.android.corepayments.APIClientError
import com.paypal.android.corepayments.BuildConfig
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.LoadRawResourceResult
import com.paypal.android.corepayments.R
import com.paypal.android.corepayments.ResourceLoader
import com.paypal.android.corepayments.common.Headers
import com.paypal.android.corepayments.graphql.GraphQLClient
import com.paypal.android.corepayments.graphql.GraphQLRequest
import com.paypal.android.corepayments.graphql.GraphQLResult
import com.paypal.android.corepayments.model.APIResult
import com.paypal.android.corepayments.model.CreateShopperSessionAppSwitchData
import com.paypal.android.corepayments.model.CreateShopperSessionData
import com.paypal.android.corepayments.model.CreateShopperSessionGraphQLResponse
import com.paypal.android.corepayments.model.CreateShopperSessionSessionData
import com.paypal.android.corepayments.model.CreateShopperSessionVariables
import com.paypal.android.corepayments.model.CreateShopperSessionWithAppSwitchEligibilityResponse
import com.paypal.android.corepayments.model.ShopperSessionConfig
import com.paypal.android.corepayments.model.TokenType
import kotlinx.serialization.InternalSerializationApi

@OptIn(InternalSerializationApi::class)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class CreateShopperSessionWithAppSwitchEligibilityAPI internal constructor(
    private val applicationContext: Context,
    private val graphQLClient: GraphQLClient,
    private val resourceLoader: ResourceLoader,
    private val authenticationSecureTokenServiceAPI: AuthenticationSecureTokenServiceAPI,
) {

    constructor(coreConfig: CoreConfig, applicationContext: Context) : this(
        applicationContext = applicationContext,
        graphQLClient = GraphQLClient(coreConfig),
        resourceLoader = ResourceLoader(),
        authenticationSecureTokenServiceAPI = AuthenticationSecureTokenServiceAPI(coreConfig),
    )

    /**
     * Calls the `createShopperSessionWithAppSwitchEligibility` GraphQL mutation.
     *
     * @param token The order or setup token.
     * @param tokenType The type of token (ORDER_ID, VAULT_ID, etc.).
     * @param email Buyer email address provided by the merchant, if available.
     * @param returnAppUrl Deep-link URL to return to the app on success.
     * @param cancelAppUrl Deep-link URL to return to the app on cancellation.
     * @param fallbackSchemeUrl Custom URL scheme used as fallback.
     * @param paymentType GraphQL paymentType value (e.g. "CONTINUE", "PAY_NOW").
     * @param paypalNativeAppInstalled Whether the PayPal native app is installed.
     * @param fallbackUrl Base URL used to construct [CreateShopperSessionWithAppSwitchEligibilityResponse.checkoutFallbackUrl].
     */
    suspend operator fun invoke(
        token: String,
        tokenType: TokenType,
        email: String?,
        returnAppUrl: String,
        cancelAppUrl: String,
        fallbackSchemeUrl: String?,
        paymentType: String,
        paypalNativeAppInstalled: Boolean,
        fallbackUrl: String,
    ): APIResult<CreateShopperSessionWithAppSwitchEligibilityResponse> {
        val graphQLRequest = createGraphQLRequest(
            token = token,
            tokenType = tokenType,
            email = email,
            returnAppUrl = returnAppUrl,
            cancelAppUrl = cancelAppUrl,
            fallbackSchemeUrl = fallbackSchemeUrl,
            paymentType = paymentType,
            paypalNativeAppInstalled = paypalNativeAppInstalled,
        ) ?: return APIResult.Failure(APIClientError.dataParsingError(correlationId = null))
        return sendGraphQLRequestWithLSATAuthentication(graphQLRequest, fallbackUrl)
    }

    private suspend fun createGraphQLRequest(
        token: String,
        tokenType: TokenType,
        email: String?,
        returnAppUrl: String,
        cancelAppUrl: String,
        fallbackSchemeUrl: String?,
        paymentType: String,
        paypalNativeAppInstalled: Boolean,
    ): GraphQLRequest<CreateShopperSessionVariables>? {
        val resourceResult = resourceLoader.loadRawResource(
            applicationContext,
            R.raw.graphql_query_create_shopper_session_with_app_switch_eligibility
        )
        val query = when (resourceResult) {
            is LoadRawResourceResult.Success -> resourceResult.value
            is LoadRawResourceResult.Failure -> return null
        }

        val variables = CreateShopperSessionVariables(
            osType = OS_TYPE,
            osVersion = Build.VERSION.RELEASE,
            token = token,
            tokenType = tokenType.toGraphQLTokenType(),
            contextId = token,
            buyerEmailAddressMerchantPassed = email,
            paypalNativeAppInstalled = paypalNativeAppInstalled,
            bnCode = null,
            integrationChannel = INTEGRATION_CHANNEL,
            paymentMethodSelected = PAYMENT_METHOD_SELECTED,
            productCode = tokenType.toProductCode(),
            paymentType = paymentType,
            returnAppUrl = returnAppUrl,
            cancelAppUrl = cancelAppUrl,
            fallbackUrlScheme = fallbackSchemeUrl,
            sdkVersion = BuildConfig.CLIENT_SDK_VERSION,
        )

        return GraphQLRequest(
            query = query,
            variables = variables,
            operationName = "CreateShopperSessionWithAppSwitchEligibility"
        )
    }

    private suspend fun sendGraphQLRequestWithLSATAuthentication(
        graphQLRequest: GraphQLRequest<CreateShopperSessionVariables>,
        fallbackUrl: String,
    ): APIResult<CreateShopperSessionWithAppSwitchEligibilityResponse> {
        val tokenResult = authenticationSecureTokenServiceAPI.createLowScopedAccessToken()
        if (tokenResult is APIResult.Failure) {
            return APIResult.Failure(tokenResult.error)
        }
        val lsat = (tokenResult as APIResult.Success).data
        val graphQLResult = graphQLClient.send<
                CreateShopperSessionGraphQLResponse,
                CreateShopperSessionVariables>(
            graphQLRequest,
            additionalHeaders = mapOf(Headers.AUTHORIZATION to "Bearer $lsat")
        )
        return when (graphQLResult) {
            is GraphQLResult.Success -> {
                val data = graphQLResult.response.data
                    ?.createShopperSessionWithAppSwitchEligibility
                    ?: return APIResult.Failure(
                        APIClientError.noResponseData(graphQLResult.correlationId)
                    )
                APIResult.Success(data.toResponse(fallbackUrl))
            }
            is GraphQLResult.Failure -> APIResult.Failure(graphQLResult.error)
        }
    }

    companion object {
        private const val OS_TYPE = "ANDROID"
        private const val INTEGRATION_CHANNEL = "PPCP_NATIVE_SDK"
        private const val PAYMENT_METHOD_SELECTED = "PAYPAL"
    }
}

private fun TokenType.toGraphQLTokenType(): String = when (this) {
    TokenType.ORDER_ID -> "CHECKOUT_TOKEN"
    TokenType.VAULT_ID -> "BILLING_TOKEN"
    TokenType.BILLING_TOKEN -> "BILLING_TOKEN"
}

private fun TokenType.toProductCode(): String = when (this) {
    TokenType.ORDER_ID -> "EC_ONE_TIME_CHECKOUT"
    TokenType.VAULT_ID -> "BILLING_AGREEMENT"
    TokenType.BILLING_TOKEN -> "BILLING_AGREEMENT"
}

private fun CreateShopperSessionData.toResponse(
    fallbackUrl: String
): CreateShopperSessionWithAppSwitchEligibilityResponse {
    val appSwitch = appSwitchEligibilityResponse
    val session = shopperSessionResponse
    return CreateShopperSessionWithAppSwitchEligibilityResponse(
        appSwitchEligible = appSwitch?.appSwitchEligible ?: false,
        redirectUrl = appSwitch?.redirectURL ?: fallbackUrl,
        checkoutFallbackUrl = fallbackUrl,
        inEligibleReason = appSwitch?.ineligibleReason,
        matchedAuthenticationMethods = emptyList(),
        shopperSessionConfig = ShopperSessionConfig(
            id = session?.sessionId ?: "",
            expiresAt = session?.expiresAt ?: ""
        )
    )
}
