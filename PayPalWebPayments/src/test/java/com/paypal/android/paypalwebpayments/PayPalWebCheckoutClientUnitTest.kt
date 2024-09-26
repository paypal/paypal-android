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
import strikt.api.expectThat
import strikt.assertions.isNull

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class PayPalWebCheckoutClientUnitTest {

    private val activity: FragmentActivity = mockk(relaxed = true)
    private val coreConfig: CoreConfig = mockk(relaxed = true)

    private val analyticsService = mockk<AnalyticsService>(relaxed = true)

    private val checkoutListener = mockk<PayPalWebCheckoutListener>(relaxed = true)
    private val vaultListener = mockk<PayPalWebVaultListener>(relaxed = true)

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

        every { payPalWebLauncher.launchPayPalWebCheckout(any(), any()) } returns null

        val request = PayPalWebCheckoutRequest("fake-order-id")
        sut.start(, request)
        verify(exactly = 1) { payPalWebLauncher.launchPayPalWebCheckout(activity, request) }
        verify(exactly = 0) { checkoutListener.onPayPalWebFailure(any()) }
    }

    @Test
    fun `start() notifies merchant of browser switch failure`() {
        sut.listener = checkoutListener

        val sdkError = PayPalSDKError(123, "fake error description")
        every { payPalWebLauncher.launchPayPalWebCheckout(any(), any()) } returns sdkError

        val request = PayPalWebCheckoutRequest("fake-order-id")
        sut.start(, request)

        val slot = slot<PayPalSDKError>()
        verify(exactly = 1) { checkoutListener.onPayPalWebFailure(capture(slot)) }

        assertSame(slot.captured, sdkError)
    }

    @Test
    fun `vault() launches PayPal web checkout`() {
        sut.vaultListener = vaultListener

        every { payPalWebLauncher.launchPayPalWebVault(any(), any()) } returns null

        val request =
            PayPalWebVaultRequest("fake-setup-token-id")
        sut.vault(request)
        verify(exactly = 1) { payPalWebLauncher.launchPayPalWebVault(activity, request) }
        verify(exactly = 0) { vaultListener.onPayPalWebVaultFailure(any()) }
    }

    @Test
    fun `vault() notifies merchant of browser switch failure`() {
        sut.vaultListener = vaultListener

        val sdkError = PayPalSDKError(123, "fake error description")
        every { payPalWebLauncher.launchPayPalWebVault(any(), any()) } returns sdkError

        val request =
            PayPalWebVaultRequest("fake-setup-token-id")
        sut.vault(request)

        val slot = slot<PayPalSDKError>()
        verify(exactly = 1) { vaultListener.onPayPalWebVaultFailure(capture(slot)) }

        assertSame(slot.captured, sdkError)
    }

    @Test
    fun `handleBrowserSwitchResult notifies merchant of Checkout success`() {
        sut.listener = checkoutListener

        val successResult = PayPalWebCheckoutResult("fake-order-id", "fake-payer-id")
        every {
            payPalWebLauncher.deliverBrowserSwitchResult(activity)
        } returns PayPalWebStatus.CheckoutSuccess(successResult)

        sut.handleBrowserSwitchResult()

        val slot = slot<PayPalWebCheckoutResult>()
        verify(exactly = 1) { checkoutListener.onPayPalWebSuccess(capture(slot)) }
        assertSame(successResult, slot.captured)
    }

    @Test
    fun `handleBrowserSwitchResult notifies merchant of Checkout failure`() {
        sut.listener = checkoutListener

        val error = PayPalSDKError(123, "fake-error-description")
        every {
            payPalWebLauncher.deliverBrowserSwitchResult(activity)
        } returns PayPalWebStatus.CheckoutError(error, null)

        sut.handleBrowserSwitchResult()

        val slot = slot<PayPalSDKError>()
        verify(exactly = 1) { checkoutListener.onPayPalWebFailure(capture(slot)) }
        assertSame(error, slot.captured)
    }

    @Test
    fun `handleBrowserSwitchResult notifies merchant of Checkout cancelation`() {
        sut.listener = checkoutListener

        val status = PayPalWebStatus.CheckoutCanceled("fake-order_id")
        every {
            payPalWebLauncher.deliverBrowserSwitchResult(activity)
        } returns status

        sut.handleBrowserSwitchResult()

        verify(exactly = 1) { checkoutListener.onPayPalWebCanceled() }
    }

    @Test
    fun `handleBrowserSwitchResult notifies merchant of vault success`() {
        sut.vaultListener = vaultListener

        val successResult = PayPalWebVaultResult("fake-approval-session-id")
        every {
            payPalWebLauncher.deliverBrowserSwitchResult(activity)
        } returns PayPalWebStatus.VaultSuccess(successResult)

        sut.handleBrowserSwitchResult()

        val slot = slot<PayPalWebVaultResult>()
        verify(exactly = 1) { vaultListener.onPayPalWebVaultSuccess(capture(slot)) }
        assertSame(successResult, slot.captured)
    }

    @Test
    fun `handleBrowserSwitchResult notifies merchant of vault failure`() {
        sut.vaultListener = vaultListener

        val error = PayPalSDKError(123, "fake-error-description")
        every {
            payPalWebLauncher.deliverBrowserSwitchResult(activity)
        } returns PayPalWebStatus.VaultError(error)

        sut.handleBrowserSwitchResult()

        val slot = slot<PayPalSDKError>()
        verify(exactly = 1) { vaultListener.onPayPalWebVaultFailure(capture(slot)) }
        assertSame(error, slot.captured)
    }

    @Test
    fun `handleBrowserSwitchResult notifies merchant of vault cancelation`() {
        sut.vaultListener = vaultListener

        every {
            payPalWebLauncher.deliverBrowserSwitchResult(activity)
        } returns PayPalWebStatus.VaultCanceled

        sut.handleBrowserSwitchResult()
        verify(exactly = 1) { vaultListener.onPayPalWebVaultCanceled() }
    }

    @Test
    fun `handleBrowserSwitchResult doesn't deliver result when browserSwitchResult is null`() {
        sut.listener = checkoutListener
        sut.vaultListener = vaultListener
        every { payPalWebLauncher.deliverBrowserSwitchResult(activity) } returns null

        sut.handleBrowserSwitchResult()
        verify { checkoutListener.wasNot(Called) }
        verify { vaultListener.wasNot(Called) }
    }

    @Test
    fun `when client is created, lifecycle observer is added`() {
        val lifeCycle = mockk<Lifecycle>(relaxed = true)
        every { activity.lifecycle } returns lifeCycle

        PayPalWebCheckoutClient(coreConfig, "")
        verify { lifeCycle.addObserver(ofType(PayPalWebCheckoutLifeCycleObserver::class)) }
    }

    @Test
    fun `when client is complete, lifecycle observer is removed`() {
        val lifeCycle = mockk<Lifecycle>(relaxed = true)
        every { activity.lifecycle } returns lifeCycle

        sut.removeObservers()

        verify { lifeCycle.removeObserver(sut.observer) }
        expectThat(sut.listener).isNull()
        expectThat(sut.vaultListener).isNull()
    }
}
