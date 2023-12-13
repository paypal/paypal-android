package com.paypal.android.paypalwebpayments

import android.net.Uri
import androidx.fragment.app.FragmentActivity
import com.braintreepayments.api.BrowserSwitchClient
import com.braintreepayments.api.BrowserSwitchOptions
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.Environment
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@RunWith(RobolectricTestRunner::class)
class PayPalWebLauncherUnitTest {

    private lateinit var browserSwitchClient: BrowserSwitchClient
    private lateinit var sut: PayPalWebLauncher

    // TODO: consider using androidx.test activity instead of mockk
    // Ref: https://robolectric.org/androidx_test/#activities
    private val activity: FragmentActivity = mockk(relaxed = true)

    private val liveConfig = CoreConfig("live-client-id", Environment.LIVE)
    private val sandboxConfig = CoreConfig("live-client-id", Environment.SANDBOX)

    @Before
    fun beforeEach() {
        browserSwitchClient = mockk(relaxed = true)
    }

    @Test
    fun `launchPayPalWebCheckout() browser switches to SANDBOX PayPal web checkout`() {
        sut = PayPalWebLauncher("custom_url_scheme", sandboxConfig, browserSwitchClient)

        val slot = slot<BrowserSwitchOptions>()
        every { browserSwitchClient.start(activity, capture(slot)) } just runs

        val fundingSource = PayPalWebCheckoutFundingSource.PAYPAL
        val request = PayPalWebCheckoutRequest("fake-order-id", fundingSource)
        sut.launchPayPalWebCheckout(activity, request)

        val expectedUrl = "https://www.sandbox.paypal.com/checkoutnow?" +
                "token=fake-order-id" +
                "&redirect_uri=custom_url_scheme" +
                "%3A%2F%2Fx-callback-url%2Fpaypal-sdk%2Fpaypal-checkout&native_xo=1" +
                "&fundingSource=paypal"

        val browserSwitchOptions = slot.captured
        expectThat(browserSwitchOptions) {
            get { metadata?.get("order_id") }.isEqualTo("fake-order_id")
            get { returnUrlScheme }.isEqualTo("custom_url_scheme")
            get { url }.isEqualTo(Uri.parse(expectedUrl))
        }
    }

    @Test
    fun `launchPayPalWebCheckout() browser switches to LIVE PayPal web checkout`() {
        sut = PayPalWebLauncher("custom_url_scheme", liveConfig, browserSwitchClient)

        val slot = slot<BrowserSwitchOptions>()
        every { browserSwitchClient.start(activity, capture(slot)) } just runs

        val fundingSource = PayPalWebCheckoutFundingSource.PAYPAL
        val request = PayPalWebCheckoutRequest("fake-order-id", fundingSource)
        sut.launchPayPalWebCheckout(activity, request)

        val expectedUrl = "https://www.paypal.com/checkoutnow?" +
                "token=fake-order-id" +
                "&redirect_uri=custom_url_scheme" +
                "%3A%2F%2Fx-callback-url%2Fpaypal-sdk%2Fpaypal-checkout&native_xo=1" +
                "&fundingSource=paypal"

        val browserSwitchOptions = slot.captured
        expectThat(browserSwitchOptions) {
            get { metadata?.get("order_id") }.isEqualTo("fake-order_id")
            get { returnUrlScheme }.isEqualTo("custom_url_scheme")
            get { url }.isEqualTo(Uri.parse(expectedUrl))
        }
    }

    @Test
    fun `launchPayPalWebCheckout() browser switches to PayPal Credit web checkout`() {
        sut = PayPalWebLauncher("custom_url_scheme", liveConfig, browserSwitchClient)

        val slot = slot<BrowserSwitchOptions>()
        every { browserSwitchClient.start(activity, capture(slot)) } just runs

        val fundingSource = PayPalWebCheckoutFundingSource.PAYPAL_CREDIT
        val request = PayPalWebCheckoutRequest("fake-order-id", fundingSource)
        sut.launchPayPalWebCheckout(activity, request)

        val expectedUrl = "https://www.paypal.com/checkoutnow?" +
                "token=fake-order-id" +
                "&redirect_uri=custom_url_scheme" +
                "%3A%2F%2Fx-callback-url%2Fpaypal-sdk%2Fpaypal-checkout&native_xo=1" +
                "&fundingSource=credit"

        val browserSwitchOptions = slot.captured
        expectThat(browserSwitchOptions) {
            get { metadata?.get("order_id") }.isEqualTo("fake-order_id")
            get { returnUrlScheme }.isEqualTo("custom_url_scheme")
            get { url }.isEqualTo(Uri.parse(expectedUrl))
        }
    }

    @Test
    fun `launchPayPalWebCheckout() browser switches to PayPal Pay Later web checkout`() {
        sut = PayPalWebLauncher("custom_url_scheme", liveConfig, browserSwitchClient)

        val slot = slot<BrowserSwitchOptions>()
        every { browserSwitchClient.start(activity, capture(slot)) } just runs

        val fundingSource = PayPalWebCheckoutFundingSource.PAY_LATER
        val request = PayPalWebCheckoutRequest("fake-order-id", fundingSource)
        sut.launchPayPalWebCheckout(activity, request)

        val expectedUrl = "https://www.paypal.com/checkoutnow?" +
                "token=fake-order-id" +
                "&redirect_uri=custom_url_scheme" +
                "%3A%2F%2Fx-callback-url%2Fpaypal-sdk%2Fpaypal-checkout&native_xo=1" +
                "&fundingSource=credit"

        val browserSwitchOptions = slot.captured
        expectThat(browserSwitchOptions) {
            get { metadata?.get("order_id") }.isEqualTo("fake-order_id")
            get { returnUrlScheme }.isEqualTo("custom_url_scheme")
            get { url }.isEqualTo(Uri.parse(expectedUrl))
        }
    }
}
