package com.paypal.android.core.graphql.fundingEligibility.models

import com.paypal.android.core.graphql.common.getJSONArrayOrNull
import com.paypal.android.core.graphql.common.toStringsList
import org.json.JSONObject

internal data class SupportedPaymentMethodsTypeEligibility(
    private val jsonObject: JSONObject,
    val eligible: Boolean = jsonObject.getBoolean("eligible"),
    val reasons: List<String>? = jsonObject.getJSONArrayOrNull("reasons").toStringsList()
)
