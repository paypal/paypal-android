package com.paypal.android.core.graphql.fundingEligibility

import com.paypal.android.core.graphql.common.getJSONArrayOrNull
import com.paypal.android.core.graphql.common.toStringsList
import org.json.JSONObject

data class FundingEligibilityResponse(
    private val jsonObject: JSONObject,
    val fundingEligibility: FundingEligibility = FundingEligibility(
        jsonObject.getJSONObject("fundingEligibility")
    )
)

data class FundingEligibility(
    private val jsonObject: JSONObject,
    val venmo: SupportedPaymentMethodsTypeEligibility = SupportedPaymentMethodsTypeEligibility(
        jsonObject.getJSONObject("venmo")
    ),
    val card: SupportedPaymentMethodsTypeEligibility = SupportedPaymentMethodsTypeEligibility(
        jsonObject.getJSONObject("card")
    ),
    val paypal: SupportedPaymentMethodsTypeEligibility = SupportedPaymentMethodsTypeEligibility(
        jsonObject.getJSONObject("paypal")
    ),
    val payLater: SupportedPaymentMethodsTypeEligibility = SupportedPaymentMethodsTypeEligibility(
        jsonObject.getJSONObject("paylater")
    ),
    val credit: SupportedPaymentMethodsTypeEligibility = SupportedPaymentMethodsTypeEligibility(
        jsonObject.getJSONObject("credit")
    ),
)

data class SupportedPaymentMethodsTypeEligibility(
    private val jsonObject: JSONObject,
    val eligible: Boolean = jsonObject.getBoolean("eligible"),
    val reasons: List<String>? = jsonObject.getJSONArrayOrNull("reasons").toStringsList()
)
