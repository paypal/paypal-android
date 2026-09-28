package com.paypal.android.corepayments.browserswitch

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.annotation.RestrictTo
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class AuthTabClient internal constructor() {

    @Suppress("ReturnCount") // Keeps validation and launcher failures local to this call.
    fun launch(
        activity: ComponentActivity,
        options: BrowserSwitchOptions,
    ): LaunchAuthTabResult {
        val returnUrlScheme = options.returnUrlScheme
        val appLinkUri = if (returnUrlScheme == null) options.appLinkUrl?.toUri() else null
        val appLinkHost = appLinkUri?.host
        val validationError = when {
            returnUrlScheme == null && appLinkUri == null ->
                "Auth Tab requires a return URL scheme or App Link."
            returnUrlScheme == null && appLinkHost == null ->
                "Auth Tab App Link must include a host."
            else -> null
        }
        if (validationError != null) {
            return LaunchAuthTabResult.Failure(
                IllegalArgumentException(validationError)
            )
        }

        val launcher = AuthTabLauncher.from(activity)
            ?: attachBeforeStarted(activity)
            ?: return LaunchAuthTabResult.Failure(
                IllegalStateException(
                    "Auth Tab launcher was not registered before the host Activity reached STARTED."
                )
            )
        return try {
            launcher.launch(options)
            LaunchAuthTabResult.Success
        } catch (_: ActivityNotFoundException) {
            LaunchAuthTabResult.ActivityNotFound
        } catch (error: IllegalStateException) {
            LaunchAuthTabResult.Failure(error)
        } catch (error: SecurityException) {
            LaunchAuthTabResult.Failure(error)
        }
    }

    private fun attachBeforeStarted(activity: ComponentActivity): AuthTabLauncher? =
        if (!activity.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            AuthTabLauncher.attach(activity)
        } else {
            null
        }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        internal const val EXTRA_AUTH_TAB_RESULT_CODE =
            "com.paypal.android.corepayments.extra.AUTH_TAB_RESULT_CODE"
        internal const val EXTRA_BROWSER_SWITCH_STATE =
            "com.paypal.android.corepayments.extra.AUTH_TAB_BROWSER_SWITCH_STATE"

        fun restoredBrowserSwitchState(intent: Intent): String? =
            intent.takeIf { it.hasExtra(EXTRA_AUTH_TAB_RESULT_CODE) }
                ?.getStringExtra(EXTRA_BROWSER_SWITCH_STATE)
                ?.takeIf { encodedState ->
                    BrowserSwitchPendingState.fromBase64(encodedState)
                        ?.originalOptions
                        ?.launchMode == BrowserSwitchLaunchMode.AUTH_TAB
                }

        fun clearRestoredBrowserSwitchState(intent: Intent) {
            intent.removeExtra(EXTRA_BROWSER_SWITCH_STATE)
        }
    }
}
