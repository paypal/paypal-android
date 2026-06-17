package com.paypal.android.corepayments

import org.junit.Assert.assertEquals
import org.junit.Test

class EnvironmentUnitTest {

    @Test
    fun `it should return the correct url for the LIVE environment`() {
        assertEquals("https://api-m.paypal.com", Environment.LIVE.url)
    }

    @Test
    fun `it should return the correct url for the SANDBOX environment`() {
        assertEquals("https://api-m.sandbox.paypal.com", Environment.SANDBOX.url)
    }

    @Test
    fun `it should return the correct graphQL endpoint for the LIVE environment`() {
        assertEquals("https://www.paypal.com", Environment.LIVE.graphQLEndpoint)
    }

    @Test
    fun `it should return the correct graphQL endpoint for the SANDBOX environment`() {
        assertEquals("https://www.sandbox.paypal.com", Environment.SANDBOX.graphQLEndpoint)
    }

    @Test
    fun `Custom environment should use the provided url and graphQL endpoint`() {
        val customEnv = Environment.Custom(
            customUrl = "https://api.msmaster.qa.paypal.com",
            customGraphQLEndpoint = "https://www.braintree.stage.paypal.com"
        )
        assertEquals("https://api.msmaster.qa.paypal.com", customEnv.url)
        assertEquals("https://www.braintree.stage.paypal.com", customEnv.graphQLEndpoint)
    }
}
