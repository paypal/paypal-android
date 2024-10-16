package com.paypal.android.paypalwebpayments

import android.content.Intent
import androidx.fragment.app.FragmentActivity
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.corepayments.analytics.AnalyticsService
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import junit.framework.TestCase.assertSame
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class PayPalWebCheckoutClientUnitTest {

    private val activity: FragmentActivity = mockk(relaxed = true)
    private val analyticsService = mockk<AnalyticsService>(relaxed = true)

    private val checkoutListener = mockk<PayPalWebCheckoutListener>(relaxed = true)
    private val vaultListener = mockk<PayPalWebVaultListener>(relaxed = true)

    private val intent = Intent()

    private lateinit var payPalWebLauncher: PayPalWebLauncher
    private lateinit var sut: PayPalWebCheckoutClient

    @Before
    fun beforeEach() {
        payPalWebLauncher = mockk(relaxed = true)
        sut = PayPalWebCheckoutClient(analyticsService, payPalWebLauncher)
    }

    @Test
    fun `start() launches PayPal web checkout`() {
        sut.listener = checkoutListener

        val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
        every { payPalWebLauncher.launchPayPalWebCheckout(any(), any()) } returns launchResult

        val request = PayPalWebCheckoutRequest("fake-order-id")
        sut.start(activity, request)
        verify(exactly = 1) { payPalWebLauncher.launchPayPalWebCheckout(activity, request) }
        verify(exactly = 0) { checkoutListener.onPayPalWebFailure(any()) }
    }

    @Test
    fun `start() notifies merchant of browser switch failure`() {
        sut.listener = checkoutListener

        val sdkError = PayPalSDKError(123, "fake error description")
        val launchResult = PayPalPresentAuthChallengeResult.Failure(sdkError)
        every { payPalWebLauncher.launchPayPalWebCheckout(any(), any()) } returns launchResult

        val request = PayPalWebCheckoutRequest("fake-order-id")
        sut.start(activity, request)

        val slot = slot<PayPalSDKError>()
        verify(exactly = 1) { checkoutListener.onPayPalWebFailure(capture(slot)) }

        assertSame(slot.captured, sdkError)
    }

    @Test
    fun `vault() launches PayPal web checkout`() {
        sut.vaultListener = vaultListener

        val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
        every { payPalWebLauncher.launchPayPalWebVault(any(), any()) } returns launchResult

        val request =
            PayPalWebVaultRequest("fake-setup-token-id")
        sut.vault(activity, request)
        verify(exactly = 1) { payPalWebLauncher.launchPayPalWebVault(activity, request) }
        verify(exactly = 0) { vaultListener.onPayPalWebVaultFailure(any()) }
    }

    @Test
    fun `vault() notifies merchant of browser switch failure`() {
        sut.vaultListener = vaultListener

        val sdkError = PayPalSDKError(123, "fake error description")
        val launchResult = PayPalPresentAuthChallengeResult.Failure(sdkError)
        every { payPalWebLauncher.launchPayPalWebVault(any(), any()) } returns launchResult

        val request =
            PayPalWebVaultRequest("fake-setup-token-id")
        sut.vault(activity, request)

        val slot = slot<PayPalSDKError>()
        verify(exactly = 1) { vaultListener.onPayPalWebVaultFailure(capture(slot)) }

        assertSame(slot.captured, sdkError)
    }

    @Test
    fun `completeAuthChallenge notifies merchant of Checkout success`() {
        sut.listener = checkoutListener

        val successResult = PayPalWebCheckoutResult("fake-order-id", "fake-payer-id")
        every {
            payPalWebLauncher.completeAuthRequest(intent, "auth state")
        } returns PayPalWebStatus.CheckoutSuccess(successResult)

        sut.completeAuthChallenge(intent, "auth state")

        val slot = slot<PayPalWebCheckoutResult>()
        verify(exactly = 1) { checkoutListener.onPayPalWebSuccess(capture(slot)) }
        assertSame(successResult, slot.captured)
    }

    @Test
    fun `completeAuthChallenge notifies merchant of Checkout failure`() {
        sut.listener = checkoutListener

        val error = PayPalSDKError(123, "fake-error-description")
        every {
            payPalWebLauncher.completeAuthRequest(intent, "auth state")
        } returns PayPalWebStatus.CheckoutError(error, null)

        sut.completeAuthChallenge(intent, "auth state")

        val slot = slot<PayPalSDKError>()
        verify(exactly = 1) { checkoutListener.onPayPalWebFailure(capture(slot)) }
        assertSame(error, slot.captured)
    }

    @Test
    fun `completeAuthChallenge notifies merchant of Checkout cancelation`() {
        sut.listener = checkoutListener

        val status = PayPalWebStatus.CheckoutCanceled("fake-order_id")
        every {
            payPalWebLauncher.completeAuthRequest(intent, "auth state")
        } returns status

        sut.completeAuthChallenge(intent, "auth state")

        verify(exactly = 1) { checkoutListener.onPayPalWebCanceled() }
    }

    @Test
    fun `completeAuthChallenge notifies merchant of vault success`() {
        sut.vaultListener = vaultListener

        val successResult = PayPalWebVaultResult("fake-approval-session-id")
        every {
            payPalWebLauncher.completeAuthRequest(intent, "auth state")
        } returns PayPalWebStatus.VaultSuccess(successResult)

        sut.completeAuthChallenge(intent, "auth state")

        val slot = slot<PayPalWebVaultResult>()
        verify(exactly = 1) { vaultListener.onPayPalWebVaultSuccess(capture(slot)) }
        assertSame(successResult, slot.captured)
    }

    @Test
    fun `completeAuthChallenge notifies merchant of vault failure`() {
        sut.vaultListener = vaultListener

        val error = PayPalSDKError(123, "fake-error-description")
        every {
            payPalWebLauncher.completeAuthRequest(intent, "auth state")
        } returns PayPalWebStatus.VaultError(error)

        sut.completeAuthChallenge(intent, "auth state")

        val slot = slot<PayPalSDKError>()
        verify(exactly = 1) { vaultListener.onPayPalWebVaultFailure(capture(slot)) }
        assertSame(error, slot.captured)
    }

    @Test
    fun `completeAuthChallenge notifies merchant of vault cancelation`() {
        sut.vaultListener = vaultListener

        every {
            payPalWebLauncher.completeAuthRequest(intent, "auth state")
        } returns PayPalWebStatus.VaultCanceled

        sut.completeAuthChallenge(intent, "auth state")
        verify(exactly = 1) { vaultListener.onPayPalWebVaultCanceled() }
    }

    @Test
    fun `completeAuthChallenge doesn't deliver result when browserSwitchResult is null`() {
        sut.listener = checkoutListener
        sut.vaultListener = vaultListener

        every {
            payPalWebLauncher.completeAuthRequest(intent, "auth state")
        } returns PayPalWebStatus.NoResult

        sut.completeAuthChallenge(intent, "auth state")

        verify { checkoutListener.wasNot(Called) }
        verify { vaultListener.wasNot(Called) }
    }
}
