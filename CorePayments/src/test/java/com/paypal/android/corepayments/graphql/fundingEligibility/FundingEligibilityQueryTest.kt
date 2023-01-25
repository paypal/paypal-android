package com.paypal.android.corepayments.graphql.fundingEligibility

import com.paypal.android.corepayments.graphql.fundingEligibility.models.FundingEligibilityIntent
import com.paypal.android.corepayments.graphql.fundingEligibility.models.SupportedCountryCurrencyType
import com.paypal.android.corepayments.graphql.fundingEligibility.models.SupportedPaymentMethodsType
import io.mockk.mockk
import junit.framework.TestCase
import org.json.JSONObject
import org.junit.Before
import org.junit.Test

class FundingEligibilityQueryTest : TestCase() {

    private lateinit var fundingEligibilityQuery: FundingEligibilityQuery

    @Before
    public override fun setUp() {
        fundingEligibilityQuery = FundingEligibilityQuery(
            clientId = "clientId",
            fundingEligibilityIntent = FundingEligibilityIntent.CAPTURE,
            currencyCode = SupportedCountryCurrencyType.USD,
            enableFunding = listOf(SupportedPaymentMethodsType.VENMO)
        )
    }

    @Test
    fun testGetQueryParams() {
        val result = fundingEligibilityQuery.queryParams
        assertEquals(result[FundingEligibilityQuery.PARAM_CLIENT_ID], "clientId")
        assertEquals(
            result[FundingEligibilityQuery.PARAM_CURRENCY],
            SupportedCountryCurrencyType.USD
        )
        assertEquals(result[FundingEligibilityQuery.PARAM_INTENT], FundingEligibilityIntent.CAPTURE)
        assertEquals(
            result[FundingEligibilityQuery.PARAM_ENABLE_FUNDING],
            listOf(SupportedPaymentMethodsType.VENMO)
        )
    }

    @Test
    fun testGetQueryName() {
        val result = fundingEligibilityQuery.queryName
        assertEquals(result, "fundingEligibility")
    }

    @Test
    fun testRequestBody() {
        val result = fundingEligibilityQuery.requestBody()
        assertNotNull(result["query"])
    }

    @Test
    fun testParse() {
        val jsonObject: JSONObject = mockk(relaxed = true)
        val result = fundingEligibilityQuery.parse(jsonObject)
        assertNotNull(result)
    }
}
