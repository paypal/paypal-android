package com.paypal.android.paypalwebpayments

import android.content.Intent
import androidx.fragment.app.FragmentActivity
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.paypalwebpayments.analytics.PayPalWebAnalytics
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertSame
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class PayPalWebCheckoutClientUnitTest {

    private val activity: FragmentActivity = mockk(relaxed = true)
    private val analytics = mockk<PayPalWebAnalytics>(relaxed = true)

    private val intent = Intent()

    private lateinit var payPalWebLauncher: PayPalWebLauncher
    private lateinit var sut: PayPalWebCheckoutClient

    @Before
    fun beforeEach() {
        payPalWebLauncher = mockk(relaxed = true)
        sut = PayPalWebCheckoutClient(analytics, payPalWebLauncher)
    }

    @Test
    fun `start() launches PayPal web checkout`() {
        val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
        every { payPalWebLauncher.launchPayPalWebCheckout(any(), any()) } returns launchResult

        val request = PayPalWebCheckoutRequest("fake-order-id")
        sut.start(activity, request)
        verify(exactly = 1) { payPalWebLauncher.launchPayPalWebCheckout(activity, request) }
    }

    @Test
    fun `start() notifies merchant of browser switch failure`() {
        val sdkError = PayPalSDKError(123, "fake error description")
        val launchResult = PayPalPresentAuthChallengeResult.Failure(sdkError)
        every { payPalWebLauncher.launchPayPalWebCheckout(any(), any()) } returns launchResult

        val request = PayPalWebCheckoutRequest("fake-order-id")
        val result = sut.start(activity, request)
        assertSame(launchResult, result)
    }

    @Test
    fun `vault() launches PayPal web checkout`() {
        val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
        every { payPalWebLauncher.launchPayPalWebVault(any(), any()) } returns launchResult

        val request =
            PayPalWebVaultRequest("fake-setup-token-id")
        sut.vault(activity, request)
        verify(exactly = 1) { payPalWebLauncher.launchPayPalWebVault(activity, request) }
    }

    @Test
    fun `vault() notifies merchant of browser switch failure`() {
        val sdkError = PayPalSDKError(123, "fake error description")
        val launchResult = PayPalPresentAuthChallengeResult.Failure(sdkError)
        every { payPalWebLauncher.launchPayPalWebVault(any(), any()) } returns launchResult

        val request =
            PayPalWebVaultRequest("fake-setup-token-id")
        val result = sut.vault(activity, request) as PayPalPresentAuthChallengeResult.Failure

        assertSame(sdkError, result.error)
    }

    @Test
    fun `finishStart() with user auth state forwards success result from auth launcher`() {
        val successResult =
            PayPalWebCheckoutFinishStartResult.Success("fake-order-id", "fake-payer-id")
        every {
            payPalWebLauncher.completeCheckoutAuthRequest(intent, "auth state")
        } returns successResult

        val result = sut.finishStart(intent, "auth state")
        assertSame(successResult, result)
    }

    @Test
    fun `finishStart() with user auth state forwards error result from auth launcher`() {
        val error = PayPalSDKError(123, "fake-error-description")
        val failureResult = PayPalWebCheckoutFinishStartResult.Failure(error, null)
        every {
            payPalWebLauncher.completeCheckoutAuthRequest(intent, "auth state")
        } returns failureResult

        val result = sut.finishStart(intent, "auth state")
        assertSame(failureResult, result)
    }

    @Test
    fun `finishStart() with user auth state forwards cancellation result from auth launcher`() {
        val canceledResult = PayPalWebCheckoutFinishStartResult.Canceled("fake-order_id")
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
    fun `finishStart() with session auth state forwards success result from auth launcher`() {
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
    fun `finishStart() with session auth state forwards error result from auth launcher`() {
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
    fun `finishStart() with session auth state forwards cancellation result from auth launcher`() {
        val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
        every { payPalWebLauncher.launchPayPalWebCheckout(any(), any()) } returns launchResult

        val canceledResult = PayPalWebCheckoutFinishStartResult.Canceled("fake-order_id")
        every {
            payPalWebLauncher.completeCheckoutAuthRequest(intent, "auth state")
        } returns canceledResult

        val request = PayPalWebCheckoutRequest("fake-order-id")
        sut.start(activity, request)
        val result = sut.finishStart(intent)
        assertSame(canceledResult, result)
    }

    @Test
    fun `finishStart() with session auth state clears session to prevent delivering success event twice`() {
        val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
        every { payPalWebLauncher.launchPayPalWebCheckout(any(), any()) } returns launchResult

        val successResult =
            PayPalWebCheckoutFinishStartResult.Success("fake-order-id", "fake-payer-id")
        every {
            payPalWebLauncher.completeCheckoutAuthRequest(intent, "auth state")
        } returns successResult

        sut.start(activity, PayPalWebCheckoutRequest("fake-order-id"))
        assertSame(successResult, sut.finishStart(intent))
        assertNull(sut.finishStart(intent))
    }

    @Test
    fun `finishStart() with session auth state clears session to prevent delivering error event twice`() {
        val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
        every { payPalWebLauncher.launchPayPalWebCheckout(any(), any()) } returns launchResult

        val error = PayPalSDKError(123, "fake-error-description")
        val failureResult = PayPalWebCheckoutFinishStartResult.Failure(error, null)
        every {
            payPalWebLauncher.completeCheckoutAuthRequest(intent, "auth state")
        } returns failureResult

        sut.start(activity, PayPalWebCheckoutRequest("fake-order-id"))
        assertSame(failureResult, sut.finishStart(intent))
        assertNull(sut.finishStart(intent))
    }

    @Test
    fun `finishStart() with session auth state clears session to prevent delivering cancellation event twice`() {
        val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
        every { payPalWebLauncher.launchPayPalWebCheckout(any(), any()) } returns launchResult

        val canceledResult = PayPalWebCheckoutFinishStartResult.Canceled("fake-order_id")
        every {
            payPalWebLauncher.completeCheckoutAuthRequest(intent, "auth state")
        } returns canceledResult

        sut.start(activity, PayPalWebCheckoutRequest("fake-order-id"))
        assertSame(canceledResult, sut.finishStart(intent))
        assertNull(sut.finishStart(intent))
    }

    @Test
    fun `finishVault() forwards vault success from PayPal web launcher`() {
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
    fun `finishVault() notifies merchant of vault failure`() {
        val error = PayPalSDKError(123, "fake-error-description")
        every {
            payPalWebLauncher.completeVaultAuthRequest(intent, "auth state")
        } returns PayPalWebCheckoutFinishVaultResult.Failure(error)

        val result = sut.finishVault(intent, "auth state")
                as PayPalWebCheckoutFinishVaultResult.Failure

        assertSame(error, result.error)
    }

    @Test
    fun `finishVault forwards vault cancellation`() {
        every {
            payPalWebLauncher.completeVaultAuthRequest(intent, "auth state")
        } returns PayPalWebCheckoutFinishVaultResult.Canceled

        val result = sut.finishVault(intent, "auth state")
        assertTrue(result is PayPalWebCheckoutFinishVaultResult.Canceled)
    }

    @Test
    fun `finishVault forwards no result`() {
        every {
            payPalWebLauncher.completeVaultAuthRequest(intent, "auth state")
        } returns PayPalWebCheckoutFinishVaultResult.NoResult

        val result = sut.finishVault(intent, "auth state")
        assertTrue(result is PayPalWebCheckoutFinishVaultResult.NoResult)
    }
}
