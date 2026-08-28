package com.paypal.android.corepayments

import android.content.Intent
import androidx.browser.auth.AuthTabIntent
import androidx.core.net.toUri
import com.paypal.android.corepayments.browserswitch.AuthTabActivity
import com.paypal.android.corepayments.browserswitch.BrowserSwitchLaunchMode
import com.paypal.android.corepayments.browserswitch.BrowserSwitchOptions
import com.paypal.android.corepayments.browserswitch.BrowserSwitchPendingState
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DeepLinkUtilsUnitTest {

    private val options = BrowserSwitchOptions(
        targetUri = "https://example.com/checkout".toUri(),
        requestCode = 123,
        returnUrlScheme = "example.app",
        appLinkUrl = null,
        metadata = JSONObject().put("order_id", "order-123"),
        launchMode = BrowserSwitchLaunchMode.AUTH_TAB,
    )
    private val authState = BrowserSwitchPendingState(options).toBase64EncodedJSON()

    @Test
    fun `auth tab canceled result is captured without relying on uri shape`() {
        val intent = Intent()
            .setData("example.app://checkout".toUri())
            .putExtra(AuthTabActivity.EXTRA_AUTH_TAB_RESULT_CODE, AuthTabIntent.RESULT_CANCELED)

        val result = captureDeepLink(options.requestCode, intent, authState)

        assertTrue(result is CaptureDeepLinkResult.Canceled)
        val canceled = result as CaptureDeepLinkResult.Canceled
        assertEquals("order-123", canceled.originalOptions.metadata?.getString("order_id"))
        assertEquals(BrowserSwitchLaunchMode.AUTH_TAB, canceled.originalOptions.launchMode)
    }

    @Test
    fun `auth tab successful result captures the redirect uri`() {
        val redirectUri = "example.app://checkout/success?PayerID=payer-123".toUri()
        val intent = Intent()
            .setData(redirectUri)
            .putExtra(AuthTabActivity.EXTRA_AUTH_TAB_RESULT_CODE, AuthTabIntent.RESULT_OK)

        val result = captureDeepLink(options.requestCode, intent, authState)

        assertTrue(result is CaptureDeepLinkResult.Success)
        assertEquals(redirectUri, (result as CaptureDeepLinkResult.Success).deepLink.uri)
    }

    @Test
    fun `auth tab verification failure is captured as failure`() {
        val intent = Intent()
            .setData("example.app://checkout".toUri())
            .putExtra(
                AuthTabActivity.EXTRA_AUTH_TAB_RESULT_CODE,
                AuthTabIntent.RESULT_VERIFICATION_FAILED,
            )

        val result = captureDeepLink(options.requestCode, intent, authState)

        assertTrue(result is CaptureDeepLinkResult.Failure)
        assertEquals(
            "Auth Tab failed with result code ${AuthTabIntent.RESULT_VERIFICATION_FAILED}.",
            (result as CaptureDeepLinkResult.Failure).reason.errorDescription,
        )
    }
}
