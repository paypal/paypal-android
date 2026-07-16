package com.paypal.android.corepayments.api

import android.content.Context
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
import com.paypal.android.corepayments.model.CreateShopperSessionAppSwitchEligibilityInput
import com.paypal.android.corepayments.model.CreateShopperSessionData
import com.paypal.android.corepayments.model.CreateShopperSessionExperimentationContext
import com.paypal.android.corepayments.model.CreateShopperSessionGraphQLResponse
import com.paypal.android.corepayments.model.CreateShopperSessionPhone
import com.paypal.android.corepayments.model.CreateShopperSessionShopperSessionInput
import com.paypal.android.corepayments.model.CreateShopperSessionVariables
import com.paypal.android.corepayments.model.CreateShopperSessionWithAppSwitchEligibilityParams
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
    private val merchantId: String
) {

    constructor(coreConfig: CoreConfig, applicationContext: Context) : this(
        applicationContext = applicationContext,
        graphQLClient = GraphQLClient(coreConfig),
        resourceLoader = ResourceLoader(),
        authenticationSecureTokenServiceAPI = AuthenticationSecureTokenServiceAPI(coreConfig),
        merchantId = coreConfig.merchantId
    )

    /**
     * Calls the `createShopperSessionWithAppSwitchEligibility` GraphQL mutation.
     *
     * @param token The order or setup token (used as contextId).
     * @param tokenType The type of token (ORDER_ID, VAULT_ID, etc.).
     * @param params The app-switch return URLs and payment context for the session.
     */
    suspend operator fun invoke(
        token: String,
        tokenType: TokenType,
        params: CreateShopperSessionWithAppSwitchEligibilityParams,
    ): APIResult<CreateShopperSessionWithAppSwitchEligibilityResponse> {
        val graphQLRequest = createGraphQLRequest(
            token = token,
            tokenType = tokenType,
            params = params,
        ) ?: return APIResult.Failure(APIClientError.dataParsingError(correlationId = null))
        return sendGraphQLRequestWithLSATAuthentication(graphQLRequest)
    }

    private suspend fun createGraphQLRequest(
        token: String,
        tokenType: TokenType,
        params: CreateShopperSessionWithAppSwitchEligibilityParams,
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
            appSwitchEligibilityInput = CreateShopperSessionAppSwitchEligibilityInput(
                contextId = token,
                experimentationContext = CreateShopperSessionExperimentationContext(
                    appSwitchSupported = true,
                    buyerGUID = null,
                    merchantAccountId = merchantId,
                    integrationChannel = INTEGRATION_CHANNEL,
                    isWebLLSEligible = false,
                    isWebView = false,
                    paymentType = params.paymentType,
                ),
                merchantOptInForAppSwitch = true,
                osType = OS_TYPE,
                paypalNativeAppInstalled = params.paypalNativeAppInstalled,
                tokenType = tokenType.toGraphQLTokenType(),
                buyerEmailAddressMerchantPassed = params.buyerEmailAddressMerchantPassed,
                shoppersSessionId = params.existingPayPalSessionId,
            ),
            shopperSessionInput = CreateShopperSessionShopperSessionInput(
                returnAppUrl = params.returnAppUrl,
                cancelAppUrl = params.cancelAppUrl,
                fallbackUrlScheme = params.fallbackSchemeUrl,
                sdkVersion = BuildConfig.CLIENT_SDK_VERSION,
                phone = if (params.countryCode != null && params.nationalNumber != null) {
                    CreateShopperSessionPhone(
                        countryCode = params.countryCode,
                        nationalNumber = params.nationalNumber,
                    )
                } else {
                    null
                },
            ),
        )

        return GraphQLRequest(
            query = query,
            variables = variables,
            operationName = OPERATION_NAME
        )
    }

    private suspend fun sendGraphQLRequestWithLSATAuthentication(
        graphQLRequest: GraphQLRequest<CreateShopperSessionVariables>,
    ): APIResult<CreateShopperSessionWithAppSwitchEligibilityResponse> {
        val tokenResult = authenticationSecureTokenServiceAPI.createLowScopedAccessToken()
        return if (tokenResult is APIResult.Failure) {
            APIResult.Failure(tokenResult.error)
        } else {
            val lsat = (tokenResult as APIResult.Success).data
            val graphQLResult = graphQLClient.send<
                    CreateShopperSessionGraphQLResponse,
                    CreateShopperSessionVariables>(
                graphQLRequest,
                additionalHeaders = mapOf(Headers.AUTHORIZATION to "Bearer $lsat")
            )
            when (graphQLResult) {
                is GraphQLResult.Success -> {
                    val data = graphQLResult.response.data
                        ?.external
                        ?.createShopperSessionWithAppSwitchEligibility
                    if (data == null) {
                        APIResult.Failure(APIClientError.noResponseData(graphQLResult.correlationId))
                    } else {
                        APIResult.Success(data.toResponse())
                    }
                }
                is GraphQLResult.Failure -> APIResult.Failure(graphQLResult.error)
            }
        }
    }

    companion object {
        private const val OPERATION_NAME = "createShopperSessionWithAppSwitchEligibility"
        private const val OS_TYPE = "ANDROID"
        private const val INTEGRATION_CHANNEL = "PPCP_NATIVE_SDK"
    }
}

private fun TokenType.toGraphQLTokenType(): String = when (this) {
    TokenType.ORDER_ID -> "CHECKOUT_TOKEN"
    TokenType.VAULT_ID -> "BILLING_TOKEN"
    TokenType.BILLING_TOKEN -> "BILLING_TOKEN"
}

private fun CreateShopperSessionData.toResponse(): CreateShopperSessionWithAppSwitchEligibilityResponse {
    val appSwitch = appSwitchEligibilityResponse
    val session = shopperSessionResponse
    return CreateShopperSessionWithAppSwitchEligibilityResponse(
        appSwitchEligible = appSwitch?.appSwitchEligible ?: false,
        redirectUrl = appSwitch?.checkoutUrls?.redirectURL ?: "",
        checkoutFallbackUrl = appSwitch?.checkoutUrls?.checkoutFallbackUrl ?: "",
        ineligibleReason = appSwitch?.ineligibleReason,
        matchedAuthenticationMethods = emptyList(),
        shopperSessionConfig = ShopperSessionConfig(
            id = session?.sessionId ?: "",
            expiresAt = session?.expiresAt ?: ""
        )
    )
}
