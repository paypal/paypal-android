package com.paypal.android.cardpayments

import android.content.Context
import androidx.annotation.RawRes
import com.paypal.android.cardpayments.api.UpdateSetupTokenResponse
import com.paypal.android.cardpayments.api.UpdateSetupTokenVariables
import com.paypal.android.cardpayments.api.VaultBillingAddress
import com.paypal.android.cardpayments.api.VaultCard
import com.paypal.android.cardpayments.api.VaultPaymentSource
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.LoadRawResourceResult
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.corepayments.ResourceLoader
import com.paypal.android.corepayments.graphql.GraphQLClient
import com.paypal.android.corepayments.graphql.GraphQLRequest
import com.paypal.android.corepayments.graphql.GraphQLResult
import kotlinx.serialization.InternalSerializationApi

internal class DataVaultPaymentMethodTokensAPI internal constructor(
    private val coreConfig: CoreConfig,
    private val applicationContext: Context,
    private val graphQLClient: GraphQLClient,
    private val resourceLoader: ResourceLoader
) {

    constructor(context: Context, coreConfig: CoreConfig) : this(
        coreConfig,
        context.applicationContext,
        GraphQLClient(coreConfig),
        ResourceLoader()
    )

    suspend fun updateSetupToken(setupTokenId: String, card: Card): UpdateSetupTokenResult {
        @RawRes val resId = R.raw.graphql_query_update_setup_token
        return when (val result = resourceLoader.loadRawResource(applicationContext, resId)) {
            is LoadRawResourceResult.Success ->
                sendUpdateSetupTokenGraphQLRequest(result.value, setupTokenId, card)

            is LoadRawResourceResult.Failure -> UpdateSetupTokenResult.Failure(result.error)
        }
    }

    @OptIn(InternalSerializationApi::class)
    private suspend fun sendUpdateSetupTokenGraphQLRequest(
        query: String,
        setupTokenId: String,
        card: Card
    ): UpdateSetupTokenResult {

        val cardNumber = card.number.replace("\\s".toRegex(), "")
        val cardExpiry = "${card.expirationYear}-${card.expirationMonth}"

        val vaultBillingAddress = card.billingAddress?.let {
            VaultBillingAddress(
                addressLine1 = it.streetAddress,
                addressLine2 = it.extendedAddress,
                adminArea1 = it.region,
                adminArea2 = it.locality,
                postalCode = it.postalCode,
                countryCode = it.countryCode
            )
        }

        val vaultCard = VaultCard(
            number = cardNumber,
            expiry = cardExpiry,
            name = card.cardholderName,
            securityCode = card.securityCode,
            billingAddress = vaultBillingAddress
        )

        val paymentSource = VaultPaymentSource(card = vaultCard)

        val variables = UpdateSetupTokenVariables(
            clientId = coreConfig.clientId,
            vaultSetupToken = setupTokenId,
            paymentSource = paymentSource
        )

        val graphQLRequest = GraphQLRequest(query, variables, "UpdateVaultSetupToken")
        val graphQLResponse =
            graphQLClient.send<UpdateSetupTokenResponse, UpdateSetupTokenVariables>(graphQLRequest)
        return when (graphQLResponse) {
            is GraphQLResult.Success -> {
                val response = graphQLResponse.response
                val responseData = response.data
                if (responseData == null) {
                    val error = CardError.updateSetupTokenResponseBodyMissing(
                        response.errors,
                        graphQLResponse.correlationId
                    )
                    UpdateSetupTokenResult.Failure(error)
                } else {
                    parseSuccessfulUpdateSuccess(responseData, graphQLResponse.correlationId)
                }
            }

            is GraphQLResult.Failure -> {
                UpdateSetupTokenResult.Failure(graphQLResponse.error)
            }
        }
    }

    @OptIn(InternalSerializationApi::class)
    private fun parseSuccessfulUpdateSuccess(
        responseBody: UpdateSetupTokenResponse,
        correlationId: String?
    ): UpdateSetupTokenResult {
        return runCatching {
            val setupToken = responseBody.updateVaultSetupToken
            val status = setupToken.status
            val approveHref = if (status == "PAYER_ACTION_REQUIRED") {
                setupToken.links?.find { it.rel == "approve" }?.href
            } else {
                null
            }
            UpdateSetupTokenResult.Success(
                setupTokenId = setupToken.id,
                status = status,
                approveHref = approveHref
            )
        }.getOrElse { error ->
            val message = "Update Setup Token Failed: Response parsing failed."
            val sdkError = PayPalSDKError(0, message, correlationId, reason = error)
            UpdateSetupTokenResult.Failure(sdkError)
        }
    }
}
