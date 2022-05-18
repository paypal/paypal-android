package com.paypal.android.core.graphql.fundingEligibility

import com.paypal.android.core.graphql.common.Query
import com.paypal.android.core.graphql.fundingEligibility.models.FundingEligibilityIntent
import com.paypal.android.core.graphql.fundingEligibility.models.FundingEligibilityResponse
import com.paypal.android.core.graphql.fundingEligibility.models.SupportedCountryCurrencyType
import com.paypal.android.core.graphql.fundingEligibility.models.SupportedPaymentMethodsType
import org.json.JSONObject

class FundingEligibilityQuery(
    private val clientId: String,
    private val fundingEligibilityIntent: FundingEligibilityIntent,
    private val currencyCode: SupportedCountryCurrencyType,
    private val enableFunding: List<SupportedPaymentMethodsType>
) : Query<FundingEligibilityResponse>() {

    override val queryParams: Map<String, Any>
        get() = mapOf(
            PARAM_CLIENT_ID to clientId,
            PARAM_CURRENCY to currencyCode,
            PARAM_INTENT to fundingEligibilityIntent,
            PARAM_ENABLE_FUNDING to enableFunding
        )

    override val queryName: String
        get() = "fundingEligibility"

    override val dataFieldsForResponse: String
        get() = """{
                    paypal {
                        eligible
                        reasons
                    }
                    credit {
                        eligible
                        reasons
                    }
                    paylater {
                        eligible
                        reasons
                    }
                    card{
                        eligible
                    }
                     venmo{
                        eligible
                        reasons
                     }
                }"""

    companion object {
        const val PARAM_CLIENT_ID = "clientId"
        const val PARAM_INTENT = "intent"
        const val PARAM_CURRENCY = "currency"
        const val PARAM_ENABLE_FUNDING = "enableFunding"
    }

    override fun parse(jsonObject: JSONObject): FundingEligibilityResponse {
        return FundingEligibilityResponse(jsonObject)
    }
}
