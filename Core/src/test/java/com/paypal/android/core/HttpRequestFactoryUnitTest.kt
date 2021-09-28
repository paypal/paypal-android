package com.paypal.android.core

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import java.net.URL

@RunWith(Enclosed::class)
class HttpRequestFactoryUnitTest {

    companion object {
        private const val CLIENT_ID = "sample-client-id"
        private const val CLIENT_SECRET = "sample-client-secret"
    }

    private lateinit var sut: HttpRequestFactory

    @Before
    fun beforeEach() {
        sut = HttpRequestFactory()
    }

    @RunWith(ParameterizedRobolectricTestRunner::class)
    class URLTests(private val configuration: PaymentsConfiguration, private val expected: URL, private val envName: String) {

        companion object {
            private val SANDBOX_CONFIGURATION = PaymentsConfiguration(CLIENT_ID, CLIENT_SECRET, Environment.SANDBOX)
            private val STAGING_CONFIGURATION = PaymentsConfiguration(CLIENT_ID, CLIENT_SECRET, Environment.STAGING)
            private val LIVE_CONFIGURATION = PaymentsConfiguration(CLIENT_ID, CLIENT_SECRET, Environment.LIVE)

            @JvmStatic
            @ParameterizedRobolectricTestRunner.Parameters(name = "{0} environment")
            fun configurationScenarios() = listOf(
                arrayOf(SANDBOX_CONFIGURATION, URL("https://api.sandbox.paypal.com/sample/path"), "Sandbox"),
                arrayOf(STAGING_CONFIGURATION, URL("https://api.msmaster.qa.paypal.com/sample/path"), "Staging"),
                arrayOf(LIVE_CONFIGURATION, URL("https://api.paypal.com/sample/path"), "Live")
            )
        }

        private val apiRequest = APIRequest("sample/path", HttpMethod.GET, null)

        private lateinit var sut: HttpRequestFactory

        @Before
        fun beforeEach() {
            sut = HttpRequestFactory()
        }

        @Test
        fun `it should properly format the url for the`() {
            val result = sut.createHttpRequestFromAPIRequest(apiRequest, configuration)
            assertEquals(expected, result.url)
        }
    }
}
