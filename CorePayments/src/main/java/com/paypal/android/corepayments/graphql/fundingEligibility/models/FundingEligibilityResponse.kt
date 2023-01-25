package com.paypal.android.corepayments.graphql.fundingEligibility.models

import org.json.JSONObject

internal data class FundingEligibilityResponse(
    private val jsonObject: JSONObject,
    val fundingEligibility: FundingEligibility = FundingEligibility(
        jsonObject.getJSONObject("fundingEligibility")
    )
)
