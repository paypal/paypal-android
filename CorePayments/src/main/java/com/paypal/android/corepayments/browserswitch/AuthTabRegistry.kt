package com.paypal.android.corepayments.browserswitch

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.browser.auth.AuthTabIntent
import androidx.core.net.toUri
import java.util.WeakHashMap

/**
 * Owns direct Auth Tab registrations on merchant [ComponentActivity] instances.
 *
 * The non-lifecycle-aware ActivityResultRegistry overload is intentional: checkout commonly starts
 * after the host Activity is RESUMED. The pending request is separately saved through the host's
 * SavedStateRegistry, then re-registered from [onActivityCreated] with the same registry key before
 * the replacement Activity reaches STARTED.
 */
@Suppress("TooManyFunctions") // ActivityLifecycleCallbacks contributes seven required callbacks.
internal class AuthTabRegistry(
    private val registryKey: String = REGISTRY_KEY,
    private val savedStateKey: String = SAVED_STATE_KEY,
) : Application.ActivityLifecycleCallbacks {

    private val activityStates = WeakHashMap<ComponentActivity, ActivityState>()
    private var registeredApplication: Application? = null

    @Synchronized
    fun initialize(application: Application) {
        if (registeredApplication === application) return

        registeredApplication?.unregisterActivityLifecycleCallbacks(this)
        application.registerActivityLifecycleCallbacks(this)
        registeredApplication = application
    }

    fun register(
        activity: ComponentActivity,
        options: BrowserSwitchOptions,
    ): ActivityResultLauncher<Intent> {
        val state = attach(activity)
        check(state.pendingRequest == null) {
            "An Auth Tab is already pending for this Activity."
        }

        val pendingRequest = PendingRequest.from(options)
        state.pendingRequest = pendingRequest
        return registerLauncher(activity, state, pendingRequest)
    }

    fun cancel(activity: ComponentActivity) {
        activityStates[activity]?.let(::clearPendingRequest)
    }

    fun dispose() {
        registeredApplication?.unregisterActivityLifecycleCallbacks(this)
        registeredApplication = null
        activityStates.values.forEach { state -> state.launcher?.unregister() }
        activityStates.clear()
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (activity is ComponentActivity) {
            attach(activity)
        }
    }

    override fun onActivityDestroyed(activity: Activity) {
        if (activity !is ComponentActivity) return

        val state = activityStates.remove(activity) ?: return
        state.launcher?.unregister()
        state.launcher = null

        if (!activity.isChangingConfigurations) {
            // A finished Activity will not be recreated with this SavedStateRegistry, so discard
            // its in-memory request. During a configuration change, the provider registered in
            // attach() has already copied the request into the replacement Activity's saved state.
            state.pendingRequest = null
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityResumed(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit

    private fun attach(activity: ComponentActivity): ActivityState {
        activityStates[activity]?.let { return it }

        val restoredRequest = activity.savedStateRegistry
            .consumeRestoredStateForKey(savedStateKey)
            ?.let(PendingRequest::from)
        val state = ActivityState(pendingRequest = restoredRequest)
        activityStates[activity] = state
        activity.savedStateRegistry.registerSavedStateProvider(savedStateKey) {
            state.pendingRequest?.toBundle() ?: Bundle()
        }

        if (restoredRequest != null) {
            registerLauncher(activity, state, restoredRequest)
        }
        return state
    }

    private fun registerLauncher(
        activity: ComponentActivity,
        state: ActivityState,
        pendingRequest: PendingRequest,
    ): ActivityResultLauncher<Intent> {
        val launcher = activity.activityResultRegistry.register(
            registryKey,
            AuthenticateUserResultContract(),
        ) { result ->
            if (state.pendingRequest == pendingRequest) {
                try {
                    handleResult(activity, pendingRequest, result)
                } finally {
                    clearPendingRequest(state)
                }
            }
        }

        // register() synchronously delivers a result that was pending in a restored registry. In
        // that case handleResult() has already cleared the request before a launcher is returned.
        if (state.pendingRequest == pendingRequest) {
            state.launcher = launcher
        } else {
            launcher.unregister()
        }
        return launcher
    }

    private fun handleResult(
        activity: ComponentActivity,
        pendingRequest: PendingRequest,
        result: AuthTabResult,
    ) {
        val resultCode = if (result.resultCode == AuthTabIntent.RESULT_OK && result.resultUri == null) {
            AuthTabIntent.RESULT_UNKNOWN_CODE
        } else {
            result.resultCode
        }
        val resultUri = result.resultUri ?: pendingRequest.fallbackResultUri
        if (resultUri != null) {
            val returnIntent = Intent(Intent.ACTION_VIEW, resultUri).apply {
                // Keep Android's manifest resolution so the merchant's actual registered return
                // receiver is selected, while preventing another app from intercepting the link.
                setPackage(activity.packageName)
                putExtra(AuthTabClient.EXTRA_AUTH_TAB_RESULT_CODE, resultCode)
                putExtra(
                    AuthTabClient.EXTRA_BROWSER_SWITCH_STATE,
                    pendingRequest.encodedBrowserSwitchState,
                )
            }
            activity.startActivity(returnIntent)
        }
    }

    private fun clearPendingRequest(state: ActivityState) {
        state.pendingRequest = null
        state.launcher?.unregister()
        state.launcher = null
    }

    private data class ActivityState(
        var pendingRequest: PendingRequest?,
        var launcher: ActivityResultLauncher<Intent>? = null,
    )

    private data class PendingRequest(
        val encodedBrowserSwitchState: String,
        val fallbackResultUri: Uri?,
    ) {
        fun toBundle() = Bundle().apply {
            putString(KEY_BROWSER_SWITCH_STATE, encodedBrowserSwitchState)
            putString(KEY_FALLBACK_RESULT_URI, fallbackResultUri?.toString())
        }

        companion object {
            fun from(options: BrowserSwitchOptions): PendingRequest {
                val fallbackResultUri = options.appLinkUrl?.toUri()
                    ?: options.returnUrlScheme?.let { scheme ->
                        "$scheme://x-callback-url/paypal-sdk/paypal-checkout/cancel".toUri()
                    }
                // The launch URL is not needed to finish a returned checkout and may contain
                // short-lived identifiers. Persist only the fields captureDeepLink() consumes.
                val completionOptions = options.copy(targetUri = Uri.EMPTY)
                return PendingRequest(
                    encodedBrowserSwitchState = BrowserSwitchPendingState(completionOptions)
                        .toBase64EncodedJSON(),
                    fallbackResultUri = fallbackResultUri,
                )
            }

            fun from(bundle: Bundle): PendingRequest? {
                val encodedState = bundle.getString(KEY_BROWSER_SWITCH_STATE)
                val pendingState = encodedState?.let(BrowserSwitchPendingState::fromBase64)
                return if (
                    encodedState != null &&
                    pendingState?.originalOptions?.launchMode == BrowserSwitchLaunchMode.AUTH_TAB
                ) {
                    PendingRequest(
                        encodedBrowserSwitchState = encodedState,
                        fallbackResultUri = bundle.getString(KEY_FALLBACK_RESULT_URI)?.toUri(),
                    )
                } else {
                    null
                }
            }
        }
    }

    private data class AuthTabResult(
        val resultCode: Int,
        val resultUri: Uri?,
    )

    private class AuthenticateUserResultContract : ActivityResultContract<Intent, AuthTabResult>() {
        override fun createIntent(context: Context, input: Intent) = input

        override fun parseResult(resultCode: Int, intent: Intent?): AuthTabResult {
            val normalizedResultCode = when (resultCode) {
                AuthTabIntent.RESULT_OK,
                AuthTabIntent.RESULT_CANCELED,
                AuthTabIntent.RESULT_VERIFICATION_FAILED,
                AuthTabIntent.RESULT_VERIFICATION_TIMED_OUT -> resultCode
                else -> AuthTabIntent.RESULT_UNKNOWN_CODE
            }
            val resultUri = if (normalizedResultCode == AuthTabIntent.RESULT_OK) intent?.data else null
            return AuthTabResult(normalizedResultCode, resultUri)
        }
    }

    companion object {
        internal val shared = AuthTabRegistry()

        internal const val REGISTRY_KEY =
            "com.paypal.android.corepayments.browserswitch.AUTH_TAB"
        private const val SAVED_STATE_KEY =
            "com.paypal.android.corepayments.browserswitch.AUTH_TAB_STATE"
        private const val KEY_BROWSER_SWITCH_STATE = "browserSwitchState"
        private const val KEY_FALLBACK_RESULT_URI = "fallbackResultUri"
    }
}
