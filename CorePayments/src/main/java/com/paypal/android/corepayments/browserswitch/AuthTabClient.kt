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

    fun launch(
        activity: ComponentActivity,
        options: BrowserSwitchOptions,
    ): LaunchAuthTabResult {
        authTabRegistry.initialize(activity.application)
        val registration = try {
            Result.success(authTabRegistry.register(activity, options))
        } catch (error: IllegalArgumentException) {
            Result.failure(error)
        } catch (error: IllegalStateException) {
            Result.failure(error)
        }

        return registration.fold(
            onSuccess = { launcher -> launchRegistered(activity, options, launcher) },
            onFailure = { error ->
                authTabRegistry.cancel(activity)
                LaunchAuthTabResult.Failure(error as Exception)
            },
        )
    }

    private fun launchRegistered(
        activity: ComponentActivity,
        options: BrowserSwitchOptions,
        launcher: ActivityResultLauncher<Intent>,
    ): LaunchAuthTabResult = try {
        val authTabIntent = AuthTabIntent.Builder().build()
        val returnUrlScheme = options.returnUrlScheme
        if (returnUrlScheme != null) {
            authTabIntent.launch(launcher, options.targetUri, returnUrlScheme)
        } else {
            val appLinkUri = requireNotNull(options.appLinkUrl) {
                "Auth Tab requires a return URL scheme or App Link."
            }.toUri()
            val appLinkHost = requireNotNull(appLinkUri.host) {
                "Auth Tab App Link must include a host."
            }
            authTabIntent.launch(
                launcher,
                options.targetUri,
                appLinkHost,
                appLinkUri.path.orEmpty(),
            )
        }
        LaunchAuthTabResult.Success
    } catch (_: ActivityNotFoundException) {
        authTabRegistry.cancel(activity)
        LaunchAuthTabResult.ActivityNotFound
    } catch (error: IllegalArgumentException) {
        authTabRegistry.cancel(activity)
        LaunchAuthTabResult.Failure(error)
    } catch (error: IllegalStateException) {
        authTabRegistry.cancel(activity)
        LaunchAuthTabResult.Failure(error)
    } catch (error: SecurityException) {
        authTabRegistry.cancel(activity)
        LaunchAuthTabResult.Failure(error)
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

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed class LaunchAuthTabResult {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data object Success : LaunchAuthTabResult()

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data object ActivityNotFound : LaunchAuthTabResult()

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class Failure(val error: Exception) : LaunchAuthTabResult()
}
