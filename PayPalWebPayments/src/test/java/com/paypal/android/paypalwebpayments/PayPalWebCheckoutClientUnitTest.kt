package com.paypal.android.paypalwebpayments

import android.content.Intent
import androidx.fragment.app.FragmentActivity
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.Environment
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.corepayments.UpdateClientConfigAPI
import com.paypal.android.corepayments.api.CreateShopperSessionWithAppSwitchEligibilityAPI
import com.paypal.android.corepayments.api.PatchCCOWithAppSwitchEligibility
import com.paypal.android.corepayments.common.DeviceInspector
import com.paypal.android.corepayments.model.APIResult
import com.paypal.android.corepayments.model.ShopperSessionWithAppSwitchEligibility
import com.paypal.android.paypalwebpayments.analytics.PayPalWebAnalytics
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertSame
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class PayPalWebCheckoutClientUnitTest {

    @MockK
    private val activity: FragmentActivity = mockk(relaxed = true)

    @MockK
    private val analytics = mockk<PayPalWebAnalytics>(relaxed = true)

    @MockK
    private val patchCCOWithAppSwitchEligibility: PatchCCOWithAppSwitchEligibility =
        mockk(relaxed = true)

    @MockK
    private val createShopperSessionAPI: CreateShopperSessionWithAppSwitchEligibilityAPI =
        mockk(relaxed = true)

    @MockK
    private val deviceInspector: DeviceInspector = mockk(relaxed = true)
    private val coreConfig = CoreConfig("fake-client-id", "fake-merchant-id", Environment.SANDBOX)

    @MockK
    private val updateClientConfigAPI: UpdateClientConfigAPI = mockk(relaxed = true)

    private val intent = Intent()

    @MockK
    private val payPalWebLauncher: PayPalWebLauncher = mockk(relaxed = true)
    private lateinit var sut: PayPalWebCheckoutClient

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun beforeEach() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)
        sut = PayPalWebCheckoutClient(
            analytics = analytics,
            payPalWebLauncher = payPalWebLauncher,
            sessionStore = PayPalWebCheckoutSessionStore(),
            updateClientConfigAPI = updateClientConfigAPI,
            patchCCOWithAppSwitchEligibility = patchCCOWithAppSwitchEligibility,
            createShopperSessionAPI = createShopperSessionAPI,
            deviceInspector = deviceInspector,
            coreConfig = coreConfig,
            applicationScope = CoroutineScope(testDispatcher)
        )
    }

    @After
    fun afterEach() {
        Dispatchers.resetMain()
    }

    // ── Shared test fixtures ──────────────────────────────────────────────────

    private val fakeReturnToAppUrlConfig = ReturnToAppUrlConfig(
        returnAppUrl = "https://example.com/return",
        cancelAppUrl = "https://example.com/cancel",
        fallbackSchemeUrl = "com.example.app://paypal-sdk/paypal-checkout"
    )

    private val fakeCheckoutRequest = PayPalWebCheckoutRequest(
        userIdentity = PayPalUserIdentity.Unknown,
        returnToAppUrlConfig = fakeReturnToAppUrlConfig
    )

    private val fakeVaultRequest = PayPalWebVaultRequest(
        userIdentity = PayPalUserIdentity.Unknown,
        returnToAppUrlConfig = fakeReturnToAppUrlConfig
    )

    private val successLaunchResult = PayPalPresentAuthChallengeResult.Success("fake-auth-state")

    private fun fakeSession(
        appSwitchEligible: Boolean = false,
        redirectURL: String? = null,
        checkoutFallbackUrl: String? = "https://sandbox.paypal.com/checkoutnow",
        ssid: String? = "fake-ssid"
    ) = ShopperSessionWithAppSwitchEligibility(
        appSwitchEligible = appSwitchEligible,
        redirectURL = redirectURL,
        checkoutFallbackUrl = checkoutFallbackUrl,
        ineligibleReason = null,
        shopperSessionId = ssid,
        expiresAt = null,
    )

    // ── start() / checkout tests ──────────────────────────────────────────────

    @Test
    fun `start() launches browser via checkoutFallbackUrl when SSID succeeds`() = runTest {
        val session = fakeSession(checkoutFallbackUrl = "https://sandbox.paypal.com/checkoutnow")
        coEvery { createShopperSessionAPI(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns APIResult.Success(session)
        every { payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any()) } returns successLaunchResult

        var capturedResult: PayPalPresentAuthChallengeResult? = null
        sut.start(activity, fakeCheckoutRequest, CreateOrderHandler { cb ->
            cb(CreateOrderResponse.Success("fake-order-id"))
        }) { capturedResult = it }
        advanceUntilIdle()

        assertTrue(capturedResult is PayPalPresentAuthChallengeResult.Success)
        verify { payPalWebLauncher.launchWithUrl(any(), any(), "fake-order-id", any(), any()) }
    }

    @Test
    fun `start() falls back to patchCCO when SSID call fails`() = runTest {
        coEvery { createShopperSessionAPI(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns APIResult.Failure(
            PayPalSDKError(0, "network error")
        )
        every { payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any()) } returns successLaunchResult

        var capturedResult: PayPalPresentAuthChallengeResult? = null
        sut.start(activity, fakeCheckoutRequest, CreateOrderHandler { cb ->
            cb(CreateOrderResponse.Success("fake-order-id"))
        }) { capturedResult = it }
        advanceUntilIdle()

        assertTrue(capturedResult is PayPalPresentAuthChallengeResult.Success)
        verify { payPalWebLauncher.launchWithUrl(any(), any(), "fake-order-id", any(), any()) }
    }

    @Test
    fun `start() delivers failure when createOrderHandler returns failure`() = runTest {
        var capturedResult: PayPalPresentAuthChallengeResult? = null
        sut.start(activity, fakeCheckoutRequest, CreateOrderHandler { cb ->
            cb(CreateOrderResponse.Failure(Exception("server error")))
        }) { capturedResult = it }
        advanceUntilIdle()

        assertTrue(capturedResult is PayPalPresentAuthChallengeResult.Failure)
    }

    @Test
    fun `start() stores auth state in session store so finishStart can retrieve it`() = runTest {
        val session = fakeSession()
        coEvery { createShopperSessionAPI(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns APIResult.Success(session)
        every { payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any()) } returns successLaunchResult

        val finishResult = PayPalWebCheckoutFinishStartResult.Success("fake-order-id", "fake-payer-id")
        every { payPalWebLauncher.completeCheckoutAuthRequest(intent, "fake-auth-state") } returns finishResult

        sut.start(activity, fakeCheckoutRequest, CreateOrderHandler { cb ->
            cb(CreateOrderResponse.Success("fake-order-id"))
        }) { /* launch callback */ }
        advanceUntilIdle()

        assertSame(finishResult, sut.finishStart(intent))
    }

    // ── vault() tests ─────────────────────────────────────────────────────────

    @Test
    fun `vault() launches browser via checkoutFallbackUrl when SSID succeeds`() = runTest {
        val session = fakeSession(checkoutFallbackUrl = "https://sandbox.paypal.com/agreements/approve")
        coEvery { createShopperSessionAPI(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns APIResult.Success(session)
        every { payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any()) } returns successLaunchResult

        var capturedResult: PayPalPresentAuthChallengeResult? = null
        sut.vault(activity, fakeVaultRequest, CreateSetupTokenHandler { cb ->
            cb(CreateSetupTokenResponse.Success("fake-setup-token-id"))
        }) { capturedResult = it }
        advanceUntilIdle()

        assertTrue(capturedResult is PayPalPresentAuthChallengeResult.Success)
        verify { payPalWebLauncher.launchWithUrl(any(), any(), "fake-setup-token-id", any(), any()) }
    }

    @Test
    fun `vault() falls back to direct vault URL when SSID call fails`() = runTest {
        coEvery { createShopperSessionAPI(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns APIResult.Failure(
            PayPalSDKError(0, "network error")
        )
        every { payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any()) } returns successLaunchResult

        var capturedResult: PayPalPresentAuthChallengeResult? = null
        sut.vault(activity, fakeVaultRequest, CreateSetupTokenHandler { cb ->
            cb(CreateSetupTokenResponse.Success("fake-setup-token-id"))
        }) { capturedResult = it }
        advanceUntilIdle()

        assertTrue(capturedResult is PayPalPresentAuthChallengeResult.Success)
        verify { payPalWebLauncher.launchWithUrl(any(), any(), "fake-setup-token-id", any(), any()) }
    }

    @Test
    fun `vault() delivers failure when createSetupTokenHandler returns failure`() = runTest {
        var capturedResult: PayPalPresentAuthChallengeResult? = null
        sut.vault(activity, fakeVaultRequest, CreateSetupTokenHandler { cb ->
            cb(CreateSetupTokenResponse.Failure(Exception("server error")))
        }) { capturedResult = it }
        advanceUntilIdle()

        assertTrue(capturedResult is PayPalPresentAuthChallengeResult.Failure)
    }

    @Test
    fun `vault() stores auth state in session store so finishVault can retrieve it`() = runTest {
        val session = fakeSession()
        coEvery { createShopperSessionAPI(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns APIResult.Success(session)
        every { payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any()) } returns successLaunchResult

        val vaultSuccess = PayPalWebCheckoutFinishVaultResult.Success("fake-approval-session-id")
        every { payPalWebLauncher.completeVaultAuthRequest(intent, "fake-auth-state") } returns vaultSuccess

        sut.vault(activity, fakeVaultRequest, CreateSetupTokenHandler { cb ->
            cb(CreateSetupTokenResponse.Success("fake-setup-token-id"))
        }) { /* launch callback */ }
        advanceUntilIdle()

        assertSame(vaultSuccess, sut.finishVault(intent))
    }

    // ── finishStart() / finishVault() null guard tests ────────────────────────

    @Test
    fun `finishStart() returns null when start has not been called`() {
        assertNull(sut.finishStart(intent))
    }

    @Test
    fun `finishVault() returns null when vault has not been called`() {
        assertNull(sut.finishVault(intent))
    }
}
