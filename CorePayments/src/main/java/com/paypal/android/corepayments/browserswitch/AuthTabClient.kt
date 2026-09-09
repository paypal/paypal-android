package com.paypal.android.corepayments.browserswitch

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RestrictTo
import androidx.browser.auth.AuthTabIntent
import androidx.core.net.toUri

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class AuthTabClient internal constructor(
    private val authTabRegistry: AuthTabRegistry = AuthTabRegistry.shared,
) {

    @Suppress("ReturnCount") // Keeps each catch local to the registry registration call.
    fun launch(
        activity: ComponentActivity,
        options: BrowserSwitchOptions,
    ): LaunchAuthTabResult {
        authTabRegistry.initialize(activity.application)
        val launcher = try {
            authTabRegistry.register(activity, options)
        } catch (error: IllegalArgumentException) {
            authTabRegistry.cancel(activity)
            return LaunchAuthTabResult.Failure(error)
        } catch (error: IllegalStateException) {
            authTabRegistry.cancel(activity)
            return LaunchAuthTabResult.Failure(error)
        }
        return launchRegistered(activity, options, launcher)
    }

    private fun launchRegistered(
        activity: ComponentActivity,
        options: BrowserSwitchOptions,
        launcher: ActivityResultLauncher<Intent>,
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
            authTabRegistry.cancel(activity)
            return LaunchAuthTabResult.Failure(
                IllegalArgumentException(validationError)
            )
        }

        val authTabIntent = AuthTabIntent.Builder().build()
        return try {
            if (returnUrlScheme != null) {
                authTabIntent.launch(launcher, options.targetUri, returnUrlScheme)
            } else {
                authTabIntent.launch(
                    launcher,
                    options.targetUri,
                    appLinkHost.orEmpty(),
                    appLinkUri?.path.orEmpty(),
                )
            }
            LaunchAuthTabResult.Success
        } catch (_: ActivityNotFoundException) {
            authTabRegistry.cancel(activity)
            LaunchAuthTabResult.ActivityNotFound
        } catch (error: IllegalStateException) {
            authTabRegistry.cancel(activity)
            LaunchAuthTabResult.Failure(error)
        } catch (error: SecurityException) {
            authTabRegistry.cancel(activity)
            LaunchAuthTabResult.Failure(error)
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        internal const val EXTRA_AUTH_TAB_RESULT_CODE =
            "com.paypal.android.corepayments.extra.AUTH_TAB_RESULT_CODE"
        internal const val EXTRA_BROWSER_SWITCH_STATE =
            "com.paypal.android.corepayments.extra.AUTH_TAB_BROWSER_SWITCH_STATE"

        internal const val COMPONENT_ACTIVITY_REQUIRED_MESSAGE =
            "PayPal Auth Tab requires the Activity passed to start() or vault() to extend " +
                "androidx.activity.ComponentActivity (including FragmentActivity and " +
                "AppCompatActivity); plain android.app.Activity is not supported."

        fun requireCompatibleActivity(activity: Activity): ComponentActivity {
            if (activity !is ComponentActivity) {
                throw IllegalStateException(COMPONENT_ACTIVITY_REQUIRED_MESSAGE)
            }
            return activity
        }

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
