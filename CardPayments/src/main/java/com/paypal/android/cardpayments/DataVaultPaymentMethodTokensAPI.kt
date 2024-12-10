package com.paypal.android.cardpayments

import android.content.Context
import androidx.annotation.RawRes
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.LoadRawResourceResult
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.corepayments.ResourceLoader
import com.paypal.android.corepayments.graphql.GraphQLClient
import com.paypal.android.corepayments.graphql.GraphQLResult
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

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

    private suspend fun sendUpdateSetupTokenGraphQLRequest(
        query: String,
        setupTokenId: String,
        card: Card
    ): UpdateSetupTokenResult {

        val cardNumber = card.number.replace("\\s".toRegex(), "")
        val cardExpiry = "${card.expirationYear}-${card.expirationMonth}"

        val cardJSON = JSONObject()
            .put("number", cardNumber)
            .put("expiry", cardExpiry)

        card.cardholderName?.let { cardJSON.put("name", it) }
        cardJSON.put("securityCode", card.securityCode)

        card.billingAddress?.apply {
            val billingAddressJSON = JSONObject()
                .put("addressLine1", streetAddress)
                .put("addressLine2", extendedAddress)
                .put("adminArea1", region)
                .put("adminArea2", locality)
                .put("postalCode", postalCode)
                .put("countryCode", countryCode)
            cardJSON.put("billingAddress", billingAddressJSON)
        }

        val paymentSourceJSON = JSONObject()
        paymentSourceJSON.put("card", cardJSON)

        val variables = JSONObject()
            .put("clientId", coreConfig.clientId)
            .put("vaultSetupToken", setupTokenId)
            .put("paymentSource", paymentSourceJSON)

        val graphQLRequest = JSONObject()
            .put("query", query)
            .put("variables", variables)
        val graphQLResponse =
            graphQLClient.send(graphQLRequest, queryName = "UpdateVaultSetupToken")
        return when (graphQLResponse) {
            is GraphQLResult.Success -> {
                val responseJSON = graphQLResponse.data
                if (responseJSON == null) {
                    val error = graphQLResponse.run {
                        CardError.updateSetupTokenResponseBodyMissing(errors, correlationId)
                    }
                    UpdateSetupTokenResult.Failure(error)
                } else {
                    parseSuccessfulUpdateSuccessJSON(responseJSON, graphQLResponse.correlationId)
                }
            }

            is GraphQLResult.Failure -> {
                UpdateSetupTokenResult.Failure(graphQLResponse.error)
            }
        }
    }

    private fun parseSuccessfulUpdateSuccessJSON(
        responseBody: JSONObject,
        correlationId: String?
    ): UpdateSetupTokenResult {
        return try {
            val setupTokenJSON = responseBody.getJSONObject("updateVaultSetupToken")
            val status = setupTokenJSON.getString("status")
            val approveHref = if (status == "PAYER_ACTION_REQUIRED") {
                findLinkHref(setupTokenJSON, "approve")
            } else {
                null
            }
            UpdateSetupTokenResult.Success(
                setupTokenId = setupTokenJSON.getString("id"),
                status = status,
                approveHref = approveHref
            )
        } catch (jsonError: JSONException) {
            val message = "Update Setup Token Failed: GraphQL JSON body was invalid."
            val error = PayPalSDKError(0, message, correlationId, reason = jsonError)
            UpdateSetupTokenResult.Failure(error)
        }
    }

    private fun findLinkHref(responseJSON: JSONObject, rel: String): String? {
        val linksJSON = responseJSON.optJSONArray("links") ?: JSONArray()
        for (i in 0 until linksJSON.length()) {
            val link = linksJSON.getJSONObject(i)
            if (link.optString("rel") == rel) {
                return link.optString("href")
            }
        }
        return null
    }
}
