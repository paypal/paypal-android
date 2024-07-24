package com.paypal.android.corepayments.apis.eligibility

import android.content.Context
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.corepayments.R
import com.paypal.android.corepayments.ResourceLoader
import com.paypal.android.corepayments.features.eligibility.EligibilityRequest
import com.paypal.android.corepayments.graphql.GraphQLClient
import com.paypal.android.corepayments.optBooleanAtKeyPath
import org.json.JSONArray
import org.json.JSONObject

internal class EligibilityAPI(
    private val config: CoreConfig,
    private val graphQLClient: GraphQLClient = GraphQLClient(config),
    private val resourceLoader: ResourceLoader = ResourceLoader()
) {
    companion object {
        const val VARIABLE_CLIENT_ID = "clientId"
        const val VARIABLE_INTENT = "intent"
        const val VARIABLE_CURRENCY = "currency"
        const val VARIABLE_ENABLE_FUNDING = "enableFunding"
    }

    @Throws(PayPalSDKError::class)
    suspend fun checkEligibility(context: Context, request: EligibilityRequest): Eligibility {
        val query = resourceLoader.loadRawResource(context, R.raw.graphql_query_funding_eligibility)
        val enableFundingMethods = listOf(SupportedPaymentMethod.VENMO.name)
        val currency = SupportedCountryCurrencyType.valueOf(request.currencyCode)
        val variables = JSONObject()
            .put(VARIABLE_CLIENT_ID, config.clientId)
            .put(VARIABLE_INTENT, request.intent.name)
            .put(VARIABLE_CURRENCY, currency.name)
            .put(VARIABLE_ENABLE_FUNDING, JSONArray(enableFundingMethods))

        val graphQLRequest = JSONObject()
            .put("query", query)
            .put("variables", variables)
        val graphQLResponse = graphQLClient.send(graphQLRequest)
        if (graphQLResponse.data != null) {
            val fundingEligibility =
                graphQLResponse.data.optJSONObject("fundingEligibility") ?: JSONObject()

            return fundingEligibility.run {
                Eligibility(
                    isVenmoEligible = optBooleanAtKeyPath("venmo.eligible"),
                    isCreditCardEligible = optBooleanAtKeyPath("card.eligible"),
                    isPayPalEligible = optBooleanAtKeyPath("paypal.eligible"),
                    isPayLaterEligible = optBooleanAtKeyPath("paylater.eligible"),
                    isPayPalCreditEligible = optBooleanAtKeyPath("credit.eligible"),
                )
            }
        } else {
            throw PayPalSDKError(
                0,
                "Error in checking eligibility: ${graphQLResponse.errors}",
                graphQLResponse.correlationId
            )
        }
    }
}
