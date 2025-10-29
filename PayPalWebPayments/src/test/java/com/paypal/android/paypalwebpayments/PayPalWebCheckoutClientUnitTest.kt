package com.paypal.android.paypalwebpayments

import android.content.Intent
import androidx.fragment.app.FragmentActivity
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.corepayments.UpdateClientConfigAPI
import com.paypal.android.paypalwebpayments.analytics.PayPalWebAnalytics
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
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Suppress("LargeClass")
class PayPalWebCheckoutClientUnitTest {

    private val activity: FragmentActivity = mockk(relaxed = true)
    private val analytics = mockk<PayPalWebAnalytics>(relaxed = true)
    private val updateClientConfigAPI = mockk<UpdateClientConfigAPI>(relaxed = true)

    private val intent = Intent()

    private lateinit var payPalWebLauncher: PayPalWebLauncher
    private lateinit var sut: PayPalWebCheckoutClient

    @Before
    fun beforeEach() {
        payPalWebLauncher = mockk(relaxed = true)
        sut = PayPalWebCheckoutClient(
            analytics = analytics,
            payPalWebLauncher = payPalWebLauncher,
            sessionStore = PayPalWebCheckoutSessionStore(),
            updateClientConfigAPI = updateClientConfigAPI,
            mainDispatcher = Dispatchers.Main
        )
    }

    @Test
    fun `start() launches PayPal web checkout`() = runTest {
        val sut = PayPalWebCheckoutClient(
            analytics = analytics,
            payPalWebLauncher = payPalWebLauncher,
            sessionStore = PayPalWebCheckoutSessionStore(),
            updateClientConfigAPI = updateClientConfigAPI,
            mainDispatcher = Dispatchers.Main
        )
        val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
        every { payPalWebLauncher.launchPayPalWebCheckout(any(), any()) } returns launchResult

        val request = PayPalWebCheckoutRequest("fake-order-id")
        sut.start(activity, request)
        verify(exactly = 1) { payPalWebLauncher.launchPayPalWebCheckout(activity, request) }
    }

    @Test
    fun `start() notifies merchant of browser switch failure`() = runTest {
        val sut = PayPalWebCheckoutClient(
            analytics = analytics,
            payPalWebLauncher = payPalWebLauncher,
            sessionStore = PayPalWebCheckoutSessionStore(),
            updateClientConfigAPI = updateClientConfigAPI,
            mainDispatcher = Dispatchers.Main
        )
        val sdkError = PayPalSDKError(123, "fake error description")
        val launchResult = PayPalPresentAuthChallengeResult.Failure(sdkError)
        every { payPalWebLauncher.launchPayPalWebCheckout(any(), any()) } returns launchResult

        val request = PayPalWebCheckoutRequest("fake-order-id")
        val result = sut.start(activity, request)
        assertSame(launchResult, result)
    }

    @Test
    fun `start() with callback calls updateCCO before launching web checkout`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val sut = PayPalWebCheckoutClient(
            analytics = analytics,
            payPalWebLauncher = payPalWebLauncher,
            sessionStore = PayPalWebCheckoutSessionStore(),
            updateClientConfigAPI = updateClientConfigAPI,
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
            mainDispatcher = Dispatchers.Main
        )
        val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
        every { payPalWebLauncher.launchPayPalWebVault(any(), any()) } returns launchResult

        val request = PayPalWebVaultRequest("fake-setup-token-id")
        sut.vault(activity, request)
        verify(exactly = 1) { payPalWebLauncher.launchPayPalWebVault(activity, request) }
    }

    @Test
    fun `vault() notifies merchant of browser switch failure`() {
        val sut = PayPalWebCheckoutClient(
            analytics = analytics,
            payPalWebLauncher = payPalWebLauncher,
            sessionStore = PayPalWebCheckoutSessionStore(),
            updateClientConfigAPI = updateClientConfigAPI,
            mainDispatcher = Dispatchers.Main
        )
        val sdkError = PayPalSDKError(123, "fake error description")
        val launchResult = PayPalPresentAuthChallengeResult.Failure(sdkError)
        every { payPalWebLauncher.launchPayPalWebVault(any(), any()) } returns launchResult

        val request =
            PayPalWebVaultRequest("fake-setup-token-id")
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
            mainDispatcher = Dispatchers.Main
        )
        val request = PayPalWebCheckoutRequest("fake-order-id")
        previousClient.start(activity, request)

        val sut = PayPalWebCheckoutClient(
            analytics = analytics,
            payPalWebLauncher = payPalWebLauncher,
            sessionStore = PayPalWebCheckoutSessionStore(),
            updateClientConfigAPI = updateClientConfigAPI,
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
            mainDispatcher = Dispatchers.Main
        )
        val request = PayPalWebCheckoutRequest("fake-order-id")
        previousClient.start(activity, request)

        val sut = PayPalWebCheckoutClient(
            analytics = analytics,
            payPalWebLauncher = payPalWebLauncher,
            sessionStore = PayPalWebCheckoutSessionStore(),
            updateClientConfigAPI = updateClientConfigAPI,
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
            mainDispatcher = Dispatchers.Main
        )
        val request = PayPalWebCheckoutRequest("fake-order-id")
        previousClient.start(activity, request)

        val sut = PayPalWebCheckoutClient(
            analytics = analytics,
            payPalWebLauncher = payPalWebLauncher,
            sessionStore = PayPalWebCheckoutSessionStore(),
            updateClientConfigAPI = updateClientConfigAPI,
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
            mainDispatcher = Dispatchers.Main
        )
        every {
            payPalWebLauncher.completeVaultAuthRequest(intent, "auth state")
        } returns PayPalWebCheckoutFinishVaultResult.NoResult

        val result = sut.finishVault(intent, "auth state")
        assertTrue(result is PayPalWebCheckoutFinishVaultResult.NoResult)
    }

    @Test
    fun `finishVault() with session auth state returns null when start has not been called`() {
        val sut = PayPalWebCheckoutClient(
            analytics = analytics,
            payPalWebLauncher = payPalWebLauncher,
            sessionStore = PayPalWebCheckoutSessionStore(),
            updateClientConfigAPI = updateClientConfigAPI,
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
            mainDispatcher = Dispatchers.Main
        )
        previousClient.vault(activity, PayPalWebVaultRequest("fake-setup-token-id"))

        val sut = PayPalWebCheckoutClient(
            analytics = analytics,
            payPalWebLauncher = payPalWebLauncher,
            sessionStore = PayPalWebCheckoutSessionStore(),
            updateClientConfigAPI = updateClientConfigAPI,
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
            mainDispatcher = Dispatchers.Main
        )
        previousClient.vault(activity, PayPalWebVaultRequest("fake-setup-token-id"))

        val sut = PayPalWebCheckoutClient(
            analytics = analytics,
            payPalWebLauncher = payPalWebLauncher,
            sessionStore = PayPalWebCheckoutSessionStore(),
            updateClientConfigAPI = updateClientConfigAPI,
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
            mainDispatcher = Dispatchers.Main
        )
        previousClient.vault(activity, PayPalWebVaultRequest("fake-setup-token-id"))

        val sut = PayPalWebCheckoutClient(
            analytics = analytics,
            payPalWebLauncher = payPalWebLauncher,
            sessionStore = PayPalWebCheckoutSessionStore(),
            updateClientConfigAPI = updateClientConfigAPI,
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
            mainDispatcher = Dispatchers.Main
        )
        previousClient.vault(activity, PayPalWebVaultRequest("fake-setup-token-id"))

        val sut = PayPalWebCheckoutClient(
            analytics = analytics,
            payPalWebLauncher = payPalWebLauncher,
            sessionStore = PayPalWebCheckoutSessionStore(),
            updateClientConfigAPI = updateClientConfigAPI,
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
    }
}
