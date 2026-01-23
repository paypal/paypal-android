package com.paypal.android.paypalwebpayments

import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import com.paypal.android.corepayments.BrowserSwitchRequestCodes.PAYPAL_CHECKOUT
import com.paypal.android.corepayments.BrowserSwitchRequestCodes.PAYPAL_VAULT
import com.paypal.android.corepayments.browserswitch.BrowserSwitchClient
import com.paypal.android.corepayments.browserswitch.BrowserSwitchOptions
import com.paypal.android.corepayments.browserswitch.BrowserSwitchPendingState
import com.paypal.android.corepayments.browserswitch.BrowserSwitchStartResult
import com.paypal.android.corepayments.model.TokenType
import io.mockk.every
import io.mockk.mockk
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
    private val intent = Intent()

    @Before
    fun beforeEach() {
        browserSwitchClient = mockk(relaxed = true)
    }

    @Test
    fun `launchPayPalWebCheckout() browser switches to SANDBOX PayPal web checkout`() {
        sut = PayPalWebLauncher(browserSwitchClient)

        val slot = slot<BrowserSwitchOptions>()
        every {
            browserSwitchClient.start(activity, capture(slot))
        } returns BrowserSwitchStartResult.Success

        sut.launchWithUrl(
            activity,
            Uri.parse("https://www.sandbox.paypal.com/checkoutnow"),
            "fake-order-id",
            TokenType.ORDER_ID,
            "com.example.app"
        )

        val browserSwitchOptions = slot.captured
        expectThat(browserSwitchOptions) {
            get { metadata?.get("order_id") }.isEqualTo("fake-order-id")
            get { returnUrlScheme }.isEqualTo("com.example.app")
            get { targetUri }.isEqualTo(Uri.parse("https://www.sandbox.paypal.com/checkoutnow"))
            get { requestCode }.isEqualTo(PAYPAL_CHECKOUT)
        }
    }

    @Test
    fun `launchPayPalWebCheckout() browser switches to LIVE PayPal web checkout`() {
        sut = PayPalWebLauncher(browserSwitchClient)

        val slot = slot<BrowserSwitchOptions>()
        every {
            browserSwitchClient.start(activity, capture(slot))
        } returns BrowserSwitchStartResult.Success

        sut.launchWithUrl(
            activity,
            Uri.parse("https://www.paypal.com/checkoutnow"),
            "fake-order-id",
            TokenType.ORDER_ID,
            "com.example.app"
        )

        val browserSwitchOptions = slot.captured
        expectThat(browserSwitchOptions) {
            get { metadata?.get("order_id") }.isEqualTo("fake-order-id")
            get { returnUrlScheme }.isEqualTo("com.example.app")
            get { targetUri }.isEqualTo(Uri.parse("https://www.paypal.com/checkoutnow"))
            get { requestCode }.isEqualTo(PAYPAL_CHECKOUT)
        }
    }

    @Test
    fun `launchPayPalWebCheckout() browser switches to PayPal Credit web checkout`() {
        sut = PayPalWebLauncher(browserSwitchClient)

        val slot = slot<BrowserSwitchOptions>()
        every {
            browserSwitchClient.start(activity, capture(slot))
        } returns BrowserSwitchStartResult.Success

        sut.launchWithUrl(
            activity,
            Uri.parse("https://www.paypal.com/checkoutnow"),
            "fake-order-id",
            TokenType.ORDER_ID,
            "com.example.app"
        )

        val browserSwitchOptions = slot.captured
        expectThat(browserSwitchOptions) {
            get { metadata?.get("order_id") }.isEqualTo("fake-order-id")
            get { returnUrlScheme }.isEqualTo("com.example.app")
            get { targetUri }.isEqualTo(Uri.parse("https://www.paypal.com/checkoutnow"))
            get { requestCode }.isEqualTo(PAYPAL_CHECKOUT)
        }
    }

    @Test
    fun `launchPayPalWebCheckout() browser switches to PayPal Pay Later web checkout`() {
        sut = PayPalWebLauncher(browserSwitchClient)

        val slot = slot<BrowserSwitchOptions>()
        every {
            browserSwitchClient.start(activity, capture(slot))
        } returns BrowserSwitchStartResult.Success

        sut.launchWithUrl(
            activity,
            Uri.parse("https://www.paypal.com/checkoutnow"),
            "fake-order-id",
            TokenType.ORDER_ID,
            "com.example.app"
        )

        val browserSwitchOptions = slot.captured
        expectThat(browserSwitchOptions) {
            get { metadata?.get("order_id") }.isEqualTo("fake-order-id")
            get { returnUrlScheme }.isEqualTo("com.example.app")
            get { targetUri }.isEqualTo(Uri.parse("https://www.paypal.com/checkoutnow"))
            get { requestCode }.isEqualTo(PAYPAL_CHECKOUT)
        }
    }

    @Test
    fun `launchPayPalWebCheckout() returns an error when it cannot browser switch`() {
        sut = PayPalWebLauncher(browserSwitchClient)

        val browserSwitchError = Exception("error message from browser switch")
        every {
            browserSwitchClient.start(context = any(), options = any<BrowserSwitchOptions>())
        } returns BrowserSwitchStartResult.Failure(browserSwitchError)

        val result = sut.launchWithUrl(
            activity,
            Uri.parse("https://www.sandbox.paypal.com/checkoutnow"),
            "fake-order-id",
            TokenType.ORDER_ID,
            "com.example.app"
        )
                as PayPalPresentAuthChallengeResult.Failure
        assertEquals("error message from browser switch", result.error.errorDescription)
    }

    @Test
    fun `launchPayPalWebVault() browser switches to SANDBOX PayPal vault checkout`() {
        sut = PayPalWebLauncher(browserSwitchClient)

        val slot = slot<BrowserSwitchOptions>()
        every {
            browserSwitchClient.start(activity, capture(slot))
        } returns BrowserSwitchStartResult.Success

        sut.launchWithUrl(
            activity,
            Uri.parse("https://sandbox.paypal.com/agreements/approve"),
            "fake-setup-token",
            TokenType.VAULT_ID,
            "com.example.app"
        )

        val browserSwitchOptions = slot.captured
        expectThat(browserSwitchOptions) {
            get { metadata?.get("setup_token_id") }.isEqualTo("fake-setup-token")
            get { returnUrlScheme }.isEqualTo("com.example.app")
            get { targetUri }.isEqualTo(Uri.parse("https://sandbox.paypal.com/agreements/approve"))
            get { requestCode }.isEqualTo(PAYPAL_VAULT)
        }
    }

    @Test
    fun `launchPayPalWebVault() browser switches to LIVE PayPal vault checkout`() {
        sut = PayPalWebLauncher(browserSwitchClient)

        val slot = slot<BrowserSwitchOptions>()
        every {
            browserSwitchClient.start(activity, capture(slot))
        } returns BrowserSwitchStartResult.Success

        sut.launchWithUrl(
            activity,
            Uri.parse("https://paypal.com/agreements/approve"),
            "fake-setup-token",
            TokenType.VAULT_ID,
            "com.example.app"
        )

        val browserSwitchOptions = slot.captured
        expectThat(browserSwitchOptions) {
            get { metadata?.get("setup_token_id") }.isEqualTo("fake-setup-token")
            get { returnUrlScheme }.isEqualTo("com.example.app")
            get { targetUri }.isEqualTo(Uri.parse("https://paypal.com/agreements/approve"))
            get { requestCode }.isEqualTo(PAYPAL_VAULT)
        }
    }

    @Test
    fun `launchPayPalWebVault() returns an error when it cannot browser switch`() {
        sut = PayPalWebLauncher(browserSwitchClient)

        val browserSwitchError = Exception("error message from browser switch")
        every {
            browserSwitchClient.start(context = any(), options = any<BrowserSwitchOptions>())
        } returns BrowserSwitchStartResult.Failure(browserSwitchError)

        val result = sut.launchWithUrl(
            activity,
            Uri.parse("https://www.sandbox.paypal.com/checkoutnow"),
            "fake-order-id",
            TokenType.ORDER_ID,
            "com.example.app"
        )
                as PayPalPresentAuthChallengeResult.Failure
        assertEquals("error message from browser switch", result.error.errorDescription)
    }

    @Test
    fun `completeCheckoutAuthRequest() parses successful checkout result`() {
        val originalOptions = BrowserSwitchOptions(
            targetUri = "https://www.sandbox.paypal.com/checkoutnow".toUri(),
            requestCode = PAYPAL_CHECKOUT,
            returnUrlScheme = "com.example.app",
            appLinkUrl = null,
            metadata = createCheckoutMetadata("fake-order-id")
        )
        val authState = BrowserSwitchPendingState(originalOptions).toBase64EncodedJSON()
        intent.data = createCheckoutDeepLinkUrl("fake-payer-id")

        sut = PayPalWebLauncher(browserSwitchClient)
        val result = sut.completeCheckoutAuthRequest(intent, authState)
                as PayPalWebCheckoutFinishStartResult.Success
        assertEquals("fake-order-id", result.orderId)
        assertEquals("fake-payer-id", result.payerId)
    }

    @Test
    fun `completeCheckoutAuthRequest() parses checkout failure when Payer Id is blank`() {
        val originalOptions = BrowserSwitchOptions(
            targetUri = "https://www.sandbox.paypal.com/checkoutnow".toUri(),
            requestCode = PAYPAL_CHECKOUT,
            returnUrlScheme = "com.example.app",
            appLinkUrl = null,
            metadata = createCheckoutMetadata("fake-order-id")
        )
        val authState = BrowserSwitchPendingState(originalOptions).toBase64EncodedJSON()
        intent.data = createCheckoutDeepLinkUrl("")

        sut = PayPalWebLauncher(browserSwitchClient)
        val result = sut.completeCheckoutAuthRequest(intent, authState)
                as PayPalWebCheckoutFinishStartResult.Failure
        val expectedDescription =
            "Result did not contain the expected data. Payer ID or Order ID is null."
        assertEquals(expectedDescription, result.error.errorDescription)
    }

    @Test
    fun `completeCheckoutAuthRequest() parses checkout failure when Order Id is blank`() {
        val originalOptions = BrowserSwitchOptions(
            targetUri = "https://www.sandbox.paypal.com/checkoutnow".toUri(),
            requestCode = PAYPAL_CHECKOUT,
            returnUrlScheme = "com.example.app",
            appLinkUrl = null,
            metadata = createCheckoutMetadata("")
        )
        val authState = BrowserSwitchPendingState(originalOptions).toBase64EncodedJSON()
        intent.data = createCheckoutDeepLinkUrl("fake-payer-id")

        sut = PayPalWebLauncher(browserSwitchClient)
        val result = sut.completeCheckoutAuthRequest(intent, authState)
                as PayPalWebCheckoutFinishStartResult.Failure
        val expectedDescription =
            "Result did not contain the expected data. Payer ID or Order ID is null."
        assertEquals(expectedDescription, result.error.errorDescription)
    }

    @Test
    fun `completeCheckoutAuthRequest() parses checkout failure when metadata is null`() {
        val originalOptions = BrowserSwitchOptions(
            targetUri = "https://www.sandbox.paypal.com/checkoutnow".toUri(),
            requestCode = PAYPAL_CHECKOUT,
            returnUrlScheme = "com.example.app",
            appLinkUrl = null,
            metadata = null
        )
        val authState = BrowserSwitchPendingState(originalOptions).toBase64EncodedJSON()
        intent.data = createCheckoutDeepLinkUrl("fake-payer-id")

        sut = PayPalWebLauncher(browserSwitchClient)
        val result = sut.completeCheckoutAuthRequest(intent, authState)
                as PayPalWebCheckoutFinishStartResult.Failure
        val expectedDescription =
            "An unknown error occurred. Contact developer.paypal.com/support."
        assertEquals(expectedDescription, result.error.errorDescription)
    }

    @Test
    fun `completeCheckoutAuthRequest() parses checkout cancellation deep link url indicates failure`() {
        val originalOptions = BrowserSwitchOptions(
            targetUri = "https://www.sandbox.paypal.com/checkoutnow".toUri(),
            requestCode = PAYPAL_CHECKOUT,
            returnUrlScheme = "com.example.app",
            appLinkUrl = null,
            metadata = createCheckoutMetadata("fake-order-id")
        )
        val authState = BrowserSwitchPendingState(originalOptions).toBase64EncodedJSON()
        intent.data = "com.example.app://testurl.com/checkout?opType=cancel".toUri()

        sut = PayPalWebLauncher(browserSwitchClient)
        val result = sut.completeCheckoutAuthRequest(intent, authState)
        assertTrue(result is PayPalWebCheckoutFinishStartResult.Canceled)
    }

    @Test
    fun `completeVaultAuthRequest() parses successful vault result`() {
        val originalOptions = BrowserSwitchOptions(
            targetUri = "https://www.sandbox.paypal.com/checkoutnow".toUri(),
            requestCode = PAYPAL_VAULT,
            returnUrlScheme = "com.example.app",
            appLinkUrl = null,
            metadata = createVaultMetadata("fake-setup-token-id")
        )
        val authState = BrowserSwitchPendingState(originalOptions).toBase64EncodedJSON()
        intent.data = createVaultDeepLinkUrl("fake-approval-session-id")

        sut = PayPalWebLauncher(browserSwitchClient)
        val result = sut.completeVaultAuthRequest(intent, authState)
                as PayPalWebCheckoutFinishVaultResult.Success
        assertEquals("fake-approval-session-id", result.approvalSessionId)
    }

    @Test
    fun `completeVaultAuthRequest() parses cancellation vault result when deep link path contains the word cancel`() {
        val originalOptions = BrowserSwitchOptions(
            targetUri = "https://www.sandbox.paypal.com/checkoutnow".toUri(),
            requestCode = PAYPAL_VAULT,
            returnUrlScheme = "com.example.app",
            appLinkUrl = null,
            metadata = createVaultMetadata("fake-setup-token-id")
        )
        val authState = BrowserSwitchPendingState(originalOptions).toBase64EncodedJSON()
        intent.data =
            "com.example.app://testurl.com/checkout/cancel?approval_session_id=fake-approval-session-id".toUri()

        sut = PayPalWebLauncher(browserSwitchClient)
        val result = sut.completeVaultAuthRequest(intent, authState)
        assertTrue(result is PayPalWebCheckoutFinishVaultResult.Canceled)
    }

    @Test
    fun `completeVaultAuthRequest() parses vault failure when approval session id is blank`() {
        val originalOptions = BrowserSwitchOptions(
            targetUri = "https://www.sandbox.paypal.com/checkoutnow".toUri(),
            requestCode = PAYPAL_VAULT,
            returnUrlScheme = "com.example.app",
            appLinkUrl = null,
            metadata = createVaultMetadata("fake-setup-token-id")
        )
        val authState = BrowserSwitchPendingState(originalOptions).toBase64EncodedJSON()
        intent.data = createVaultDeepLinkUrl("")

        sut = PayPalWebLauncher(browserSwitchClient)
        val result = sut.completeVaultAuthRequest(intent, authState)
                as PayPalWebCheckoutFinishVaultResult.Failure
        val expectedDescription =
            "Result did not contain the expected data. Payer ID or Order ID is null."
        assertEquals(expectedDescription, result.error.errorDescription)
    }

    private fun createCheckoutMetadata(orderId: String) = JSONObject()
        .put("order_id", orderId)

    private fun createCheckoutDeepLinkUrl(payerId: String) =
        Uri.parse("com.example.app://testurl.com/checkout?PayerID=$payerId")

    private fun createVaultMetadata(setupTokenId: String) = JSONObject()
        .put("setup_token_id", setupTokenId)

    private fun createVaultDeepLinkUrl(approvalSessionId: String) =
        Uri.parse("com.example.app://testurl.com/checkout?approval_session_id=$approvalSessionId")

    // LAUNCH WITH URL TESTS

    @Test
    fun `launchWithUrl() launches with ORDER_ID token type and correct request code`() {
        sut = PayPalWebLauncher(browserSwitchClient)

        val slot = slot<BrowserSwitchOptions>()
        every {
            browserSwitchClient.start(activity, capture(slot))
        } returns BrowserSwitchStartResult.Success

        sut.launchWithUrl(
            activity,
            Uri.parse("https://paypal.com/app-switch"),
            "order-123",
            TokenType.ORDER_ID,
            "custom_url_scheme"
        )

        val browserSwitchOptions = slot.captured
        expectThat(browserSwitchOptions) {
            get { metadata?.get("order_id") }.isEqualTo("order-123")
            get { returnUrlScheme }.isEqualTo("custom_url_scheme")
            get { targetUri }.isEqualTo(Uri.parse("https://paypal.com/app-switch"))
            get { requestCode }.isEqualTo(PAYPAL_CHECKOUT)
        }
    }

    @Test
    fun `launchWithUrl() launches with VAULT_ID token type and correct request code`() {
        sut = PayPalWebLauncher(browserSwitchClient)

        val slot = slot<BrowserSwitchOptions>()
        every {
            browserSwitchClient.start(activity, capture(slot))
        } returns BrowserSwitchStartResult.Success

        sut.launchWithUrl(
            activity,
            Uri.parse("https://paypal.com/vault-switch"),
            "setup-456",
            TokenType.VAULT_ID,
            "custom_url_scheme"
        )

        val browserSwitchOptions = slot.captured
        expectThat(browserSwitchOptions) {
            get { metadata?.get("setup_token_id") }.isEqualTo("setup-456")
            get { returnUrlScheme }.isEqualTo("custom_url_scheme")
            get { targetUri }.isEqualTo(Uri.parse("https://paypal.com/vault-switch"))
            get { requestCode }.isEqualTo(PAYPAL_VAULT)
        }
    }

    @Test
    fun `launchWithUrl() returns error when browser switch fails`() {
        sut = PayPalWebLauncher(browserSwitchClient)

        val browserSwitchError = Exception("error message from browser switch")
        every {
            browserSwitchClient.start(context = any(), options = any<BrowserSwitchOptions>())
        } returns BrowserSwitchStartResult.Failure(browserSwitchError)

        val result =
            sut.launchWithUrl(
                activity,
                Uri.parse("https://test.com"),
                "token-123",
                TokenType.ORDER_ID,
                "custom_url_scheme"
            )
                    as PayPalPresentAuthChallengeResult.Failure
        assertEquals("error message from browser switch", result.error.errorDescription)
    }

    @Test
    fun `launchWithUrl() with appLinkUrl sets appLinkUri and returnUrlScheme`() {
        sut = PayPalWebLauncher(browserSwitchClient)

        val slot = slot<BrowserSwitchOptions>()
        every {
            browserSwitchClient.start(activity, capture(slot))
        } returns BrowserSwitchStartResult.Success

        val appLinkUrl = "https://example.com/return"
        sut.launchWithUrl(
            activity,
            Uri.parse("https://paypal.com/checkout"),
            "order-123",
            TokenType.ORDER_ID,
            "custom_url_scheme",
            appLinkUrl
        )

        val browserSwitchOptions = slot.captured
        expectThat(browserSwitchOptions) {
            get { metadata?.get("order_id") }.isEqualTo("order-123")
            get { returnUrlScheme }.isEqualTo("custom_url_scheme") // Should use provided returnUrlScheme as fallback
            get { targetUri }.isEqualTo(Uri.parse("https://paypal.com/checkout"))
            get { requestCode }.isEqualTo(PAYPAL_CHECKOUT)
            get { this.appLinkUrl }.isEqualTo(appLinkUrl)
        }
    }

    @Test
    fun `launchWithUrl() with null appLinkUrl uses returnUrlScheme`() {
        sut = PayPalWebLauncher(browserSwitchClient)

        val slot = slot<BrowserSwitchOptions>()
        every {
            browserSwitchClient.start(activity, capture(slot))
        } returns BrowserSwitchStartResult.Success

        sut.launchWithUrl(
            activity,
            Uri.parse("https://paypal.com/checkout"),
            "order-123",
            TokenType.ORDER_ID,
            "custom_url_scheme",
            null
        )

        val browserSwitchOptions = slot.captured
        expectThat(browserSwitchOptions) {
            get { metadata?.get("order_id") }.isEqualTo("order-123")
            get { returnUrlScheme }.isEqualTo("custom_url_scheme")
            get { targetUri }.isEqualTo(Uri.parse("https://paypal.com/checkout"))
            get { requestCode }.isEqualTo(PAYPAL_CHECKOUT)
            get { appLinkUrl }.isEqualTo(null)
        }
    }

    @Test
    fun `launchWithUrl() with vault token and appLinkUrl works correctly`() {
        sut = PayPalWebLauncher(browserSwitchClient)

        val slot = slot<BrowserSwitchOptions>()
        every {
            browserSwitchClient.start(activity, capture(slot))
        } returns BrowserSwitchStartResult.Success

        val appLinkUrl = "https://example.com/vault/return"
        sut.launchWithUrl(
            activity,
            Uri.parse("https://paypal.com/vault"),
            "setup-456",
            TokenType.VAULT_ID,
            "custom_url_scheme",
            appLinkUrl
        )

        val browserSwitchOptions = slot.captured
        expectThat(browserSwitchOptions) {
            get { metadata?.get("setup_token_id") }.isEqualTo("setup-456")
            get { returnUrlScheme }.isEqualTo("custom_url_scheme") // Should use provided returnUrlScheme as fallback
            get { targetUri }.isEqualTo(Uri.parse("https://paypal.com/vault"))
            get { requestCode }.isEqualTo(PAYPAL_VAULT)
            get { this.appLinkUrl }.isEqualTo(appLinkUrl)
        }
    }

    @Test
    fun `launchWithUrl() with default parameters maintains backward compatibility`() {
        sut = PayPalWebLauncher(browserSwitchClient)

        val slot = slot<BrowserSwitchOptions>()
        every {
            browserSwitchClient.start(activity, capture(slot))
        } returns BrowserSwitchStartResult.Success

        // Call with old signature (should default appLinkUrl to null)
        sut.launchWithUrl(
            activity,
            Uri.parse("https://paypal.com/checkout"),
            "order-123",
            TokenType.ORDER_ID,
            "custom_url_scheme"
        )

        val browserSwitchOptions = slot.captured
        expectThat(browserSwitchOptions) {
            get { metadata?.get("order_id") }.isEqualTo("order-123")
            get { returnUrlScheme }.isEqualTo("custom_url_scheme")
            get { targetUri }.isEqualTo(Uri.parse("https://paypal.com/checkout"))
            get { requestCode }.isEqualTo(PAYPAL_CHECKOUT)
            get { appLinkUrl }.isEqualTo(null)
        }
    }

    // ACTIVITY RESULT LAUNCHER TESTS

    @Test
    fun `launchWithUrl() with activity result launcher launches auth tab successfully`() {
        sut = PayPalWebLauncher(browserSwitchClient)

        val activityResultLauncher = mockk<ActivityResultLauncher<Intent>>(relaxed = true)
        val slot = slot<BrowserSwitchOptions>()
        every {
            browserSwitchClient.start(activityResultLauncher, capture(slot), any())
        } returns BrowserSwitchStartResult.Success

        val result = sut.launchWithUrl(
            uri = Uri.parse("https://paypal.com/checkout"),
            token = "order-123",
            tokenType = TokenType.ORDER_ID,
            activityResultLauncher = activityResultLauncher,
            returnUrlScheme = "com.example.app",
            appLinkUrl = "https://example.com/return",
            context = activity
        )

        assertTrue(result is PayPalPresentAuthChallengeResult.Success)
        val browserSwitchOptions = slot.captured
        expectThat(browserSwitchOptions) {
            get { metadata?.get("order_id") }.isEqualTo("order-123")
            get { returnUrlScheme }.isEqualTo("com.example.app")
            get { targetUri }.isEqualTo(Uri.parse("https://paypal.com/checkout"))
            get { requestCode }.isEqualTo(PAYPAL_CHECKOUT)
            get { appLinkUrl }.isEqualTo("https://example.com/return")
        }
    }

    @Test
    fun `launchWithUrl() with activity result launcher and vault token works correctly`() {
        sut = PayPalWebLauncher(browserSwitchClient)

        val activityResultLauncher = mockk<ActivityResultLauncher<Intent>>(relaxed = true)
        val slot = slot<BrowserSwitchOptions>()
        every {
            browserSwitchClient.start(activityResultLauncher, capture(slot), any())
        } returns BrowserSwitchStartResult.Success

        val result = sut.launchWithUrl(
            uri = Uri.parse("https://paypal.com/vault"),
            token = "setup-456",
            tokenType = TokenType.VAULT_ID,
            activityResultLauncher = activityResultLauncher,
            returnUrlScheme = "com.example.app",
            appLinkUrl = "https://example.com/vault/return",
            context = activity
        )

        assertTrue(result is PayPalPresentAuthChallengeResult.Success)
        val browserSwitchOptions = slot.captured
        expectThat(browserSwitchOptions) {
            get { metadata?.get("setup_token_id") }.isEqualTo("setup-456")
            get { returnUrlScheme }.isEqualTo("com.example.app")
            get { targetUri }.isEqualTo(Uri.parse("https://paypal.com/vault"))
            get { requestCode }.isEqualTo(PAYPAL_VAULT)
            get { appLinkUrl }.isEqualTo("https://example.com/vault/return")
        }
    }

    @Test
    fun `launchWithUrl() with activity result launcher returns error when browser switch fails`() {
        sut = PayPalWebLauncher(browserSwitchClient)

        val activityResultLauncher = mockk<ActivityResultLauncher<Intent>>(relaxed = true)
        val browserSwitchError = Exception("Auth tab launch failed")
        every {
            browserSwitchClient.start(activityResultLauncher, any(), any())
        } returns BrowserSwitchStartResult.Failure(browserSwitchError)

        val result = sut.launchWithUrl(
            uri = Uri.parse("https://paypal.com/checkout"),
            token = "order-123",
            tokenType = TokenType.ORDER_ID,
            activityResultLauncher = activityResultLauncher,
            returnUrlScheme = "com.example.app",
            appLinkUrl = null,
            context = activity
        ) as PayPalPresentAuthChallengeResult.Failure

        assertEquals("Auth tab launch failed", result.error.errorDescription)
    }

    @Test
    fun `launchWithUrl() with activity result launcher and null appLinkUrl uses returnUrlScheme`() {
        sut = PayPalWebLauncher(browserSwitchClient)

        val activityResultLauncher = mockk<ActivityResultLauncher<Intent>>(relaxed = true)
        val slot = slot<BrowserSwitchOptions>()
        every {
            browserSwitchClient.start(activityResultLauncher, capture(slot), any())
        } returns BrowserSwitchStartResult.Success

        val result = sut.launchWithUrl(
            uri = Uri.parse("https://paypal.com/checkout"),
            token = "order-123",
            tokenType = TokenType.ORDER_ID,
            activityResultLauncher = activityResultLauncher,
            returnUrlScheme = "com.example.app",
            appLinkUrl = null,
            context = activity
        )

        assertTrue(result is PayPalPresentAuthChallengeResult.Success)
        val browserSwitchOptions = slot.captured
        expectThat(browserSwitchOptions) {
            get { metadata?.get("order_id") }.isEqualTo("order-123")
            get { returnUrlScheme }.isEqualTo("com.example.app")
            get { targetUri }.isEqualTo(Uri.parse("https://paypal.com/checkout"))
            get { requestCode }.isEqualTo(PAYPAL_CHECKOUT)
            get { appLinkUrl }.isEqualTo(null)
        }
    }
}
