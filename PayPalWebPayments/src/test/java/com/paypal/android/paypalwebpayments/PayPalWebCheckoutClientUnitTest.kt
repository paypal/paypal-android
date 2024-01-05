package com.paypal.android.paypalwebpayments

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.corepayments.analytics.AnalyticsService
import io.mockk.*
import junit.framework.TestCase.assertSame
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class PayPalWebCheckoutClientUnitTest {

    private val activity: FragmentActivity = mockk(relaxed = true)
    private val coreConfig: CoreConfig = mockk(relaxed = true)

    private val analyticsService = mockk<AnalyticsService>(relaxed = true)

    private lateinit var payPalWebLauncher: PayPalWebLauncher
    private lateinit var sut: PayPalWebCheckoutClient

    @Before
    fun beforeEach() {
        payPalWebLauncher = mockk(relaxed = true)
        sut = PayPalWebCheckoutClient(activity, analyticsService, payPalWebLauncher)
    }

    @Test
    fun `start() launches PayPal web checkout`() {
        sut.listener = mockk(relaxed = true)

        every { payPalWebLauncher.launchPayPalWebCheckout(any(), any()) } returns null

        val request = PayPalWebCheckoutRequest("fake-order-id")
        sut.start(request)
        verify(exactly = 1) { payPalWebLauncher.launchPayPalWebCheckout(activity, request) }
        verify(exactly = 0) { sut.listener?.onPayPalWebFailure(any()) }
    }

    @Test
    fun `start() notifies merchant of browser switch failure`() {
        sut.listener = mockk(relaxed = true)

        val sdkError = PayPalSDKError(123, "fake error description")
        every { payPalWebLauncher.launchPayPalWebCheckout(any(), any()) } returns sdkError

        val request = PayPalWebCheckoutRequest("fake-order-id")
        sut.start(request)

        val slot = slot<PayPalSDKError>()
        verify(exactly = 1) { sut.listener?.onPayPalWebFailure(capture(slot)) }

        assertSame(slot.captured, sdkError)
    }

    @Test
    fun `vault() launches PayPal web checkout`() {
        sut.vaultListener = mockk(relaxed = true)

        every { payPalWebLauncher.launchPayPalWebVault(any(), any()) } returns null

        val request =
            PayPalWebVaultRequest("fake-setup-token-id", "https://example.com/approval/url")
        sut.vault(request)
        verify(exactly = 1) { payPalWebLauncher.launchPayPalWebVault(activity, request) }
        verify(exactly = 0) { sut.vaultListener?.onPayPalWebVaultFailure(any()) }
    }

    @Test
    fun `vault() notifies merchant of browser switch failure`() {
        sut.vaultListener = mockk(relaxed = true)

        val sdkError = PayPalSDKError(123, "fake error description")
        every { payPalWebLauncher.launchPayPalWebVault(any(), any()) } returns sdkError

        val request =
            PayPalWebVaultRequest("fake-setup-token-id", "https://example.com/approval/url")
        sut.vault(request)

        val slot = slot<PayPalSDKError>()
        verify(exactly = 1) { sut.vaultListener?.onPayPalWebVaultFailure(capture(slot)) }

        assertSame(slot.captured, sdkError)
    }

    @Test
    fun `handleBrowserSwitchResult notifies merchant of Checkout success`() {
        sut.listener = mockk(relaxed = true)

        val successResult = PayPalWebCheckoutResult("fake-order-id", "fake-payer-id")
        every {
            payPalWebLauncher.deliverBrowserSwitchResult(activity)
        } returns PayPalWebStatus.CheckoutSuccess(successResult)

        sut.handleBrowserSwitchResult()

        val slot = slot<PayPalWebCheckoutResult>()
        verify(exactly = 1) { sut.listener?.onPayPalWebSuccess(capture(slot)) }
        assertSame(successResult, slot.captured)
    }

    @Test
    fun `handleBrowserSwitchResult notifies merchant of Checkout failure`() {
        sut.listener = mockk(relaxed = true)

        val error = PayPalSDKError(123, "fake-error-description")
        every {
            payPalWebLauncher.deliverBrowserSwitchResult(activity)
        } returns PayPalWebStatus.CheckoutError(error)

        sut.handleBrowserSwitchResult()

        val slot = slot<PayPalSDKError>()
        verify(exactly = 1) { sut.listener?.onPayPalWebFailure(capture(slot)) }
        assertSame(error, slot.captured)
    }

    @Test
    fun `handleBrowserSwitchResult notifies merchant of Checkout cancelation`() {
        sut.listener = mockk(relaxed = true)

        val status = PayPalWebStatus.CheckoutCanceled("fake-order_id")
        every {
            payPalWebLauncher.deliverBrowserSwitchResult(activity)
        } returns status

        sut.handleBrowserSwitchResult()

        verify(exactly = 1) { sut.listener?.onPayPalWebCanceled() }
    }

    @Test
    fun `handleBrowserSwitchResult doesn't deliver result when browserSwitchResult is null`() {
        sut.listener = mockk(relaxed = true)
        every { payPalWebLauncher.deliverBrowserSwitchResult(activity) } returns null

        sut.handleBrowserSwitchResult()
        verify { sut.listener?.wasNot(Called) }
    }

    @Test
    fun `when client is created, lifecycle observer is added`() {
        val lifeCycle = mockk<Lifecycle>(relaxed = true)
        every { activity.lifecycle } returns lifeCycle

        PayPalWebCheckoutClient(activity, coreConfig, "")
        verify { lifeCycle.addObserver(ofType(PayPalWebCheckoutLifeCycleObserver::class)) }
    }
}
