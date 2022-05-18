package com.paypal.android.core.graphql.fundingEligibility.models

import org.json.JSONObject

data class FundingEligibilityResponse(
    private val jsonObject: JSONObject,
    val fundingEligibility: FundingEligibility = FundingEligibility(
        jsonObject.getJSONObject("fundingEligibility")
    )
)
