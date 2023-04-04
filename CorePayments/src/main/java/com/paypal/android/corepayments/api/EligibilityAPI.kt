package com.paypal.android.corepayments.api

import android.content.Context
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.PayPalGraphQLClient
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.corepayments.R
import com.paypal.android.corepayments.ResourceLoader
import com.paypal.android.corepayments.SecureTokenServiceAPI
import com.paypal.android.corepayments.api.models.Eligibility
import com.paypal.android.corepayments.graphql.fundingEligibility.models.FundingEligibility
import com.paypal.android.corepayments.graphql.fundingEligibility.models.FundingEligibilityIntent
import com.paypal.android.corepayments.graphql.fundingEligibility.models.SupportedCountryCurrencyType
import com.paypal.android.corepayments.graphql.fundingEligibility.models.SupportedPaymentMethodsType
import org.json.JSONObject


/**
 *  API that checks merchants eligibility for different payment methods.
 */
internal class EligibilityAPI internal constructor(
    private val secureTokenServiceAPI: SecureTokenServiceAPI,
    private val graphQLClient: PayPalGraphQLClient,
    private val resourceLoader: ResourceLoader
) {

    /**
     *  EligibilityAPI constructor
     *  @param coreConfig configuration parameters for eligibility API
     */
    constructor(context: Context, coreConfig: CoreConfig) : this(
        SecureTokenServiceAPI(coreConfig),
        PayPalGraphQLClient(coreConfig),
        ResourceLoader(context)
    )

    suspend fun checkEligibility2(): Eligibility {
        val clientId = secureTokenServiceAPI.fetchCachedOrRemoteClientID()

        val query = resourceLoader.loadRawResource(R.raw.graphql_query_funding_eligibility)
        val variables = JSONObject()
            .put("clientId", clientId)
            .put("intent", FundingEligibilityIntent.CAPTURE)
            .put("currency", SupportedCountryCurrencyType.USD)
            .put("enableFunding", listOf(SupportedPaymentMethodsType.VENMO))

        val graphQLRequest = JSONObject()
            .put("query", query)
            .put("variables", variables)
        val graphQLResponse = graphQLClient.send(graphQLRequest)

        return if (graphQLResponse.isSuccessful) {
            val fundingEligibility =
                FundingEligibility(graphQLResponse.data.getJSONObject("fundingEligibility"))
            Eligibility(
                isCreditCardEligible = fundingEligibility.card.eligible,
                isPayLaterEligible = fundingEligibility.payLater.eligible,
                isPaypalCreditEligible = fundingEligibility.credit.eligible,
                isPaypalEligible = fundingEligibility.paypal.eligible,
                isVenmoEligible = fundingEligibility.venmo.eligible,
            )
        } else {
            throw PayPalSDKError(
                0,
                "Error in checking eligibility: ${graphQLResponse.errors}",
                graphQLResponse.correlationID
            )
        }
    }

    companion object {
        const val TAG = "Eligibility API"
    }
}
