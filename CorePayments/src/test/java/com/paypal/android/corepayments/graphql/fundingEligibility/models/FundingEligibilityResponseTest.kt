package com.paypal.android.corepayments.graphql.fundingEligibility.models

import junit.framework.TestCase
import org.json.JSONObject
import org.junit.Test

class FundingEligibilityResponseTest : TestCase() {

    @Test
    fun testFundingEligibilityResponse() {
        val jsonObject = JSONObject(VALID_FUNDING_ELIGIBILITY_RESPONSE)
        val fundingEligibilityResponse = FundingEligibilityResponse(jsonObject)
        assertEquals(fundingEligibilityResponse.fundingEligibility.venmo.eligible, true)
        assertEquals(fundingEligibilityResponse.fundingEligibility.card.eligible, false)
    }

    companion object {
        const val VALID_FUNDING_ELIGIBILITY_RESPONSE = """
            {
        "fundingEligibility": {
            "venmo": {
                "eligible": true,
                "reasons": [
                    "isPaymentMethodEnabled",
                    "isMSPEligible",
                    "isUnilateralPaymentSupported",
                    "isEnvEligible",
                    "isMerchantCountryEligible",
                    "isBuyerCountryEligible",
                    "isIntentEligible",
                    "isCommitEligible",
                    "isVaultEligible",
                    "isCurrencyEligible",
                    "isPaymentMethodDisabled",
                    "isDeviceEligible",
                    "VENMO OPT-IN WITH ENABLE_FUNDING"
                ]
            },
            "card": {
                "eligible": false
            },
            "paypal": {
                "eligible": true,
                "reasons": [
                    "isPaymentMethodEnabled",
                    "isMSPEligible",
                    "isUnilateralPaymentSupported",
                    "isEnvEligible",
                    "isMerchantCountryEligible",
                    "isBuyerCountryEligible",
                    "isIntentEligible",
                    "isCommitEligible",
                    "isVaultEligible",
                    "isCurrencyEligible",
                    "isPaymentMethodDisabled",
                    "isDeviceEligible"
                ]
            },
            "paylater": {
                "eligible": true,
                "reasons": [
                    "isPaymentMethodEnabled",
                    "isMSPEligible",
                    "isUnilateralPaymentSupported",
                    "isEnvEligible",
                    "isMerchantCountryEligible",
                    "isBuyerCountryEligible",
                    "isIntentEligible",
                    "isCommitEligible",
                    "isVaultEligible",
                    "isCurrencyEligible",
                    "isPaymentMethodDisabled",
                    "isDeviceEligible",
                    "CRC OFFERS SERV CALLED: Trmt_xo_xobuyernodeserv_call_crcoffersserv",
                    "CRC OFFERS SERV ELIGIBLE"
                ]
            },
            "credit": {
                "eligible": false,
                "reasons": [
                    "INELIGIBLE DUE TO PAYLATER ELIGIBLE"
                ]
            }
        }
}
        """
    }
}
