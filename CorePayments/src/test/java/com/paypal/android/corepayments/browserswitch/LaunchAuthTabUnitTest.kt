package com.paypal.android.corepayments.browserswitch

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.browser.auth.AuthTabIntent
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LaunchAuthTabUnitTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val sut = LaunchAuthTab()

    @Test
    fun `createIntent configures a custom scheme Auth Tab`() {
        val options = BrowserSwitchOptions(
            targetUri = "https://example.com/checkout".toUri(),
            requestCode = 123,
            returnUrlScheme = "merchant.app",
            appLinkUrl = null,
            launchMode = BrowserSwitchLaunchMode.AUTH_TAB,
        )

        val intent = sut.createIntent(context, options)

        assertEquals(options.targetUri, intent.data)
        assertEquals(
            options.returnUrlScheme,
            intent.getStringExtra(AuthTabIntent.EXTRA_REDIRECT_SCHEME),
        )
        assertNull(intent.getStringExtra(AuthTabIntent.EXTRA_HTTPS_REDIRECT_HOST))
    }

    @Test
    fun `createIntent configures an App Link Auth Tab`() {
        val options = BrowserSwitchOptions(
            targetUri = "https://example.com/checkout".toUri(),
            requestCode = 123,
            returnUrlScheme = null,
            appLinkUrl = "https://merchant.example/checkout/return",
            launchMode = BrowserSwitchLaunchMode.AUTH_TAB,
        )

        val intent = sut.createIntent(context, options)

        assertEquals(
            "merchant.example",
            intent.getStringExtra(AuthTabIntent.EXTRA_HTTPS_REDIRECT_HOST),
        )
        assertEquals(
            "/checkout/return",
            intent.getStringExtra(AuthTabIntent.EXTRA_HTTPS_REDIRECT_PATH),
        )
        assertNull(intent.getStringExtra(AuthTabIntent.EXTRA_REDIRECT_SCHEME))
    }

    @Test
    fun `parseResult preserves supported result codes and success URI`() {
        val successUri = "merchant.app://checkout/success".toUri()

        val success = sut.parseResult(Activity.RESULT_OK, Intent().setData(successUri))
        val canceled = sut.parseResult(Activity.RESULT_CANCELED, null)

        assertEquals(AuthTabResult(AuthTabIntent.RESULT_OK, successUri), success)
        assertEquals(AuthTabResult(AuthTabIntent.RESULT_CANCELED, null), canceled)
    }

    @Test
    fun `parseResult preserves verification failures without a redirect URI`() {
        val resultIntent = Intent().setData("merchant.app://ignored".toUri())

        listOf(
            AuthTabIntent.RESULT_VERIFICATION_FAILED,
            AuthTabIntent.RESULT_VERIFICATION_TIMED_OUT,
        ).forEach { resultCode ->
            assertEquals(
                AuthTabResult(resultCode, null),
                sut.parseResult(resultCode, resultIntent),
            )
        }
    }

    @Test
    fun `parseResult normalizes unsupported result code`() {
        val result = sut.parseResult(42, Intent().setData("merchant.app://ignored".toUri()))

        assertEquals(AuthTabResult(AuthTabIntent.RESULT_UNKNOWN_CODE, null), result)
    }
}
