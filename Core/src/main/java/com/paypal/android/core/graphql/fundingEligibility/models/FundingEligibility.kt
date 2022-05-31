package com.paypal.android.core.graphql.fundingEligibility.models

import org.json.JSONObject

internal data class FundingEligibility(
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
