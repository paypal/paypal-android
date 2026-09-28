package com.paypal.android.corepayments.browserswitch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
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
        var launchSucceeded = false
        try {
            launcher.launch(options)
            launchSucceeded = true
        } finally {
            if (!launchSucceeded) {
                pendingRequest = null
            }
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
