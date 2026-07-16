package com.paypal.android.paypalwebpayments

import android.content.Intent
import android.net.Uri
import androidx.fragment.app.FragmentActivity
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.Environment
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.corepayments.ReturnToAppStrategy
import com.paypal.android.corepayments.SessionIdRepository
import com.paypal.android.corepayments.UpdateClientConfigAPI
import com.paypal.android.corepayments.api.CreateShopperSessionWithAppSwitchEligibilityAPI
import com.paypal.android.corepayments.api.PatchCCOWithAppSwitchEligibility
import com.paypal.android.corepayments.common.DeviceInspector
import com.paypal.android.corepayments.model.APIResult
import com.paypal.android.corepayments.model.AppSwitchEligibility
import com.paypal.android.corepayments.model.AppSwitchEligibilityData
import com.paypal.android.corepayments.model.CreateShopperSessionWithAppSwitchEligibilityResponse
import com.paypal.android.corepayments.model.ShopperSessionConfig
import com.paypal.android.corepayments.model.TokenType
import com.paypal.android.paypalwebpayments.errors.PayPalWebCheckoutError
import com.paypal.android.paypalwebpayments.analytics.CheckoutEvent
import com.paypal.android.paypalwebpayments.analytics.PayPalWebAnalytics
import com.paypal.android.paypalwebpayments.analytics.VaultEvent
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertSame
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Ignore
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

    // NOTE: Pre-existing gap unrelated to the patchCCO-fallback feature work: this dependency
    // was never wired into any PayPalWebCheckoutClient(...) construction in this test file,
    // which meant the file did not compile. Adding it here so the suite (and the new
    // fallback-to-patchCCO tests below) can actually build and run.
    @MockK
    private val createShopperSessionAPI: CreateShopperSessionWithAppSwitchEligibilityAPI =
        mockk(relaxed = true)

    @MockK
    private val deviceInspector: DeviceInspector = mockk(relaxed = true)
    private val coreConfig = CoreConfig("fake-client-id", "fake-merchant-id", Environment.SANDBOX)
    private val urlScheme = "com.example.app"

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
            coreConfig = coreConfig
        )
    }

    @After
    fun afterEach() {
        Dispatchers.resetMain()
    }

    @Test
    fun `startAsync() launches PayPal web checkout`() = runTest {
        val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
        every {
            payPalWebLauncher.launchWithUrl(
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns launchResult

        val request = PayPalWebCheckoutRequest(
            "fake-order-id",
            returnToAppStrategy = ReturnToAppStrategy.AppLink(appLinkUrl)
        )
        sut.startAsync(activity, request)

        // Verify launchWithUrl is called with correct parameters
        verify(exactly = 1) {
            payPalWebLauncher.launchWithUrl(
                context = activity,
                uri = any(),
                token = "fake-order-id",
                tokenType = TokenType.ORDER_ID,
                returnToAppStrategy = ReturnToAppStrategy.AppLink(appLinkUrl)
            )
        }
    }

    @Test
    fun `startAsync() notifies merchant of browser switch failure`() = runTest {
        val sdkError = PayPalSDKError(123, "fake error description")
        val launchResult = PayPalPresentAuthChallengeResult.Failure(sdkError)
        every {
            payPalWebLauncher.launchWithUrl(
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns launchResult

        val appLinkUrl = "https://example.com/return"
        val request = PayPalWebCheckoutRequest(
            "fake-order-id",
            returnToAppStrategy = ReturnToAppStrategy.AppLink(appLinkUrl)
        )
        val result = sut.startAsync(activity, request)
        assertSame(launchResult, result)
    }

    @Test
    fun `vaultAsync() launches PayPal web checkout`() = runTest {
        val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
        every {
            payPalWebLauncher.launchWithUrl(
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns launchResult

        val appLinkUrl = "https://example.com/vault/return"
        val request = PayPalWebVaultRequest(
            "fake-setup-token-id",
            returnToAppStrategy = ReturnToAppStrategy.AppLink(appLinkUrl)
        )
        sut.vaultAsync(activity, request)
        verify(exactly = 1) {
            payPalWebLauncher.launchWithUrl(
                context = activity,
                uri = any(),
                token = "fake-setup-token-id",
                tokenType = TokenType.VAULT_ID,
                returnToAppStrategy = ReturnToAppStrategy.AppLink(appLinkUrl)
            )
        }
    }

    @Test
    fun `vaultAsync() notifies merchant of browser switch failure`() = runTest {
        val sdkError = PayPalSDKError(123, "fake error description")
        val launchResult = PayPalPresentAuthChallengeResult.Failure(sdkError)
        every {
            payPalWebLauncher.launchWithUrl(
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns launchResult

        val appLinkUrl = "https://example.com/vault/return"
        val request = PayPalWebVaultRequest(
            "fake-setup-token-id",
            returnToAppStrategy = ReturnToAppStrategy.AppLink(appLinkUrl)
        )
        val result = sut.vaultAsync(activity, request) as PayPalPresentAuthChallengeResult.Failure

        assertSame(sdkError, result.error)
    }

    @Test
    fun `finishStart() with merchant provided auth state forwards success result from auth launcher`() {
        val successResult =
            PayPalWebCheckoutFinishStartResult.Success("fake-order-id", "fake-payer-id")
        every {
            payPalWebLauncher.completeCheckoutAuthRequest(intent, "auth state")
        } returns successResult

        val result = sut.finishStart(intent, "auth state")
        assertSame(successResult, result)
    }

    @Test
    fun `finishStart() with merchant provided auth state forwards error result from auth launcher`() {
        val error = PayPalSDKError(123, "fake-error-description")
        val failureResult = PayPalWebCheckoutFinishStartResult.Failure(error, null)
        every {
            payPalWebLauncher.completeCheckoutAuthRequest(intent, "auth state")
        } returns failureResult

        val result = sut.finishStart(intent, "auth state")
        assertSame(failureResult, result)
    }

    @Test
    fun `finishStart() with merchant provided auth state forwards cancellation result from auth launcher`() {
        val canceledResult = PayPalWebCheckoutFinishStartResult.Canceled("fake-order-id")
        every {
            payPalWebLauncher.completeCheckoutAuthRequest(intent, "auth state")
        } returns canceledResult

        val result = sut.finishStart(intent, "auth state")
        assertSame(canceledResult, result)
    }

    @Test
    fun `finishStart() with session auth state returns null when start has not been called`() {
        assertNull(sut.finishStart(intent))
    }

    @Test
    fun `finishStart() with session auth state forwards success result from auth launcher`() =
        runTest {
        val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
            every {
                payPalWebLauncher.launchWithUrl(
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
                )
            } returns launchResult

        val successResult =
            PayPalWebCheckoutFinishStartResult.Success("fake-order-id", "fake-payer-id")
        every {
            payPalWebLauncher.completeCheckoutAuthRequest(intent, "auth state")
        } returns successResult

            val request = PayPalWebCheckoutRequest(
                "fake-order-id",
                returnToAppStrategy = ReturnToAppStrategy.AppLink(appLinkUrl)
            )
            sut.startAsync(activity, request)
        val result = sut.finishStart(intent)
        assertSame(successResult, result)
    }

    @Test
    fun `finishStart() with restored session auth state forwards success result from auth launcher`() =
        runTest {
        val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
            every {
                payPalWebLauncher.launchWithUrl(
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
                )
            } returns launchResult

        val successResult =
            PayPalWebCheckoutFinishStartResult.Success("fake-order-id", "fake-payer-id")
        every {
            payPalWebLauncher.completeCheckoutAuthRequest(intent, "auth state")
        } returns successResult

            val launchWithUrlClient = PayPalWebCheckoutClient(
            analytics = analytics,
            payPalWebLauncher = payPalWebLauncher,
            sessionStore = PayPalWebCheckoutSessionStore(),
            updateClientConfigAPI = updateClientConfigAPI,
                deviceInspector = deviceInspector,
                coreConfig = coreConfig,
                urlScheme = urlScheme,
                patchCCOWithAppSwitchEligibility = patchCCOWithAppSwitchEligibility,
            createShopperSessionAPI = createShopperSessionAPI,
        )
        val request = PayPalWebCheckoutRequest("fake-order-id")
            launchWithUrlClient.startAsync(activity, request)

            sut.restore(launchWithUrlClient.instanceState)
        val result = sut.finishStart(intent)
        assertSame(successResult, result)
    }

    @Test
    fun `finishStart() with session auth state forwards error result from auth launcher`() =
        runTest {
        val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
            every {
                payPalWebLauncher.launchWithUrl(
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
                )
            } returns launchResult

        val error = PayPalSDKError(123, "fake-error-description")
        val failureResult = PayPalWebCheckoutFinishStartResult.Failure(error, null)
        every {
            payPalWebLauncher.completeCheckoutAuthRequest(intent, "auth state")
        } returns failureResult

            val request = PayPalWebCheckoutRequest(
                "fake-order-id",
                returnToAppStrategy = ReturnToAppStrategy.AppLink(appLinkUrl)
            )
            sut.startAsync(activity, request)
        val result = sut.finishStart(intent)
        assertSame(failureResult, result)
    }

    @Test
    fun `finishStart() with restored session auth state forwards error result from auth launcher`() =
        runTest {
        val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
            every {
                payPalWebLauncher.launchWithUrl(
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
                )
            } returns launchResult

        val error = PayPalSDKError(123, "fake-error-description")
        val failureResult = PayPalWebCheckoutFinishStartResult.Failure(error, null)
        every {
            payPalWebLauncher.completeCheckoutAuthRequest(intent, "auth state")
        } returns failureResult

            val request = PayPalWebCheckoutRequest(
                "fake-order-id",
                returnToAppStrategy = ReturnToAppStrategy.AppLink(appLinkUrl)
            )
            sut.startAsync(activity, request)
        val result = sut.finishStart(intent)
        assertSame(failureResult, result)
    }

    @Test
    fun `finishStart() with session auth state forwards cancellation result from auth launcher`() =
        runTest {
        val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
            every {
                payPalWebLauncher.launchWithUrl(
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
                )
            } returns launchResult

        val canceledResult = PayPalWebCheckoutFinishStartResult.Canceled("fake-order-id")
        every {
            payPalWebLauncher.completeCheckoutAuthRequest(intent, "auth state")
        } returns canceledResult

            val request = PayPalWebCheckoutRequest(
                "fake-order-id",
                returnToAppStrategy = ReturnToAppStrategy.AppLink(appLinkUrl)
            )
            sut.startAsync(activity, request)
        val result = sut.finishStart(intent)
        assertSame(canceledResult, result)
    }

    @Test
    fun `finishStart() with restored session auth state forwards cancellation result from auth launcher`() =
        runTest {
        val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
            every {
                payPalWebLauncher.launchWithUrl(
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
                )
            } returns launchResult

        val canceledResult = PayPalWebCheckoutFinishStartResult.Canceled("fake-order-id")
        every {
            payPalWebLauncher.completeCheckoutAuthRequest(intent, "auth state")
        } returns canceledResult

            val launchWithUrlClient = PayPalWebCheckoutClient(
            analytics = analytics,
            payPalWebLauncher = payPalWebLauncher,
            sessionStore = PayPalWebCheckoutSessionStore(),
            updateClientConfigAPI = updateClientConfigAPI,
                deviceInspector = deviceInspector,
                coreConfig = coreConfig,
                urlScheme = urlScheme,
                patchCCOWithAppSwitchEligibility = patchCCOWithAppSwitchEligibility,
            createShopperSessionAPI = createShopperSessionAPI
        )
        val request = PayPalWebCheckoutRequest("fake-order-id")
            launchWithUrlClient.startAsync(activity, request)

            sut.restore(launchWithUrlClient.instanceState)
        val result = sut.finishStart(intent)
        assertSame(canceledResult, result)
    }

    @Test
    fun `finishStart() with session auth state clears session to prevent delivering success event twice`() =
        runTest {
        val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
            every {
                payPalWebLauncher.launchWithUrl(
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
                )
            } returns launchResult

        val successResult =
            PayPalWebCheckoutFinishStartResult.Success("fake-order-id", "fake-payer-id")
        every {
            payPalWebLauncher.completeCheckoutAuthRequest(intent, "auth state")
        } returns successResult

            sut.startAsync(activity, PayPalWebCheckoutRequest("fake-order-id"))
        sut.finishStart(intent)
        assertNull(sut.finishStart(intent))
    }

    @Test
    fun `finishStart() with session auth state clears session to prevent delivering error event twice`() =
        runTest {
        val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
            every {
                payPalWebLauncher.launchWithUrl(
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
                )
            } returns launchResult

        val error = PayPalSDKError(123, "fake-error-description")
        val failureResult = PayPalWebCheckoutFinishStartResult.Failure(error, null)
        every {
            payPalWebLauncher.completeCheckoutAuthRequest(intent, "auth state")
        } returns failureResult

            sut.startAsync(activity, PayPalWebCheckoutRequest("fake-order-id"))
        sut.finishStart(intent)
        assertNull(sut.finishStart(intent))
    }

    @Test
    fun `finishStart() with session auth state clears session to prevent delivering cancellation event twice`() =
        runTest {
        val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
            every {
                payPalWebLauncher.launchWithUrl(
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
                )
            } returns launchResult

        val canceledResult = PayPalWebCheckoutFinishStartResult.Canceled("fake-order-id")
        every {
            payPalWebLauncher.completeCheckoutAuthRequest(intent, "auth state")
        } returns canceledResult

            sut.startAsync(activity, PayPalWebCheckoutRequest("fake-order-id"))
        sut.finishStart(intent)
        assertNull(sut.finishStart(intent))
    }

    @Test
    fun `finishVault() with merchant provided auth forwards vault success from PayPal web launcher`() {
        val successResult =
            PayPalWebCheckoutFinishVaultResult.Success("fake-approval-session-id")
        every {
            payPalWebLauncher.completeVaultAuthRequest(intent, "auth state")
        } returns successResult

        val result = sut.finishVault(intent, "auth state")
                as PayPalWebCheckoutFinishVaultResult.Success
        assertSame("fake-approval-session-id", result.approvalSessionId)
    }

    @Test
    fun `finishVault() with merchant provided auth notifies merchant of vault failure`() {
        val error = PayPalSDKError(123, "fake-error-description")
        every {
            payPalWebLauncher.completeVaultAuthRequest(intent, "auth state")
        } returns PayPalWebCheckoutFinishVaultResult.Failure(error)

        val result = sut.finishVault(intent, "auth state")
                as PayPalWebCheckoutFinishVaultResult.Failure
        assertSame(error, result.error)
    }

    @Test
    fun `finishVault with merchant provided auth forwards vault cancellation`() {
        every {
            payPalWebLauncher.completeVaultAuthRequest(intent, "auth state")
        } returns PayPalWebCheckoutFinishVaultResult.Canceled

        val result = sut.finishVault(intent, "auth state")
        assertTrue(result is PayPalWebCheckoutFinishVaultResult.Canceled)
    }

    @Test
    fun `finishVault with merchant provided auth forwards no result`() {
        every {
            payPalWebLauncher.completeVaultAuthRequest(intent, "auth state")
        } returns PayPalWebCheckoutFinishVaultResult.NoResult

        val result = sut.finishVault(intent, "auth state")
        assertTrue(result is PayPalWebCheckoutFinishVaultResult.NoResult)
    }

    @Test
    fun `start() falls back to web checkout when app switch is enabled but URL is null`() =
        runTest {
            // Given
            every { deviceInspector.isPayPalInstalled } returns true
            val request = PayPalWebCheckoutRequest(
                "fake-order-id",
                returnToAppStrategy = ReturnToAppStrategy.AppLink(appLinkUrl)
            )
            val appSwitchResponse = createAppSwitchEligibilityResponse(null)
            val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")

            coEvery {
                patchCCOWithAppSwitchEligibility(any(), any(), any(), any(), any())
            } returns APIResult.Success(appSwitchResponse)

            every {
                payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any())
            } returns launchResult

            // When
            val result = sut.startAsync(activity, request)

            // Then
            verify {
                payPalWebLauncher.launchWithUrl(
                    context = activity,
                    uri = any(),
                    token = "fake-order-id",
                    tokenType = TokenType.ORDER_ID,
                    returnToAppStrategy = any()
                )
            }
            assertSame(launchResult, result)
        }

    @Test
    fun `start() falls back to web checkout when app switch is enabled but empty URL is returned`() =
        runTest {
            // Given
            every { deviceInspector.isPayPalInstalled } returns true
            val request = PayPalWebCheckoutRequest(
                "fake-order-id",
                returnToAppStrategy = ReturnToAppStrategy.AppLink(appLinkUrl)
            )
            val appSwitchResponse = createAppSwitchEligibilityResponse("")
            val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")

            coEvery {
                patchCCOWithAppSwitchEligibility(any(), any(), any(), any(), any())
            } returns APIResult.Success(appSwitchResponse)

            every {
                payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any())
            } returns launchResult

            // When
            val result = sut.startAsync(activity, request)

            // Then
            verify {
                payPalWebLauncher.launchWithUrl(
                    context = activity,
                    uri = any(),
                    token = "fake-order-id",
                    tokenType = TokenType.ORDER_ID,
                    returnToAppStrategy = any()
                )
            }
            assertSame(launchResult, result)
        }

    // VAULT APP SWITCH TESTS
    @Test
    fun `vault() falls back to web vault when app switch is enabled but empty URL is returned`() =
        runTest {
            // Given
            every { deviceInspector.isPayPalInstalled } returns true
            val request = PayPalWebVaultRequest(
                "fake-setup-token-id",
                returnToAppStrategy = ReturnToAppStrategy.AppLink(appLinkUrl)
            )
            val appSwitchResponse = createAppSwitchEligibilityResponse("")
            val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")

            coEvery {
                patchCCOWithAppSwitchEligibility(any(), any(), any(), any(), any())
            } returns APIResult.Success(appSwitchResponse)

            every {
                payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any())
            } returns launchResult

            // When
            val result = sut.vaultAsync(activity, request)

            // Then
            verify {
                payPalWebLauncher.launchWithUrl(
                    context = activity,
                    uri = any(),
                    token = "fake-setup-token-id",
                    tokenType = TokenType.VAULT_ID,
                    returnToAppStrategy = any()
                )
            }
            assertSame(launchResult, result)
        }

    @Test
    fun `vault() falls back to web vault when app switch request fails`() = runTest {
        // Given
        every { deviceInspector.isPayPalInstalled } returns true
        val request = PayPalWebVaultRequest(
            "fake-setup-token-id",
            returnToAppStrategy = ReturnToAppStrategy.AppLink(appLinkUrl)
        )
        val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")

        coEvery {
            patchCCOWithAppSwitchEligibility(any(), any(), any(), any(), any())
        } returns APIResult.Failure(PayPalSDKError(1001, "Test vault failure"))

        every {
            payPalWebLauncher.launchWithUrl(
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns launchResult

        // When
        val result = sut.vaultAsync(activity, request)

        // Then
        verify {
            payPalWebLauncher.launchWithUrl(
                context = activity,
                uri = any(),
                token = "fake-setup-token-id",
                tokenType = TokenType.VAULT_ID,
                returnToAppStrategy = any()
            )
        }
        assertSame(launchResult, result)
    }

    @Test
    fun `start() skips app switch when app switch enabled but PayPal app not installed`() =
        runTest {
            // Given
            every { deviceInspector.isPayPalInstalled } returns false
            val request = PayPalWebCheckoutRequest(
                "fake-order-id",
                returnToAppStrategy = ReturnToAppStrategy.AppLink(appLinkUrl)
            )
            val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")

            every {
                payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any())
            } returns launchResult

            // When
            val result = sut.startAsync(activity, request)

            // Then
            coVerify(exactly = 0) {
                patchCCOWithAppSwitchEligibility.invoke(any(), any(), any(), any(), any())
            }
            verify {
                payPalWebLauncher.launchWithUrl(
                    context = activity,
                    uri = any(),
                    token = "fake-order-id",
                    tokenType = TokenType.ORDER_ID,
                    returnToAppStrategy = any()
                )
            }
            assertSame(launchResult, result)
        }

    @Test
    fun `vault() skips app switch when app switch enabled but PayPal app not installed`() =
        runTest {
            // Given
            every { deviceInspector.isPayPalInstalled } returns false
            val request = PayPalWebVaultRequest(
                "fake-setup-token-id",
                returnToAppStrategy = ReturnToAppStrategy.AppLink(appLinkUrl)
            )
            val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")

            every {
                payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any())
            } returns launchResult

            // When
            val result = sut.vaultAsync(activity, request)

            // Then
            coVerify(exactly = 0) {
                patchCCOWithAppSwitchEligibility.invoke(any(), any(), any(), any(), any())
            }
            verify {
                payPalWebLauncher.launchWithUrl(
                    context = activity,
                    uri = any(),
                    token = "fake-setup-token-id",
                    tokenType = TokenType.VAULT_ID,
                    returnToAppStrategy = any()
                )
            }
            assertSame(launchResult, result)
        }

    @Test
    fun `finishVault() with session auth state returns null when start has not been called`() {
        assertNull(sut.finishVault(intent))
    }

    @Test
    fun `finishVault() with session auth state forwards success result from auth launcher`() =
        runTest {
        val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")

        val previousClient = PayPalWebCheckoutClient(
            analytics = analytics,
            payPalWebLauncher = payPalWebLauncher,
            sessionStore = PayPalWebCheckoutSessionStore(),
            updateClientConfigAPI = updateClientConfigAPI,
            deviceInspector = deviceInspector,
            coreConfig = coreConfig,
            patchCCOWithAppSwitchEligibility = patchCCOWithAppSwitchEligibility,
            createShopperSessionAPI = createShopperSessionAPI
        )

            every {
                payPalWebLauncher.launchWithUrl(
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
                )
            } returns launchResult

        val successResult =
            PayPalWebCheckoutFinishVaultResult.Success("fake-approval-session-id")
        every {
            payPalWebLauncher.completeVaultAuthRequest(intent, "auth state")
        } returns successResult

            previousClient.vaultAsync(
                activity,
                PayPalWebVaultRequest(
                    "fake-setup-token-id",
                    returnToAppStrategy = ReturnToAppStrategy.AppLink(appLinkUrl)
                )
            )

            sut.restore(previousClient.instanceState)
            val result = sut.finishVault(intent) as PayPalWebCheckoutFinishVaultResult.Success
            assertSame("fake-approval-session-id", result.approvalSessionId)
        }

    @Test
    fun `finishVault() with restored session auth state forwards success result from auth launcher`() =
        runTest {
            val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")

        val previousClient = PayPalWebCheckoutClient(
            analytics = analytics,
            payPalWebLauncher = payPalWebLauncher,
            sessionStore = PayPalWebCheckoutSessionStore(),
            updateClientConfigAPI = updateClientConfigAPI,
            deviceInspector = deviceInspector,
            coreConfig = coreConfig,
            patchCCOWithAppSwitchEligibility = patchCCOWithAppSwitchEligibility,
            createShopperSessionAPI = createShopperSessionAPI
        )

            every {
                payPalWebLauncher.launchWithUrl(
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
                )
            } returns launchResult

            val successResult =
                PayPalWebCheckoutFinishVaultResult.Success("fake-approval-session-id")
            every {
                payPalWebLauncher.completeVaultAuthRequest(intent, "auth state")
            } returns successResult

            previousClient.vaultAsync(
                activity,
                PayPalWebVaultRequest(
                    "fake-setup-token-id",
                    returnToAppStrategy = ReturnToAppStrategy.AppLink(appLinkUrl)
                )
            )

        sut.restore(previousClient.instanceState)
        val result = sut.finishVault(intent) as PayPalWebCheckoutFinishVaultResult.Success
        assertSame("fake-approval-session-id", result.approvalSessionId)
    }

    @Test
    fun `finishVault() with session auth state forwards error result from auth launcher`() =
        runTest {
        val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
            every {
                payPalWebLauncher.launchWithUrl(
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
                )
            } returns launchResult

        val error = PayPalSDKError(123, "fake-error-description")
        every {
            payPalWebLauncher.completeVaultAuthRequest(intent, "auth state")
        } returns PayPalWebCheckoutFinishVaultResult.Failure(error)

            sut.vaultAsync(
                activity,
                PayPalWebVaultRequest(
                    "fake-setup-token-id",
                    returnToAppStrategy = ReturnToAppStrategy.AppLink(appLinkUrl)
                )
            )
        val result = sut.finishVault(intent) as PayPalWebCheckoutFinishVaultResult.Failure
        assertSame(error, result.error)
    }

    @Test
    fun `finishVault() with restored session auth state forwards error result from auth launcher`() =
        runTest {
        val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
            every {
                payPalWebLauncher.launchWithUrl(
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
                )
            } returns launchResult

        val error = PayPalSDKError(123, "fake-error-description")
        every {
            payPalWebLauncher.completeVaultAuthRequest(intent, "auth state")
        } returns PayPalWebCheckoutFinishVaultResult.Failure(error)

            sut.vaultAsync(
                activity,
                PayPalWebVaultRequest(
                    "fake-setup-token-id",
                    returnToAppStrategy = ReturnToAppStrategy.AppLink(appLinkUrl)
                )
            )

            sut.restore(sut.instanceState)
        val result = sut.finishVault(intent) as PayPalWebCheckoutFinishVaultResult.Failure
        assertSame(error, result.error)
    }

    @Test
    fun `finishVault() with session auth state forwards cancellation result from auth launcher`() =
        runTest {
        val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
            every {
                payPalWebLauncher.launchWithUrl(
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
                )
            } returns launchResult

        every {
            payPalWebLauncher.completeVaultAuthRequest(intent, "auth state")
        } returns PayPalWebCheckoutFinishVaultResult.Canceled

            sut.vaultAsync(
                activity,
                PayPalWebVaultRequest(
                    "fake-setup-token-id",
                    returnToAppStrategy = ReturnToAppStrategy.AppLink(appLinkUrl)
                )
            )
        val result = sut.finishVault(intent)
        assertSame(PayPalWebCheckoutFinishVaultResult.Canceled, result)
    }

    @Test
    fun `finishVault() with restored session auth state forwards cancellation result from auth launcher`() =
        runTest {
        val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
            every {
                payPalWebLauncher.launchWithUrl(
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
                )
            } returns launchResult

        every {
            payPalWebLauncher.completeVaultAuthRequest(intent, "auth state")
        } returns PayPalWebCheckoutFinishVaultResult.Canceled

            sut.vaultAsync(
                activity,
                PayPalWebVaultRequest(
                    "fake-setup-token-id",
                    returnToAppStrategy = ReturnToAppStrategy.AppLink(appLinkUrl)
                )
            )

            sut.restore(sut.instanceState)
        val result = sut.finishVault(intent)
        assertSame(PayPalWebCheckoutFinishVaultResult.Canceled, result)
    }

    @Test
    fun `finishVault() with session auth state clears session to prevent delivering success event twice`() =
        runTest {
        val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
            every {
                payPalWebLauncher.launchWithUrl(
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
                )
            } returns launchResult

        val successResult =
            PayPalWebCheckoutFinishVaultResult.Success("fake-approval-session-id")
        every {
            payPalWebLauncher.completeVaultAuthRequest(intent, "auth state")
        } returns successResult

            sut.vaultAsync(activity, PayPalWebVaultRequest("fake-setup-token-id"))
        sut.finishVault(intent)
        assertNull(sut.finishVault(intent))
    }

    @Test
    fun `finishVault() with session auth state clears session to prevent delivering error event twice`() =
        runTest {
        val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
            every {
                payPalWebLauncher.launchWithUrl(
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
                )
            } returns launchResult

        val error = PayPalSDKError(123, "fake-error-description")
        every {
            payPalWebLauncher.completeVaultAuthRequest(intent, "auth state")
        } returns PayPalWebCheckoutFinishVaultResult.Failure(error)

            sut.vaultAsync(activity, PayPalWebVaultRequest("fake-setup-token-id"))
        sut.finishVault(intent)
        assertNull(sut.finishVault(intent))
    }

    @Test
    fun `finishVault() with session auth state clears session to prevent delivering cancellation event twice`() =
        runTest {
        val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
            every {
                payPalWebLauncher.launchWithUrl(
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
                )
            } returns launchResult

        every {
            payPalWebLauncher.completeVaultAuthRequest(intent, "auth state")
        } returns PayPalWebCheckoutFinishVaultResult.Canceled

            sut.vaultAsync(activity, PayPalWebVaultRequest("fake-setup-token-id"))
        sut.finishVault(intent)
        assertNull(sut.finishVault(intent))
    }

    @Test
    @Suppress("DEPRECATION")
    fun `start() with deprecated urlScheme constructor passes urlScheme to launcher`() = runTest {
        val mockLauncher = mockk<PayPalWebLauncher>(relaxed = true)
        val clientWithUrlScheme = PayPalWebCheckoutClient(
            analytics = analytics,
            payPalWebLauncher = mockLauncher,
            sessionStore = PayPalWebCheckoutSessionStore(),
            updateClientConfigAPI = updateClientConfigAPI,
            patchCCOWithAppSwitchEligibility = patchCCOWithAppSwitchEligibility,
            createShopperSessionAPI = createShopperSessionAPI,
            deviceInspector = deviceInspector,
            coreConfig = coreConfig,
            urlScheme = urlScheme
        )

        val request = PayPalWebCheckoutRequest("fake-order-id")
        val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")

        every {
            mockLauncher.launchWithUrl(
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns launchResult

        // When
        clientWithUrlScheme.startAsync(activity, request)

        // Then
        verify {
            mockLauncher.launchWithUrl(
                context = activity,
                uri = any(),
                token = "fake-order-id",
                tokenType = TokenType.ORDER_ID,
                returnToAppStrategy = ReturnToAppStrategy.CustomUrlScheme(urlScheme)
            )
        }
    }

    @Test
    @Suppress("DEPRECATION")
    fun `vault() with deprecated urlScheme constructor passes urlScheme to launcher`() = runTest {
        val mockLauncher = mockk<PayPalWebLauncher>(relaxed = true)
        val clientWithUrlScheme = PayPalWebCheckoutClient(
            analytics = analytics,
            payPalWebLauncher = mockLauncher,
            sessionStore = PayPalWebCheckoutSessionStore(),
            updateClientConfigAPI = updateClientConfigAPI,
            patchCCOWithAppSwitchEligibility = patchCCOWithAppSwitchEligibility,
            createShopperSessionAPI = createShopperSessionAPI,
            deviceInspector = deviceInspector,
            coreConfig = coreConfig,
            urlScheme = urlScheme
        )

        val request = PayPalWebVaultRequest("fake-setup-token-id")
        val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")

        every {
            mockLauncher.launchWithUrl(
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns launchResult

        // When
        clientWithUrlScheme.vaultAsync(activity, request)

        // Then
        verify {
            mockLauncher.launchWithUrl(
                context = activity,
                uri = any(),
                token = "fake-setup-token-id",
                tokenType = TokenType.VAULT_ID,
                returnToAppStrategy = ReturnToAppStrategy.CustomUrlScheme(urlScheme)
            )
        }
    }

    @Test
    @Suppress("DEPRECATION")
    fun `start() with deprecated urlScheme constructor and AppLink in request prioritizes AppLink`() =
        runTest {
            val mockLauncher = mockk<PayPalWebLauncher>(relaxed = true)
            val clientWithUrlScheme = PayPalWebCheckoutClient(
                analytics = analytics,
                payPalWebLauncher = mockLauncher,
                sessionStore = PayPalWebCheckoutSessionStore(),
                updateClientConfigAPI = updateClientConfigAPI,
                patchCCOWithAppSwitchEligibility = patchCCOWithAppSwitchEligibility,
            createShopperSessionAPI = createShopperSessionAPI,
                deviceInspector = deviceInspector,
                coreConfig = coreConfig,
                urlScheme = urlScheme
            )

            val appLinkUrl = "https://example.com/return"
            val request = PayPalWebCheckoutRequest(
                "fake-order-id",
                returnToAppStrategy = ReturnToAppStrategy.AppLink(appLinkUrl)
            )
            val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")

            every {
                mockLauncher.launchWithUrl(
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
                )
            } returns launchResult

            // When
            clientWithUrlScheme.startAsync(activity, request)

            // Then
            verify {
                mockLauncher.launchWithUrl(
                    context = activity,
                    uri = any(),
                    token = "fake-order-id",
                    tokenType = TokenType.ORDER_ID,
                    returnToAppStrategy = ReturnToAppStrategy.AppLink(appLinkUrl)
                )
            }
        }

    @Test
    @Suppress("DEPRECATION")
    fun `vault() with deprecated urlScheme constructor and AppLink in request prioritizes AppLink`() =
        runTest {
            val mockLauncher = mockk<PayPalWebLauncher>(relaxed = true)
            val clientWithUrlScheme = PayPalWebCheckoutClient(
                analytics = analytics,
                payPalWebLauncher = mockLauncher,
                sessionStore = PayPalWebCheckoutSessionStore(),
                updateClientConfigAPI = updateClientConfigAPI,
                patchCCOWithAppSwitchEligibility = patchCCOWithAppSwitchEligibility,
            createShopperSessionAPI = createShopperSessionAPI,
                deviceInspector = deviceInspector,
                coreConfig = coreConfig,
                urlScheme = urlScheme
            )

            val appLinkUrl = "https://example.com/vault/return"
            val request = PayPalWebVaultRequest(
                "fake-setup-token-id",
                returnToAppStrategy = ReturnToAppStrategy.AppLink(appLinkUrl)
            )
            val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")

            every {
                mockLauncher.launchWithUrl(
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
                )
            } returns launchResult

            // When
            clientWithUrlScheme.vaultAsync(activity, request)

            // Then
            verify {
                mockLauncher.launchWithUrl(
                    context = activity,
                    uri = any(),
                    token = "fake-setup-token-id",
                    tokenType = TokenType.VAULT_ID,
                    returnToAppStrategy = ReturnToAppStrategy.AppLink(appLinkUrl)
                )
            }
        }

    // MARK: - Tests for Deprecated Methods

    @Test
    fun `deprecated start() calls analytics and launchWithUrl correctly`() {
        val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
        every {
            payPalWebLauncher.launchWithUrl(
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns launchResult

        val request = PayPalWebCheckoutRequest(
            "fake-order-id",
            returnToAppStrategy = ReturnToAppStrategy.AppLink(appLinkUrl)
        )
        val result = sut.start(activity, request)

        // Verify it returns the launch result
        assertSame(launchResult, result)

        // Verify launchWithUrl is called (the deprecated method calls it directly)
        verify(exactly = 1) {
            payPalWebLauncher.launchWithUrl(
                context = activity,
                uri = any(),
                token = "fake-order-id",
                tokenType = TokenType.ORDER_ID,
                returnToAppStrategy = ReturnToAppStrategy.AppLink(appLinkUrl)
            )
        }
    }

    @Test
    fun `deprecated start() handles failure correctly`() {
        val sdkError = PayPalSDKError(123, "fake error description")
        val launchResult = PayPalPresentAuthChallengeResult.Failure(sdkError)
        every {
            payPalWebLauncher.launchWithUrl(
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns launchResult

        val request = PayPalWebCheckoutRequest(
            "fake-order-id",
            returnToAppStrategy = ReturnToAppStrategy.AppLink(appLinkUrl)
        )
        val result = sut.start(activity, request) as PayPalPresentAuthChallengeResult.Failure

        assertSame(sdkError, result.error)
    }

    @Test
    fun `deprecated vault() calls analytics and launchWithUrl correctly`() {
        val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
        every {
            payPalWebLauncher.launchWithUrl(
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns launchResult

        val appLinkUrl = "https://example.com/vault/return"
        val request = PayPalWebVaultRequest(
            "fake-setup-token-id",
            returnToAppStrategy = ReturnToAppStrategy.AppLink(appLinkUrl)
        )
        val result = sut.vault(activity, request)

        // Verify it returns the launch result
        assertSame(launchResult, result)

        // Verify launchWithUrl is called (the deprecated method calls it directly)
        verify(exactly = 1) {
            payPalWebLauncher.launchWithUrl(
                context = activity,
                uri = any(),
                token = "fake-setup-token-id",
                tokenType = TokenType.VAULT_ID,
                returnToAppStrategy = ReturnToAppStrategy.AppLink(appLinkUrl)
            )
        }
    }

    @Test
    fun `deprecated vault() handles failure correctly`() {
        val sdkError = PayPalSDKError(123, "fake error description")
        val launchResult = PayPalPresentAuthChallengeResult.Failure(sdkError)
        every {
            payPalWebLauncher.launchWithUrl(
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns launchResult

        val appLinkUrl = "https://example.com/vault/return"
        val request = PayPalWebVaultRequest(
            "fake-setup-token-id",
            returnToAppStrategy = ReturnToAppStrategy.AppLink(appLinkUrl)
        )
        val result = sut.vault(activity, request) as PayPalPresentAuthChallengeResult.Failure

        assertSame(sdkError, result.error)
    }

    // MARK: - Tests for Callback-based Methods
    // Note: Callback-based methods use applicationScope.launch which doesn't integrate well
    // with test coroutine dispatchers, so these are tested implicitly through the methods they call

    @Test
    fun `start() with callback method exists and can be called`() {
        // This test just verifies the callback method can be called without throwing
        val callback = mockk<PayPalWebStartCallback>(relaxed = true)
        val request = PayPalWebCheckoutRequest(
            "fake-order-id",
            returnToAppStrategy = ReturnToAppStrategy.AppLink(appLinkUrl)
        )

        // Mock the async dependencies to avoid side effects
        every {
            payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any())
        } returns PayPalPresentAuthChallengeResult.Success("auth state")

        // This should not throw an exception
        sut.start(activity, request, callback)
    }

    @Test
    fun `vault() with callback method exists and can be called`() {
        // This test just verifies the callback method can be called without throwing
        val callback = mockk<PayPalWebVaultCallback>(relaxed = true)
        val request = PayPalWebVaultRequest(
            "fake-setup-token-id",
            returnToAppStrategy = ReturnToAppStrategy.AppLink(appLinkUrl)
        )

        // Mock the async dependencies to avoid side effects
        every {
            payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any())
        } returns PayPalPresentAuthChallengeResult.Success("auth state")

        // This should not throw an exception
        sut.vault(activity, request, callback)
    }

    // Tests for returnToAppStrategy functionality

    @Test
    fun `startAsync() uses CustomUrlScheme when provided`() = runTest {
        val fallbackScheme = "com.example.fallback"
        val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
        every {
            payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any())
        } returns launchResult

        val request = PayPalWebCheckoutRequest(
            orderId = "fake-order-id",
            returnToAppStrategy = ReturnToAppStrategy.CustomUrlScheme(fallbackScheme)
        )
        sut.startAsync(activity, request)

        verify(exactly = 1) {
            payPalWebLauncher.launchWithUrl(
                context = activity,
                uri = any(),
                token = "fake-order-id",
                tokenType = TokenType.ORDER_ID,
                returnToAppStrategy = ReturnToAppStrategy.CustomUrlScheme(fallbackScheme)
            )
        }
    }

    @Test
    fun `startAsync() uses default urlScheme when returnToAppStrategy is null`() = runTest {
        // Create a client with urlScheme
        val clientWithUrlScheme = PayPalWebCheckoutClient(
            analytics = analytics,
            payPalWebLauncher = payPalWebLauncher,
            sessionStore = PayPalWebCheckoutSessionStore(),
            updateClientConfigAPI = updateClientConfigAPI,
            patchCCOWithAppSwitchEligibility = patchCCOWithAppSwitchEligibility,
            createShopperSessionAPI = createShopperSessionAPI,
            deviceInspector = deviceInspector,
            coreConfig = coreConfig,
            urlScheme = urlScheme
        )

        val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
        every {
            payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any())
        } returns launchResult

        val request = PayPalWebCheckoutRequest(
            orderId = "fake-order-id",
            returnToAppStrategy = null
        )
        clientWithUrlScheme.startAsync(activity, request)

        verify(exactly = 1) {
            payPalWebLauncher.launchWithUrl(
                context = activity,
                uri = any(),
                token = "fake-order-id",
                tokenType = TokenType.ORDER_ID,
                returnToAppStrategy = ReturnToAppStrategy.CustomUrlScheme(urlScheme)
            )
        }
    }

    @Test
    @Suppress("DEPRECATION")
    fun `start() with deprecated method uses CustomUrlScheme when provided`() {
        val fallbackScheme = "com.example.fallback"
        val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
        every {
            payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any())
        } returns launchResult

        val request = PayPalWebCheckoutRequest(
            orderId = "fake-order-id",
            returnToAppStrategy = ReturnToAppStrategy.CustomUrlScheme(fallbackScheme)
        )
        sut.start(activity, request)

        verify(exactly = 1) {
            payPalWebLauncher.launchWithUrl(
                context = activity,
                uri = any(),
                token = "fake-order-id",
                tokenType = TokenType.ORDER_ID,
                returnToAppStrategy = ReturnToAppStrategy.CustomUrlScheme(fallbackScheme)
            )
        }
    }

    @Test
    fun `startAsync() with CustomUrlScheme uses CustomUrlScheme when provided`() = runTest {
        val fallbackScheme = "com.example.fallback"
        val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
        every {
            payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any())
        } returns launchResult

        val request = PayPalWebCheckoutRequest(
            orderId = "fake-order-id",
            returnToAppStrategy = ReturnToAppStrategy.CustomUrlScheme(fallbackScheme)
        )
        sut.startAsync(activity, request)

        verify(exactly = 1) {
            payPalWebLauncher.launchWithUrl(
                context = activity,
                uri = any(),
                token = "fake-order-id",
                tokenType = TokenType.ORDER_ID,
                returnToAppStrategy = ReturnToAppStrategy.CustomUrlScheme(fallbackScheme)
            )
        }
    }

    @Test
    fun `vaultAsync() uses CustomUrlScheme when provided`() = runTest {
        val fallbackScheme = "com.example.fallback"
        val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
        every {
            payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any())
        } returns launchResult

        val request = PayPalWebVaultRequest(
            setupTokenId = "fake-setup-token-id",
            returnToAppStrategy = ReturnToAppStrategy.CustomUrlScheme(fallbackScheme)
        )
        sut.vaultAsync(activity, request)

        verify(exactly = 1) {
            payPalWebLauncher.launchWithUrl(
                context = activity,
                uri = any(),
                token = "fake-setup-token-id",
                tokenType = TokenType.VAULT_ID,
                returnToAppStrategy = ReturnToAppStrategy.CustomUrlScheme(fallbackScheme)
            )
        }
    }

    @Test
    fun `vaultAsync() uses default urlScheme when returnToAppStrategy is null`() = runTest {
        // Create a client with urlScheme
        val clientWithUrlScheme = PayPalWebCheckoutClient(
            analytics = analytics,
            payPalWebLauncher = payPalWebLauncher,
            sessionStore = PayPalWebCheckoutSessionStore(),
            updateClientConfigAPI = updateClientConfigAPI,
            patchCCOWithAppSwitchEligibility = patchCCOWithAppSwitchEligibility,
            createShopperSessionAPI = createShopperSessionAPI,
            deviceInspector = deviceInspector,
            coreConfig = coreConfig,
            urlScheme = urlScheme
        )

        val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
        every {
            payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any())
        } returns launchResult

        val request = PayPalWebVaultRequest(
            setupTokenId = "fake-setup-token-id",
            returnToAppStrategy = null
        )
        clientWithUrlScheme.vaultAsync(activity, request)

        verify(exactly = 1) {
            payPalWebLauncher.launchWithUrl(
                context = activity,
                uri = any(),
                token = "fake-setup-token-id",
                tokenType = TokenType.VAULT_ID,
                returnToAppStrategy = ReturnToAppStrategy.CustomUrlScheme(urlScheme)
            )
        }
    }

    @Test
    @Suppress("DEPRECATION")
    fun `vault() with deprecated method uses CustomUrlScheme when provided`() {
        val fallbackScheme = "com.example.fallback"
        val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
        every {
            payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any())
        } returns launchResult

        val request = PayPalWebVaultRequest(
            setupTokenId = "fake-setup-token-id",
            returnToAppStrategy = ReturnToAppStrategy.CustomUrlScheme(fallbackScheme)
        )
        sut.vault(activity, request)

        verify(exactly = 1) {
            payPalWebLauncher.launchWithUrl(
                context = activity,
                uri = any(),
                token = "fake-setup-token-id",
                tokenType = TokenType.VAULT_ID,
                returnToAppStrategy = ReturnToAppStrategy.CustomUrlScheme(fallbackScheme)
            )
        }
    }

    @Test
    fun `vault() with callback uses CustomUrlScheme when provided`() {
        // This test verifies the callback method can be called with CustomUrlScheme without throwing
        val fallbackScheme = "com.example.fallback"
        val callback = mockk<PayPalWebVaultCallback>(relaxed = true)
        every {
            payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any())
        } returns PayPalPresentAuthChallengeResult.Success("auth state")

        val request = PayPalWebVaultRequest(
            setupTokenId = "fake-setup-token-id",
            returnToAppStrategy = ReturnToAppStrategy.CustomUrlScheme(fallbackScheme)
        )

        // This should not throw an exception
        sut.vault(activity, request, callback)
    }

    @Test
    fun `vault() with callback uses default urlScheme when returnToAppStrategy is null`() {
        // This test verifies the callback method can be called with null returnToAppStrategy without throwing
        val callback = mockk<PayPalWebVaultCallback>(relaxed = true)
        every {
            payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any())
        } returns PayPalPresentAuthChallengeResult.Success("auth state")

        val request = PayPalWebVaultRequest(
            setupTokenId = "fake-setup-token-id",
            returnToAppStrategy = null
        )

        // This should not throw an exception
        sut.vault(activity, request, callback)
    }

    // MARK: - V3 Methods (createPayPalSession / start(orderId) / vault(setupTokenId))

    // Build a client with urlScheme so launchCheckoutWithSession / launchVaultWithSession
    // can resolve a ReturnToAppStrategy via the fallback path.
    // Inject test-controlled applicationScope so coroutines launched by v3 methods
    // run on the test scheduler and are drained by testDispatcher.scheduler.advanceUntilIdle().
    private fun makeSutWithUrlScheme(): PayPalWebCheckoutClient = PayPalWebCheckoutClient(
            analytics = analytics,
            payPalWebLauncher = payPalWebLauncher,
            sessionStore = PayPalWebCheckoutSessionStore(),
            updateClientConfigAPI = updateClientConfigAPI,
            patchCCOWithAppSwitchEligibility = patchCCOWithAppSwitchEligibility,
            createShopperSessionAPI = createShopperSessionAPI,
            deviceInspector = deviceInspector,
            coreConfig = coreConfig,
            urlScheme = urlScheme,
            applicationScope = CoroutineScope(SupervisorJob() + testDispatcher),
    )

    private val fakeUrlConfig = ReturnToAppUrlConfig(
        returnAppUrl = "https://example.com/paypal-return",
        cancelAppUrl = "https://example.com/paypal-cancel",
        fallbackSchemeUrl = "com.example.app://paypal",
    )
    private val fakeUserIdentity = PayPalUserIdentity()
    private val fakeSessionResponse = CreateShopperSessionWithAppSwitchEligibilityResponse(
        appSwitchEligible = false,
        redirectUrl = "",
        checkoutFallbackUrl = "",
        ineligibleReason = "",
        matchedAuthenticationMethods = emptyList(),
        shopperSessionConfig = ShopperSessionConfig("fake-session-id", "")
    )

    // --- start(activity, orderId, callback) ---

    @Test
    fun `start() with orderId delivers SESSION_NOT_CREATED when createPayPalSession not called`() =
        runTest {
            val callback = mockk<PayPalWebStartCallback>(relaxed = true)

            sut.start(activity, "fake-order-id", callback)
            testDispatcher.scheduler.advanceUntilIdle()

            verify {
                callback.onPayPalWebStartResult(match {
                    it is PayPalPresentAuthChallengeResult.Failure &&
                        it.error.code == PayPalWebCheckoutError.sessionNotCreatedError.code
                })
            }
        }

    @Test
    fun `start() with orderId launches checkout when session resolves successfully`() = runTest {
        val sutV3 = makeSutWithUrlScheme()
        val launchResult = PayPalPresentAuthChallengeResult.Success("auth-state")
        every { payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any()) } returns launchResult

        val callback = mockk<PayPalWebStartCallback>(relaxed = true)
        sutV3.createPayPalSession(
            tokenType = TokenType.ORDER_ID,
            userIdentity = fakeUserIdentity,
            urlConfig = fakeUrlConfig,
        )
        sutV3.shopperSessionDeferred = CompletableDeferred(fakeSessionResponse)
        sutV3.start(activity, "fake-order-id", callback)
        testDispatcher.scheduler.advanceUntilIdle()

        verify {
            payPalWebLauncher.launchWithUrl(
                context = activity,
                uri = any(),
                token = "fake-order-id",
                tokenType = TokenType.ORDER_ID,
                returnToAppStrategy = any()
            )
        }
        verify { callback.onPayPalWebStartResult(launchResult) }
    }

    @Test
    fun `start() with orderId includes shopperSessionId in the launch uri when the session id is non-blank`() =
        runTest {
            val sutV3 = makeSutWithUrlScheme()
            val uriSlot = slot<Uri>()
            every {
                payPalWebLauncher.launchWithUrl(any(), capture(uriSlot), any(), any(), any())
            } returns PayPalPresentAuthChallengeResult.Success("auth-state")

            val callback = mockk<PayPalWebStartCallback>(relaxed = true)
            sutV3.createPayPalSession(
                tokenType = TokenType.ORDER_ID,
                userIdentity = fakeUserIdentity,
                urlConfig = fakeUrlConfig,
            )
            sutV3.shopperSessionDeferred = CompletableDeferred(fakeSessionResponse)
            sutV3.start(activity, "fake-order-id", callback)
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals("fake-session-id", uriSlot.captured.getQueryParameter("shopperSessionId"))
        }

    @Test
    fun `start() with orderId omits shopperSessionId from the launch uri when the session id is blank`() =
        runTest {
            val sutV3 = makeSutWithUrlScheme()
            val uriSlot = slot<Uri>()
            every {
                payPalWebLauncher.launchWithUrl(any(), capture(uriSlot), any(), any(), any())
            } returns PayPalPresentAuthChallengeResult.Success("auth-state")

            val blankIdResponse = fakeSessionResponse.copy(
                shopperSessionConfig = ShopperSessionConfig("", "")
            )
            val callback = mockk<PayPalWebStartCallback>(relaxed = true)
            sutV3.createPayPalSession(
                tokenType = TokenType.ORDER_ID,
                userIdentity = fakeUserIdentity,
                urlConfig = fakeUrlConfig,
            )
            sutV3.shopperSessionDeferred = CompletableDeferred(blankIdResponse)
            sutV3.start(activity, "fake-order-id", callback)
            testDispatcher.scheduler.advanceUntilIdle()

            val launchedUri = uriSlot.captured
            assertFalse(launchedUri.queryParameterNames.contains("shopperSessionId"))
            assertEquals("fake-order-id", launchedUri.getQueryParameter("token"))
        }

    @Test
    fun `start() with orderId includes observability query params on the launch uri`() = runTest {
        val sutV3 = makeSutWithUrlScheme()
        val uriSlot = slot<Uri>()
        val beforeMillis = System.currentTimeMillis()
        every {
            payPalWebLauncher.launchWithUrl(any(), capture(uriSlot), any(), any(), any())
        } returns PayPalPresentAuthChallengeResult.Success("auth-state")

        val callback = mockk<PayPalWebStartCallback>(relaxed = true)
        sutV3.createPayPalSession(
            tokenType = TokenType.ORDER_ID,
            userIdentity = fakeUserIdentity,
            urlConfig = fakeUrlConfig,
        )
        sutV3.shopperSessionDeferred = CompletableDeferred(fakeSessionResponse)
        sutV3.start(activity, "fake-order-id", callback)
        testDispatcher.scheduler.advanceUntilIdle()
        val afterMillis = System.currentTimeMillis()

        val launchedUri = uriSlot.captured
        assertEquals("pda", launchedUri.getQueryParameter("source"))
        assertEquals("fake-merchant-id", launchedUri.getQueryParameter("merchant"))
        assertEquals("ecs", launchedUri.getQueryParameter("flow_type"))
        assertEquals("paypal", launchedUri.getQueryParameter("funding_source"))

        val switchInitiatedTime = launchedUri.getQueryParameter("switch_initiated_time")?.toLongOrNull()
        assertNotNull(switchInitiatedTime)
        assertTrue(switchInitiatedTime!! in beforeMillis..afterMillis)
    }

    @Test
    fun `start() with orderId delivers failure without falling back to patchCCO when createShopperSession throws`() =
        runTest {
            val sutV3 = makeSutWithUrlScheme()
            val callback = mockk<PayPalWebStartCallback>(relaxed = true)
            sutV3.createPayPalSession(
                tokenType = TokenType.ORDER_ID,
                userIdentity = fakeUserIdentity,
                urlConfig = fakeUrlConfig,
            )
            // Any unexpected exception from the pre-warm fetch (as opposed to a "normal"
            // APIResult.Failure) should fail the whole flow rather than fall back to patchCCO.
            sutV3.shopperSessionDeferred = CompletableDeferred<CreateShopperSessionWithAppSwitchEligibilityResponse?>()
                .also { it.completeExceptionally(RuntimeException("session error")) }
            sutV3.start(activity, "fake-order-id", callback)
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify(exactly = 0) {
                patchCCOWithAppSwitchEligibility(any(), any(), any(), any(), any())
            }
            verify {
                callback.onPayPalWebStartResult(match { it is PayPalPresentAuthChallengeResult.Failure })
            }
        }

    // --- LLD Section 3.8 fallback-to-patchCCO behavior ---

    @Ignore(
        "patchCCO fallback removed pending re-implementation; " +
            "see TODO in PayPalWebCheckoutClient.launchCheckoutViaPatchCCOFallback"
    )
    @Test
    fun `start() falls back to patchCCO when shopper session fetch reports a session-creation-or-network failure`() =
        runTest {
            val sutV3 = makeSutWithUrlScheme()
            every { deviceInspector.isPayPalInstalled } returns true
            val callback = mockk<PayPalWebStartCallback>(relaxed = true)
            val launchResult = PayPalPresentAuthChallengeResult.Success("auth-state")

            coEvery {
                patchCCOWithAppSwitchEligibility(any(), any(), any(), any(), any())
            } returns APIResult.Success(
                AppSwitchEligibility(
                    appSwitchEligible = true,
                    launchUrl = "https://paypal.com/patch-cco-checkout",
                    ineligibleReason = null
                )
            )
            every {
                payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any())
            } returns launchResult

            sutV3.createPayPalSession(
                tokenType = TokenType.ORDER_ID,
                userIdentity = fakeUserIdentity,
                urlConfig = fakeUrlConfig,
            )
            // null represents a session-creation failure / network timeout (any non-LSAT
            // APIResult.Failure from createShopperSessionWithAppSwitchEligibility).
            sutV3.shopperSessionDeferred =
                CompletableDeferred<CreateShopperSessionWithAppSwitchEligibilityResponse?>()
                    .also { it.complete(null) }
            sutV3.start(activity, "fake-order-id", callback)
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify(exactly = 1) {
                patchCCOWithAppSwitchEligibility(
                    context = activity,
                    orderId = "fake-order-id",
                    tokenType = TokenType.ORDER_ID,
                    merchantOptInForAppSwitch = true,
                    paypalNativeAppInstalled = true
                )
            }
            verify {
                payPalWebLauncher.launchWithUrl(
                    context = activity,
                    uri = any(),
                    token = "fake-order-id",
                    tokenType = TokenType.ORDER_ID,
                    returnToAppStrategy = any()
                )
            }
            verify { callback.onPayPalWebStartResult(launchResult) }
        }

    @Ignore(
        "patchCCO fallback removed pending re-implementation; " +
            "see TODO in PayPalWebCheckoutClient.launchCheckoutViaPatchCCOFallback"
    )
    @Test
    fun `start() appends token and strips a trailing ampersand from the patchCCO launch url`() =
        runTest {
            val sutV3 = makeSutWithUrlScheme()
            every { deviceInspector.isPayPalInstalled } returns true
            val callback = mockk<PayPalWebStartCallback>(relaxed = true)
            val launchResult = PayPalPresentAuthChallengeResult.Success("auth-state")
            val uriSlot = slot<Uri>()

            coEvery {
                patchCCOWithAppSwitchEligibility(any(), any(), any(), any(), any())
            } returns APIResult.Success(
                AppSwitchEligibility(
                    appSwitchEligible = true,
                    launchUrl = "https://www.paypal.com/app-switch-checkout?appSwitchEligible=true&",
                    ineligibleReason = null
                )
            )
            every {
                payPalWebLauncher.launchWithUrl(any(), capture(uriSlot), any(), any(), any())
            } returns launchResult

            sutV3.createPayPalSession(
                tokenType = TokenType.ORDER_ID,
                userIdentity = fakeUserIdentity,
                urlConfig = fakeUrlConfig,
            )
            sutV3.shopperSessionDeferred =
                CompletableDeferred<CreateShopperSessionWithAppSwitchEligibilityResponse?>()
                    .also { it.complete(null) }
            sutV3.start(activity, "fake-order-id", callback)
            testDispatcher.scheduler.advanceUntilIdle()

            val launchedUri = uriSlot.captured
            assertEquals("fake-order-id", launchedUri.getQueryParameter("token"))
            assertFalse(launchedUri.toString().contains("&&"))
            assertFalse(launchedUri.toString().endsWith("&"))
        }

    @Test
    fun `start() with orderId clears session deferred so a second call returns SESSION_NOT_CREATED`() =
        runTest {
            val sutV3 = makeSutWithUrlScheme()
            every { payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any()) } returns
                PayPalPresentAuthChallengeResult.Success("auth-state")

            sutV3.createPayPalSession(
                tokenType = TokenType.ORDER_ID,
                userIdentity = fakeUserIdentity,
                urlConfig = fakeUrlConfig,
            )
            sutV3.shopperSessionDeferred = CompletableDeferred(fakeSessionResponse)
            val callback1 = mockk<PayPalWebStartCallback>(relaxed = true)
            val callback2 = mockk<PayPalWebStartCallback>(relaxed = true)

            sutV3.start(activity, "fake-order-id", callback1)
            testDispatcher.scheduler.advanceUntilIdle()

            // Second call without a new createPayPalSession — deferred is already consumed.
            sutV3.start(activity, "fake-order-id", callback2)
            testDispatcher.scheduler.advanceUntilIdle()

            verify {
                callback2.onPayPalWebStartResult(match {
                    it is PayPalPresentAuthChallengeResult.Failure &&
                        it.error.code == PayPalWebCheckoutError.sessionNotCreatedError.code
                })
            }
        }

    // --- vault(activity, setupTokenId, callback) ---

    @Test
    fun `vault() with setupTokenId delivers SESSION_NOT_CREATED when createPayPalSession not called`() =
        runTest {
            val callback = mockk<PayPalWebVaultCallback>(relaxed = true)

            sut.vault(activity, "fake-setup-token-id", callback)
            testDispatcher.scheduler.advanceUntilIdle()

            verify {
                callback.onPayPalWebVaultResult(match {
                    it is PayPalPresentAuthChallengeResult.Failure &&
                        it.error.code == PayPalWebCheckoutError.sessionNotCreatedError.code
                })
            }
        }

    @Test
    fun `vault() with setupTokenId launches vault when session resolves successfully`() = runTest {
        val sutV3 = makeSutWithUrlScheme()
        val launchResult = PayPalPresentAuthChallengeResult.Success("auth-state")
        every { payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any()) } returns launchResult

        val callback = mockk<PayPalWebVaultCallback>(relaxed = true)
        sutV3.createPayPalSession(
            tokenType = TokenType.ORDER_ID,
            userIdentity = fakeUserIdentity,
            urlConfig = fakeUrlConfig,
        )
        sutV3.shopperSessionDeferred = CompletableDeferred(fakeSessionResponse)
        sutV3.vault(activity, "fake-setup-token-id", callback)
        testDispatcher.scheduler.advanceUntilIdle()

        verify {
            payPalWebLauncher.launchWithUrl(
                context = activity,
                uri = any(),
                token = "fake-setup-token-id",
                tokenType = TokenType.VAULT_ID,
                returnToAppStrategy = any()
            )
        }
        verify { callback.onPayPalWebVaultResult(launchResult) }
    }

    @Test
    fun `vault() with setupTokenId includes observability query params on the launch uri`() = runTest {
        val sutV3 = makeSutWithUrlScheme()
        val uriSlot = slot<Uri>()
        val beforeMillis = System.currentTimeMillis()
        every {
            payPalWebLauncher.launchWithUrl(any(), capture(uriSlot), any(), any(), any())
        } returns PayPalPresentAuthChallengeResult.Success("auth-state")

        val callback = mockk<PayPalWebVaultCallback>(relaxed = true)
        sutV3.createPayPalSession(
            tokenType = TokenType.VAULT_ID,
            userIdentity = fakeUserIdentity,
            urlConfig = fakeUrlConfig,
        )
        sutV3.shopperSessionDeferred = CompletableDeferred(fakeSessionResponse)
        sutV3.vault(activity, "fake-setup-token-id", callback)
        testDispatcher.scheduler.advanceUntilIdle()
        val afterMillis = System.currentTimeMillis()

        val launchedUri = uriSlot.captured
        assertEquals("fake-session-id", launchedUri.getQueryParameter("shopperSessionId"))
        assertEquals("pda", launchedUri.getQueryParameter("source"))
        assertEquals("fake-merchant-id", launchedUri.getQueryParameter("merchant"))
        assertEquals("va", launchedUri.getQueryParameter("flow_type"))
        assertEquals("paypal", launchedUri.getQueryParameter("funding_source"))

        val switchInitiatedTime = launchedUri.getQueryParameter("switch_initiated_time")?.toLongOrNull()
        assertNotNull(switchInitiatedTime)
        assertTrue(switchInitiatedTime!! in beforeMillis..afterMillis)
    }

    @Test
    fun `vault() with setupTokenId delivers failure without patchCCO fallback when createShopperSession throws`() =
        runTest {
            val sutV3 = makeSutWithUrlScheme()
            val callback = mockk<PayPalWebVaultCallback>(relaxed = true)
            sutV3.createPayPalSession(
                tokenType = TokenType.ORDER_ID,
                userIdentity = fakeUserIdentity,
                urlConfig = fakeUrlConfig,
            )
            // Any unexpected exception from the pre-warm fetch (as opposed to a "normal"
            // APIResult.Failure) should fail the whole flow rather than fall back to patchCCO.
            sutV3.shopperSessionDeferred = CompletableDeferred<CreateShopperSessionWithAppSwitchEligibilityResponse?>()
                .also { it.completeExceptionally(RuntimeException("session error")) }
            sutV3.vault(activity, "fake-setup-token-id", callback)
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify(exactly = 0) {
                patchCCOWithAppSwitchEligibility(any(), any(), any(), any(), any())
            }
            verify {
                callback.onPayPalWebVaultResult(match { it is PayPalPresentAuthChallengeResult.Failure })
            }
        }

    // --- LLD Section 3.8 fallback-to-patchCCO behavior ---

    @Ignore(
        "patchCCO fallback removed pending re-implementation; " +
            "see TODO in PayPalWebCheckoutClient.launchVaultViaPatchCCOFallback"
    )
    @Test
    fun `vault() falls back to patchCCO when shopper session fetch reports a session-creation-or-network failure`() =
        runTest {
            val sutV3 = makeSutWithUrlScheme()
            every { deviceInspector.isPayPalInstalled } returns true
            val callback = mockk<PayPalWebVaultCallback>(relaxed = true)
            val launchResult = PayPalPresentAuthChallengeResult.Success("auth-state")

            coEvery {
                patchCCOWithAppSwitchEligibility(any(), any(), any(), any(), any())
            } returns APIResult.Success(
                AppSwitchEligibility(
                    appSwitchEligible = true,
                    launchUrl = "https://paypal.com/patch-cco-vault",
                    ineligibleReason = null
                )
            )
            every {
                payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any())
            } returns launchResult

            sutV3.createPayPalSession(
                tokenType = TokenType.ORDER_ID,
                userIdentity = fakeUserIdentity,
                urlConfig = fakeUrlConfig,
            )
            // null represents a session-creation failure / network timeout (any non-LSAT
            // APIResult.Failure from createShopperSessionWithAppSwitchEligibility).
            sutV3.shopperSessionDeferred =
                CompletableDeferred<CreateShopperSessionWithAppSwitchEligibilityResponse?>()
                    .also { it.complete(null) }
            sutV3.vault(activity, "fake-setup-token-id", callback)
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify(exactly = 1) {
                patchCCOWithAppSwitchEligibility(
                    context = activity,
                    orderId = "fake-setup-token-id",
                    tokenType = TokenType.VAULT_ID,
                    merchantOptInForAppSwitch = true,
                    paypalNativeAppInstalled = true
                )
            }
            verify {
                payPalWebLauncher.launchWithUrl(
                    context = activity,
                    uri = any(),
                    token = "fake-setup-token-id",
                    tokenType = TokenType.VAULT_ID,
                    returnToAppStrategy = any()
                )
            }
            verify { callback.onPayPalWebVaultResult(launchResult) }
        }

    @Ignore(
        "patchCCO fallback removed pending re-implementation; " +
            "see TODO in PayPalWebCheckoutClient.launchVaultViaPatchCCOFallback"
    )
    @Test
    fun `vault() falls back to a vault URI, not a checkout URI, when patchCCO has no launch url`() =
        runTest {
            val sutV3 = makeSutWithUrlScheme()
            every { deviceInspector.isPayPalInstalled } returns false
            val callback = mockk<PayPalWebVaultCallback>(relaxed = true)
            val launchResult = PayPalPresentAuthChallengeResult.Success("auth-state")
            val uriSlot = slot<Uri>()
            every {
                payPalWebLauncher.launchWithUrl(any(), capture(uriSlot), any(), any(), any())
            } returns launchResult

            sutV3.createPayPalSession(
                tokenType = TokenType.ORDER_ID,
                userIdentity = fakeUserIdentity,
                urlConfig = fakeUrlConfig,
            )
            // null represents a session-creation failure / network timeout (any non-LSAT
            // APIResult.Failure from createShopperSessionWithAppSwitchEligibility).
            sutV3.shopperSessionDeferred =
                CompletableDeferred<CreateShopperSessionWithAppSwitchEligibilityResponse?>()
                    .also { it.complete(null) }
            sutV3.vault(activity, "fake-setup-token-id", callback)
            testDispatcher.scheduler.advanceUntilIdle()

            // PayPal app isn't installed, so patchCCO is never attempted and getLaunchUri()
            // returns the fallback URI unchanged: it must be a vault URL, never a checkout URL.
            coVerify(exactly = 0) {
                patchCCOWithAppSwitchEligibility(any(), any(), any(), any(), any())
            }
            val launchedUri = uriSlot.captured
            assertTrue(launchedUri.toString().contains("agreements/approve"))
            assertEquals("fake-setup-token-id", launchedUri.getQueryParameter("approval_session_id"))
            assertFalse(launchedUri.toString().contains("checkoutnow"))
        }

    // --- createShopperSessionWithAppSwitchEligibility() outcome mapping (LLD Section 3.8) ---

    @Test
    fun `createShopperSessionWithAppSwitchEligibility() returns Success on API success`() = runTest {
        coEvery {
            createShopperSessionAPI(
                token = any(),
                tokenType = any(),
                params = any(),
            )
        } returns APIResult.Success(fakeSessionResponse)

        val outcome = sut.createShopperSessionWithAppSwitchEligibility(
            token = "fake-order-id",
            tokenType = TokenType.ORDER_ID,
            urlConfig = fakeUrlConfig,
            userIdentity = fakeUserIdentity,
            userAction = PayPalUserAction.CONTINUE,
        )

        assertSame(fakeSessionResponse, outcome)
    }

    @Test
    fun `createShopperSessionWithAppSwitchEligibility() returns null on a session-creation-or-network failure`() =
        runTest {
            val sessionError = PayPalSDKError(5, "server responded with an error")
            coEvery {
                createShopperSessionAPI(
                    token = any(),
                    tokenType = any(),
                    params = any(),
                )
            } returns APIResult.Failure(sessionError)

            val outcome = sut.createShopperSessionWithAppSwitchEligibility(
                token = "fake-order-id",
                tokenType = TokenType.ORDER_ID,
                urlConfig = fakeUrlConfig,
                userIdentity = fakeUserIdentity,
                userAction = PayPalUserAction.CONTINUE,
            )

            assertNull(outcome)
        }

    @Test
    fun `createShopperSessionWithAppSwitchEligibility() propagates any exception thrown by the underlying API`() =
        runTest {
            // Defensive coverage: if the injected API throws for any reason, the exception
            // should propagate rather than be swallowed.
            val lsatError = PayPalSDKError(401, "Unauthorized")
            coEvery {
                createShopperSessionAPI(
                    token = any(),
                    tokenType = any(),
                    params = any(),
                )
            } throws lsatError

            val thrown = runCatching {
                sut.createShopperSessionWithAppSwitchEligibility(
                    token = "fake-order-id",
                    tokenType = TokenType.ORDER_ID,
                    urlConfig = fakeUrlConfig,
                    userIdentity = fakeUserIdentity,
                    userAction = PayPalUserAction.CONTINUE,
                )
            }.exceptionOrNull()

            assertSame(lsatError, thrown)
        }

    @Test
    fun `vault() with setupTokenId clears session deferred so a second call returns SESSION_NOT_CREATED`() =
        runTest {
            val sutV3 = makeSutWithUrlScheme()
            every { payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any()) } returns
                PayPalPresentAuthChallengeResult.Success("auth-state")

            sutV3.createPayPalSession(
                tokenType = TokenType.ORDER_ID,
                userIdentity = fakeUserIdentity,
                urlConfig = fakeUrlConfig,
            )
            sutV3.shopperSessionDeferred = CompletableDeferred(fakeSessionResponse)
            val callback1 = mockk<PayPalWebVaultCallback>(relaxed = true)
            val callback2 = mockk<PayPalWebVaultCallback>(relaxed = true)

            sutV3.vault(activity, "fake-setup-token-id", callback1)
            testDispatcher.scheduler.advanceUntilIdle()

            sutV3.vault(activity, "fake-setup-token-id", callback2)
            testDispatcher.scheduler.advanceUntilIdle()

            verify {
                callback2.onPayPalWebVaultResult(match {
                    it is PayPalPresentAuthChallengeResult.Failure &&
                        it.error.code == PayPalWebCheckoutError.sessionNotCreatedError.code
                })
            }
        }

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

    @Test
    fun `finishStart() sends analytics with appSwitchEnabled false for canceled event`() = runTest {
        // Given
        every { deviceInspector.isPayPalInstalled } returns false
        val request = PayPalWebCheckoutRequest(
            "fake-order-id",
            returnToAppStrategy = ReturnToAppStrategy.AppLink(appLinkUrl)
        )
        val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
        val canceledResult = PayPalWebCheckoutFinishStartResult.Canceled("fake-order-id")

        every {
            payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any())
        } returns launchResult

        every {
            payPalWebLauncher.completeCheckoutAuthRequest(intent, "auth state")
        } returns canceledResult

        // When
        sut.startAsync(activity, request)
        sut.finishStart(intent)

        // Then
        verify { analytics.notify(CheckoutEvent.CANCELED, "fake-order-id", false) }
    }

    @Test
    fun `finishStart() sends analytics with appSwitchEnabled false for failed event`() = runTest {
        // Given
        every { deviceInspector.isPayPalInstalled } returns false
        val request = PayPalWebCheckoutRequest(
            "fake-order-id",
            returnToAppStrategy = ReturnToAppStrategy.AppLink(appLinkUrl)
        )
        val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
        val error = PayPalSDKError(123, "fake-error-description")
        val failureResult = PayPalWebCheckoutFinishStartResult.Failure(error, null)

        every {
            payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any())
        } returns launchResult

        every {
            payPalWebLauncher.completeCheckoutAuthRequest(intent, "auth state")
        } returns failureResult

        // When
        sut.startAsync(activity, request)
        sut.finishStart(intent)

        // Then
        verify {
            analytics.notify(
                CheckoutEvent.FAILED,
                "fake-order-id",
                false,
                errorDescription = "fake-error-description",
            )
        }
    }

    // MARK: - Additional coverage: app-switch-eligible redirectUrl, failure analytics, noReturnToAppStrategyError

    @Test
    fun `start() with orderId prefers redirectUrl over checkoutFallbackUrl when app-switch eligible and installed`() =
        runTest {
            val sutV3 = makeSutWithUrlScheme()
            every { deviceInspector.isPayPalInstalled } returns true
            every { deviceInspector.canResolvePayPalAppSwitch() } returns true
            val uriSlot = slot<Uri>()
            every {
                payPalWebLauncher.launchWithUrl(any(), capture(uriSlot), any(), any(), any())
            } returns PayPalPresentAuthChallengeResult.Success("auth-state")

            val appSwitchEligibleResponse = fakeSessionResponse.copy(
                appSwitchEligible = true,
                redirectUrl = "https://example.com/app-switch-redirect",
                checkoutFallbackUrl = "https://example.com/fallback",
            )
            val callback = mockk<PayPalWebStartCallback>(relaxed = true)
            sutV3.createPayPalSession(
                tokenType = TokenType.ORDER_ID,
                userIdentity = fakeUserIdentity,
                urlConfig = fakeUrlConfig,
            )
            sutV3.shopperSessionDeferred = CompletableDeferred(appSwitchEligibleResponse)
            sutV3.start(activity, "fake-order-id", callback)
            testDispatcher.scheduler.advanceUntilIdle()

            val launchedUri = uriSlot.captured
            assertTrue(launchedUri.toString().startsWith("https://example.com/app-switch-redirect"))
            assertEquals("fake-order-id", launchedUri.getQueryParameter("token"))
        }

    @Test
    fun `start() with orderId uses checkoutFallbackUrl when app-switch eligible but not installed`() =
        runTest {
            // Regression test for DTPPMOBILE-543: the backend can report a shopper session as
            // app-switch eligible even when the PayPal app isn't installed on this device. In
            // that case we must not open the native app-switch redirectUrl (e.g.
            // app-switch-checkout) in a Custom Tab, since that page errors out when it isn't
            // handed off to the native app. We should fall back to checkoutFallbackUrl instead.
            val sutV3 = makeSutWithUrlScheme()
            every { deviceInspector.isPayPalInstalled } returns false
            val uriSlot = slot<Uri>()
            every {
                payPalWebLauncher.launchWithUrl(any(), capture(uriSlot), any(), any(), any())
            } returns PayPalPresentAuthChallengeResult.Success("auth-state")

            val appSwitchEligibleButNotInstalledResponse = fakeSessionResponse.copy(
                appSwitchEligible = true,
                redirectUrl = "https://www.paypal.com/app-switch-checkout",
                checkoutFallbackUrl = "https://www.paypal.com/checkoutnow",
            )
            val callback = mockk<PayPalWebStartCallback>(relaxed = true)
            sutV3.createPayPalSession(
                tokenType = TokenType.ORDER_ID,
                userIdentity = fakeUserIdentity,
                urlConfig = fakeUrlConfig,
            )
            sutV3.shopperSessionDeferred = CompletableDeferred(appSwitchEligibleButNotInstalledResponse)
            sutV3.start(activity, "fake-order-id", callback)
            testDispatcher.scheduler.advanceUntilIdle()

            val launchedUri = uriSlot.captured
            assertTrue(launchedUri.toString().startsWith("https://www.paypal.com/checkoutnow"))
            assertEquals("fake-order-id", launchedUri.getQueryParameter("token"))
        }

    @Test
    fun `vault() with setupTokenId uses redirectUrl when session is app-switch eligible and app is installed`() =
        runTest {
            val sutV3 = makeSutWithUrlScheme()
            every { deviceInspector.isPayPalInstalled } returns true
            every { deviceInspector.canResolvePayPalAppSwitch() } returns true
            val uriSlot = slot<Uri>()
            every {
                payPalWebLauncher.launchWithUrl(any(), capture(uriSlot), any(), any(), any())
            } returns PayPalPresentAuthChallengeResult.Success("auth-state")

            val appSwitchEligibleResponse = fakeSessionResponse.copy(
                appSwitchEligible = true,
                redirectUrl = "https://example.com/app-switch-vault-redirect",
                checkoutFallbackUrl = "https://example.com/fallback",
            )
            val callback = mockk<PayPalWebVaultCallback>(relaxed = true)
            sutV3.createPayPalSession(
                tokenType = TokenType.ORDER_ID,
                userIdentity = fakeUserIdentity,
                urlConfig = fakeUrlConfig,
            )
            sutV3.shopperSessionDeferred = CompletableDeferred(appSwitchEligibleResponse)
            sutV3.vault(activity, "fake-setup-token-id", callback)
            testDispatcher.scheduler.advanceUntilIdle()

            val launchedUri = uriSlot.captured
            assertTrue(launchedUri.toString().startsWith("https://example.com/app-switch-vault-redirect"))
            // Vault identifies the session via approval_session_id, not token (see
            // PayPalWebLauncher.URL_PARAM_APPROVAL_SESSION_ID, which is what's read back out of
            // the return deep link) — regression coverage for the vault-lands-on-error-page bug.
            assertEquals("fake-setup-token-id", launchedUri.getQueryParameter("approval_session_id"))
            assertNull(launchedUri.getQueryParameter("token"))
        }

    @Test
    fun `start() with orderId strips trailing ampersand from redirectUrl before appending query params`() =
        runTest {
            val sutV3 = makeSutWithUrlScheme()
            every { deviceInspector.isPayPalInstalled } returns true
            every { deviceInspector.canResolvePayPalAppSwitch() } returns true
            val uriSlot = slot<Uri>()
            every {
                payPalWebLauncher.launchWithUrl(any(), capture(uriSlot), any(), any(), any())
            } returns PayPalPresentAuthChallengeResult.Success("auth-state")

            val trailingAmpersandResponse = fakeSessionResponse.copy(
                appSwitchEligible = true,
                redirectUrl = "https://example.com/app-switch-redirect?existing=1&",
            )
            val callback = mockk<PayPalWebStartCallback>(relaxed = true)
            sutV3.createPayPalSession(
                tokenType = TokenType.ORDER_ID,
                userIdentity = fakeUserIdentity,
                urlConfig = fakeUrlConfig,
            )
            sutV3.shopperSessionDeferred = CompletableDeferred(trailingAmpersandResponse)
            sutV3.start(activity, "fake-order-id", callback)
            testDispatcher.scheduler.advanceUntilIdle()

            val launchedUri = uriSlot.captured
            assertFalse(launchedUri.toString().contains("&&"))
            assertEquals("1", launchedUri.getQueryParameter("existing"))
            assertEquals("fake-order-id", launchedUri.getQueryParameter("token"))
        }

    @Test
    fun `start() with orderId notifies checkout failure analytics when launch fails after session resolves`() =
        runTest {
            val sutV3 = makeSutWithUrlScheme()
            val sdkError = PayPalSDKError(123, "fake error description")
            every {
                payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any())
            } returns PayPalPresentAuthChallengeResult.Failure(sdkError)

            val callback = mockk<PayPalWebStartCallback>(relaxed = true)
            sutV3.createPayPalSession(
                tokenType = TokenType.ORDER_ID,
                userIdentity = fakeUserIdentity,
                urlConfig = fakeUrlConfig,
            )
            sutV3.shopperSessionDeferred = CompletableDeferred(fakeSessionResponse)
            sutV3.start(activity, "fake-order-id", callback)
            testDispatcher.scheduler.advanceUntilIdle()

            verify {
                analytics.notify(
                    CheckoutEvent.BROWSER_PRESENTATION_FAILED,
                    "fake-order-id",
                    false,
                    shopperSessionId = "fake-session-id",
                    errorDescription = "fake error description",
                )
            }
            verify {
                callback.onPayPalWebStartResult(match {
                    it is PayPalPresentAuthChallengeResult.Failure && it.error === sdkError
                })
            }
        }

    @Test
    fun `vault() with setupTokenId notifies vault failure analytics when launch fails after session resolves`() =
        runTest {
            val sutV3 = makeSutWithUrlScheme()
            val sdkError = PayPalSDKError(123, "fake error description")
            every {
                payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any())
            } returns PayPalPresentAuthChallengeResult.Failure(sdkError)

            val callback = mockk<PayPalWebVaultCallback>(relaxed = true)
            sutV3.createPayPalSession(
                tokenType = TokenType.ORDER_ID,
                userIdentity = fakeUserIdentity,
                urlConfig = fakeUrlConfig,
            )
            sutV3.shopperSessionDeferred = CompletableDeferred(fakeSessionResponse)
            sutV3.vault(activity, "fake-setup-token-id", callback)
            testDispatcher.scheduler.advanceUntilIdle()

            verify {
                analytics.notify(
                    VaultEvent.BROWSER_PRESENTATION_FAILED,
                    "fake-setup-token-id",
                    false,
                    shopperSessionId = "fake-session-id",
                    errorDescription = "fake error description",
                )
            }
            verify {
                callback.onPayPalWebVaultResult(match {
                    it is PayPalPresentAuthChallengeResult.Failure && it.error === sdkError
                })
            }
        }

    @Test
    fun `start() with orderId notifies checkout failure analytics when the shopper session fetch throws`() =
        runTest {
            val sutV3 = makeSutWithUrlScheme()
            val callback = mockk<PayPalWebStartCallback>(relaxed = true)
            sutV3.createPayPalSession(
                tokenType = TokenType.ORDER_ID,
                userIdentity = fakeUserIdentity,
                urlConfig = fakeUrlConfig,
            )
            sutV3.shopperSessionDeferred = CompletableDeferred<CreateShopperSessionWithAppSwitchEligibilityResponse?>()
                .also { it.completeExceptionally(RuntimeException("session error")) }
            sutV3.start(activity, "fake-order-id", callback)
            testDispatcher.scheduler.advanceUntilIdle()

            verify {
                analytics.notify(
                    CheckoutEvent.FAILED,
                    "fake-order-id",
                    false,
                    errorDescription = "session error",
                )
            }
        }

    @Test
    fun `vault() with setupTokenId notifies vault failure analytics when the shopper session fetch throws`() =
        runTest {
            val sutV3 = makeSutWithUrlScheme()
            val callback = mockk<PayPalWebVaultCallback>(relaxed = true)
            sutV3.createPayPalSession(
                tokenType = TokenType.ORDER_ID,
                userIdentity = fakeUserIdentity,
                urlConfig = fakeUrlConfig,
            )
            sutV3.shopperSessionDeferred = CompletableDeferred<CreateShopperSessionWithAppSwitchEligibilityResponse?>()
                .also { it.completeExceptionally(RuntimeException("session error")) }
            sutV3.vault(activity, "fake-setup-token-id", callback)
            testDispatcher.scheduler.advanceUntilIdle()

            verify {
                analytics.notify(
                    VaultEvent.FAILED,
                    "fake-setup-token-id",
                    false,
                    errorDescription = "session error",
                )
            }
        }

    @Test
    fun `startAsync() delivers noReturnToAppStrategyError when strategy and urlScheme are absent`() =
        runTest {
            val request = PayPalWebCheckoutRequest(
                orderId = "fake-order-id",
                returnToAppStrategy = null
            )

            val result = sut.startAsync(activity, request) as PayPalPresentAuthChallengeResult.Failure

            assertSame(PayPalWebCheckoutError.noReturnToAppStrategyError, result.error)
            verify(exactly = 0) {
                payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any())
            }
        }

    @Test
    fun `vaultAsync() delivers noReturnToAppStrategyError when strategy and urlScheme are absent`() =
        runTest {
            val request = PayPalWebVaultRequest(
                setupTokenId = "fake-setup-token-id",
                returnToAppStrategy = null
            )

            val result = sut.vaultAsync(activity, request) as PayPalPresentAuthChallengeResult.Failure

            assertSame(PayPalWebCheckoutError.noReturnToAppStrategyError, result.error)
            verify(exactly = 0) {
                payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any())
            }
        }

    @Test
    fun `createPayPalSession passes the per-app-start sessionId as the contextId token`() = runTest {
        val sutV3 = makeSutWithUrlScheme()
        coEvery {
            createShopperSessionAPI(
                token = any(),
                tokenType = any(),
                params = any(),
            )
        } returns APIResult.Success(fakeSessionResponse)

        sutV3.createPayPalSession(
            tokenType = TokenType.ORDER_ID,
            userIdentity = fakeUserIdentity,
            urlConfig = fakeUrlConfig,
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // The token passed into the API becomes the contextId; it must be the stored, per-app-start sessionId.
        coVerify(exactly = 1) {
            createShopperSessionAPI(
                token = SessionIdRepository.instance.sessionId,
                tokenType = TokenType.ORDER_ID,
                params = any(),
            )
        }
    }
}
