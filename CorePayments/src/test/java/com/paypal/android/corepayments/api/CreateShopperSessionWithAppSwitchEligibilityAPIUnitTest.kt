package com.paypal.android.corepayments.api

import android.content.Context
import com.paypal.android.corepayments.LoadRawResourceResult
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.corepayments.ResourceLoader
import com.paypal.android.corepayments.graphql.GraphQLClient
import com.paypal.android.corepayments.graphql.GraphQLResponse
import com.paypal.android.corepayments.graphql.GraphQLResult
import com.paypal.android.corepayments.model.APIResult
import com.paypal.android.corepayments.model.CreateShopperSessionMutationResponse
import com.paypal.android.corepayments.model.CreateShopperSessionVariables
import com.paypal.android.corepayments.model.ShopperSessionConfigData
import com.paypal.android.corepayments.model.ShopperSessionWithEligibilityData
import io.mockk.coEvery
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.InternalSerializationApi
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class CreateShopperSessionWithAppSwitchEligibilityAPIUnitTest {

    private val context: Context = mockk(relaxed = true)
    private val graphQLClient: GraphQLClient = mockk(relaxed = true)
    private val resourceLoader: ResourceLoader = mockk(relaxed = true)
    private val authenticationSecureTokenServiceAPI: AuthenticationSecureTokenServiceAPI =
        mockk(relaxed = true)

    private lateinit var sut: CreateShopperSessionWithAppSwitchEligibilityAPI

    @Before
    fun beforeEach() {
        sut = CreateShopperSessionWithAppSwitchEligibilityAPI(
            graphQLClient = graphQLClient,
            resourceLoader = resourceLoader,
            authenticationSecureTokenServiceAPI = authenticationSecureTokenServiceAPI,
        )
        // Default happy path — individual tests override as needed
        coEvery { resourceLoader.loadRawResource(any(), any()) } returns
            LoadRawResourceResult.Success("query { }")
        coEvery { authenticationSecureTokenServiceAPI.createLowScopedAccessToken() } returns
            APIResult.Success("fake-lsat-token")
    }

    // ── Companion constants ───────────────────────────────────────────────────

    @Test
    fun `INTEGRATION_CHANNEL is PPCP_NATIVE_SDK`() {
        assertEquals("PPCP_NATIVE_SDK", CreateShopperSessionWithAppSwitchEligibilityAPI.INTEGRATION_CHANNEL)
    }

    @Test
    fun `OS_TYPE is ANDROID`() {
        assertEquals("ANDROID", CreateShopperSessionWithAppSwitchEligibilityAPI.OS_TYPE)
    }

    @Test
    fun `PAYMENT_METHOD_PAYPAL is PAYPAL`() {
        assertEquals("PAYPAL", CreateShopperSessionWithAppSwitchEligibilityAPI.PAYMENT_METHOD_PAYPAL)
    }

    @Test
    fun `TOKEN_TYPE_ORDER_ID is ORDER_ID`() {
        assertEquals("ORDER_ID", CreateShopperSessionWithAppSwitchEligibilityAPI.TOKEN_TYPE_ORDER_ID)
    }

    @Test
    fun `TOKEN_TYPE_VAULT_ID is VAULT_ID`() {
        assertEquals("VAULT_ID", CreateShopperSessionWithAppSwitchEligibilityAPI.TOKEN_TYPE_VAULT_ID)
    }

    // ── invoke() — failure paths ──────────────────────────────────────────────

    @Test
    fun `invoke() returns failure when LSAT token creation fails`() = runTest {
        coEvery { authenticationSecureTokenServiceAPI.createLowScopedAccessToken() } returns
            APIResult.Failure(PayPalSDKError(0, "lsat error"))

        val result = invokeWithRealImpl()

        assertTrue(result is APIResult.Failure)
    }

    @Test
    fun `invoke() returns failure when raw resource cannot be loaded`() = runTest {
        coEvery { resourceLoader.loadRawResource(any(), any()) } returns
            LoadRawResourceResult.Failure(PayPalSDKError(0, "resource missing"))

        val result = invokeWithRealImpl()

        assertTrue(result is APIResult.Failure)
    }

    @Test
    fun `invoke() returns failure when GraphQL call fails`() = runTest {
        coEvery {
            graphQLClient.send<CreateShopperSessionMutationResponse, CreateShopperSessionVariables>(any(), any())
        } returns GraphQLResult.Failure(PayPalSDKError(0, "network error"))

        val result = invokeWithRealImpl()

        assertTrue(result is APIResult.Failure)
    }

    @OptIn(InternalSerializationApi::class)
    @Test
    fun `invoke() returns failure when GraphQL response contains no data`() = runTest {
        coEvery {
            graphQLClient.send<CreateShopperSessionMutationResponse, CreateShopperSessionVariables>(any(), any())
        } returns GraphQLResult.Success(
            response = GraphQLResponse(data = null),
            correlationId = null
        )

        val result = invokeWithRealImpl()

        assertTrue(result is APIResult.Failure)
    }

    @OptIn(InternalSerializationApi::class)
    @Test
    fun `invoke() returns failure when mutation data is null`() = runTest {
        coEvery {
            graphQLClient.send<CreateShopperSessionMutationResponse, CreateShopperSessionVariables>(any(), any())
        } returns GraphQLResult.Success(
            response = GraphQLResponse(
                data = CreateShopperSessionMutationResponse(
                    createShopperSessionWithAppSwitchEligibility = null
                )
            ),
            correlationId = null
        )

        val result = invokeWithRealImpl()

        assertTrue(result is APIResult.Failure)
    }

    // ── invoke() — success / response mapping ────────────────────────────────

    @Test
    fun `invoke() maps shopperSessionConfig id to shopperSessionId`() = runTest {
        coEvery {
            graphQLClient.send<CreateShopperSessionMutationResponse, CreateShopperSessionVariables>(any(), any())
        } returns successGraphQLResult(
            shopperSessionConfig = ShopperSessionConfigData(id = "ssid-abc", expiresAt = "2026-12-31")
        )

        val result = invokeWithRealImpl() as APIResult.Success

        assertEquals("ssid-abc", result.data.shopperSessionId)
    }

    @Test
    fun `invoke() maps shopperSessionConfig expiresAt`() = runTest {
        coEvery {
            graphQLClient.send<CreateShopperSessionMutationResponse, CreateShopperSessionVariables>(any(), any())
        } returns successGraphQLResult(
            shopperSessionConfig = ShopperSessionConfigData(id = "ssid-abc", expiresAt = "2026-12-31")
        )

        val result = invokeWithRealImpl() as APIResult.Success

        assertEquals("2026-12-31", result.data.expiresAt)
    }

    @Test
    fun `invoke() maps appSwitchEligible from response`() = runTest {
        coEvery {
            graphQLClient.send<CreateShopperSessionMutationResponse, CreateShopperSessionVariables>(any(), any())
        } returns successGraphQLResult(appSwitchEligible = true)

        val result = invokeWithRealImpl() as APIResult.Success

        assertTrue(result.data.appSwitchEligible)
    }

    @Test
    fun `invoke() maps redirectURL from response`() = runTest {
        coEvery {
            graphQLClient.send<CreateShopperSessionMutationResponse, CreateShopperSessionVariables>(any(), any())
        } returns successGraphQLResult(redirectURL = "https://paypal.com/app-switch")

        val result = invokeWithRealImpl() as APIResult.Success

        assertEquals("https://paypal.com/app-switch", result.data.redirectURL)
    }

    @Test
    fun `invoke() maps checkoutFallbackUrl from response`() = runTest {
        coEvery {
            graphQLClient.send<CreateShopperSessionMutationResponse, CreateShopperSessionVariables>(any(), any())
        } returns successGraphQLResult(checkoutFallbackUrl = "https://paypal.com/checkoutnow")

        val result = invokeWithRealImpl() as APIResult.Success

        assertEquals("https://paypal.com/checkoutnow", result.data.checkoutFallbackUrl)
    }

    @Test
    fun `invoke() returns null shopperSessionId when shopperSessionConfig is absent`() = runTest {
        coEvery {
            graphQLClient.send<CreateShopperSessionMutationResponse, CreateShopperSessionVariables>(any(), any())
        } returns successGraphQLResult(shopperSessionConfig = null)

        val result = invokeWithRealImpl() as APIResult.Success

        assertNull(result.data.shopperSessionId)
        assertNull(result.data.expiresAt)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private suspend fun invokeWithRealImpl() = sut(
        context = context,
        bnCode = null,
        flowType = "EC_ONE_TIME_CHECKOUT",
        paymentType = "continue",
        tokenType = CreateShopperSessionWithAppSwitchEligibilityAPI.TOKEN_TYPE_ORDER_ID,
        buyerEmail = null,
        paypalNativeAppInstalled = false,
        returnAppUrl = "https://example.com/return",
        cancelAppUrl = "https://example.com/cancel",
        fallbackSchemeUrl = "com.example://paypal-sdk/paypal-checkout",
        useFakeResponse = false,
    )

    @OptIn(InternalSerializationApi::class)
    private fun successGraphQLResult(
        appSwitchEligible: Boolean = false,
        redirectURL: String? = null,
        checkoutFallbackUrl: String? = null,
        shopperSessionConfig: ShopperSessionConfigData? = null,
    ) = GraphQLResult.Success(
        response = GraphQLResponse(
            data = CreateShopperSessionMutationResponse(
                createShopperSessionWithAppSwitchEligibility = ShopperSessionWithEligibilityData(
                    appSwitchEligible = appSwitchEligible,
                    redirectURL = redirectURL,
                    checkoutFallbackUrl = checkoutFallbackUrl,
                    shopperSessionConfig = shopperSessionConfig,
                )
            )
        ),
        correlationId = null
    )
}
