package com.paypal.android.corepayments.browserswitch

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.browser.auth.AuthTabIntent
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController

@RunWith(RobolectricTestRunner::class)
class AuthTabLauncherUnitTest {

    private val application: Application = ApplicationProvider.getApplicationContext()
    private lateinit var lifecycleCallbacks: AuthTabLauncherLifecycleCallbacks
    private lateinit var client: AuthTabClient
    private lateinit var controller: ActivityController<AuthTabHostActivity>

    private val options = BrowserSwitchOptions(
        targetUri = "https://example.com/checkout".toUri(),
        requestCode = 123,
        returnUrlScheme = "merchant.app",
        appLinkUrl = null,
        launchMode = BrowserSwitchLaunchMode.AUTH_TAB,
    )

    @Before
    fun beforeEach() {
        lifecycleCallbacks = AuthTabLauncherLifecycleCallbacks()
        lifecycleCallbacks.initialize(application)
        client = AuthTabClient()
        controller = Robolectric.buildActivity(AuthTabHostActivity::class.java).setup()
    }

    @After
    fun afterEach() {
        lifecycleCallbacks.dispose()
        controller.pause().stop().destroy()
    }

    @Test
    fun `launch uses the Activity-owned contract and returns success to the manifest receiver`() {
        val activity = controller.get()

        assertEquals(LaunchAuthTabResult.Success, client.launch(activity, options))
        val authTabLaunch = shadowOf(activity).nextStartedActivityForResult
        assertEquals(options.targetUri, authTabLaunch.intent.data)
        assertEquals(
            options.returnUrlScheme,
            authTabLaunch.intent.getStringExtra(AuthTabIntent.EXTRA_REDIRECT_SCHEME),
        )

        val successUri = "merchant.app://x-callback-url/paypal-sdk/paypal-checkout/success".toUri()
        val wasDelivered = activity.activityResultRegistry.dispatchResult(
            authTabLaunch.requestCode,
            Activity.RESULT_OK,
            Intent().setData(successUri),
        )

        assertTrue(wasDelivered)
        val returnIntent = shadowOf(application).nextStartedActivity
        assertEquals(Intent.ACTION_VIEW, returnIntent.action)
        assertEquals(successUri, returnIntent.data)
        assertEquals(activity.packageName, returnIntent.`package`)
        assertNull(returnIntent.component)
        assertEquals(
            AuthTabIntent.RESULT_OK,
            returnIntent.getIntExtra(
                AuthTabClient.EXTRA_AUTH_TAB_RESULT_CODE,
                AuthTabIntent.RESULT_UNKNOWN_CODE,
            ),
        )
        assertNotNull(AuthTabClient.restoredBrowserSwitchState(returnIntent))
    }

    @Test
    fun `a recreated Activity receives an in-flight result through the same registry key`() {
        val originalActivity = controller.get()
        assertEquals(LaunchAuthTabResult.Success, client.launch(originalActivity, options))
        val requestCode = shadowOf(originalActivity).nextStartedActivityForResult.requestCode
        shadowOf(application).nextStartedActivity
        val savedState = Bundle()
        controller.pause().saveInstanceState(savedState).stop().destroy()

        lifecycleCallbacks.dispose()
        lifecycleCallbacks = AuthTabLauncherLifecycleCallbacks().also {
            it.initialize(application)
        }
        controller = Robolectric.buildActivity(AuthTabHostActivity::class.java).create(savedState)
        val restoredActivity = controller.get()
        assertEquals(Lifecycle.State.CREATED, restoredActivity.lifecycle.currentState)

        val wasDelivered = restoredActivity.activityResultRegistry.dispatchResult(
            requestCode,
            Activity.RESULT_CANCELED,
            null,
        )

        assertTrue(wasDelivered)
        assertNull(shadowOf(application).nextStartedActivity)
        controller.start().resume()
        val returnIntent = shadowOf(application).nextStartedActivity
        assertEquals(
            "merchant.app://x-callback-url/paypal-sdk/paypal-checkout/cancel".toUri(),
            returnIntent.data,
        )
        assertEquals(
            AuthTabIntent.RESULT_CANCELED,
            returnIntent.getIntExtra(
                AuthTabClient.EXTRA_AUTH_TAB_RESULT_CODE,
                AuthTabIntent.RESULT_UNKNOWN_CODE,
            ),
        )
        val restoredBrowserSwitchState = AuthTabClient.restoredBrowserSwitchState(returnIntent)
        assertNotNull(restoredBrowserSwitchState)
        val restoredOptions = BrowserSwitchPendingState
            .fromBase64(requireNotNull(restoredBrowserSwitchState))
            ?.originalOptions
        assertEquals(Uri.EMPTY, restoredOptions?.targetUri)
        assertEquals(options.requestCode, restoredOptions?.requestCode)
        assertEquals(options.returnUrlScheme, restoredOptions?.returnUrlScheme)
        assertEquals(options.launchMode, restoredOptions?.launchMode)
    }

    @Test
    fun `a second launch fails while the first Auth Tab is pending`() {
        val activity = controller.get()
        assertEquals(LaunchAuthTabResult.Success, client.launch(activity, options))

        val result = client.launch(activity, options)

        assertTrue(result is LaunchAuthTabResult.Failure)
        assertEquals(
            "An Auth Tab is already pending for this Activity.",
            (result as LaunchAuthTabResult.Failure).error.message,
        )
    }

    class AuthTabHostActivity : ComponentActivity()
}
