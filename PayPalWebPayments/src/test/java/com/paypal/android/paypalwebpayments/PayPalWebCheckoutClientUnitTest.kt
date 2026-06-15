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
import com.paypal.android.corepayments.model.AppSwitchEligibility
import com.paypal.android.corepayments.model.AppSwitchEligibilityData
import com.paypal.android.corepayments.model.ShopperSessionWithAppSwitchEligibility
import com.paypal.android.paypalwebpayments.analytics.CheckoutEvent
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
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@Suppress("LargeClass")
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
    private val appLinkUrl = "https://example.com/"

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

    // ── SSID checkout tests ───────────────────────────────────────────────────

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
        shopperSessionId = ssid
    )

    @Test
    fun `startAsync() launches browser via checkoutFallbackUrl when SSID succeeds`() = runTest {
        val session = fakeSession(checkoutFallbackUrl = "https://sandbox.paypal.com/checkoutnow")
        coEvery { createShopperSessionAPI(any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns APIResult.Success(session)
        every { payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any()) } returns successLaunchResult

        val handler = CreateOrderHandler { cb -> cb(CreateOrderResponse.Success("fake-order-id")) }
        val result = sut.startAsync(activity, fakeCheckoutRequest, handler)

        assertTrue(result is PayPalPresentAuthChallengeResult.Success)
        verify { payPalWebLauncher.launchWithUrl(any(), any(), "fake-order-id", any(), any()) }
    }

    @Test
    fun `startAsync() falls back to patchCCO when SSID call fails`() = runTest {
        coEvery { createShopperSessionAPI(any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns APIResult.Failure(
            com.paypal.android.corepayments.PayPalSDKError(0, "network error")
        )
        every { payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any()) } returns successLaunchResult

        val handler = CreateOrderHandler { cb -> cb(CreateOrderResponse.Success("fake-order-id")) }
        val result = sut.startAsync(activity, fakeCheckoutRequest, handler)

        assertTrue(result is PayPalPresentAuthChallengeResult.Success)
        verify { payPalWebLauncher.launchWithUrl(any(), any(), "fake-order-id", any(), any()) }
    }

    @Test
    fun `startAsync() returns failure when createOrderHandler returns failure`() = runTest {
        val handler = CreateOrderHandler { cb -> cb(CreateOrderResponse.Failure(Exception("server error"))) }
        val result = sut.startAsync(activity, fakeCheckoutRequest, handler)

        assertTrue(result is PayPalPresentAuthChallengeResult.Failure)
    }

    @Test
    fun `startAsync() stores auth state in session store so finishStart can retrieve it`() = runTest {
        val session = fakeSession()
        coEvery { createShopperSessionAPI(any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns APIResult.Success(session)
        every { payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any()) } returns successLaunchResult

        val finishResult = PayPalWebCheckoutFinishStartResult.Success("fake-order-id", "fake-payer-id")
        every { payPalWebLauncher.completeCheckoutAuthRequest(intent, "fake-auth-state") } returns finishResult

        sut.startAsync(activity, fakeCheckoutRequest, CreateOrderHandler { cb -> cb(CreateOrderResponse.Success("fake-order-id")) })
        val result = sut.finishStart(intent)

        assertSame(finishResult, result)
    }

    // ── SSID vault tests ──────────────────────────────────────────────────────

    @Test
    fun `vaultAsync() launches browser via checkoutFallbackUrl when SSID succeeds`() = runTest {
        val session = fakeSession(checkoutFallbackUrl = "https://sandbox.paypal.com/agreements/approve")
        coEvery { createShopperSessionAPI(any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns APIResult.Success(session)
        every { payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any()) } returns successLaunchResult

        val handler = CreateSetupTokenHandler { cb -> cb(CreateSetupTokenResponse.Success("fake-setup-token-id")) }
        val result = sut.vaultAsync(activity, fakeVaultRequest, handler)

        assertTrue(result is PayPalPresentAuthChallengeResult.Success)
        verify { payPalWebLauncher.launchWithUrl(any(), any(), "fake-setup-token-id", any(), any()) }
    }

    @Test
    fun `vaultAsync() falls back to direct vault URL when SSID call fails`() = runTest {
        coEvery { createShopperSessionAPI(any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns APIResult.Failure(
            com.paypal.android.corepayments.PayPalSDKError(0, "network error")
        )
        every { payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any()) } returns successLaunchResult

        val handler = CreateSetupTokenHandler { cb -> cb(CreateSetupTokenResponse.Success("fake-setup-token-id")) }
        val result = sut.vaultAsync(activity, fakeVaultRequest, handler)

        assertTrue(result is PayPalPresentAuthChallengeResult.Success)
        verify { payPalWebLauncher.launchWithUrl(any(), any(), "fake-setup-token-id", any(), any()) }
    }

    @Test
    fun `vaultAsync() returns failure when createSetupTokenHandler returns failure`() = runTest {
        val handler = CreateSetupTokenHandler { cb -> cb(CreateSetupTokenResponse.Failure(Exception("server error"))) }
        val result = sut.vaultAsync(activity, fakeVaultRequest, handler)

        assertTrue(result is PayPalPresentAuthChallengeResult.Failure)
    }

    @Test
    fun `vaultAsync() stores auth state in session store so finishVault can retrieve it`() = runTest {
        val session = fakeSession()
        coEvery { createShopperSessionAPI(any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns APIResult.Success(session)
        every { payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any()) } returns successLaunchResult

        val vaultSuccess = PayPalWebCheckoutFinishVaultResult.Success("fake-approval-session-id")
        every { payPalWebLauncher.completeVaultAuthRequest(intent, "fake-auth-state") } returns vaultSuccess

        sut.vaultAsync(activity, fakeVaultRequest, CreateSetupTokenHandler { cb -> cb(CreateSetupTokenResponse.Success("fake-setup-token-id")) })
        val result = sut.finishVault(intent)

        assertSame(vaultSuccess, result)
    }

    // TODO(DTPPMOBILE-Story2): rewrite
//    @Test
//    fun `startAsync() launches PayPal web checkout`() = runTest { ... }

    // TODO(DTPPMOBILE-Story2): rewrite
//    @Test
//    fun `startAsync() notifies merchant of browser switch failure`() = runTest { ... }

    // TODO(DTPPMOBILE-Story2): rewrite
//    @Test
//    fun `vaultAsync() launches PayPal web checkout`() = runTest { ... }

    // TODO(DTPPMOBILE-Story2): rewrite
//    @Test
//    fun `vaultAsync() notifies merchant of browser switch failure`() = runTest { ... }

    @Test
    fun `finishStart() with session auth state returns null when start has not been called`() {
        assertNull(sut.finishStart(intent))
    }

    // TODO(DTPPMOBILE-Story2): rewrite
//      @Test
//      fun `finishStart() with session auth state forwards success result from auth launcher`() =
//          runTest {
//          val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
//              every {
//                  payPalWebLauncher.launchWithUrl(
//                      any(),
//                      any(),
//                      any(),
//                      any(),
//                      any()
//                  )
//              } returns launchResult
//  
//          val successResult =
//              PayPalWebCheckoutFinishStartResult.Success("fake-order-id", "fake-payer-id")
//          every {
//              payPalWebLauncher.completeCheckoutAuthRequest(intent, "auth state")
//          } returns successResult
//  
//              val request = PayPalWebCheckoutRequest(
//                  urlConfig = PayPalURLConfig("https://example.com/return", "https://example.com/cancel")
//              )
//              sut.startAsync(activity, request)
//          val result = sut.finishStart(intent)
//          assertSame(successResult, result)
//      }

    // TODO(DTPPMOBILE-Story2): rewrite
//      @Test
//      fun `finishStart() with restored session auth state forwards success result from auth launcher`() =
//          runTest {
//          val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
//              every {
//                  payPalWebLauncher.launchWithUrl(
//                      any(),
//                      any(),
//                      any(),
//                      any(),
//                      any()
//                  )
//              } returns launchResult
//  
//          val successResult =
//              PayPalWebCheckoutFinishStartResult.Success("fake-order-id", "fake-payer-id")
//          every {
//              payPalWebLauncher.completeCheckoutAuthRequest(intent, "auth state")
//          } returns successResult
//  
//              val launchWithUrlClient = PayPalWebCheckoutClient(
//              analytics = analytics,
//              payPalWebLauncher = payPalWebLauncher,
//              sessionStore = PayPalWebCheckoutSessionStore(),
//              updateClientConfigAPI = updateClientConfigAPI,
//                  deviceInspector = deviceInspector,
//                  coreConfig = coreConfig,
//                  urlScheme = urlScheme,
//                  patchCCOWithAppSwitchEligibility = patchCCOWithAppSwitchEligibility,
//          )
//          val request = PayPalWebCheckoutRequest(
//              urlConfig = PayPalURLConfig("https://example.com/return", "https://example.com/cancel")
//          )
//              launchWithUrlClient.startAsync(activity, request)
//  
//              sut.restore(launchWithUrlClient.instanceState)
//          val result = sut.finishStart(intent)
//          assertSame(successResult, result)
//      }

    // TODO(DTPPMOBILE-Story2): rewrite
//      @Test
//      fun `finishStart() with session auth state forwards error result from auth launcher`() =
//          runTest {
//          val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
//              every {
//                  payPalWebLauncher.launchWithUrl(
//                      any(),
//                      any(),
//                      any(),
//                      any(),
//                      any()
//                  )
//              } returns launchResult
//  
//          val error = PayPalSDKError(123, "fake-error-description")
//          val failureResult = PayPalWebCheckoutFinishStartResult.Failure(error, null)
//          every {
//              payPalWebLauncher.completeCheckoutAuthRequest(intent, "auth state")
//          } returns failureResult
//  
//              val request = PayPalWebCheckoutRequest(
//                  urlConfig = PayPalURLConfig("https://example.com/return", "https://example.com/cancel")
//              )
//              sut.startAsync(activity, request)
//          val result = sut.finishStart(intent)
//          assertSame(failureResult, result)
//      }

    // TODO(DTPPMOBILE-Story2): rewrite
//      @Test
//      fun `finishStart() with restored session auth state forwards error result from auth launcher`() =
//          runTest {
//          val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
//              every {
//                  payPalWebLauncher.launchWithUrl(
//                      any(),
//                      any(),
//                      any(),
//                      any(),
//                      any()
//                  )
//              } returns launchResult
//  
//          val error = PayPalSDKError(123, "fake-error-description")
//          val failureResult = PayPalWebCheckoutFinishStartResult.Failure(error, null)
//          every {
//              payPalWebLauncher.completeCheckoutAuthRequest(intent, "auth state")
//          } returns failureResult
//  
//              val request = PayPalWebCheckoutRequest(
//                  urlConfig = PayPalURLConfig("https://example.com/return", "https://example.com/cancel")
//              )
//              sut.startAsync(activity, request)
//          val result = sut.finishStart(intent)
//          assertSame(failureResult, result)
//      }

    // TODO(DTPPMOBILE-Story2): rewrite
//      @Test
//      fun `finishStart() with session auth state forwards cancellation result from auth launcher`() =
//          runTest {
//          val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
//              every {
//                  payPalWebLauncher.launchWithUrl(
//                      any(),
//                      any(),
//                      any(),
//                      any(),
//                      any()
//                  )
//              } returns launchResult
//  
//          val canceledResult = PayPalWebCheckoutFinishStartResult.Canceled("fake-order-id")
//          every {
//              payPalWebLauncher.completeCheckoutAuthRequest(intent, "auth state")
//          } returns canceledResult
//  
//              val request = PayPalWebCheckoutRequest(
//                  urlConfig = PayPalURLConfig("https://example.com/return", "https://example.com/cancel")
//              )
//              sut.startAsync(activity, request)
//          val result = sut.finishStart(intent)
//          assertSame(canceledResult, result)
//      }

    // TODO(DTPPMOBILE-Story2): rewrite
//      @Test
//      fun `finishStart() with restored session auth state forwards cancellation result from auth launcher`() =
//          runTest {
//          val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
//              every {
//                  payPalWebLauncher.launchWithUrl(
//                      any(),
//                      any(),
//                      any(),
//                      any(),
//                      any()
//                  )
//              } returns launchResult
//  
//          val canceledResult = PayPalWebCheckoutFinishStartResult.Canceled("fake-order-id")
//          every {
//              payPalWebLauncher.completeCheckoutAuthRequest(intent, "auth state")
//          } returns canceledResult
//  
//              val launchWithUrlClient = PayPalWebCheckoutClient(
//              analytics = analytics,
//              payPalWebLauncher = payPalWebLauncher,
//              sessionStore = PayPalWebCheckoutSessionStore(),
//              updateClientConfigAPI = updateClientConfigAPI,
//                  deviceInspector = deviceInspector,
//                  coreConfig = coreConfig,
//                  urlScheme = urlScheme,
//                  patchCCOWithAppSwitchEligibility = patchCCOWithAppSwitchEligibility
//          )
//          val request = PayPalWebCheckoutRequest(
//              urlConfig = PayPalURLConfig("https://example.com/return", "https://example.com/cancel")
//          )
//              launchWithUrlClient.startAsync(activity, request)
//  
//              sut.restore(launchWithUrlClient.instanceState)
//          val result = sut.finishStart(intent)
//          assertSame(canceledResult, result)
//      }

    // TODO(DTPPMOBILE-Story2): rewrite
//      @Test
//      fun `finishStart() with session auth state clears session to prevent delivering success event twice`() =
//          runTest {
//          val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
//              every {
//                  payPalWebLauncher.launchWithUrl(
//                      any(),
//                      any(),
//                      any(),
//                      any(),
//                      any()
//                  )
//              } returns launchResult
//  
//          val successResult =
//              PayPalWebCheckoutFinishStartResult.Success("fake-order-id", "fake-payer-id")
//          every {
//              payPalWebLauncher.completeCheckoutAuthRequest(intent, "auth state")
//          } returns successResult
//  
//              sut.startAsync(activity, PayPalWebCheckoutRequest(urlConfig = PayPalURLConfig("https://example.com/return", "https://example.com/cancel")))
//          sut.finishStart(intent)
//          assertNull(sut.finishStart(intent))
//      }

    // TODO(DTPPMOBILE-Story2): rewrite
//      @Test
//      fun `finishStart() with session auth state clears session to prevent delivering error event twice`() =
//          runTest {
//          val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
//              every {
//                  payPalWebLauncher.launchWithUrl(
//                      any(),
//                      any(),
//                      any(),
//                      any(),
//                      any()
//                  )
//              } returns launchResult
//  
//          val error = PayPalSDKError(123, "fake-error-description")
//          val failureResult = PayPalWebCheckoutFinishStartResult.Failure(error, null)
//          every {
//              payPalWebLauncher.completeCheckoutAuthRequest(intent, "auth state")
//          } returns failureResult
//  
//              sut.startAsync(activity, PayPalWebCheckoutRequest(urlConfig = PayPalURLConfig("https://example.com/return", "https://example.com/cancel")))
//          sut.finishStart(intent)
//          assertNull(sut.finishStart(intent))
//      }

    // TODO(DTPPMOBILE-Story2): rewrite
//      @Test
//      fun `finishStart() with session auth state clears session to prevent delivering cancellation event twice`() =
//          runTest {
//          val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
//              every {
//                  payPalWebLauncher.launchWithUrl(
//                      any(),
//                      any(),
//                      any(),
//                      any(),
//                      any()
//                  )
//              } returns launchResult
//  
//          val canceledResult = PayPalWebCheckoutFinishStartResult.Canceled("fake-order-id")
//          every {
//              payPalWebLauncher.completeCheckoutAuthRequest(intent, "auth state")
//          } returns canceledResult
//  
//              sut.startAsync(activity, PayPalWebCheckoutRequest(urlConfig = PayPalURLConfig("https://example.com/return", "https://example.com/cancel")))
//          sut.finishStart(intent)
//          assertNull(sut.finishStart(intent))
//      }

    // TODO(DTPPMOBILE-Story2): rewrite
//    @Test
//    fun `start() falls back to web checkout when app switch is enabled but URL is null`() = runTest { ... }

    // TODO(DTPPMOBILE-Story2): rewrite
//    @Test
//    fun `start() falls back to web checkout when app switch is enabled but empty URL is returned`() = runTest { ... }

    // TODO(DTPPMOBILE-Story2): rewrite
//    @Test
//    fun `start() skips app switch check when appSwitchWhenEligible is false`() = runTest { ... }

    // VAULT APP SWITCH TESTS

    // TODO(DTPPMOBILE-Story2): rewrite
//    @Test
//    fun `vault() falls back to web vault when app switch is enabled but empty URL is returned`() = runTest { ... }

    // TODO(DTPPMOBILE-Story2): rewrite
//    @Test
//    fun `vault() falls back to web vault when app switch request fails`() = runTest { ... }

    // TODO(DTPPMOBILE-Story2): rewrite
//    @Test
//    fun `vault() skips app switch check when appSwitchWhenEligible is false`() = runTest { ... }

    // TODO(DTPPMOBILE-Story2): rewrite
//    @Test
//    fun `start() skips app switch when app switch enabled but PayPal app not installed`() = runTest { ... }

    // TODO(DTPPMOBILE-Story2): rewrite
//    @Test
//    fun `vault() skips app switch when app switch enabled but PayPal app not installed`() = runTest { ... }

    // TODO(DTPPMOBILE-Story2): rewrite
//    @Test
//    fun `start() uses web checkout when app switch disabled regardless of PayPal app installation`() = runTest { ... }

    // TODO(DTPPMOBILE-Story2): rewrite
//    @Test
//    fun `vault() uses web vault when app switch disabled regardless of PayPal app installation`() = runTest { ... }

    @Test
    fun `finishVault() with session auth state returns null when start has not been called`() {
        assertNull(sut.finishVault(intent))
    }

    // TODO(DTPPMOBILE-Story2): rewrite
//      @Test
//      fun `finishVault() with session auth state forwards success result from auth launcher`() =
//          runTest {
//          val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
//  
//          val previousClient = PayPalWebCheckoutClient(
//              analytics = analytics,
//              payPalWebLauncher = payPalWebLauncher,
//              sessionStore = PayPalWebCheckoutSessionStore(),
//              updateClientConfigAPI = updateClientConfigAPI,
//              deviceInspector = deviceInspector,
//              coreConfig = coreConfig,
//              patchCCOWithAppSwitchEligibility = patchCCOWithAppSwitchEligibility
//          )
//  
//              every {
//                  payPalWebLauncher.launchWithUrl(
//                      any(),
//                      any(),
//                      any(),
//                      any(),
//                      any()
//                  )
//              } returns launchResult
//  
//          val successResult =
//              PayPalWebCheckoutFinishVaultResult.Success("fake-approval-session-id")
//          every {
//              payPalWebLauncher.completeVaultAuthRequest(intent, "auth state")
//          } returns successResult
//  
//              previousClient.vaultAsync(
//                  activity,
//                  PayPalWebVaultRequest(
//                      urlConfig = PayPalURLConfig("https://example.com/return", "https://example.com/cancel")
//                  )
//              )
//  
//              sut.restore(previousClient.instanceState)
//              val result = sut.finishVault(intent) as PayPalWebCheckoutFinishVaultResult.Success
//              assertSame("fake-approval-session-id", result.approvalSessionId)
//          }

    // TODO(DTPPMOBILE-Story2): rewrite
//      @Test
//      fun `finishVault() with restored session auth state forwards success result from auth launcher`() =
//          runTest {
//              val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
//  
//          val previousClient = PayPalWebCheckoutClient(
//              analytics = analytics,
//              payPalWebLauncher = payPalWebLauncher,
//              sessionStore = PayPalWebCheckoutSessionStore(),
//              updateClientConfigAPI = updateClientConfigAPI,
//              deviceInspector = deviceInspector,
//              coreConfig = coreConfig,
//              patchCCOWithAppSwitchEligibility = patchCCOWithAppSwitchEligibility
//          )
//  
//              every {
//                  payPalWebLauncher.launchWithUrl(
//                      any(),
//                      any(),
//                      any(),
//                      any(),
//                      any()
//                  )
//              } returns launchResult
//  
//              val successResult =
//                  PayPalWebCheckoutFinishVaultResult.Success("fake-approval-session-id")
//              every {
//                  payPalWebLauncher.completeVaultAuthRequest(intent, "auth state")
//              } returns successResult
//  
//              previousClient.vaultAsync(
//                  activity,
//                  PayPalWebVaultRequest(
//                      urlConfig = PayPalURLConfig("https://example.com/return", "https://example.com/cancel")
//                  )
//              )
//  
//          sut.restore(previousClient.instanceState)
//          val result = sut.finishVault(intent) as PayPalWebCheckoutFinishVaultResult.Success
//          assertSame("fake-approval-session-id", result.approvalSessionId)
//      }

    // TODO(DTPPMOBILE-Story2): rewrite
//      @Test
//      fun `finishVault() with session auth state forwards error result from auth launcher`() =
//          runTest {
//          val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
//              every {
//                  payPalWebLauncher.launchWithUrl(
//                      any(),
//                      any(),
//                      any(),
//                      any(),
//                      any()
//                  )
//              } returns launchResult
//  
//          val error = PayPalSDKError(123, "fake-error-description")
//          every {
//              payPalWebLauncher.completeVaultAuthRequest(intent, "auth state")
//          } returns PayPalWebCheckoutFinishVaultResult.Failure(error)
//  
//              sut.vaultAsync(
//                  activity,
//                  PayPalWebVaultRequest(
//                      urlConfig = PayPalURLConfig("https://example.com/return", "https://example.com/cancel")
//                  )
//              )
//          val result = sut.finishVault(intent) as PayPalWebCheckoutFinishVaultResult.Failure
//          assertSame(error, result.error)
//      }

    // TODO(DTPPMOBILE-Story2): rewrite
//      @Test
//      fun `finishVault() with restored session auth state forwards error result from auth launcher`() =
//          runTest {
//          val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
//              every {
//                  payPalWebLauncher.launchWithUrl(
//                      any(),
//                      any(),
//                      any(),
//                      any(),
//                      any()
//                  )
//              } returns launchResult
//  
//          val error = PayPalSDKError(123, "fake-error-description")
//          every {
//              payPalWebLauncher.completeVaultAuthRequest(intent, "auth state")
//          } returns PayPalWebCheckoutFinishVaultResult.Failure(error)
//  
//              sut.vaultAsync(
//                  activity,
//                  PayPalWebVaultRequest(
//                      urlConfig = PayPalURLConfig("https://example.com/return", "https://example.com/cancel")
//                  )
//              )
//  
//              sut.restore(sut.instanceState)
//          val result = sut.finishVault(intent) as PayPalWebCheckoutFinishVaultResult.Failure
//          assertSame(error, result.error)
//      }

    // TODO(DTPPMOBILE-Story2): rewrite
//      @Test
//      fun `finishVault() with session auth state forwards cancellation result from auth launcher`() =
//          runTest {
//          val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
//              every {
//                  payPalWebLauncher.launchWithUrl(
//                      any(),
//                      any(),
//                      any(),
//                      any(),
//                      any()
//                  )
//              } returns launchResult
//  
//          every {
//              payPalWebLauncher.completeVaultAuthRequest(intent, "auth state")
//          } returns PayPalWebCheckoutFinishVaultResult.Canceled
//  
//              sut.vaultAsync(
//                  activity,
//                  PayPalWebVaultRequest(
//                      urlConfig = PayPalURLConfig("https://example.com/return", "https://example.com/cancel")
//                  )
//              )
//          val result = sut.finishVault(intent)
//          assertSame(PayPalWebCheckoutFinishVaultResult.Canceled, result)
//      }

    // TODO(DTPPMOBILE-Story2): rewrite
//      @Test
//      fun `finishVault() with restored session auth state forwards cancellation result from auth launcher`() =
//          runTest {
//          val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
//              every {
//                  payPalWebLauncher.launchWithUrl(
//                      any(),
//                      any(),
//                      any(),
//                      any(),
//                      any()
//                  )
//              } returns launchResult
//  
//          every {
//              payPalWebLauncher.completeVaultAuthRequest(intent, "auth state")
//          } returns PayPalWebCheckoutFinishVaultResult.Canceled
//  
//              sut.vaultAsync(
//                  activity,
//                  PayPalWebVaultRequest(
//                      urlConfig = PayPalURLConfig("https://example.com/return", "https://example.com/cancel")
//                  )
//              )
//  
//              sut.restore(sut.instanceState)
//          val result = sut.finishVault(intent)
//          assertSame(PayPalWebCheckoutFinishVaultResult.Canceled, result)
//      }

    // TODO(DTPPMOBILE-Story2): rewrite
//      @Test
//      fun `finishVault() with session auth state clears session to prevent delivering success event twice`() =
//          runTest {
//          val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
//              every {
//                  payPalWebLauncher.launchWithUrl(
//                      any(),
//                      any(),
//                      any(),
//                      any(),
//                      any()
//                  )
//              } returns launchResult
//  
//          val successResult =
//              PayPalWebCheckoutFinishVaultResult.Success("fake-approval-session-id")
//          every {
//              payPalWebLauncher.completeVaultAuthRequest(intent, "auth state")
//          } returns successResult
//  
//              sut.vaultAsync(activity, PayPalWebVaultRequest(urlConfig = PayPalURLConfig("https://example.com/return", "https://example.com/cancel")))
//          sut.finishVault(intent)
//          assertNull(sut.finishVault(intent))
//      }

    // TODO(DTPPMOBILE-Story2): rewrite
//      @Test
//      fun `finishVault() with session auth state clears session to prevent delivering error event twice`() =
//          runTest {
//          val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
//              every {
//                  payPalWebLauncher.launchWithUrl(
//                      any(),
//                      any(),
//                      any(),
//                      any(),
//                      any()
//                  )
//              } returns launchResult
//  
//          val error = PayPalSDKError(123, "fake-error-description")
//          every {
//              payPalWebLauncher.completeVaultAuthRequest(intent, "auth state")
//          } returns PayPalWebCheckoutFinishVaultResult.Failure(error)
//  
//              sut.vaultAsync(activity, PayPalWebVaultRequest(urlConfig = PayPalURLConfig("https://example.com/return", "https://example.com/cancel")))
//          sut.finishVault(intent)
//          assertNull(sut.finishVault(intent))
//      }

    // TODO(DTPPMOBILE-Story2): rewrite
//      @Test
//      fun `finishVault() with session auth state clears session to prevent delivering cancellation event twice`() =
//          runTest {
//          val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
//              every {
//                  payPalWebLauncher.launchWithUrl(
//                      any(),
//                      any(),
//                      any(),
//                      any(),
//                      any()
//                  )
//              } returns launchResult
//  
//          every {
//              payPalWebLauncher.completeVaultAuthRequest(intent, "auth state")
//          } returns PayPalWebCheckoutFinishVaultResult.Canceled
//  
//              sut.vaultAsync(activity, PayPalWebVaultRequest(urlConfig = PayPalURLConfig("https://example.com/return", "https://example.com/cancel")))
//          sut.finishVault(intent)
//          assertNull(sut.finishVault(intent))
//      }

    // TODO(DTPPMOBILE-Story2): rewrite
//    @Test
//    fun `start() with deprecated urlScheme constructor passes urlScheme to launcher`() = runTest { ... }

    // TODO(DTPPMOBILE-Story2): rewrite
//    @Test
//    fun `vault() with deprecated urlScheme constructor passes urlScheme to launcher`() = runTest { ... }

    // TODO(DTPPMOBILE-Story2): rewrite
//    @Test
//    fun `start() with deprecated urlScheme constructor and AppLink in request prioritizes AppLink`() = runTest { ... }

    // TODO(DTPPMOBILE-Story2): rewrite
//    @Test
//    fun `vault() with deprecated urlScheme constructor and AppLink in request prioritizes AppLink`() = runTest { ... }

    // MARK: - Tests for Deprecated Methods

    // TODO(DTPPMOBILE-Story2): rewrite
//    @Test
//    fun `deprecated start() calls analytics and launchWithUrl correctly`() { ... }

    // TODO(DTPPMOBILE-Story2): rewrite
//    @Test
//    fun `deprecated start() handles failure correctly`() { ... }

    // TODO(DTPPMOBILE-Story2): rewrite
//    @Test
//    fun `deprecated vault() calls analytics and launchWithUrl correctly`() { ... }

    // TODO(DTPPMOBILE-Story2): rewrite
//    @Test
//    fun `deprecated vault() handles failure correctly`() { ... }

    // MARK: - Tests for Callback-based Methods

    // TODO(DTPPMOBILE-Story2): rewrite
//    @Test
//    fun `start() with callback method exists and can be called`() { ... }

    // TODO(DTPPMOBILE-Story2): rewrite
//    @Test
//    fun `vault() with callback method exists and can be called`() { ... }

    // Tests for returnToAppStrategy functionality

    // TODO(DTPPMOBILE-Story2): rewrite
//    @Test
//    fun `startAsync() uses CustomUrlScheme when provided`() = runTest { ... }

    // TODO(DTPPMOBILE-Story2): rewrite
//    @Test
//    fun `startAsync() uses default urlScheme when returnToAppStrategy is null`() = runTest { ... }

    // TODO(DTPPMOBILE-Story2): rewrite
//    @Test
//    fun `start() with deprecated method uses CustomUrlScheme when provided`() { ... }

    // TODO(DTPPMOBILE-Story2): rewrite
//    @Test
//    fun `startAsync() with CustomUrlScheme uses CustomUrlScheme when provided`() = runTest { ... }

    // TODO(DTPPMOBILE-Story2): rewrite
//    @Test
//    fun `vaultAsync() uses CustomUrlScheme when provided`() = runTest { ... }

    // TODO(DTPPMOBILE-Story2): rewrite
//    @Test
//    fun `vaultAsync() uses default urlScheme when returnToAppStrategy is null`() = runTest { ... }

    // TODO(DTPPMOBILE-Story2): rewrite
//    @Test
//    fun `vault() with deprecated method uses CustomUrlScheme when provided`() { ... }

    // TODO(DTPPMOBILE-Story2): rewrite
//    @Test
//    fun `vault() with callback uses CustomUrlScheme when provided`() { ... }

    // TODO(DTPPMOBILE-Story2): rewrite
//    @Test
//    fun `vault() with callback uses default urlScheme when returnToAppStrategy is null`() { ... }

    fun createAppSwithEligibility(launchUrl: String?) = AppSwitchEligibilityData(
        appSwitchEligible = !launchUrl.isNullOrEmpty(),
        redirectURL = launchUrl,
        ineligibleReason = if (launchUrl.isNullOrEmpty()) "App switch not eligible" else null
    )

    private fun createAppSwitchEligibilityResponse(redirectURL: String?): AppSwitchEligibility {
        return AppSwitchEligibility(
            appSwitchEligible = true,
            launchUrl = redirectURL,
            ineligibleReason = null
        )
    }

    // MARK: - Analytics Tests for appSwitchEnabled

    // TODO(DTPPMOBILE-Story2): rewrite
//    @Test
//    fun `startAsync() sends analytics with appSwitchEnabled false when app switch is not used`() = runTest { ... }

    // TODO(DTPPMOBILE-Story2): rewrite
//      @Test
//      fun `finishStart() sends analytics with appSwitchEnabled false for canceled event`() = runTest {
//          // Given
//          every { deviceInspector.isPayPalInstalled } returns false
//          val request = PayPalWebCheckoutRequest(
//              urlConfig = PayPalURLConfig("https://example.com/return", "https://example.com/cancel")
//          )
//          val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
//          val canceledResult = PayPalWebCheckoutFinishStartResult.Canceled("fake-order-id")
//  
//          every {
//              payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any())
//          } returns launchResult
//  
//          every {
//              payPalWebLauncher.completeCheckoutAuthRequest(intent, "auth state")
//          } returns canceledResult
//  
//          // When
//          sut.startAsync(activity, request)
//          sut.finishStart(intent)
//  
//          // Then
//          verify { analytics.notify(CheckoutEvent.CANCELED, any(), false) }
//      }

    // TODO(DTPPMOBILE-Story2): rewrite
//      @Test
//      fun `finishStart() sends analytics with appSwitchEnabled false for failed event`() = runTest {
//          // Given
//          every { deviceInspector.isPayPalInstalled } returns false
//          val request = PayPalWebCheckoutRequest(
//              urlConfig = PayPalURLConfig("https://example.com/return", "https://example.com/cancel")
//          )
//          val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
//          val error = PayPalSDKError(123, "fake-error-description")
//          val failureResult = PayPalWebCheckoutFinishStartResult.Failure(error, null)
//  
//          every {
//              payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any())
//          } returns launchResult
//  
//          every {
//              payPalWebLauncher.completeCheckoutAuthRequest(intent, "auth state")
//          } returns failureResult
//  
//          // When
//          sut.startAsync(activity, request)
//          sut.finishStart(intent)
//  
//          // Then
//          verify { analytics.notify(CheckoutEvent.FAILED, any(), false) }
//      }

    // TODO(DTPPMOBILE-Story2): rewrite
//    @Test
//    fun `vaultAsync() sends analytics with appSwitchEnabled false when app switch is not used`() = runTest { ... }

    // TODO(DTPPMOBILE-Story2): rewrite
//    @Test
//    fun `deprecated start() sends analytics with appSwitchEnabled false`() { ... }

    // TODO(DTPPMOBILE-Story2): rewrite
//    @Test
//    fun `deprecated vault() sends analytics with appSwitchEnabled false`() { ... }
}
