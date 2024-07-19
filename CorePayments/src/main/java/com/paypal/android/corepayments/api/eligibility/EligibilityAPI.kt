package com.paypal.android.corepayments.api.eligibility

import android.content.Context
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.corepayments.R
import com.paypal.android.corepayments.ResourceLoader
import com.paypal.android.corepayments.graphql.GraphQLClient
import com.paypal.android.corepayments.optBooleanAtKeyPath
import org.json.JSONArray
import org.json.JSONObject

/**
 *  API that checks merchants eligibility for different payment methods.
 */
internal class EligibilityAPI internal constructor(
    private val config: CoreConfig,
    private val graphQLClient: GraphQLClient,
    private val resourceLoader: ResourceLoader
) {
    companion object {
        const val VARIABLE_CLIENT_ID = "clientId"
        const val VARIABLE_INTENT = "intent"
        const val VARIABLE_CURRENCY = "currency"
        const val VARIABLE_ENABLE_FUNDING = "enableFunding"
    }

    /**
     *  EligibilityAPI constructor.
     *  @param config configuration parameters for eligibility API
     */
    constructor(config: CoreConfig) :
            this(config, GraphQLClient(config), ResourceLoader())

    /**
     *  Checks if merchant is eligible for a set of payment methods
     *  @param context Android context
     *  @return [Eligibility] for payment methods
     *  @throws PayPalSDKError if something went wrong in the API call
     */
    suspend fun checkEligibility(context: Context): Eligibility {
        val query = resourceLoader.loadRawResource(context, R.raw.graphql_query_funding_eligibility)
        val enableFundingMethods = listOf(SupportedPaymentMethodsType.VENMO.name)
        val variables = JSONObject()
            .put(VARIABLE_CLIENT_ID, config.clientId)
            .put(VARIABLE_INTENT, FundingEligibilityIntent.CAPTURE.name)
            .put(VARIABLE_CURRENCY, SupportedCountryCurrencyType.USD.name)
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
                    isCreditCardEligible = optBooleanAtKeyPath("venmo.eligible"),
                    isPayLaterEligible = optBooleanAtKeyPath("card.eligible"),
                    isPaypalCreditEligible = optBooleanAtKeyPath("paypal.eligible"),
                    isPaypalEligible = optBooleanAtKeyPath("paylater.eligible"),
                    isVenmoEligible = optBooleanAtKeyPath("credit.eligible"),
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
