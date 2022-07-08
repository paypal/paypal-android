package com.paypal.android.core

import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RobolectricTestRunner
import java.net.URL

@RunWith(Enclosed::class)
class HttpRequestFactoryUnitTest {

    @RunWith(ParameterizedRobolectricTestRunner::class)
    class URLTests(private val configuration: CoreConfig, private val expected: URL) {

        companion object {
            private val SANDBOX_CONFIGURATION = CoreConfig(Environment.SANDBOX)
            private val STAGING_CONFIGURATION = CoreConfig(Environment.STAGING)
            private val LIVE_CONFIGURATION = CoreConfig(Environment.LIVE)

            @JvmStatic
            @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
            fun configurationScenarios() = listOf(
                arrayOf(
                    SANDBOX_CONFIGURATION,
                    URL("https://api.sandbox.paypal.com/sample/path")
                ),
                arrayOf(
                    STAGING_CONFIGURATION,
                    URL("https://api.msmaster.qa.paypal.com/sample/path")
                ),
                arrayOf(LIVE_CONFIGURATION, URL("https://api.paypal.com/sample/path"))
            )
        }

        private val apiRequest = APIRequest("sample/path", HttpMethod.GET, null)

        private lateinit var sut: HttpRequestFactory

        private val authHandler = mockk<AuthHandler>(relaxed = true)

        @Before
        fun beforeEach() {
            sut = HttpRequestFactory()
            every { authHandler.getAuthHeader() } returns "mock_header"
        }

        @Test
        fun `it should properly format the url for `() {
            val result = sut.createHttpRequestFromAPIRequest(apiRequest, configuration, authHandler)
            assertEquals(expected, result.url)
        }
    }

    @RunWith(RobolectricTestRunner::class)
    class NonParameterizedTests {

        private val configuration = CoreConfig()

        private val authHandler = mockk<AuthHandler>()

        private val requestBody = """{ "sample": "json" }"""

        private lateinit var sut: HttpRequestFactory

        @Before
        fun beforeEach() {
            sut = HttpRequestFactory("sample-language")
            every { authHandler.getAuthHeader() } returns "mock_header"
        }

        @Test
        fun `it should forward the http method`() {
            val apiRequest = APIRequest("sample/path", HttpMethod.POST, requestBody)
            val result = sut.createHttpRequestFromAPIRequest(apiRequest, configuration, authHandler)

            assertEquals(HttpMethod.POST, result.method)
        }

        @Test
        fun `it should forward the http body`() {
            val apiRequest = APIRequest("sample/path", HttpMethod.POST, requestBody)
            val result = sut.createHttpRequestFromAPIRequest(apiRequest, configuration, authHandler)

            assertEquals(requestBody, result.body)
        }

        @Test
        fun `it should set accept encoding default header`() {
            val apiRequest = APIRequest("sample/path", HttpMethod.POST, requestBody)
            val result = sut.createHttpRequestFromAPIRequest(apiRequest, configuration, authHandler)

            assertEquals("gzip", result.headers["Accept-Encoding"])
        }

        @Test
        fun `it should set accept language default header`() {
            val apiRequest = APIRequest("sample/path", HttpMethod.POST, requestBody)
            val result = sut.createHttpRequestFromAPIRequest(apiRequest, configuration, authHandler)

            assertEquals("sample-language", result.headers["Accept-Language"])
        }

        @Test
        fun `it should add basic auth authorization header`() {
            val mockClientId = "mock_client_id"
            val apiRequest = APIRequest("sample/path", HttpMethod.POST, requestBody)
            val result = sut.createHttpRequestFromAPIRequest(
                apiRequest,
                configuration,
                AuthHandler.fromClientId(mockClientId)
            )

            val expected = "Basic ${"$mockClientId:".base64encoded()}"
            assertEquals(expected, result.headers["Authorization"])
        }

        @Test
        fun `it should add bearer token authorization header`() {
            val mockAccessToken = "mock_access_token"
            val apiRequest = APIRequest("sample/path", HttpMethod.POST, requestBody)
            val result = sut.createHttpRequestFromAPIRequest(
                apiRequest,
                configuration,
                AuthHandler.fromToken(mockAccessToken)
            )

            val expected = "Bearer $mockAccessToken"
            assertEquals(expected, result.headers["Authorization"])
        }

        @Test
        fun `it should add content type json header for POST requests`() {
            val apiRequest = APIRequest("sample/path", HttpMethod.POST, requestBody)
            val result = sut.createHttpRequestFromAPIRequest(apiRequest, configuration, authHandler)

            assertEquals("application/json", result.headers["Content-Type"])
        }

        @Test
        fun `it should add content type json header for GET requests`() {
            val apiRequest = APIRequest("sample/path", HttpMethod.GET, requestBody)
            val result = sut.createHttpRequestFromAPIRequest(apiRequest, configuration, authHandler)

            assertNull(result.headers["Content-Type"])
        }
    }
}
