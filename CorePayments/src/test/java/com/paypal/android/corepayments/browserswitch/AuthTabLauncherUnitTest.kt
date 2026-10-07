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
import io.mockk.every
import io.mockk.mockk
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

/**
 * Covers the call-scoped [AuthTabLauncher]/[AuthTabRecreator] design: registration happens directly on
 * the merchant's own [ComponentActivity] at checkout time (no SDK-owned Activity, no ContentProvider),
 * is discarded on every terminal path, and survives recreation only via a host-local
 * `SavedStateRegistry.AutoRecreated` hook.
 */
@RunWith(RobolectricTestRunner::class)
class AuthTabLauncherUnitTest {

    private val application: Application = ApplicationProvider.getApplicationContext()
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
        client = AuthTabClient()
        controller = Robolectric.buildActivity(AuthTabHostActivity::class.java).setup()
    }

    @After
    fun afterEach() {
        controller.pause().stop().destroy()
    }

    @Test
    fun `launch registers directly on the host Activity and returns success to the manifest receiver`() {
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
    fun `a second checkout on the same Activity succeeds after the first result is delivered`() {
        val activity = controller.get()
        assertEquals(LaunchAuthTabResult.Success, client.launch(activity, options))
        val firstRequestCode = shadowOf(activity).nextStartedActivityForResult.requestCode
        shadowOf(application).nextStartedActivity // drain the original Auth Tab launch intent
        activity.activityResultRegistry.dispatchResult(
            firstRequestCode,
            Activity.RESULT_CANCELED,
            null,
        )
        shadowOf(application).nextStartedActivity // drain the first return intent

        val secondResult = client.launch(activity, options)

        assertEquals(LaunchAuthTabResult.Success, secondResult)
        assertNotNull(shadowOf(activity).nextStartedActivityForResult)
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

    @Test
    fun `launch fails gracefully when the host Activity is not started`() {
        val belowStartedActivity = Robolectric.buildActivity(AuthTabHostActivity::class.java).get()
        assertEquals(Lifecycle.State.INITIALIZED, belowStartedActivity.lifecycle.currentState)

        val result = client.launch(belowStartedActivity, options)

        assertTrue(result is LaunchAuthTabResult.Failure)
    }

    @Test
    fun `process recreation restores an in-flight result through the same registry key`() {
        val originalActivity = controller.get()
        assertEquals(LaunchAuthTabResult.Success, client.launch(originalActivity, options))
        val requestCode = shadowOf(originalActivity).nextStartedActivityForResult.requestCode
        shadowOf(application).nextStartedActivity // drain, nothing should be here yet anyway

        val savedState = Bundle()
        controller.pause().saveInstanceState(savedState).stop().destroy()

        controller = Robolectric.buildActivity(AuthTabHostActivity::class.java).create(savedState)
        val restoredActivity = controller.get()
        assertEquals(Lifecycle.State.CREATED, restoredActivity.lifecycle.currentState)

        val wasDelivered = restoredActivity.activityResultRegistry.dispatchResult(
            requestCode,
            Activity.RESULT_CANCELED,
            null,
        )

        assertTrue(wasDelivered)
        // Lifecycle-gated: nothing dispatched until the restored Activity reaches STARTED.
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
    fun `recreation with no pending request does not register a launcher`() {
        // No checkout was launched — nothing pending, but still exercise a raw recreation cycle to
        // confirm AuthTabRecreator is a strict no-op (no SavedStateProvider was ever registered, so
        // there is nothing to consume on restore).
        val savedState = Bundle()
        controller.pause().saveInstanceState(savedState).stop().destroy()

        controller = Robolectric.buildActivity(AuthTabHostActivity::class.java).create(savedState)
        controller.start().resume()
        val restoredActivity = controller.get()

        // A fresh checkout on the restored Activity must succeed normally — proving no stray
        // registration under REGISTRY_KEY survived from the no-op recreation path.
        assertEquals(LaunchAuthTabResult.Success, client.launch(restoredActivity, options))
    }

    @Test
    fun `recreation after a completed checkout leaves no stale pending request and allows a fresh checkout`() {
        val activity = controller.get()
        assertEquals(LaunchAuthTabResult.Success, client.launch(activity, options))
        val requestCode = shadowOf(activity).nextStartedActivityForResult.requestCode
        shadowOf(application).nextStartedActivity // drain the original Auth Tab launch intent
        // Complete the first checkout so pendingRequest/launcher are cleared, but recreation may
        // still be armed (runOnNextRecreation cannot be un-armed).
        activity.activityResultRegistry.dispatchResult(requestCode, Activity.RESULT_CANCELED, null)
        shadowOf(application).nextStartedActivity // drain the cancel return intent

        val savedState = Bundle()
        controller.pause().saveInstanceState(savedState).stop().destroy()
        controller = Robolectric.buildActivity(AuthTabHostActivity::class.java).create(savedState)
        controller.start().resume()
        val restoredActivity = controller.get()

        // No pending request existed at save time, so recreation must be a no-op: no return intent...
        assertNull(shadowOf(application).nextStartedActivity)
        // ...and no stray registration (launcher or SavedStateProvider) survived to block a new checkout.
        assertEquals(LaunchAuthTabResult.Success, client.launch(restoredActivity, options))
    }

    @Test
    fun `cleanup unregisters both the launcher and the saved state provider when dispatch throws`() {
        val activity = controller.get()
        val throwingDispatcher = mockk<AuthTabResultDispatcher>()
        every { throwingDispatcher.dispatch(any(), any()) } throws IllegalStateException("boom")
        val launcher = AuthTabLauncher(throwingDispatcher)
        launcher.launch(activity, options)
        val requestCode = shadowOf(activity).nextStartedActivityForResult.requestCode

        try {
            activity.activityResultRegistry.dispatchResult(requestCode, Activity.RESULT_CANCELED, null)
        } catch (_: IllegalStateException) {
            // Expected: dispatch() throws synchronously through dispatchResult(); cleanup still must
            // have run in the finally block before the exception propagated.
        }

        // Both the launcher and the SavedStateProvider must be unregistered despite the throw, so a
        // brand-new AuthTabLauncher can register cleanly on the same Activity.
        val secondLauncher = AuthTabLauncher(AuthTabResultDispatcher(activity.applicationContext))
        secondLauncher.launch(activity, options)
        assertNotNull(shadowOf(activity).nextStartedActivityForResult)
    }

    class AuthTabHostActivity : ComponentActivity()
}
