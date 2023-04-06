package com.paypal.android.corepayments.api

import android.content.Context
import com.paypal.android.corepayments.API
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.corepayments.R
import com.paypal.android.corepayments.ResourceLoader
import com.paypal.android.corepayments.api.models.Eligibility
import com.paypal.android.corepayments.graphql.common.GraphQLClient
import com.paypal.android.corepayments.graphql.common.GraphQLClientImpl
import com.paypal.android.corepayments.graphql.fundingEligibility.FundingEligibilityQuery
import com.paypal.android.corepayments.graphql.fundingEligibility.models.FundingEligibilityIntent
import com.paypal.android.corepayments.graphql.fundingEligibility.models.SupportedCountryCurrencyType
import com.paypal.android.corepayments.graphql.fundingEligibility.models.SupportedPaymentMethodsType
import org.json.JSONArray
import org.json.JSONObject

/**
 *  API that checks merchants eligibility for different payment methods.
 */
internal class EligibilityAPI internal constructor(
    private val api: API,
    private val graphQLClient: GraphQLClient,
    private val resourceLoader: ResourceLoader
) {
    /**
     *  EligibilityAPI constructor.
     *  @param context Android context
     *  @param config configuration parameters for eligibility API
     */
    constructor(context: Context, config: CoreConfig) :
            this(API(config, context), GraphQLClientImpl(config), ResourceLoader(context))

    /**
     *  Checks if merchant is eligible for a set of payment methods
     *  @return [Eligibility] for payment methods
     *  @throws PayPalSDKError if something went wrong in the API call
     */
    suspend fun checkEligibility(): Eligibility {
        val clientID = api.fetchCachedOrRemoteClientID()

        val query = resourceLoader.loadRawResource(R.raw.graphql_query_funding_eligibility)
        val enableFundingMethods = listOf(SupportedPaymentMethodsType.VENMO.name)
        val variables = JSONObject()
            .put("clientId", clientID)
            .put("intent", FundingEligibilityIntent.CAPTURE.name)
            .put("currency", SupportedCountryCurrencyType.USD.name)
            .put("enableFunding", JSONArray(enableFundingMethods))

        val graphQLRequest = JSONObject()
            .put("query", query)
            .put("variables", variables)
        val graphQLResponse = graphQLClient.send(graphQLRequest)
        return Eligibility(false, false, false, false, false)

        val fundingEligibilityQuery = FundingEligibilityQuery(
            clientId = api.fetchCachedOrRemoteClientID(),
            fundingEligibilityIntent = FundingEligibilityIntent.CAPTURE,
            currencyCode = SupportedCountryCurrencyType.USD,
            enableFunding = listOf(SupportedPaymentMethodsType.VENMO)
        )
        val response = graphQLClient.executeQuery(fundingEligibilityQuery)
        return if (response.data != null) {
            Eligibility(
                isCreditCardEligible = response.data.fundingEligibility.card.eligible,
                isPayLaterEligible = response.data.fundingEligibility.payLater.eligible,
                isPaypalCreditEligible = response.data.fundingEligibility.credit.eligible,
                isPaypalEligible = response.data.fundingEligibility.paypal.eligible,
                isVenmoEligible = response.data.fundingEligibility.venmo.eligible,
            )
        } else {
            throw PayPalSDKError(
                0,
                "Error in checking eligibility: ${response.errors}",
                response.correlationId
            )
        }
    }

    companion object {
        const val TAG = "Eligibility API"
    }
}
