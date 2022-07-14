package com.paypal.android.core.api

import com.paypal.android.core.CoreConfig
import com.paypal.android.core.PayPalSDKError
import com.paypal.android.core.api.models.Eligibility
import com.paypal.android.core.graphql.common.GraphQLClient
import com.paypal.android.core.graphql.common.GraphQLClientImpl
import com.paypal.android.core.graphql.fundingEligibility.FundingEligibilityQuery
import com.paypal.android.core.graphql.fundingEligibility.models.FundingEligibilityIntent
import com.paypal.android.core.graphql.fundingEligibility.models.SupportedCountryCurrencyType
import com.paypal.android.core.graphql.fundingEligibility.models.SupportedPaymentMethodsType

class EligibilityAPI internal constructor(
    private val coreConfig: CoreConfig,
    private val graphQLClient: GraphQLClient = GraphQLClientImpl(coreConfig)
) {

    constructor(coreConfig: CoreConfig) : this(coreConfig, GraphQLClientImpl(coreConfig))

    suspend fun checkEligibility(): Eligibility {
        if (coreConfig.clientId.isNullOrEmpty()) {
            throw PayPalSDKError(
                0,
                "Client Id should not be null or empty"
            )
        }
        val fundingEligibilityQuery = FundingEligibilityQuery(
            clientId = coreConfig.clientId,
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
