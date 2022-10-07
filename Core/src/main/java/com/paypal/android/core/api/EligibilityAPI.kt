package com.paypal.android.core.api

import com.paypal.android.core.API
import com.paypal.android.core.CoreConfig
import com.paypal.android.core.PayPalSDKError
import com.paypal.android.core.api.models.Eligibility
import com.paypal.android.core.graphql.common.GraphQLClient
import com.paypal.android.core.graphql.common.GraphQLClientImpl
import com.paypal.android.core.graphql.fundingEligibility.FundingEligibilityQuery
import com.paypal.android.core.graphql.fundingEligibility.models.FundingEligibilityIntent
import com.paypal.android.core.graphql.fundingEligibility.models.SupportedCountryCurrencyType
import com.paypal.android.core.graphql.fundingEligibility.models.SupportedPaymentMethodsType

/**
 *  API that checks merchants eligibility for different payment methods.
 */
internal class EligibilityAPI internal constructor(
    private val api: API,
    private val graphQLClient: GraphQLClient
) {

    /**
     *  EligibilityAPI constructor
     *  @param coreConfig configuration parameters for eligibility API
     */
    constructor(coreConfig: CoreConfig) : this(
        API(coreConfig),
        GraphQLClientImpl(coreConfig)
    )

    /**
     *  Checks if merchant is eligible for a set of payment methods
     *  @return [Eligibility] for payment methods
     *  @throws PayPalSDKError if something went wrong in the API call
     */
    suspend fun checkEligibility(): Eligibility {
        val fundingEligibilityQuery = FundingEligibilityQuery(
            clientId = api.getClientId(),
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
