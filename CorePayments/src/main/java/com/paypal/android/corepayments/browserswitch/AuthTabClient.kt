package com.paypal.android.corepayments.browserswitch

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.annotation.MainThread
import androidx.annotation.RestrictTo
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class AuthTabClient internal constructor() {

    @MainThread
    fun launch(
        activity: ComponentActivity,
        options: BrowserSwitchOptions,
    ): LaunchAuthTabResult {
        val precondition = validationFailure(activity, options)
        return precondition ?: try {
            AuthTabLauncher(AuthTabResultDispatcher(activity.applicationContext))
                .launch(activity, options)
            LaunchAuthTabResult.Success
        } catch (_: ActivityNotFoundException) {
            LaunchAuthTabResult.ActivityNotFound
        } catch (error: IllegalStateException) {
            LaunchAuthTabResult.Failure(error)
        } catch (error: SecurityException) {
            LaunchAuthTabResult.Failure(error)
        }
    }

    /**
     * Validates the launch request and the host's lifecycle state, returning a single failure (or
     * `null` when everything is fine) so [launch] only needs one `return`.
     */
    private fun validationFailure(
        activity: ComponentActivity,
        options: BrowserSwitchOptions,
    ): LaunchAuthTabResult.Failure? {
        val returnUrlScheme = options.returnUrlScheme
        val appLinkUri = if (returnUrlScheme == null) options.appLinkUrl?.toUri() else null
        val appLinkHost = appLinkUri?.host
        val error: Exception? = when {
            returnUrlScheme == null && appLinkUri == null ->
                IllegalArgumentException("Auth Tab requires a return URL scheme or App Link.")
            returnUrlScheme == null && appLinkHost == null ->
                IllegalArgumentException("Auth Tab App Link must include a host.")
            // A host that has already stopped (e.g. backgrounded while the SDK awaited an async
            // shopper session) cannot safely register a launcher or arm recreation; fail gracefully
            // instead of attempting a doomed registration.
            !activity.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) ->
                IllegalStateException("Auth Tab host Activity is not started; cannot launch.")
            else -> null
        }
        return error?.let { LaunchAuthTabResult.Failure(it) }
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
