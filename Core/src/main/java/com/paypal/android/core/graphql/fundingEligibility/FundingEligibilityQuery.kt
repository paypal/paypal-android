package com.paypal.android.core.graphql.fundingEligibility

import com.paypal.android.core.graphql.common.Query
import org.json.JSONObject

class FundingEligibilityQuery(
    private val clientId: String,
    private val intent: Intent,
    private val currencyCode: SupportedCountryCurrencyType,
    private val enableFunding: List<SupportedPaymentMethodsType>
) : Query<FundingEligibilityResponse>() {

    override val queryParams: Map<String, Any>
        get() = mapOf(
            PARAM_CLIENT_ID to clientId,
            PARAM_CURRENCY to currencyCode,
            PARAM_INTENT to intent,
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
