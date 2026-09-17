package com.paypal.android.corepayments.browserswitch

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.browser.auth.AuthTabIntent
import com.paypal.android.corepayments.R

/**
 * Activity-owned Auth Tab launcher installed before the host reaches STARTED.
 *
 * The launcher is stored on the host's decor view, so the SDK does not need a process-wide map of
 * Activity references. Its Activity Result callback only captures this object and an application
 * context; AndroidX owns and removes the callback with the host Activity's lifecycle.
 */
internal class AuthTabLauncher private constructor(
    private val resultDispatcher: AuthTabResultDispatcher,
    private var pendingRequest: PendingRequest?,
) {

    private lateinit var launcher: ActivityResultLauncher<BrowserSwitchOptions>

    fun launch(options: BrowserSwitchOptions) {
        check(pendingRequest == null) {
            "An Auth Tab is already pending for this Activity."
        }

        pendingRequest = PendingRequest.from(options)
        try {
            launcher.launch(options)
        } catch (error: RuntimeException) {
            pendingRequest = null
            throw error
        }
    }

    private fun handleResult(result: AuthTabResult) {
        val request = pendingRequest ?: return
        try {
            resultDispatcher.dispatch(request, result)
        } finally {
            pendingRequest = null
        }
    }

    private fun saveState(): Bundle = pendingRequest?.toBundle() ?: Bundle()

    companion object {
        private const val REGISTRY_KEY =
            "com.paypal.android.corepayments.browserswitch.AUTH_TAB"
        private const val SAVED_STATE_KEY =
            "com.paypal.android.corepayments.browserswitch.AUTH_TAB_STATE"

        fun attach(activity: ComponentActivity): AuthTabLauncher {
            from(activity)?.let { return it }

            val restoredRequest = activity.savedStateRegistry
                .consumeRestoredStateForKey(SAVED_STATE_KEY)
                ?.let(PendingRequest::from)
            val authTabLauncher = AuthTabLauncher(
                resultDispatcher = AuthTabResultDispatcher(activity.applicationContext),
                pendingRequest = restoredRequest,
            )
            activity.savedStateRegistry.registerSavedStateProvider(SAVED_STATE_KEY) {
                authTabLauncher.saveState()
            }
            authTabLauncher.launcher = activity.activityResultRegistry.register(
                REGISTRY_KEY,
                activity,
                LaunchAuthTab(),
                authTabLauncher::handleResult,
            )
            activity.window.decorView.setTag(R.id.paypal_auth_tab_launcher, authTabLauncher)
            return authTabLauncher
        }

        fun from(activity: ComponentActivity): AuthTabLauncher? =
            activity.window.decorView.getTag(R.id.paypal_auth_tab_launcher) as? AuthTabLauncher
    }
}

/** Sends the Auth Tab result through the merchant's existing deep-link receiver. */
internal class AuthTabResultDispatcher(private val applicationContext: Context) {

    fun dispatch(pendingRequest: PendingRequest, result: AuthTabResult) {
        val resultCode = if (
            result.resultCode == AuthTabIntent.RESULT_OK && result.resultUri == null
        ) {
            AuthTabIntent.RESULT_UNKNOWN_CODE
        } else {
            result.resultCode
        }
        val resultUri = result.resultUri ?: pendingRequest.fallbackResultUri ?: return
        val returnIntent = Intent(Intent.ACTION_VIEW, resultUri).apply {
            // Keep manifest resolution so the merchant's registered receiver is selected, while
            // preventing another app from intercepting the result.
            setPackage(applicationContext.packageName)
            putExtra(AuthTabClient.EXTRA_AUTH_TAB_RESULT_CODE, resultCode)
            putExtra(
                AuthTabClient.EXTRA_BROWSER_SWITCH_STATE,
                pendingRequest.encodedBrowserSwitchState,
            )
        }
        val flags = PendingIntent.FLAG_CANCEL_CURRENT or
            PendingIntent.FLAG_ONE_SHOT or
            PendingIntent.FLAG_IMMUTABLE
        PendingIntent.getActivity(applicationContext, 0, returnIntent, flags).send()
    }
}
