package com.paypal.android.core.api

import android.util.Log
import com.paypal.android.core.CoreConfig
import com.paypal.android.core.PayPalSDKError
import com.paypal.android.core.api.models.Eligibility
import com.paypal.android.core.graphql.common.GraphQLClient
import com.paypal.android.core.graphql.common.GraphQLClientImpl
import com.paypal.android.core.graphql.fundingEligibility.FundingEligibilityQuery
import com.paypal.android.core.graphql.fundingEligibility.models.FundingEligibilityIntent
import com.paypal.android.core.graphql.fundingEligibility.models.SupportedCountryCurrencyType
import com.paypal.android.core.graphql.fundingEligibility.models.SupportedPaymentMethodsType

class EligibilityAPI(
    private val coreConfig: CoreConfig
) {
    private val graphQLClient: GraphQLClient = GraphQLClientImpl()


    suspend fun checkEligibility(): Eligibility {
        val fundingEligibilityQuery = FundingEligibilityQuery(
            clientId = coreConfig.clientId,
            fundingEligibilityIntent = FundingEligibilityIntent.CAPTURE,
            currencyCode = SupportedCountryCurrencyType.USD,
            enableFunding = listOf(SupportedPaymentMethodsType.VENMO)
        )
        val response = graphQLClient.executeQuery(fundingEligibilityQuery)
        Log.d(TAG, response.toString())
        return if (response.data != null)
            Eligibility(
                isCreditCardEligible = response.data.fundingEligibility.card.eligible,
                isPayLaterEligible = response.data.fundingEligibility.payLater.eligible,
                isPaypalCreditEligible = response.data.fundingEligibility.credit.eligible,
                isPaypalEligible = response.data.fundingEligibility.paypal.eligible,
                isVenmoEligible = response.data.fundingEligibility.venmo.eligible,
            )
        else throw PayPalSDKError(0, "description", null)
    }

    companion object {
        const val TAG = "Eligibility API"
    }
}
