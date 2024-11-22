package com.paypal.android.cardpayments

import android.content.Context
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.CoreSDKResult
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.corepayments.ResourceLoader
import com.paypal.android.corepayments.graphql.GraphQLClient
import org.json.JSONArray
import org.json.JSONObject

internal class DataVaultPaymentMethodTokensAPI internal constructor(
    private val coreConfig: CoreConfig,
    private val graphQLClient: GraphQLClient,
    private val resourceLoader: ResourceLoader
) {

    constructor(coreConfig: CoreConfig) : this(
        coreConfig,
        GraphQLClient(coreConfig),
        ResourceLoader()
    )

    suspend fun updateSetupToken(
        context: Context,
        setupTokenId: String,
        card: Card
    ): CoreSDKResult<UpdateSetupTokenResult> {
        val query = resourceLoader.loadRawResource(context, R.raw.graphql_query_update_setup_token)

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
        return when (val result =
            graphQLClient.send(graphQLRequest, queryName = "UpdateVaultSetupToken")) {
            is CoreSDKResult.Success -> {
                val responseJSON = result.value.data
                if (responseJSON != null) {
                    val setupTokenJSON = responseJSON.getJSONObject("updateVaultSetupToken")
                    val status = setupTokenJSON.getString("status")
                    val approveHref = if (status == "PAYER_ACTION_REQUIRED") {
                        findLinkHref(setupTokenJSON, "approve")
                    } else {
                        null
                    }
                    CoreSDKResult.Success(
                        UpdateSetupTokenResult(
                            setupTokenId = setupTokenJSON.getString("id"),
                            status = status, approveHref = approveHref
                        )
                    )
                } else {
                    CoreSDKResult.Failure(
                        PayPalSDKError(
                            0,
                            "Error updating setup token: ${result.value.errors}",
                            result.value.correlationId
                        )
                    )
                }
            }

            // forward all failure results
            is CoreSDKResult.Failure -> result
        }
    }

    private fun findLinkHref(responseJSON: JSONObject, rel: String): String? {
        val linksJSON = responseJSON.optJSONArray("links") ?: JSONArray()
        for (i in 0 until linksJSON.length()) {
            val link = linksJSON.getJSONObject(i)
            if (link.getString("rel") == rel) {
                return link.getString("href")
            }
        }
        return null
    }
}
