package com.paypal.android.paypalwebpayments

import android.net.Uri
import androidx.fragment.app.FragmentActivity
import com.braintreepayments.api.BrowserSwitchClient
import com.braintreepayments.api.BrowserSwitchException
import com.braintreepayments.api.BrowserSwitchOptions
import com.braintreepayments.api.BrowserSwitchResult
import com.braintreepayments.api.BrowserSwitchStatus
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.Environment
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.json.JSONObject
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
            get { metadata?.get("order_id") }.isEqualTo("fake-order-id")
            get { metadata?.get("request_type") }.isEqualTo("checkout")
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
            get { metadata?.get("order_id") }.isEqualTo("fake-order-id")
            get { metadata?.get("request_type") }.isEqualTo("checkout")
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
            get { metadata?.get("order_id") }.isEqualTo("fake-order-id")
            get { metadata?.get("request_type") }.isEqualTo("checkout")
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
                "&fundingSource=paylater"

        val browserSwitchOptions = slot.captured
        expectThat(browserSwitchOptions) {
            get { metadata?.get("order_id") }.isEqualTo("fake-order-id")
            get { metadata?.get("request_type") }.isEqualTo("checkout")
            get { returnUrlScheme }.isEqualTo("custom_url_scheme")
            get { url }.isEqualTo(Uri.parse(expectedUrl))
        }
    }

    @Test
    fun `launchPayPalWebCheckout() returns an error when it cannot browser switch`() {
        sut = PayPalWebLauncher("custom_url_scheme", liveConfig, browserSwitchClient)

        val browserSwitchException = mockk<BrowserSwitchException>()
        every { browserSwitchException.message } returns "error message from browser switch"

        every { browserSwitchClient.start(any(), any()) } throws browserSwitchException

        val request = PayPalWebCheckoutRequest("fake-order-id")
        val error = sut.launchPayPalWebCheckout(activity, request)
        assertEquals("error message from browser switch", error?.errorDescription)
    }

    @Test
    fun `launchPayPalWebVault() browser switches to approval url`() {
        sut = PayPalWebLauncher("custom_url_scheme", sandboxConfig, browserSwitchClient)

        val slot = slot<BrowserSwitchOptions>()
        every { browserSwitchClient.start(activity, capture(slot)) } just runs

        val vaultRequest =
            PayPalWebVaultRequest("fake-setup-token-id", "https://example.com/approval/url")
        sut.launchPayPalWebVault(activity, vaultRequest)

        val browserSwitchOptions = slot.captured
        expectThat(browserSwitchOptions) {
            get { metadata?.get("setup_token_id") }.isEqualTo("fake-setup-token-id")
            get { metadata?.get("request_type") }.isEqualTo("vault")
            get { returnUrlScheme }.isEqualTo("custom_url_scheme")
            get { url }.isEqualTo(Uri.parse("https://example.com/approval/url"))
        }
    }

    @Test
    fun `launchPayPalWebVault() returns an error when it cannot browser switch`() {
        sut = PayPalWebLauncher("custom_url_scheme", sandboxConfig, browserSwitchClient)

        val browserSwitchException = mockk<BrowserSwitchException>()
        every { browserSwitchException.message } returns "error message from browser switch"

        every { browserSwitchClient.start(any(), any()) } throws browserSwitchException

        val vaultRequest =
            PayPalWebVaultRequest("fake-setup-token-id", "https://example.com/approval/url")
        sut.launchPayPalWebVault(activity, vaultRequest)

        val request = PayPalWebCheckoutRequest("fake-order-id")
        val error = sut.launchPayPalWebCheckout(activity, request)
        assertEquals("error message from browser switch", error?.errorDescription)
    }

    @Test
    fun `deliverBrowserSwitchResult() parses successful checkout result`() {
        val browserSwitchResult =
            createCheckoutSuccessBrowserSwitchResult(
                orderId = "fake-order-id",
                payerId = "fake-payer-id"
            )
        every { browserSwitchClient.deliverResult(activity) } returns browserSwitchResult

        sut = PayPalWebLauncher("custom_url_scheme", liveConfig, browserSwitchClient)
        val status = sut.deliverBrowserSwitchResult(activity) as PayPalWebStatus.CheckoutSuccess
        assertEquals("fake-order-id", status.result.orderId)
        assertEquals("fake-payer-id", status.result.payerId)
    }

    @Test
    fun `deliverBrowserSwitchResult() parses checkout failure when Payer Id is blank`() {
        val browserSwitchResult =
            createCheckoutSuccessBrowserSwitchResult(orderId = "fake-order-id", payerId = "")
        every { browserSwitchClient.deliverResult(activity) } returns browserSwitchResult

        sut = PayPalWebLauncher("custom_url_scheme", liveConfig, browserSwitchClient)
        val status = sut.deliverBrowserSwitchResult(activity) as PayPalWebStatus.CheckoutError
        val expectedDescription =
            "Result did not contain the expected data. Payer ID or Order ID is null."
        assertEquals(expectedDescription, status.error.errorDescription)
    }

    @Test
    fun `deliverBrowserSwitchResult() parses checkout failure when Order Id is blank`() {
        val browserSwitchResult =
            createCheckoutSuccessBrowserSwitchResult(orderId = "", payerId = "fake-payer-id")
        every { browserSwitchClient.deliverResult(activity) } returns browserSwitchResult

        sut = PayPalWebLauncher("custom_url_scheme", liveConfig, browserSwitchClient)
        val status = sut.deliverBrowserSwitchResult(activity) as PayPalWebStatus.CheckoutError
        val expectedDescription =
            "Result did not contain the expected data. Payer ID or Order ID is null."
        assertEquals(expectedDescription, status.error.errorDescription)
    }

    @Test
    fun `deliverBrowserSwitchResult() parses checkout failure when deeplink is null`() {
        val browserSwitchResult =
            createCheckoutSuccessBrowserSwitchResult(orderId = "fake-order-id", deepLinkUrl = null)
        every { browserSwitchClient.deliverResult(activity) } returns browserSwitchResult

        sut = PayPalWebLauncher("custom_url_scheme", liveConfig, browserSwitchClient)
        val status = sut.deliverBrowserSwitchResult(activity) as PayPalWebStatus.CheckoutError
        val expectedDescription =
            "An unknown error occurred. Contact developer.paypal.com/support."
        assertEquals(expectedDescription, status.error.errorDescription)
    }

    @Test
    fun `deliverBrowserSwitchResult() parses checkout failure when metadata is null`() {
        val browserSwitchResult =
            createCheckoutSuccessBrowserSwitchResult(payerId = "fake-payer-id", metadata = null)
        every { browserSwitchClient.deliverResult(activity) } returns browserSwitchResult

        sut = PayPalWebLauncher("custom_url_scheme", liveConfig, browserSwitchClient)
        val status = sut.deliverBrowserSwitchResult(activity) as PayPalWebStatus.CheckoutError
        val expectedDescription =
            "An unknown error occurred. Contact developer.paypal.com/support."
        assertEquals(expectedDescription, status.error.errorDescription)
    }

    @Test
    fun `deliverBrowserSwitchResult() parses checkout cancelations`() {
        val browserSwitchResult = createCheckoutCanceledBrowserSwitchResult("fake-order-id")
        createBrowserSwitchResult(BrowserSwitchStatus.CANCELED)
        every { browserSwitchClient.deliverResult(activity) } returns browserSwitchResult

        sut = PayPalWebLauncher("custom_url_scheme", liveConfig, browserSwitchClient)
        val status = sut.deliverBrowserSwitchResult(activity)
        assertTrue(status is PayPalWebStatus.CheckoutCanceled)
    }

    @Test
    fun `deliverBrowserSwitchResult() parses successful vault result`() {
        val browserSwitchResult = createVaultSuccessBrowserSwitchResult(
            setupTokenId = "fake-setup-token-id",
            approvalSessionId = "fake-approval-session-id",
        )
        every { browserSwitchClient.deliverResult(activity) } returns browserSwitchResult

        sut = PayPalWebLauncher("custom_url_scheme", liveConfig, browserSwitchClient)
        val status = sut.deliverBrowserSwitchResult(activity) as PayPalWebStatus.VaultSuccess
        assertEquals("fake-approval-session-id", status.result.approvalSessionId)
    }

    @Test
    fun `deliverBrowserSwitchResult() parses vault failure when approval session id is blank`() {
        val browserSwitchResult = createVaultSuccessBrowserSwitchResult(
            setupTokenId = "fake-setup-token-id",
            approvalSessionId = "",
        )
        every { browserSwitchClient.deliverResult(activity) } returns browserSwitchResult

        sut = PayPalWebLauncher("custom_url_scheme", liveConfig, browserSwitchClient)
        val status = sut.deliverBrowserSwitchResult(activity) as PayPalWebStatus.VaultError
        val expectedDescription =
            "Result did not contain the expected data. Payer ID or Order ID is null."
        assertEquals(expectedDescription, status.error.errorDescription)
    }

    @Test
    fun `deliverBrowserSwitchResult() parses vault failure when deeplink is null`() {
        val browserSwitchResult =
            createVaultSuccessBrowserSwitchResult(
                setupTokenId = "fake-setup-token-id",
                deepLinkUrl = null
            )
        every { browserSwitchClient.deliverResult(activity) } returns browserSwitchResult

        sut = PayPalWebLauncher("custom_url_scheme", liveConfig, browserSwitchClient)
        val status = sut.deliverBrowserSwitchResult(activity) as PayPalWebStatus.VaultError
        val expectedDescription =
            "An unknown error occurred. Contact developer.paypal.com/support."
        assertEquals(expectedDescription, status.error.errorDescription)
    }

    @Test
    fun `deliverBrowserSwitchResult() parses vault cancelations`() {
        val browserSwitchResult = createVaultCanceledBrowserSwitchResult("fake-setup-token-id")
        every { browserSwitchClient.deliverResult(activity) } returns browserSwitchResult

        sut = PayPalWebLauncher("custom_url_scheme", liveConfig, browserSwitchClient)
        val status = sut.deliverBrowserSwitchResult(activity)
        assertTrue(status is PayPalWebStatus.VaultCanceled)
    }

    private fun createCheckoutMetadata(orderId: String) = JSONObject()
        .put("order_id", orderId)
        .put("request_type", "checkout")

    private fun createCheckoutDeepLinkUrl(payerId: String) =
        Uri.parse("http://testurl.com/checkout?PayerID=$payerId")

    private fun createCheckoutSuccessBrowserSwitchResult(
        orderId: String? = null,
        payerId: String? = null,
        metadata: JSONObject? = orderId?.let { createCheckoutMetadata(it) },
        deepLinkUrl: Uri? = payerId?.let { createCheckoutDeepLinkUrl(it) }
    ) = createBrowserSwitchResult(BrowserSwitchStatus.SUCCESS, metadata, deepLinkUrl)

    private fun createCheckoutCanceledBrowserSwitchResult(orderId: String) =
        createBrowserSwitchResult(
            BrowserSwitchStatus.CANCELED,
            metadata = createCheckoutMetadata(orderId)
        )

    private fun createVaultMetadata(setupTokenId: String) = JSONObject()
        .put("setup_token_id", setupTokenId)
        .put("request_type", "vault")

    private fun createVaultDeepLinkUrl(approvalSessionId: String) =
        Uri.parse("http://testurl.com/checkout?approval_session_id=$approvalSessionId")

    private fun createVaultSuccessBrowserSwitchResult(
        setupTokenId: String? = null,
        approvalSessionId: String? = null,
        metadata: JSONObject? = setupTokenId?.let { createVaultMetadata(it) },
        deepLinkUrl: Uri? = approvalSessionId?.let { createVaultDeepLinkUrl(it) }
    ) = createBrowserSwitchResult(BrowserSwitchStatus.SUCCESS, metadata, deepLinkUrl)

    private fun createVaultCanceledBrowserSwitchResult(setupTokenId: String) =
        createBrowserSwitchResult(
            BrowserSwitchStatus.CANCELED,
            metadata = createVaultMetadata(setupTokenId)
        )

    private fun createBrowserSwitchResult(
        @BrowserSwitchStatus status: Int,
        metadata: JSONObject? = null,
        deepLinkUrl: Uri? = null
    ): BrowserSwitchResult {
        // TODO: migrate to Library Private constructor for BrowserSwitchResult instead of using mockk
        val browserSwitchResult = mockk<BrowserSwitchResult>()
        every { browserSwitchResult.status } returns status
        every { browserSwitchResult.deepLinkUrl } returns deepLinkUrl
        every { browserSwitchResult.requestMetadata } returns metadata
        return browserSwitchResult
    }
}
