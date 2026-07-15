package com.paypal.android.corepayments.api

import android.content.Context
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.Environment
import com.paypal.android.corepayments.Http
import com.paypal.android.corepayments.HttpRequest
import com.paypal.android.corepayments.HttpResponse
import com.paypal.android.corepayments.LoadRawResourceResult
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.corepayments.ResourceLoader
import com.paypal.android.corepayments.graphql.GraphQLClient
import com.paypal.android.corepayments.model.APIResult
import com.paypal.android.corepayments.model.TokenType
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertSame
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.InternalSerializationApi
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Regression coverage for parsing the *real* GraphQL response shape returned by
 * createShopperSessionWithAppSwitchEligibility. Unlike tests that mock [GraphQLClient] directly,
 * these tests exercise the real [GraphQLClient] (backed by a mocked [Http]) so that the raw JSON
 * body is actually decoded via kotlinx.serialization, the same way it is in production.
 */
@ExperimentalCoroutinesApi
@OptIn(InternalSerializationApi::class)
@RunWith(RobolectricTestRunner::class)
class CreateShopperSessionWithAppSwitchEligibilityAPIUnitTest {

    private lateinit var context: Context
    private lateinit var mockHttp: Http
    private lateinit var graphQLClient: GraphQLClient
    private lateinit var resourceLoader: ResourceLoader
    private lateinit var tokenServiceAPI: AuthenticationSecureTokenServiceAPI
    private lateinit var sut: CreateShopperSessionWithAppSwitchEligibilityAPI

    @Before
    fun beforeEach() {
        context = mockk(relaxed = true)
        mockHttp = mockk(relaxed = true)

        val coreConfig = CoreConfig("test-client-id", "fake-merchant-id", Environment.SANDBOX)
        graphQLClient = GraphQLClient(coreConfig, mockHttp)

        resourceLoader = mockk(relaxed = true)
        tokenServiceAPI = mockk(relaxed = true)

        coEvery {
            resourceLoader.loadRawResource(any(), any())
        } returns LoadRawResourceResult.Success("query content")
        coEvery {
            tokenServiceAPI.createLowScopedAccessToken()
        } returns APIResult.Success("fake-token")

        sut = CreateShopperSessionWithAppSwitchEligibilityAPI(
            applicationContext = context,
            graphQLClient = graphQLClient,
            resourceLoader = resourceLoader,
            authenticationSecureTokenServiceAPI = tokenServiceAPI,
            merchantId = "fake-merchant-id"
        )
    }

    @Test
    fun `invoke successfully parses a real server response with a null expiresAt`() = runTest {
        // Given: a real payload captured from GraphQLClient where
        // shopperSessionResponse.expiresAt is explicitly null (not omitted).
        val responseJson = """
            {
              "data" : {
                "external" : {
                  "createShopperSessionWithAppSwitchEligibility" : {
                    "appSwitchEligibilityResponse" : {
                      "appSwitchEligible" : true,
                      "ineligibleReason" : null,
                      "checkoutUrls" : {
                        "redirectURL" : "https://www.paypal.com/app-switch-checkout?appSwitchEligible=true&",
                        "checkoutFallbackUrl" : "https://www.te-braintree.qa.paypal.com/checkoutnow?appSwitchEligible=false"
                      }
                    },
                    "shopperSessionResponse" : {
                      "sessionId" : "11F1-7BDF-BCAACEF2-91A9-A556CD2372A8",
                      "expiresAt" : null
                    }
                  }
                }
              },
              "extensions" : {
                "correlationId" : "13199708569d7"
              }
            }
        """.trimIndent()

        coEvery { mockHttp.send(any()) } returns HttpResponse(
            status = 200,
            body = responseJson,
            headers = emptyMap()
        )

        // When
        val result = sut(
            token = "fake-order-id",
            tokenType = TokenType.ORDER_ID,
            params = CreateShopperSessionWithAppSwitchEligibilityParams(
                returnAppUrl = "https://example.com/return",
                cancelAppUrl = "https://example.com/cancel",
                fallbackSchemeUrl = null,
                paymentType = "CONTINUE",
            ),
            paypalNativeAppInstalled = true,
        )

        // Then: the response is parsed successfully (previously failed with a JSON parse error
        // because expiresAt was declared as a non-nullable String).
        assertTrue(result is APIResult.Success)
        val data = (result as APIResult.Success).data
        assertTrue(data.appSwitchEligible)
        assertEquals(
            "https://www.paypal.com/app-switch-checkout?appSwitchEligible=true&",
            data.redirectUrl
        )
        assertEquals(
            "https://www.te-braintree.qa.paypal.com/checkoutnow?appSwitchEligible=false",
            data.checkoutFallbackUrl
        )
        assertEquals("11F1-7BDF-BCAACEF2-91A9-A556CD2372A8", data.shopperSessionConfig.id)
        assertEquals("", data.shopperSessionConfig.expiresAt)
        assertFalse(data.ineligibleReason != null)
    }

    @Test
    fun `invoke includes a phone object in the request when countryCode and nationalNumber are provided`() =
        runTest {
            val requestSlot = slot<HttpRequest>()
            coEvery { mockHttp.send(capture(requestSlot)) } returns HttpResponse(
                status = 200,
                body = """{ "data" : { "external" : { "createShopperSessionWithAppSwitchEligibility" : null } } }""",
                headers = emptyMap()
            )

            sut(
                token = "fake-order-id",
                tokenType = TokenType.ORDER_ID,
                params = CreateShopperSessionWithAppSwitchEligibilityParams(
                    returnAppUrl = "https://example.com/return",
                    cancelAppUrl = "https://example.com/cancel",
                    fallbackSchemeUrl = null,
                    paymentType = "CONTINUE",
                    countryCode = "1",
                    nationalNumber = "4155551234"
                ),
                paypalNativeAppInstalled = true,
            )

            val requestBody = requestSlot.captured.body.orEmpty()
            assertTrue(requestBody.contains(""""phone":{"countryCode":"1","nationalNumber":"4155551234"}"""))
        }

    @Test
    fun `invoke omits the phone object from the request when countryCode or nationalNumber are missing`() =
        runTest {
            val requestSlot = slot<HttpRequest>()
            coEvery { mockHttp.send(capture(requestSlot)) } returns HttpResponse(
                status = 200,
                body = """{ "data" : { "external" : { "createShopperSessionWithAppSwitchEligibility" : null } } }""",
                headers = emptyMap()
            )

            sut(
                token = "fake-order-id",
                tokenType = TokenType.ORDER_ID,
                params = CreateShopperSessionWithAppSwitchEligibilityParams(
                    returnAppUrl = "https://example.com/return",
                    cancelAppUrl = "https://example.com/cancel",
                    fallbackSchemeUrl = null,
                    paymentType = "CONTINUE",
                ),
                paypalNativeAppInstalled = true,
            )

            val requestBody = requestSlot.captured.body.orEmpty()
            assertFalse(requestBody.contains(""""phone""""))
        }

    @Test
    fun `invoke includes buyerEmailAddressMerchantPassed and shoppersSessionId in the request when provided`() =
        runTest {
            val requestSlot = slot<HttpRequest>()
            coEvery { mockHttp.send(capture(requestSlot)) } returns HttpResponse(
                status = 200,
                body = """{ "data" : { "external" : { "createShopperSessionWithAppSwitchEligibility" : null } } }""",
                headers = emptyMap()
            )

            sut(
                token = "fake-order-id",
                tokenType = TokenType.ORDER_ID,
                params = CreateShopperSessionWithAppSwitchEligibilityParams(
                    returnAppUrl = "https://example.com/return",
                    cancelAppUrl = "https://example.com/cancel",
                    fallbackSchemeUrl = null,
                    paymentType = "CONTINUE",
                ),
                paypalNativeAppInstalled = true,
                buyerEmailAddressMerchantPassed = "shopper@example.com",
                existingPayPalSessionId = "11F1-7BDF-BCAACEF2-91A9-A556CD2372A8",
            )

            val requestBody = requestSlot.captured.body.orEmpty()
            assertTrue(requestBody.contains(""""buyerEmailAddressMerchantPassed":"shopper@example.com""""))
            assertTrue(
                requestBody.contains(""""shoppersSessionId":"11F1-7BDF-BCAACEF2-91A9-A556CD2372A8"""")
            )
        }

    @Test
    fun `invoke omits buyerEmailAddressMerchantPassed and shoppersSessionId from the request when not provided`() =
        runTest {
            val requestSlot = slot<HttpRequest>()
            coEvery { mockHttp.send(capture(requestSlot)) } returns HttpResponse(
                status = 200,
                body = """{ "data" : { "external" : { "createShopperSessionWithAppSwitchEligibility" : null } } }""",
                headers = emptyMap()
            )

            sut(
                token = "fake-order-id",
                tokenType = TokenType.ORDER_ID,
                params = CreateShopperSessionWithAppSwitchEligibilityParams(
                    returnAppUrl = "https://example.com/return",
                    cancelAppUrl = "https://example.com/cancel",
                    fallbackSchemeUrl = null,
                    paymentType = "CONTINUE",
                ),
                paypalNativeAppInstalled = true,
            )

            val requestBody = requestSlot.captured.body.orEmpty()
            assertFalse(requestBody.contains(""""buyerEmailAddressMerchantPassed""""))
            assertFalse(requestBody.contains(""""shoppersSessionId""""))
        }

    @Test
    fun `invoke returns an APIResult Failure when LSAT creation fails`() = runTest {
        // Given: the LSAT (auth token) request itself fails, before the GraphQL call is
        // ever attempted.
        val lsatError = PayPalSDKError(401, "Unauthorized")
        coEvery {
            tokenServiceAPI.createLowScopedAccessToken()
        } returns APIResult.Failure(lsatError)

        // When
        val result = sut(
            token = "fake-order-id",
            tokenType = TokenType.ORDER_ID,
            params = CreateShopperSessionWithAppSwitchEligibilityParams(
                returnAppUrl = "https://example.com/return",
                cancelAppUrl = "https://example.com/cancel",
                fallbackSchemeUrl = null,
                paymentType = "CONTINUE",
            ),
            paypalNativeAppInstalled = true,
        )

        // Then: the original error is passed through unchanged as an APIResult.Failure — the
        // GraphQL call is never attempted.
        assertTrue(result is APIResult.Failure)
        assertSame(lsatError, (result as APIResult.Failure).error)
    }
}
