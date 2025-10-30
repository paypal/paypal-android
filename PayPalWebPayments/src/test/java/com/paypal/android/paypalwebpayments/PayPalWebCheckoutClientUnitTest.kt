package com.paypal.android.paypalwebpayments

import android.content.Intent
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.Environment
import com.paypal.android.corepayments.PayPalSDKError
<<<<<<< HEAD
import com.paypal.android.corepayments.api.PatchCCOWithAppSwitchEligibility
import com.paypal.android.corepayments.common.DeviceInspector
import com.paypal.android.corepayments.model.APIResult
import com.paypal.android.corepayments.model.AppSwitchEligibility
import com.paypal.android.corepayments.model.TokenType
import com.paypal.android.paypalwebpayments.analytics.PayPalWebAnalytics
import io.mockk.coEvery
=======
import com.paypal.android.corepayments.UpdateClientConfigAPI
import com.paypal.android.paypalwebpayments.analytics.PayPalWebAnalytics
>>>>>>> develop
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertSame
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
<<<<<<< HEAD
=======
import kotlinx.coroutines.test.advanceUntilIdle
>>>>>>> develop
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@Suppress("LargeClass")
@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Suppress("LargeClass")
class PayPalWebCheckoutClientUnitTest {

    private val activity: FragmentActivity = mockk(relaxed = true)
    private val analytics = mockk<PayPalWebAnalytics>(relaxed = true)
<<<<<<< HEAD
    private lateinit var patchCCOWithAppSwitchEligibility: PatchCCOWithAppSwitchEligibility
    private lateinit var deviceInspector: DeviceInspector
    private val coreConfig = CoreConfig("fake-client-id", Environment.SANDBOX)
    private val urlScheme = "com.example.app"
    private val testDispatcher = StandardTestDispatcher()
    private val fakeAppSwitchUrl = "https://paypal.com/vault-app-switch"
=======
    private val updateClientConfigAPI = mockk<UpdateClientConfigAPI>(relaxed = true)
>>>>>>> develop

    private val intent = Intent()

    private lateinit var payPalWebLauncher: PayPalWebLauncher
    private lateinit var sut: PayPalWebCheckoutClient

    @Before
    fun beforeEach() {
        payPalWebLauncher = mockk(relaxed = true)
<<<<<<< HEAD
        patchCCOWithAppSwitchEligibility = mockk(relaxed = true)
        deviceInspector = mockk(relaxed = true)
        sut = PayPalWebCheckoutClient(
            analytics,
            payPalWebLauncher,
            patchCCOWithAppSwitchEligibility,
            deviceInspector,
            coreConfig,
            urlScheme,
            testDispatcher
=======
        sut = PayPalWebCheckoutClient(
            analytics = analytics,
            payPalWebLauncher = payPalWebLauncher,
            sessionStore = PayPalWebCheckoutSessionStore(),
            updateClientConfigAPI = updateClientConfigAPI,
            ioDispatcher = Dispatchers.Main,
            mainDispatcher = Dispatchers.Main
>>>>>>> develop
        )
    }

    @Test
<<<<<<< HEAD
    fun `start() fetches client token and launches PayPal web checkout`() = runTest {
=======
    fun `start() launches PayPal web checkout`() = runTest {
        val sut = PayPalWebCheckoutClient(
            analytics = analytics,
            payPalWebLauncher = payPalWebLauncher,
            sessionStore = PayPalWebCheckoutSessionStore(),
            updateClientConfigAPI = updateClientConfigAPI,
            ioDispatcher = Dispatchers.Main,
            mainDispatcher = Dispatchers.Main
        )
>>>>>>> develop
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

        val request = PayPalWebCheckoutRequest("fake-order-id")
        sut.start(activity, request)

        // Verify launchWithUrl is called with correct parameters
        verify(exactly = 1) {
            payPalWebLauncher.launchWithUrl(
                activity = activity,
                uri = any(),
                token = "fake-order-id",
                tokenType = TokenType.ORDER_ID,
                returnUrlScheme = urlScheme
            )
        }
    }

    @Test
    fun `start() notifies merchant of browser switch failure`() = runTest {
<<<<<<< HEAD
=======
        val sut = PayPalWebCheckoutClient(
            analytics = analytics,
            payPalWebLauncher = payPalWebLauncher,
            sessionStore = PayPalWebCheckoutSessionStore(),
            updateClientConfigAPI = updateClientConfigAPI,
            ioDispatcher = Dispatchers.Main,
            mainDispatcher = Dispatchers.Main
        )
>>>>>>> develop
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

        val request = PayPalWebCheckoutRequest("fake-order-id")
        val result = sut.start(activity, request)
        assertSame(launchResult, result)
    }

<<<<<<< HEAD
    // Note: Authentication failure tests removed since authentication
    // is now handled internally by PatchCCOWithAppSwitchEligibility

    @Test
    fun `vault() launches PayPal web checkout`() = runTest {
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

=======
    @Test
    fun `start() with callback calls updateCCO before launching web checkout`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val sut = PayPalWebCheckoutClient(
            analytics = analytics,
            payPalWebLauncher = payPalWebLauncher,
            sessionStore = PayPalWebCheckoutSessionStore(),
            updateClientConfigAPI = updateClientConfigAPI,
            ioDispatcher = testDispatcher,
            mainDispatcher = testDispatcher
        )

        val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
        every { payPalWebLauncher.launchPayPalWebCheckout(any(), any()) } returns launchResult

        val request = PayPalWebCheckoutRequest("fake-order-id")
        var callbackResult: PayPalPresentAuthChallengeResult? = null
        val callback = PayPalWebStartCallback { result ->
            callbackResult = result
        }

        // Call start() with callback - this should call updateCCO before launching
        sut.start(activity, request, callback)

        // Advance the dispatcher to allow the background coroutine to complete
        advanceUntilIdle()

        // Verify that callback was called with the expected result
        assertSame(launchResult, callbackResult)

        // Verify that both updateCCO and the web launcher were called
        coVerify(exactly = 1) {
            updateClientConfigAPI.updateClientConfig("fake-order-id", "paypal")
        }
        verify(exactly = 1) { payPalWebLauncher.launchPayPalWebCheckout(activity, request) }
    }

    @Test
    fun `start() with callback executes asynchronously`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val sut = PayPalWebCheckoutClient(
            analytics = analytics,
            payPalWebLauncher = payPalWebLauncher,
            sessionStore = PayPalWebCheckoutSessionStore(),
            updateClientConfigAPI = updateClientConfigAPI,
            ioDispatcher = testDispatcher,
            mainDispatcher = testDispatcher
        )

        val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
        every { payPalWebLauncher.launchPayPalWebCheckout(any(), any()) } returns launchResult

        val request = PayPalWebCheckoutRequest("fake-order-id")
        var callbackResult: PayPalPresentAuthChallengeResult? = null
        val callback = PayPalWebStartCallback { result ->
            callbackResult = result
        }

        // Call start() with callback - this should return immediately
        sut.start(activity, request, callback)

        // Initially, callback should not have been called yet
        assertEquals("Callback should not have been called yet", null, callbackResult)

        // Advance the dispatcher to allow the background coroutine to complete
        advanceUntilIdle()

        // Verify that callback was called with the expected result
        assertSame("Callback should have been called with the result", launchResult, callbackResult)

        // Verify that both updateCCO and the web launcher were called
        coVerify(exactly = 1) {
            updateClientConfigAPI.updateClientConfig("fake-order-id", "paypal")
        }
        verify(exactly = 1) { payPalWebLauncher.launchPayPalWebCheckout(activity, request) }
    }

    @Test
    fun `vault() launches PayPal web checkout`() {
        val sut = PayPalWebCheckoutClient(
            analytics = analytics,
            payPalWebLauncher = payPalWebLauncher,
            sessionStore = PayPalWebCheckoutSessionStore(),
            updateClientConfigAPI = updateClientConfigAPI,
            ioDispatcher = Dispatchers.Main,
            mainDispatcher = Dispatchers.Main
        )
        val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
        every { payPalWebLauncher.launchPayPalWebVault(any(), any()) } returns launchResult

>>>>>>> develop
        val request = PayPalWebVaultRequest("fake-setup-token-id")
        sut.vault(activity, request)
        verify(exactly = 1) {
            payPalWebLauncher.launchWithUrl(
                activity = activity,
                uri = any(),
                token = "fake-setup-token-id",
                tokenType = TokenType.VAULT_ID,
                returnUrlScheme = urlScheme
            )
        }
    }

    @Test
<<<<<<< HEAD
    fun `vault() notifies merchant of browser switch failure`() = runTest {
=======
    fun `vault() notifies merchant of browser switch failure`() {
        val sut = PayPalWebCheckoutClient(
            analytics = analytics,
            payPalWebLauncher = payPalWebLauncher,
            sessionStore = PayPalWebCheckoutSessionStore(),
            updateClientConfigAPI = updateClientConfigAPI,
            ioDispatcher = Dispatchers.Main,
            mainDispatcher = Dispatchers.Main
        )
>>>>>>> develop
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

        val request = PayPalWebVaultRequest("fake-setup-token-id")
        val result = sut.vault(activity, request) as PayPalPresentAuthChallengeResult.Failure

        assertSame(sdkError, result.error)
    }

    @Test
    fun `finishStart() with merchant provided auth state forwards success result from auth launcher`() {
        val sut = PayPalWebCheckoutClient(
            analytics = analytics,
            payPalWebLauncher = payPalWebLauncher,
            sessionStore = PayPalWebCheckoutSessionStore(),
            updateClientConfigAPI = updateClientConfigAPI,
            ioDispatcher = Dispatchers.Main,
            mainDispatcher = Dispatchers.Main
        )
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
        val sut = PayPalWebCheckoutClient(
            analytics = analytics,
            payPalWebLauncher = payPalWebLauncher,
            sessionStore = PayPalWebCheckoutSessionStore(),
            updateClientConfigAPI = updateClientConfigAPI,
            ioDispatcher = Dispatchers.Main,
            mainDispatcher = Dispatchers.Main
        )
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
        val sut = PayPalWebCheckoutClient(
            analytics = analytics,
            payPalWebLauncher = payPalWebLauncher,
            sessionStore = PayPalWebCheckoutSessionStore(),
            updateClientConfigAPI = updateClientConfigAPI,
            ioDispatcher = Dispatchers.Main,
            mainDispatcher = Dispatchers.Main
        )
        val canceledResult = PayPalWebCheckoutFinishStartResult.Canceled("fake-order-id")
        every {
            payPalWebLauncher.completeCheckoutAuthRequest(intent, "auth state")
        } returns canceledResult

        val result = sut.finishStart(intent, "auth state")
        assertSame(canceledResult, result)
    }

    @Test
    fun `finishStart() with session auth state returns null when start has not been called`() {
        val sut = PayPalWebCheckoutClient(
            analytics = analytics,
            payPalWebLauncher = payPalWebLauncher,
            sessionStore = PayPalWebCheckoutSessionStore(),
            updateClientConfigAPI = updateClientConfigAPI,
            ioDispatcher = Dispatchers.Main,
            mainDispatcher = Dispatchers.Main
        )
        assertNull(sut.finishStart(intent))
    }

    @Test
    fun `finishStart() with session auth state forwards success result from auth launcher`() =
        runTest {
        val sut = PayPalWebCheckoutClient(
            analytics = analytics,
            payPalWebLauncher = payPalWebLauncher,
            sessionStore = PayPalWebCheckoutSessionStore(),
            updateClientConfigAPI = updateClientConfigAPI,
            ioDispatcher = Dispatchers.Main,
            mainDispatcher = Dispatchers.Main
        )
        val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
        every { payPalWebLauncher.launchPayPalWebCheckout(any(), any()) } returns launchResult

        val successResult =
            PayPalWebCheckoutFinishStartResult.Success("fake-order-id", "fake-payer-id")
        every {
            payPalWebLauncher.completeCheckoutAuthRequest(intent, "auth state")
        } returns successResult

        val request = PayPalWebCheckoutRequest("fake-order-id")
        sut.start(activity, request)
        val result = sut.finishStart(intent)
        assertSame(successResult, result)
    }

    @Test
    fun `finishStart() with restored session auth state forwards success result from auth launcher`() =
        runTest {
        val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
        every { payPalWebLauncher.launchPayPalWebCheckout(any(), any()) } returns launchResult

        val successResult =
            PayPalWebCheckoutFinishStartResult.Success("fake-order-id", "fake-payer-id")
        every {
            payPalWebLauncher.completeCheckoutAuthRequest(intent, "auth state")
        } returns successResult

        val previousClient = PayPalWebCheckoutClient(
            analytics = analytics,
            payPalWebLauncher = payPalWebLauncher,
            sessionStore = PayPalWebCheckoutSessionStore(),
            updateClientConfigAPI = updateClientConfigAPI,
            ioDispatcher = Dispatchers.Main,
            mainDispatcher = Dispatchers.Main
        )
        val request = PayPalWebCheckoutRequest("fake-order-id")
        previousClient.start(activity, request)

        val sut = PayPalWebCheckoutClient(
            analytics = analytics,
            payPalWebLauncher = payPalWebLauncher,
            sessionStore = PayPalWebCheckoutSessionStore(),
            updateClientConfigAPI = updateClientConfigAPI,
            ioDispatcher = Dispatchers.Main,
            mainDispatcher = Dispatchers.Main
        )
        sut.restore(previousClient.instanceState)
        val result = sut.finishStart(intent)
        assertSame(successResult, result)
    }

    @Test
    fun `finishStart() with session auth state forwards error result from auth launcher`() =
        runTest {
        val sut = PayPalWebCheckoutClient(
            analytics = analytics,
            payPalWebLauncher = payPalWebLauncher,
            sessionStore = PayPalWebCheckoutSessionStore(),
            updateClientConfigAPI = updateClientConfigAPI,
            ioDispatcher = Dispatchers.Main,
            mainDispatcher = Dispatchers.Main
        )
        val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
        every { payPalWebLauncher.launchPayPalWebCheckout(any(), any()) } returns launchResult

        val error = PayPalSDKError(123, "fake-error-description")
        val failureResult = PayPalWebCheckoutFinishStartResult.Failure(error, null)
        every {
            payPalWebLauncher.completeCheckoutAuthRequest(intent, "auth state")
        } returns failureResult

        val request = PayPalWebCheckoutRequest("fake-order-id")
        sut.start(activity, request)
        val result = sut.finishStart(intent)
        assertSame(failureResult, result)
    }

    @Test
    fun `finishStart() with restored session auth state forwards error result from auth launcher`() =
        runTest {
        val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
        every { payPalWebLauncher.launchPayPalWebCheckout(any(), any()) } returns launchResult

        val error = PayPalSDKError(123, "fake-error-description")
        val failureResult = PayPalWebCheckoutFinishStartResult.Failure(error, null)
        every {
            payPalWebLauncher.completeCheckoutAuthRequest(intent, "auth state")
        } returns failureResult

        val previousClient = PayPalWebCheckoutClient(
            analytics = analytics,
            payPalWebLauncher = payPalWebLauncher,
            sessionStore = PayPalWebCheckoutSessionStore(),
            updateClientConfigAPI = updateClientConfigAPI,
            ioDispatcher = Dispatchers.Main,
            mainDispatcher = Dispatchers.Main
        )
        val request = PayPalWebCheckoutRequest("fake-order-id")
        previousClient.start(activity, request)

        val sut = PayPalWebCheckoutClient(
            analytics = analytics,
            payPalWebLauncher = payPalWebLauncher,
            sessionStore = PayPalWebCheckoutSessionStore(),
            updateClientConfigAPI = updateClientConfigAPI,
            ioDispatcher = Dispatchers.Main,
            mainDispatcher = Dispatchers.Main
        )
        sut.restore(previousClient.instanceState)
        val result = sut.finishStart(intent)
        assertSame(failureResult, result)
    }

    @Test
    fun `finishStart() with session auth state forwards cancellation result from auth launcher`() =
        runTest {
        val sut = PayPalWebCheckoutClient(
            analytics = analytics,
            payPalWebLauncher = payPalWebLauncher,
            sessionStore = PayPalWebCheckoutSessionStore(),
            updateClientConfigAPI = updateClientConfigAPI,
            ioDispatcher = Dispatchers.Main,
            mainDispatcher = Dispatchers.Main
        )
        val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
        every { payPalWebLauncher.launchPayPalWebCheckout(any(), any()) } returns launchResult

        val canceledResult = PayPalWebCheckoutFinishStartResult.Canceled("fake-order-id")
        every {
            payPalWebLauncher.completeCheckoutAuthRequest(intent, "auth state")
        } returns canceledResult

        val request = PayPalWebCheckoutRequest("fake-order-id")
        sut.start(activity, request)
        val result = sut.finishStart(intent)
        assertSame(canceledResult, result)
    }

    @Test
    fun `finishStart() with restored session auth state forwards cancellation result from auth launcher`() =
        runTest {
        val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
        every { payPalWebLauncher.launchPayPalWebCheckout(any(), any()) } returns launchResult

        val canceledResult = PayPalWebCheckoutFinishStartResult.Canceled("fake-order-id")
        every {
            payPalWebLauncher.completeCheckoutAuthRequest(intent, "auth state")
        } returns canceledResult

        val previousClient = PayPalWebCheckoutClient(
            analytics = analytics,
            payPalWebLauncher = payPalWebLauncher,
            sessionStore = PayPalWebCheckoutSessionStore(),
            updateClientConfigAPI = updateClientConfigAPI,
            ioDispatcher = Dispatchers.Main,
            mainDispatcher = Dispatchers.Main
        )
        val request = PayPalWebCheckoutRequest("fake-order-id")
        previousClient.start(activity, request)

        val sut = PayPalWebCheckoutClient(
            analytics = analytics,
            payPalWebLauncher = payPalWebLauncher,
            sessionStore = PayPalWebCheckoutSessionStore(),
            updateClientConfigAPI = updateClientConfigAPI,
            ioDispatcher = Dispatchers.Main,
            mainDispatcher = Dispatchers.Main
        )
        sut.restore(previousClient.instanceState)
        val result = sut.finishStart(intent)
        assertSame(canceledResult, result)
    }

    @Test
    fun `finishStart() with session auth state clears session to prevent delivering success event twice`() =
        runTest {
        val sut = PayPalWebCheckoutClient(
            analytics = analytics,
            payPalWebLauncher = payPalWebLauncher,
            sessionStore = PayPalWebCheckoutSessionStore(),
            updateClientConfigAPI = updateClientConfigAPI,
            ioDispatcher = Dispatchers.Main,
            mainDispatcher = Dispatchers.Main
        )
        val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
        every { payPalWebLauncher.launchPayPalWebCheckout(any(), any()) } returns launchResult

        val successResult =
            PayPalWebCheckoutFinishStartResult.Success("fake-order-id", "fake-payer-id")
        every {
            payPalWebLauncher.completeCheckoutAuthRequest(intent, "auth state")
        } returns successResult

        sut.start(activity, PayPalWebCheckoutRequest("fake-order-id"))
        sut.finishStart(intent)
        assertNull(sut.finishStart(intent))
    }

    @Test
    fun `finishStart() with session auth state clears session to prevent delivering error event twice`() =
        runTest {
        val sut = PayPalWebCheckoutClient(
            analytics = analytics,
            payPalWebLauncher = payPalWebLauncher,
            sessionStore = PayPalWebCheckoutSessionStore(),
            updateClientConfigAPI = updateClientConfigAPI,
            ioDispatcher = Dispatchers.Main,
            mainDispatcher = Dispatchers.Main
        )
        val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
        every { payPalWebLauncher.launchPayPalWebCheckout(any(), any()) } returns launchResult

        val error = PayPalSDKError(123, "fake-error-description")
        val failureResult = PayPalWebCheckoutFinishStartResult.Failure(error, null)
        every {
            payPalWebLauncher.completeCheckoutAuthRequest(intent, "auth state")
        } returns failureResult

        sut.start(activity, PayPalWebCheckoutRequest("fake-order-id"))
        sut.finishStart(intent)
        assertNull(sut.finishStart(intent))
    }

    @Test
    fun `finishStart() with session auth state clears session to prevent delivering cancellation event twice`() =
        runTest {
        val sut = PayPalWebCheckoutClient(
            analytics = analytics,
            payPalWebLauncher = payPalWebLauncher,
            sessionStore = PayPalWebCheckoutSessionStore(),
            updateClientConfigAPI = updateClientConfigAPI,
            ioDispatcher = Dispatchers.Main,
            mainDispatcher = Dispatchers.Main
        )
        val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
        every { payPalWebLauncher.launchPayPalWebCheckout(any(), any()) } returns launchResult

        val canceledResult = PayPalWebCheckoutFinishStartResult.Canceled("fake-order-id")
        every {
            payPalWebLauncher.completeCheckoutAuthRequest(intent, "auth state")
        } returns canceledResult

        sut.start(activity, PayPalWebCheckoutRequest("fake-order-id"))
        sut.finishStart(intent)
        assertNull(sut.finishStart(intent))
    }

    @Test
    fun `finishVault() with merchant provided auth forwards vault success from PayPal web launcher`() {
        val sut = PayPalWebCheckoutClient(
            analytics = analytics,
            payPalWebLauncher = payPalWebLauncher,
            sessionStore = PayPalWebCheckoutSessionStore(),
            updateClientConfigAPI = updateClientConfigAPI,
            ioDispatcher = Dispatchers.Main,
            mainDispatcher = Dispatchers.Main
        )
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
        val sut = PayPalWebCheckoutClient(
            analytics = analytics,
            payPalWebLauncher = payPalWebLauncher,
            sessionStore = PayPalWebCheckoutSessionStore(),
            updateClientConfigAPI = updateClientConfigAPI,
            ioDispatcher = Dispatchers.Main,
            mainDispatcher = Dispatchers.Main
        )
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
        val sut = PayPalWebCheckoutClient(
            analytics = analytics,
            payPalWebLauncher = payPalWebLauncher,
            sessionStore = PayPalWebCheckoutSessionStore(),
            updateClientConfigAPI = updateClientConfigAPI,
            ioDispatcher = Dispatchers.Main,
            mainDispatcher = Dispatchers.Main
        )
        every {
            payPalWebLauncher.completeVaultAuthRequest(intent, "auth state")
        } returns PayPalWebCheckoutFinishVaultResult.Canceled

        val result = sut.finishVault(intent, "auth state")
        assertTrue(result is PayPalWebCheckoutFinishVaultResult.Canceled)
    }

    @Test
    fun `finishVault with merchant provided auth forwards no result`() {
        val sut = PayPalWebCheckoutClient(
            analytics = analytics,
            payPalWebLauncher = payPalWebLauncher,
            sessionStore = PayPalWebCheckoutSessionStore(),
            updateClientConfigAPI = updateClientConfigAPI,
            ioDispatcher = Dispatchers.Main,
            mainDispatcher = Dispatchers.Main
        )
        every {
            payPalWebLauncher.completeVaultAuthRequest(intent, "auth state")
        } returns PayPalWebCheckoutFinishVaultResult.NoResult

        val result = sut.finishVault(intent, "auth state")
        assertTrue(result is PayPalWebCheckoutFinishVaultResult.NoResult)
    }

    @Test
<<<<<<< HEAD
    fun `start() uses app switch when enabled and app switch URL is available`() = runTest {
        // Given
        every { deviceInspector.isPayPalInstalled } returns true
        val request = PayPalWebCheckoutRequest("fake-order-id", appSwitchWhenEligible = true)
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
                returnUrlScheme = urlScheme
            )
        } returns launchResult

        // When
        val result = sut.start(activity, request)

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
                returnUrlScheme = urlScheme
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
            val result = sut.start(activity, request)

            // Then
            verify {
                payPalWebLauncher.launchWithUrl(
                    activity = activity,
                    uri = any(),
                    token = "fake-order-id",
                    tokenType = TokenType.ORDER_ID,
                    returnUrlScheme = urlScheme
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
            val result = sut.start(activity, request)

            // Then
            verify {
                payPalWebLauncher.launchWithUrl(
                    activity = activity,
                    uri = any(),
                    token = "fake-order-id",
                    tokenType = TokenType.ORDER_ID,
                    returnUrlScheme = urlScheme
                )
            }
            assertSame(launchResult, result)
        }

    @Test
    fun `start() falls back to web checkout when app switch request fails`() = runTest {
        // Given
        every { deviceInspector.isPayPalInstalled } returns true
        val request = PayPalWebCheckoutRequest("fake-order-id", appSwitchWhenEligible = true)
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
        val result = sut.start(activity, request)

        // Then
        coVerify { patchCCOWithAppSwitchEligibility(any(), any(), any(), any(), any()) }
        verify {
            payPalWebLauncher.launchWithUrl(
                activity = activity,
                uri = any(),
                token = "fake-order-id",
                tokenType = TokenType.ORDER_ID,
                returnUrlScheme = urlScheme
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
        val result = sut.start(activity, request)

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
                returnUrlScheme = urlScheme
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
        sut.start(activity, request)

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
        val request = PayPalWebVaultRequest("fake-setup-token-id", appSwitchWhenEligible = true)
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
                returnUrlScheme = urlScheme
            )
        } returns launchResult

        // When
        val result = sut.vault(activity, request)

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
                returnUrlScheme = urlScheme
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
        val result = sut.vault(activity, request)

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
                returnUrlScheme = urlScheme
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
            val result = sut.vault(activity, request)

            // Then
            verify {
                payPalWebLauncher.launchWithUrl(
                    activity = activity,
                    uri = any(),
                    token = "fake-setup-token-id",
                    tokenType = TokenType.VAULT_ID,
                    returnUrlScheme = urlScheme
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
        val result = sut.vault(activity, request)

        // Then
        verify {
            payPalWebLauncher.launchWithUrl(
                activity = activity,
                uri = any(),
                token = "fake-setup-token-id",
                tokenType = TokenType.VAULT_ID,
                returnUrlScheme = urlScheme
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
        val result = sut.vault(activity, request)

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
                returnUrlScheme = urlScheme
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
        sut.vault(activity, request)

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
            val result = sut.start(activity, request)

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
                    returnUrlScheme = urlScheme
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
            val result = sut.vault(activity, request)

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
                    returnUrlScheme = urlScheme
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
            val result = sut.start(activity, request)

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
                    returnUrlScheme = urlScheme
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
            val result = sut.vault(activity, request)

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
                    returnUrlScheme = urlScheme
                )
            }
            assertSame(launchResult, result)
        }

    private fun createAppSwitchEligibilityResponse(redirectURL: String?): AppSwitchEligibility {
        return AppSwitchEligibility(
            appSwitchEligible = true,
            launchUrl = redirectURL,
            ineligibleReason = null
        )
=======
    fun `finishVault() with session auth state returns null when start has not been called`() {
        val sut = PayPalWebCheckoutClient(
            analytics = analytics,
            payPalWebLauncher = payPalWebLauncher,
            sessionStore = PayPalWebCheckoutSessionStore(),
            updateClientConfigAPI = updateClientConfigAPI,
            ioDispatcher = Dispatchers.Main,
            mainDispatcher = Dispatchers.Main
        )
        assertNull(sut.finishVault(intent))
    }

    @Test
    fun `finishVault() with session auth state forwards success result from auth launcher`() {
        val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
        every { payPalWebLauncher.launchPayPalWebVault(any(), any()) } returns launchResult

        val successResult =
            PayPalWebCheckoutFinishVaultResult.Success("fake-approval-session-id")
        every {
            payPalWebLauncher.completeVaultAuthRequest(intent, "auth state")
        } returns successResult

        val previousClient = PayPalWebCheckoutClient(
            analytics = analytics,
            payPalWebLauncher = payPalWebLauncher,
            sessionStore = PayPalWebCheckoutSessionStore(),
            updateClientConfigAPI = updateClientConfigAPI,
            ioDispatcher = Dispatchers.Main,
            mainDispatcher = Dispatchers.Main
        )
        previousClient.vault(activity, PayPalWebVaultRequest("fake-setup-token-id"))

        val sut = PayPalWebCheckoutClient(
            analytics = analytics,
            payPalWebLauncher = payPalWebLauncher,
            sessionStore = PayPalWebCheckoutSessionStore(),
            updateClientConfigAPI = updateClientConfigAPI,
            ioDispatcher = Dispatchers.Main,
            mainDispatcher = Dispatchers.Main
        )
        sut.restore(previousClient.instanceState)
        val result = sut.finishVault(intent) as PayPalWebCheckoutFinishVaultResult.Success
        assertSame("fake-approval-session-id", result.approvalSessionId)
    }

    @Test
    fun `finishVault() with restored session auth state forwards success result from auth launcher`() {
        val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
        every { payPalWebLauncher.launchPayPalWebVault(any(), any()) } returns launchResult

        val successResult =
            PayPalWebCheckoutFinishVaultResult.Success("fake-approval-session-id")
        every {
            payPalWebLauncher.completeVaultAuthRequest(intent, "auth state")
        } returns successResult

        val previousClient = PayPalWebCheckoutClient(
            analytics = analytics,
            payPalWebLauncher = payPalWebLauncher,
            sessionStore = PayPalWebCheckoutSessionStore(),
            updateClientConfigAPI = updateClientConfigAPI,
            ioDispatcher = Dispatchers.Main,
            mainDispatcher = Dispatchers.Main
        )
        previousClient.vault(activity, PayPalWebVaultRequest("fake-setup-token-id"))

        val sut = PayPalWebCheckoutClient(
            analytics = analytics,
            payPalWebLauncher = payPalWebLauncher,
            sessionStore = PayPalWebCheckoutSessionStore(),
            updateClientConfigAPI = updateClientConfigAPI,
            ioDispatcher = Dispatchers.Main,
            mainDispatcher = Dispatchers.Main
        )
        sut.restore(previousClient.instanceState)
        val result = sut.finishVault(intent) as PayPalWebCheckoutFinishVaultResult.Success
        assertSame("fake-approval-session-id", result.approvalSessionId)
    }

    @Test
    fun `finishVault() with session auth state forwards error result from auth launcher`() {
        val sut = PayPalWebCheckoutClient(
            analytics = analytics,
            payPalWebLauncher = payPalWebLauncher,
            sessionStore = PayPalWebCheckoutSessionStore(),
            updateClientConfigAPI = updateClientConfigAPI,
            ioDispatcher = Dispatchers.Main,
            mainDispatcher = Dispatchers.Main
        )
        val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
        every { payPalWebLauncher.launchPayPalWebVault(any(), any()) } returns launchResult

        val error = PayPalSDKError(123, "fake-error-description")
        every {
            payPalWebLauncher.completeVaultAuthRequest(intent, "auth state")
        } returns PayPalWebCheckoutFinishVaultResult.Failure(error)

        sut.vault(activity, PayPalWebVaultRequest("fake-setup-token-id"))
        val result = sut.finishVault(intent) as PayPalWebCheckoutFinishVaultResult.Failure
        assertSame(error, result.error)
    }

    @Test
    fun `finishVault() with restored session auth state forwards error result from auth launcher`() {
        val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
        every { payPalWebLauncher.launchPayPalWebVault(any(), any()) } returns launchResult

        val error = PayPalSDKError(123, "fake-error-description")
        every {
            payPalWebLauncher.completeVaultAuthRequest(intent, "auth state")
        } returns PayPalWebCheckoutFinishVaultResult.Failure(error)

        val previousClient = PayPalWebCheckoutClient(
            analytics = analytics,
            payPalWebLauncher = payPalWebLauncher,
            sessionStore = PayPalWebCheckoutSessionStore(),
            updateClientConfigAPI = updateClientConfigAPI,
            ioDispatcher = Dispatchers.Main,
            mainDispatcher = Dispatchers.Main
        )
        previousClient.vault(activity, PayPalWebVaultRequest("fake-setup-token-id"))

        val sut = PayPalWebCheckoutClient(
            analytics = analytics,
            payPalWebLauncher = payPalWebLauncher,
            sessionStore = PayPalWebCheckoutSessionStore(),
            updateClientConfigAPI = updateClientConfigAPI,
            ioDispatcher = Dispatchers.Main,
            mainDispatcher = Dispatchers.Main
        )
        sut.restore(previousClient.instanceState)
        val result = sut.finishVault(intent) as PayPalWebCheckoutFinishVaultResult.Failure
        assertSame(error, result.error)
    }

    @Test
    fun `finishVault() with session auth state forwards cancellation result from auth launcher`() {
        val sut = PayPalWebCheckoutClient(
            analytics = analytics,
            payPalWebLauncher = payPalWebLauncher,
            sessionStore = PayPalWebCheckoutSessionStore(),
            updateClientConfigAPI = updateClientConfigAPI,
            ioDispatcher = Dispatchers.Main,
            mainDispatcher = Dispatchers.Main
        )
        val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
        every { payPalWebLauncher.launchPayPalWebVault(any(), any()) } returns launchResult

        every {
            payPalWebLauncher.completeVaultAuthRequest(intent, "auth state")
        } returns PayPalWebCheckoutFinishVaultResult.Canceled

        sut.vault(activity, PayPalWebVaultRequest("fake-setup-token-id"))
        val result = sut.finishVault(intent)
        assertSame(PayPalWebCheckoutFinishVaultResult.Canceled, result)
    }

    @Test
    fun `finishVault() with restored session auth state forwards cancellation result from auth launcher`() {
        val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
        every { payPalWebLauncher.launchPayPalWebVault(any(), any()) } returns launchResult

        every {
            payPalWebLauncher.completeVaultAuthRequest(intent, "auth state")
        } returns PayPalWebCheckoutFinishVaultResult.Canceled

        val previousClient = PayPalWebCheckoutClient(
            analytics = analytics,
            payPalWebLauncher = payPalWebLauncher,
            sessionStore = PayPalWebCheckoutSessionStore(),
            updateClientConfigAPI = updateClientConfigAPI,
            ioDispatcher = Dispatchers.Main,
            mainDispatcher = Dispatchers.Main
        )
        previousClient.vault(activity, PayPalWebVaultRequest("fake-setup-token-id"))

        val sut = PayPalWebCheckoutClient(
            analytics = analytics,
            payPalWebLauncher = payPalWebLauncher,
            sessionStore = PayPalWebCheckoutSessionStore(),
            updateClientConfigAPI = updateClientConfigAPI,
            ioDispatcher = Dispatchers.Main,
            mainDispatcher = Dispatchers.Main
        )
        sut.restore(previousClient.instanceState)
        val result = sut.finishVault(intent)
        assertSame(PayPalWebCheckoutFinishVaultResult.Canceled, result)
    }

    @Test
    fun `finishVault() with session auth state clears session to prevent delivering success event twice`() {
        val sut = PayPalWebCheckoutClient(
            analytics = analytics,
            payPalWebLauncher = payPalWebLauncher,
            sessionStore = PayPalWebCheckoutSessionStore(),
            updateClientConfigAPI = updateClientConfigAPI,
            ioDispatcher = Dispatchers.Main,
            mainDispatcher = Dispatchers.Main
        )
        val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
        every { payPalWebLauncher.launchPayPalWebVault(any(), any()) } returns launchResult

        val successResult =
            PayPalWebCheckoutFinishVaultResult.Success("fake-approval-session-id")
        every {
            payPalWebLauncher.completeVaultAuthRequest(intent, "auth state")
        } returns successResult

        sut.vault(activity, PayPalWebVaultRequest("fake-setup-token-id"))
        sut.finishVault(intent)
        assertNull(sut.finishVault(intent))
    }

    @Test
    fun `finishVault() with session auth state clears session to prevent delivering error event twice`() {
        val sut = PayPalWebCheckoutClient(
            analytics = analytics,
            payPalWebLauncher = payPalWebLauncher,
            sessionStore = PayPalWebCheckoutSessionStore(),
            updateClientConfigAPI = updateClientConfigAPI,
            ioDispatcher = Dispatchers.Main,
            mainDispatcher = Dispatchers.Main
        )
        val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
        every { payPalWebLauncher.launchPayPalWebVault(any(), any()) } returns launchResult

        val error = PayPalSDKError(123, "fake-error-description")
        every {
            payPalWebLauncher.completeVaultAuthRequest(intent, "auth state")
        } returns PayPalWebCheckoutFinishVaultResult.Failure(error)

        sut.vault(activity, PayPalWebVaultRequest("fake-setup-token-id"))
        sut.finishVault(intent)
        assertNull(sut.finishVault(intent))
    }

    @Test
    fun `finishVault() with session auth state clears session to prevent delivering cancellation event twice`() {
        val sut = PayPalWebCheckoutClient(
            analytics = analytics,
            payPalWebLauncher = payPalWebLauncher,
            sessionStore = PayPalWebCheckoutSessionStore(),
            updateClientConfigAPI = updateClientConfigAPI,
            ioDispatcher = Dispatchers.Main,
            mainDispatcher = Dispatchers.Main
        )
        val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
        every { payPalWebLauncher.launchPayPalWebVault(any(), any()) } returns launchResult

        every {
            payPalWebLauncher.completeVaultAuthRequest(intent, "auth state")
        } returns PayPalWebCheckoutFinishVaultResult.Canceled

        sut.vault(activity, PayPalWebVaultRequest("fake-setup-token-id"))
        sut.finishVault(intent)
        assertNull(sut.finishVault(intent))
>>>>>>> develop
    }
}
