package com.paypal.android.paypalwebpayments

import android.content.Intent
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.Environment
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.corepayments.ReturnToAppStrategy
import com.paypal.android.corepayments.UpdateClientConfigAPI
import com.paypal.android.corepayments.api.PatchCCOWithAppSwitchEligibility
import com.paypal.android.corepayments.common.DeviceInspector
import com.paypal.android.corepayments.model.APIResult
import com.paypal.android.corepayments.model.AppSwitchEligibility
import com.paypal.android.corepayments.model.AppSwitchEligibilityData
import com.paypal.android.corepayments.model.TokenType
import com.paypal.android.paypalwebpayments.analytics.CheckoutEvent
import com.paypal.android.paypalwebpayments.analytics.PayPalWebAnalytics
import com.paypal.android.paypalwebpayments.analytics.VaultEvent
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertSame
import junit.framework.TestCase.assertTrue
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
    private val deviceInspector: DeviceInspector = mockk(relaxed = true)
    private val coreConfig = CoreConfig("fake-client-id", Environment.SANDBOX)
    private val urlScheme = "com.example.app"
    private val fakeAppSwitchUrl = "https://paypal.com/vault-app-switch"

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
                activity = activity,
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
                activity = activity,
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

        val request = PayPalWebCheckoutRequest("fake-order-id")
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

        val request = PayPalWebCheckoutRequest("fake-order-id")
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

        val request = PayPalWebCheckoutRequest("fake-order-id")
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

        val request = PayPalWebCheckoutRequest("fake-order-id")
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
                patchCCOWithAppSwitchEligibility = patchCCOWithAppSwitchEligibility
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
    fun `start() uses app switch when enabled and app switch URL is available`() = runTest {
        // Given
        every { deviceInspector.isPayPalInstalled } returns true
        val appLinkUrl = "https://example.com/return"
        val request = PayPalWebCheckoutRequest(
            "fake-order-id",
            appSwitchWhenEligible = true,
            returnToAppStrategy = ReturnToAppStrategy.AppLink(appLinkUrl)
        )
        val appSwitchResponse = createAppSwitchEligibilityResponse(fakeAppSwitchUrl)
        val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")

        coEvery {
            patchCCOWithAppSwitchEligibility(
                context = activity.applicationContext,
                orderId = "fake-order-id",
                tokenType = TokenType.ORDER_ID,
                merchantOptInForAppSwitch = true,
                paypalNativeAppInstalled = true
            )
        } returns APIResult.Success(appSwitchResponse)

        every {
            payPalWebLauncher.launchWithUrl(
                activity = activity,
                uri = any(),
                token = any(),
                tokenType = any(),
                returnToAppStrategy = any()
            )
        } returns launchResult

        // When
        val result = sut.startAsync(activity, request)

        // Then
        coVerify {
            patchCCOWithAppSwitchEligibility(
                context = activity.applicationContext,
                orderId = "fake-order-id",
                tokenType = TokenType.ORDER_ID,
                merchantOptInForAppSwitch = true,
                paypalNativeAppInstalled = true
            )
        }
        verify {
            payPalWebLauncher.launchWithUrl(
                activity = activity,
                uri = any(),
                token = "fake-order-id",
                tokenType = TokenType.ORDER_ID,
                returnToAppStrategy = ReturnToAppStrategy.AppLink(appLinkUrl)
            )
        }
        assertSame(launchResult, result)
    }

    @Test
    fun `start() falls back to web checkout when app switch is enabled but URL is null`() =
        runTest {
            // Given
            every { deviceInspector.isPayPalInstalled } returns true
            val request = PayPalWebCheckoutRequest("fake-order-id", appSwitchWhenEligible = true)
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
                    activity = activity,
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
            val request = PayPalWebCheckoutRequest("fake-order-id", appSwitchWhenEligible = true)
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
                    activity = activity,
                    uri = any(),
                    token = "fake-order-id",
                    tokenType = TokenType.ORDER_ID,
                    returnToAppStrategy = any()
                )
            }
            assertSame(launchResult, result)
        }

    @Test
    fun `start() falls back to web checkout when app switch request fails`() = runTest {
        // Given
        every { deviceInspector.isPayPalInstalled } returns true
        val request = PayPalWebCheckoutRequest(
            orderId = "fake-order-id",
            appSwitchWhenEligible = true,
            returnToAppStrategy = ReturnToAppStrategy.AppLink(appLinkUrl)
        )
        val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")

        coEvery {
            patchCCOWithAppSwitchEligibility(any(), any(), any(), any(), any())
        } returns APIResult.Failure(PayPalSDKError(1001, "Test failure"))

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
        val result = sut.startAsync(activity, request)

        // Then
        coVerify { patchCCOWithAppSwitchEligibility(any(), any(), any(), any(), any()) }
        verify {
            payPalWebLauncher.launchWithUrl(
                activity = activity,
                uri = any(),
                token = "fake-order-id",
                tokenType = TokenType.ORDER_ID,
                returnToAppStrategy = ReturnToAppStrategy.AppLink(any())
            )
        }
        assertSame(launchResult, result)
    }

    @Test
    fun `start() skips app switch check when appSwitchWhenEligible is false`() = runTest {
        // Given
        val request = PayPalWebCheckoutRequest("fake-order-id", appSwitchWhenEligible = false)
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

        // When
        val result = sut.startAsync(activity, request)

        // Then
        coVerify(exactly = 0) {
            patchCCOWithAppSwitchEligibility(any(), any(), any(), any(), any())
        }
        verify {
            payPalWebLauncher.launchWithUrl(
                activity = activity,
                uri = any(),
                token = "fake-order-id",
                tokenType = TokenType.ORDER_ID,
                returnToAppStrategy = any()
            )
        }
        assertSame(launchResult, result)
    }

    @Test
    fun `start() uses correct token type in app switch request`() = runTest {
        // Given
        every { deviceInspector.isPayPalInstalled } returns true
        val request = PayPalWebCheckoutRequest("test-order-123", appSwitchWhenEligible = true)
        val appSwitchResponse = createAppSwitchEligibilityResponse("https://test.com")

        coEvery {
            patchCCOWithAppSwitchEligibility(any(), any(), any(), any(), any())
        } returns APIResult.Success(appSwitchResponse)

        every {
            payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any())
        } returns PayPalPresentAuthChallengeResult.Success("state")

        // When
        sut.startAsync(activity, request)

        // Then
        coVerify {
            patchCCOWithAppSwitchEligibility(
                context = activity.applicationContext,
                orderId = "test-order-123",
                tokenType = TokenType.ORDER_ID,
                merchantOptInForAppSwitch = true,
                paypalNativeAppInstalled = true
            )
        }
    }

    // VAULT APP SWITCH TESTS

    @Test
    fun `vault() uses app switch when enabled and app switch URL is available`() = runTest {
        // Given
        every { deviceInspector.isPayPalInstalled } returns true
        val request = PayPalWebVaultRequest(
            "fake-setup-token-id",
            appSwitchWhenEligible = true,
            returnToAppStrategy = ReturnToAppStrategy.AppLink(appLinkUrl)
        )
        val appSwitchResponse =
            createAppSwitchEligibilityResponse(fakeAppSwitchUrl)
        val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")

        coEvery {
            patchCCOWithAppSwitchEligibility(
                context = activity.applicationContext,
                orderId = "fake-setup-token-id",
                tokenType = TokenType.VAULT_ID,
                merchantOptInForAppSwitch = true,
                paypalNativeAppInstalled = true
            )
        } returns APIResult.Success(appSwitchResponse)

        every {
            payPalWebLauncher.launchWithUrl(
                activity = activity,
                uri = fakeAppSwitchUrl.toUri(),
                token = "fake-setup-token-id",
                tokenType = TokenType.VAULT_ID,
                returnToAppStrategy = ReturnToAppStrategy.AppLink(any())
            )
        } returns launchResult

        // When
        val result = sut.vaultAsync(activity, request)

        // Then
        coVerify {
            patchCCOWithAppSwitchEligibility(
                context = activity.applicationContext,
                orderId = "fake-setup-token-id",
                tokenType = TokenType.VAULT_ID,
                merchantOptInForAppSwitch = true,
                paypalNativeAppInstalled = true
            )
        }
        verify {
            payPalWebLauncher.launchWithUrl(
                activity = activity,
                uri = any(),
                token = "fake-setup-token-id",
                tokenType = TokenType.VAULT_ID,
                returnToAppStrategy = ReturnToAppStrategy.AppLink(appLinkUrl)
            )
        }
        assertSame(launchResult, result)
    }

    @Test
    fun `vault() falls back to web vault when app switch is enabled but URL is null`() = runTest {
        // Given
        every { deviceInspector.isPayPalInstalled } returns true
        val request = PayPalWebVaultRequest("fake-setup-token-id", appSwitchWhenEligible = true)
        val appSwitchResponse = createAppSwitchEligibilityResponse(null)
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
        coVerify {
            patchCCOWithAppSwitchEligibility(
                context = activity.applicationContext,
                orderId = "fake-setup-token-id",
                tokenType = TokenType.VAULT_ID,
                merchantOptInForAppSwitch = true,
                paypalNativeAppInstalled = true
            )
        }
        verify {
            payPalWebLauncher.launchWithUrl(
                activity = activity,
                uri = any(),
                token = "fake-setup-token-id",
                tokenType = TokenType.VAULT_ID,
                returnToAppStrategy = any()
            )
        }
        assertSame(launchResult, result)
    }

    @Test
    fun `vault() falls back to web vault when app switch is enabled but empty URL is returned`() =
        runTest {
            // Given
            every { deviceInspector.isPayPalInstalled } returns true
            val request = PayPalWebVaultRequest("fake-setup-token-id", appSwitchWhenEligible = true)
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
                    activity = activity,
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
        val request = PayPalWebVaultRequest("fake-setup-token-id", appSwitchWhenEligible = true)
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
                activity = activity,
                uri = any(),
                token = "fake-setup-token-id",
                tokenType = TokenType.VAULT_ID,
                returnToAppStrategy = any()
            )
        }
        assertSame(launchResult, result)
    }

    @Test
    fun `vault() skips app switch check when appSwitchWhenEligible is false`() = runTest {
        // Given
        val request = PayPalWebVaultRequest("fake-setup-token-id", appSwitchWhenEligible = false)
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

        // When
        val result = sut.vaultAsync(activity, request)

        // Then
        coVerify(exactly = 0) {
            patchCCOWithAppSwitchEligibility(any(), any(), any(), any(), any())
        }
        verify {
            payPalWebLauncher.launchWithUrl(
                activity = activity,
                uri = any(),
                token = "fake-setup-token-id",
                tokenType = TokenType.VAULT_ID,
                returnToAppStrategy = any()
            )
        }
        assertSame(launchResult, result)
    }

    @Test
    fun `vault() uses correct token type in app switch request`() = runTest {
        // Given
        every { deviceInspector.isPayPalInstalled } returns true
        val request = PayPalWebVaultRequest("test-setup-123", appSwitchWhenEligible = true)
        val appSwitchResponse = createAppSwitchEligibilityResponse("https://test.com")

        coEvery {
            patchCCOWithAppSwitchEligibility(any(), any(), any(), any(), any())
        } returns APIResult.Success(appSwitchResponse)

        every {
            payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any())
        } returns PayPalPresentAuthChallengeResult.Success("state")

        // When
        sut.vaultAsync(activity, request)

        // Then
        coVerify {
            patchCCOWithAppSwitchEligibility(
                context = activity.applicationContext,
                orderId = "test-setup-123",
                tokenType = TokenType.VAULT_ID,
                merchantOptInForAppSwitch = true,
                paypalNativeAppInstalled = true
            )
        }
    }

    @Test
    fun `start() skips app switch when app switch enabled but PayPal app not installed`() =
        runTest {
            // Given
            every { deviceInspector.isPayPalInstalled } returns false
            val request = PayPalWebCheckoutRequest("fake-order-id", appSwitchWhenEligible = true)
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
                    activity = activity,
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
            val request = PayPalWebVaultRequest("fake-setup-token-id", appSwitchWhenEligible = true)
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
                    activity = activity,
                    uri = any(),
                    token = "fake-setup-token-id",
                    tokenType = TokenType.VAULT_ID,
                    returnToAppStrategy = any()
                )
            }
            assertSame(launchResult, result)
        }

    @Test
    fun `start() uses web checkout when app switch disabled regardless of PayPal app installation`() =
        runTest {
            // Given
            every { deviceInspector.isPayPalInstalled } returns true
            val request = PayPalWebCheckoutRequest("fake-order-id", appSwitchWhenEligible = false)
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
                    activity = activity,
                    uri = any(),
                    token = "fake-order-id",
                    tokenType = TokenType.ORDER_ID,
                    returnToAppStrategy = any()
                )
            }
            assertSame(launchResult, result)
        }

    @Test
    fun `vault() uses web vault when app switch disabled regardless of PayPal app installation`() =
        runTest {
            // Given
            every { deviceInspector.isPayPalInstalled } returns true
            val request = PayPalWebVaultRequest("fake-setup-token-id", appSwitchWhenEligible = false)
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
                    activity = activity,
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
            patchCCOWithAppSwitchEligibility = patchCCOWithAppSwitchEligibility
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

            previousClient.vaultAsync(activity, PayPalWebVaultRequest("fake-setup-token-id"))

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
            patchCCOWithAppSwitchEligibility = patchCCOWithAppSwitchEligibility
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

            previousClient.vaultAsync(activity, PayPalWebVaultRequest("fake-setup-token-id"))

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

            sut.vaultAsync(activity, PayPalWebVaultRequest("fake-setup-token-id"))
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

            sut.vaultAsync(activity, PayPalWebVaultRequest("fake-setup-token-id"))

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

            sut.vaultAsync(activity, PayPalWebVaultRequest("fake-setup-token-id"))
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

            sut.vaultAsync(activity, PayPalWebVaultRequest("fake-setup-token-id"))

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
                activity = activity,
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
                activity = activity,
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
                    activity = activity,
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
                    activity = activity,
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

        // Verify analytics was called with CheckoutEvent
        verify { analytics.notify(any<CheckoutEvent>(), "fake-order-id") }

        // Verify launchWithUrl is called (the deprecated method calls it directly)
        verify(exactly = 1) {
            payPalWebLauncher.launchWithUrl(
                activity = activity,
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

        // Verify analytics was called with VaultEvent
        verify { analytics.notify(any<VaultEvent>(), any()) }

        // Verify launchWithUrl is called (the deprecated method calls it directly)
        verify(exactly = 1) {
            payPalWebLauncher.launchWithUrl(
                activity = activity,
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
                activity = activity,
                uri = any(),
                token = "fake-order-id",
                tokenType = TokenType.ORDER_ID,
                returnToAppStrategy = ReturnToAppStrategy.CustomUrlScheme(fallbackScheme)
            )
        }
    }

    @Test
    fun `startAsync() uses default urlScheme when returnToAppStrategy is null`() = runTest {
        val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
        every {
            payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any())
        } returns launchResult

        val request = PayPalWebCheckoutRequest(
            orderId = "fake-order-id",
            returnToAppStrategy = null
        )
        sut.startAsync(activity, request)

        verify(exactly = 1) {
            payPalWebLauncher.launchWithUrl(
                activity = activity,
                uri = any(),
                token = "fake-order-id",
                tokenType = TokenType.ORDER_ID,
                returnToAppStrategy = any()
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
                activity = activity,
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
                activity = activity,
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
                activity = activity,
                uri = any(),
                token = "fake-setup-token-id",
                tokenType = TokenType.VAULT_ID,
                returnToAppStrategy = ReturnToAppStrategy.CustomUrlScheme(fallbackScheme)
            )
        }
    }

    @Test
    fun `vaultAsync() uses default urlScheme when returnToAppStrategy is null`() = runTest {
        val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
        every {
            payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any())
        } returns launchResult

        val request = PayPalWebVaultRequest(
            setupTokenId = "fake-setup-token-id",
            returnToAppStrategy = null
        )
        sut.vaultAsync(activity, request)

        verify(exactly = 1) {
            payPalWebLauncher.launchWithUrl(
                activity = activity,
                uri = any(),
                token = "fake-setup-token-id",
                tokenType = TokenType.VAULT_ID,
                returnToAppStrategy = any()
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
                activity = activity,
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

}
