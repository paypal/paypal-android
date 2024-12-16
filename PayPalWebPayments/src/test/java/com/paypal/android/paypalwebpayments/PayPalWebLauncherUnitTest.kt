package com.paypal.android.paypalwebpayments

import android.content.Intent
import android.net.Uri
import androidx.fragment.app.FragmentActivity
import com.braintreepayments.api.BrowserSwitchClient
import com.braintreepayments.api.BrowserSwitchFinalResult
import com.braintreepayments.api.BrowserSwitchOptions
import com.braintreepayments.api.BrowserSwitchStartResult
import com.paypal.android.corepayments.BrowserSwitchRequestCodes
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.Environment
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import junit.framework.TestCase.assertEquals
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
    private val intent = Intent()

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
        every {
            browserSwitchClient.start(activity, capture(slot))
        } returns BrowserSwitchStartResult.Started("pending request")

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
            get { returnUrlScheme }.isEqualTo("custom_url_scheme")
            get { url }.isEqualTo(Uri.parse(expectedUrl))
            get { requestCode }.isEqualTo(BrowserSwitchRequestCodes.PAYPAL_CHECKOUT)
        }
    }

    @Test
    fun `launchPayPalWebCheckout() browser switches to LIVE PayPal web checkout`() {
        sut = PayPalWebLauncher("custom_url_scheme", liveConfig, browserSwitchClient)

        val slot = slot<BrowserSwitchOptions>()
        every {
            browserSwitchClient.start(activity, capture(slot))
        } returns BrowserSwitchStartResult.Started("pending request")

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
            get { returnUrlScheme }.isEqualTo("custom_url_scheme")
            get { url }.isEqualTo(Uri.parse(expectedUrl))
            get { requestCode }.isEqualTo(BrowserSwitchRequestCodes.PAYPAL_CHECKOUT)
        }
    }

    @Test
    fun `launchPayPalWebCheckout() browser switches to PayPal Credit web checkout`() {
        sut = PayPalWebLauncher("custom_url_scheme", liveConfig, browserSwitchClient)

        val slot = slot<BrowserSwitchOptions>()
        every {
            browserSwitchClient.start(activity, capture(slot))
        } returns BrowserSwitchStartResult.Started("pending request")

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
            get { returnUrlScheme }.isEqualTo("custom_url_scheme")
            get { url }.isEqualTo(Uri.parse(expectedUrl))
            get { requestCode }.isEqualTo(BrowserSwitchRequestCodes.PAYPAL_CHECKOUT)
        }
    }

    @Test
    fun `launchPayPalWebCheckout() browser switches to PayPal Pay Later web checkout`() {
        sut = PayPalWebLauncher("custom_url_scheme", liveConfig, browserSwitchClient)

        val slot = slot<BrowserSwitchOptions>()
        every {
            browserSwitchClient.start(activity, capture(slot))
        } returns BrowserSwitchStartResult.Started("pending request")

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
            get { returnUrlScheme }.isEqualTo("custom_url_scheme")
            get { url }.isEqualTo(Uri.parse(expectedUrl))
            get { requestCode }.isEqualTo(BrowserSwitchRequestCodes.PAYPAL_CHECKOUT)
        }
    }

    @Test
    fun `launchPayPalWebCheckout() returns an error when it cannot browser switch`() {
        sut = PayPalWebLauncher("custom_url_scheme", liveConfig, browserSwitchClient)

        val browserSwitchError = Exception("error message from browser switch")
        every {
            browserSwitchClient.start(any(), any())
        } returns BrowserSwitchStartResult.Failure(browserSwitchError)

        val request = PayPalWebCheckoutRequest("fake-order-id")
        val result = sut.launchPayPalWebCheckout(activity, request)
                as PayPalPresentAuthChallengeResult.Failure
        assertEquals("error message from browser switch", result.error.errorDescription)
    }

    @Test
    fun `launchPayPalWebVault() browser switches to SANDBOX PayPal vault checkout`() {
        sut = PayPalWebLauncher("custom_url_scheme", sandboxConfig, browserSwitchClient)

        val slot = slot<BrowserSwitchOptions>()
        every {
            browserSwitchClient.start(activity, capture(slot))
        } returns BrowserSwitchStartResult.Started("pending request")

        val request = PayPalWebVaultRequest("fake-setup-token")
        sut.launchPayPalWebVault(activity, request)

        val expectedUrl = "https://sandbox.paypal.com/agreements/approve?" +
                "approval_session_id=fake-setup-token"

        val browserSwitchOptions = slot.captured
        expectThat(browserSwitchOptions) {
            get { metadata?.get("setup_token_id") }.isEqualTo("fake-setup-token")
            get { returnUrlScheme }.isEqualTo("custom_url_scheme")
            get { url }.isEqualTo(Uri.parse(expectedUrl))
            get { requestCode }.isEqualTo(BrowserSwitchRequestCodes.PAYPAL_VAULT)
        }
    }

    @Test
    fun `launchPayPalWebVault() browser switches to LIVE PayPal vault checkout`() {
        sut = PayPalWebLauncher("custom_url_scheme", liveConfig, browserSwitchClient)

        val slot = slot<BrowserSwitchOptions>()
        every {
            browserSwitchClient.start(activity, capture(slot))
        } returns BrowserSwitchStartResult.Started("pending request")

        val request = PayPalWebVaultRequest("fake-setup-token")
        sut.launchPayPalWebVault(activity, request)

        val expectedUrl = "https://paypal.com/agreements/approve?" +
                "approval_session_id=fake-setup-token"

        val browserSwitchOptions = slot.captured
        expectThat(browserSwitchOptions) {
            get { metadata?.get("setup_token_id") }.isEqualTo("fake-setup-token")
            get { returnUrlScheme }.isEqualTo("custom_url_scheme")
            get { url }.isEqualTo(Uri.parse(expectedUrl))
            get { requestCode }.isEqualTo(BrowserSwitchRequestCodes.PAYPAL_VAULT)
        }
    }

    @Test
    fun `launchPayPalWebVault() returns an error when it cannot browser switch`() {
        sut = PayPalWebLauncher("custom_url_scheme", sandboxConfig, browserSwitchClient)

        val browserSwitchError = Exception("error message from browser switch")
        every {
            browserSwitchClient.start(any(), any())
        } returns BrowserSwitchStartResult.Failure(browserSwitchError)

        val vaultRequest = PayPalWebVaultRequest("fake-setup-token-id")
        sut.launchPayPalWebVault(activity, vaultRequest)

        val request = PayPalWebCheckoutRequest("fake-order-id")
        val result = sut.launchPayPalWebCheckout(activity, request)
                as PayPalPresentAuthChallengeResult.Failure
        assertEquals("error message from browser switch", result.error.errorDescription)
    }

    @Test
    fun `completeCheckoutAuthRequest() parses successful checkout result`() {
        val browserSwitchResult = createCheckoutSuccessBrowserSwitchResult(
            requestCode = BrowserSwitchRequestCodes.PAYPAL_CHECKOUT,
            orderId = "fake-order-id",
            payerId = "fake-payer-id"
        )

        every {
            browserSwitchClient.completeRequest(intent, "pending request")
        } returns browserSwitchResult

        sut = PayPalWebLauncher("custom_url_scheme", liveConfig, browserSwitchClient)

        val result = sut.completeCheckoutAuthRequest(intent, "pending request")
                as PayPalWebCheckoutFinishStartResult.Success
        assertEquals("fake-order-id", result.orderId)
        assertEquals("fake-payer-id", result.payerId)
    }

    @Test
    fun `completeCheckoutAuthRequest() parses checkout failure when Payer Id is blank`() {
        val browserSwitchResult = createCheckoutSuccessBrowserSwitchResult(
            requestCode = BrowserSwitchRequestCodes.PAYPAL_CHECKOUT,
            orderId = "fake-order-id",
            payerId = ""
        )
        every {
            browserSwitchClient.completeRequest(intent, "pending request")
        } returns browserSwitchResult

        sut = PayPalWebLauncher("custom_url_scheme", liveConfig, browserSwitchClient)
        val result = sut.completeCheckoutAuthRequest(intent, "pending request")
                as PayPalWebCheckoutFinishStartResult.Failure
        val expectedDescription =
            "Result did not contain the expected data. Payer ID or Order ID is null."
        assertEquals(expectedDescription, result.error.errorDescription)
    }

    @Test
    fun `completeCheckoutAuthRequest() parses checkout failure when Order Id is blank`() {
        val browserSwitchResult = createCheckoutSuccessBrowserSwitchResult(
            requestCode = BrowserSwitchRequestCodes.PAYPAL_CHECKOUT,
            orderId = "",
            payerId = "fake-payer-id"
        )
        every {
            browserSwitchClient.completeRequest(intent, "pending request")
        } returns browserSwitchResult

        sut = PayPalWebLauncher("custom_url_scheme", liveConfig, browserSwitchClient)
        val result = sut.completeCheckoutAuthRequest(intent, "pending request")
                as PayPalWebCheckoutFinishStartResult.Failure
        val expectedDescription =
            "Result did not contain the expected data. Payer ID or Order ID is null."
        assertEquals(expectedDescription, result.error.errorDescription)
    }

    @Test
    fun `completeCheckoutAuthRequest() parses checkout failure when metadata is null`() {
        val browserSwitchResult = createCheckoutSuccessBrowserSwitchResult(
            requestCode = BrowserSwitchRequestCodes.PAYPAL_CHECKOUT,
            payerId = "fake-payer-id",
            metadata = null
        )
        every {
            browserSwitchClient.completeRequest(intent, "pending request")
        } returns browserSwitchResult

        sut = PayPalWebLauncher("custom_url_scheme", liveConfig, browserSwitchClient)
        val result = sut.completeCheckoutAuthRequest(intent, "pending request")
                as PayPalWebCheckoutFinishStartResult.Failure
        val expectedDescription =
            "An unknown error occurred. Contact developer.paypal.com/support."
        assertEquals(expectedDescription, result.error.errorDescription)
    }

    @Test
    fun `completeVaultAuthRequest() parses successful vault result`() {
        val browserSwitchResult = createVaultSuccessBrowserSwitchResult(
            requestCode = BrowserSwitchRequestCodes.PAYPAL_VAULT,
            setupTokenId = "fake-setup-token-id",
            approvalSessionId = "fake-approval-session-id",
        )
        every {
            browserSwitchClient.completeRequest(intent, "pending request")
        } returns browserSwitchResult

        sut = PayPalWebLauncher("custom_url_scheme", liveConfig, browserSwitchClient)
        val result = sut.completeVaultAuthRequest(intent, "pending request")
                as PayPalWebCheckoutFinishVaultResult.Success
        assertEquals("fake-approval-session-id", result.approvalSessionId)
    }

    @Test
    fun `completeVaultAuthRequest() parses vault failure when approval session id is blank`() {
        val browserSwitchResult = createVaultSuccessBrowserSwitchResult(
            requestCode = BrowserSwitchRequestCodes.PAYPAL_VAULT,
            setupTokenId = "fake-setup-token-id",
            approvalSessionId = "",
        )
        every {
            browserSwitchClient.completeRequest(intent, "pending request")
        } returns browserSwitchResult

        sut = PayPalWebLauncher("custom_url_scheme", liveConfig, browserSwitchClient)
        val result = sut.completeVaultAuthRequest(intent, "pending request")
                as PayPalWebCheckoutFinishVaultResult.Failure
        val expectedDescription =
            "Result did not contain the expected data. Payer ID or Order ID is null."
        assertEquals(expectedDescription, result.error.errorDescription)
    }

    private fun createCheckoutMetadata(orderId: String) = JSONObject()
        .put("order_id", orderId)

    private fun createCheckoutDeepLinkUrl(payerId: String) =
        Uri.parse("http://testurl.com/checkout?PayerID=$payerId")

    private fun createCheckoutSuccessBrowserSwitchResult(
        requestCode: Int,
        orderId: String? = null,
        payerId: String? = null,
        metadata: JSONObject? = createCheckoutMetadata(orderId!!),
        deepLinkUrl: Uri = createCheckoutDeepLinkUrl(payerId!!)
    ) = createBrowserSwitchSuccessFinalResult(requestCode, metadata, deepLinkUrl)

    private fun createVaultMetadata(setupTokenId: String) = JSONObject()
        .put("setup_token_id", setupTokenId)

    private fun createVaultDeepLinkUrl(approvalSessionId: String) =
        Uri.parse("http://testurl.com/checkout?approval_session_id=$approvalSessionId")

    private fun createVaultSuccessBrowserSwitchResult(
        requestCode: Int,
        setupTokenId: String? = null,
        approvalSessionId: String? = null,
        metadata: JSONObject? = createVaultMetadata(setupTokenId!!),
        deepLinkUrl: Uri = createVaultDeepLinkUrl(approvalSessionId!!)
    ) = createBrowserSwitchSuccessFinalResult(requestCode, metadata, deepLinkUrl)

    private fun createBrowserSwitchSuccessFinalResult(
        requestCode: Int,
        metadata: JSONObject?,
        deepLinkUrl: Uri
    ): BrowserSwitchFinalResult.Success {
        val finalResult = mockk<BrowserSwitchFinalResult.Success>(relaxed = true)
        every { finalResult.returnUrl } returns deepLinkUrl
        every { finalResult.requestMetadata } returns metadata
        every { finalResult.requestCode } returns requestCode
        every { finalResult.requestUrl } returns Uri.parse("https://example.com/url")
        return finalResult
    }
}
