package com.paypal.android.corepayments.api

import android.content.Context
import android.os.Build
import androidx.annotation.RestrictTo
import com.paypal.android.corepayments.APIClientError
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.LoadRawResourceResult
import com.paypal.android.corepayments.R
import com.paypal.android.corepayments.ResourceLoader
import com.paypal.android.corepayments.common.Headers
import com.paypal.android.corepayments.graphql.GraphQLClient
import com.paypal.android.corepayments.graphql.GraphQLRequest
import com.paypal.android.corepayments.graphql.GraphQLResult
import com.paypal.android.corepayments.model.APIResult
import com.paypal.android.corepayments.model.CreateShopperSessionMutationResponse
import com.paypal.android.corepayments.model.CreateShopperSessionVariables
import com.paypal.android.corepayments.model.ShopperSessionWithAppSwitchEligibility
import kotlinx.serialization.InternalSerializationApi
import java.util.UUID

@OptIn(InternalSerializationApi::class)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class CreateShopperSessionWithAppSwitchEligibilityAPI internal constructor(
    private val graphQLClient: GraphQLClient,
    private val resourceLoader: ResourceLoader,
    private val authenticationSecureTokenServiceAPI: AuthenticationSecureTokenServiceAPI,
) {

    constructor(coreConfig: CoreConfig) : this(
        graphQLClient = GraphQLClient(coreConfig),
        resourceLoader = ResourceLoader(),
        authenticationSecureTokenServiceAPI = AuthenticationSecureTokenServiceAPI(coreConfig),
    )

    /**
     * Calls the `createShopperSessionWithAppSwitchEligibility` GraphQL mutation.
     *
     * Fires SSID creation and app-switch eligibility check in a single call.
     * On success, returns [ShopperSessionWithAppSwitchEligibility] which the SDK uses to
     * route checkout/vault via app switch or browser. On failure, the caller should silently
     * fall back to the patchCCO path.
     */
    suspend operator fun invoke(
        context: Context,
        bnCode: String?,
        flowType: String,
        paymentType: String,
        buyerEmail: String?,
        paypalNativeAppInstalled: Boolean,
        returnAppUrl: String,
        cancelAppUrl: String,
        fallbackSchemeUrl: String,
    ): APIResult<ShopperSessionWithAppSwitchEligibility> {
        val resourceResult = resourceLoader.loadRawResource(
            context,
            R.raw.graphql_query_create_shopper_session_with_app_switch_eligibility
        )
        val query = when (resourceResult) {
            is LoadRawResourceResult.Success -> resourceResult.value
            is LoadRawResourceResult.Failure ->
                return APIResult.Failure(APIClientError.dataParsingError(correlationId = null))
        }

        val variables = CreateShopperSessionVariables(
            bnCode = bnCode,
            sdkVersion = SDK_VERSION,
            osVersion = Build.VERSION.RELEASE,
            osType = OS_TYPE,
            integrationChannel = INTEGRATION_CHANNEL,
            paymentMethodSelected = PAYMENT_METHOD_PAYPAL,
            flowType = flowType,
            paymentType = paymentType,
            contextId = UUID.randomUUID().toString(),
            buyerEmailAddressMerchantPassed = buyerEmail,
            paypalNativeAppInstalled = paypalNativeAppInstalled,
            returnAppUrl = returnAppUrl,
            cancelAppUrl = cancelAppUrl,
            fallbackSchemeUrl = fallbackSchemeUrl,
        )

        val graphQLRequest = GraphQLRequest(
            query = query,
            variables = variables,
            operationName = "CreateShopperSessionWithAppSwitchEligibility"
        )

        return sendWithLSATAuthentication(graphQLRequest)
    }

    private suspend fun sendWithLSATAuthentication(
        graphQLRequest: GraphQLRequest<CreateShopperSessionVariables>,
    ): APIResult<ShopperSessionWithAppSwitchEligibility> {
        val tokenResult = authenticationSecureTokenServiceAPI.createLowScopedAccessToken()
        if (tokenResult is APIResult.Failure) {
            return APIResult.Failure(tokenResult.error)
        }
        val token = (tokenResult as APIResult.Success).data
        val graphQLResult = graphQLClient.send<
                CreateShopperSessionMutationResponse,
                CreateShopperSessionVariables>(
            graphQLRequest,
            additionalHeaders = mapOf(Headers.AUTHORIZATION to "Bearer $token")
        )
        return when (graphQLResult) {
            is GraphQLResult.Success -> {
                graphQLResult.response.data?.let { responseData ->
                    parseResponse(responseData, graphQLResult.correlationId)
                } ?: APIResult.Failure(
                    APIClientError.noResponseData(graphQLResult.correlationId)
                )
            }
            is GraphQLResult.Failure -> APIResult.Failure(graphQLResult.error)
        }
    }

    private fun parseResponse(
        response: CreateShopperSessionMutationResponse,
        correlationId: String?,
    ): APIResult<ShopperSessionWithAppSwitchEligibility> {
        val data = response.createShopperSessionWithAppSwitchEligibility
            ?: return APIResult.Failure(APIClientError.dataParsingError(correlationId))

        return APIResult.Success(
            ShopperSessionWithAppSwitchEligibility(
                appSwitchEligible = data.appSwitchEligible,
                redirectURL = data.redirectURL,
                checkoutFallbackUrl = data.checkoutFallbackUrl,
                ineligibleReason = data.ineligibleReason,
                shopperSessionId = data.shopperSessionConfig?.id,
            )
        )
    }

    companion object {
        const val INTEGRATION_CHANNEL = "PPCP_NATIVE_SDK"
        const val OS_TYPE = "ANDROID"
        const val PAYMENT_METHOD_PAYPAL = "PAYPAL"

        // TODO: replace with BuildConfig.VERSION_NAME once a version constant is wired up
        const val SDK_VERSION = "1.0.0"
    }
}
