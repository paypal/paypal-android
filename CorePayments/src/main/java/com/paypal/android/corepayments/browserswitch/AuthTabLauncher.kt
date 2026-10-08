package com.paypal.android.corepayments.browserswitch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.MainThread
import androidx.savedstate.SavedStateRegistry

/**
 * Registers an Auth Tab [androidx.activity.result.ActivityResultLauncher] directly on the merchant's
 * own [ComponentActivity] for the duration of a single checkout attempt.
 *
 * This object is created fresh for each checkout (and fresh again for each recreation, by
 * [AuthTabRecreator]) and is never cached beyond the in-flight request: it is not stored in any
 * SDK-global/static field, and it is not attached to the Activity (no decor-view tag, no lookup
 * helper). The only thing that outlives a single request is the restoration hook armed via
 * `SavedStateRegistry.runOnNextRecreation`, which is how an in-flight Auth Tab survives
 * Activity/process recreation without an SDK-owned Activity.
 *
 * Registration uses the non-lifecycle `ActivityResultRegistry.register(key, contract, callback)`
 * overload for the initial launch, because checkout normally starts from a `RESUMED` Activity (a
 * button click) — the lifecycle-aware overload would reject that. [AuthTabRecreator] uses the
 * lifecycle-aware overload instead, re-registering on the restored Activity's `ON_CREATE` (legal
 * pre-STARTED there), which gives STARTED-gated delivery plus automatic unregistration.
 *
 * `SavedStateRegistry.registerSavedStateProvider` (unlike `ActivityResultRegistry.register`) throws if
 * a provider is already registered under the same key, so it doubles as the reliable cross-instance
 * "an Auth Tab is already pending for this Activity" check — the registry, not this object's own
 * fields, is the real source of truth, since a fresh [AuthTabLauncher] instance is created per call.
 *
 * All entry points are `@MainThread` — they touch `ActivityResultRegistry`/`SavedStateRegistry`, which
 * are main-thread-only APIs. Callers reach them via `withContext(Dispatchers.Main)` upstream.
 *
 * Invariant: the non-lifecycle `register()` call can synchronously deliver an already-pending result
 * for [REGISTRY_KEY] before returning. [launch] deliberately registers before setting [pendingRequest],
 * so any such stale delivery finds `pendingRequest == null` in [handleResult] and is dropped as a no-op
 * instead of being misattributed to the new request (which would otherwise leave the real result
 * silently undelivered later, since the launcher field wouldn't yet be assigned to unregister).
 */
internal class AuthTabLauncher(
    private val resultDispatcher: AuthTabResultDispatcher,
) {

    private var launcher: ActivityResultLauncher<BrowserSwitchOptions>? = null
    private var pendingRequest: PendingRequest? = null
    private var savedStateRegistry: SavedStateRegistry? = null

    /** Initial launch path: non-lifecycle registration, legal while the Activity is RESUMED. */
    @MainThread
    fun launch(activity: ComponentActivity, options: BrowserSwitchOptions) {
        try {
            activity.savedStateRegistry.registerSavedStateProvider(SAVED_STATE_KEY) {
                pendingRequest?.toBundle() ?: Bundle()
            }
        } catch (error: IllegalArgumentException) {
            throw IllegalStateException("An Auth Tab is already pending for this Activity.", error)
        }
        savedStateRegistry = activity.savedStateRegistry

        var setupSucceeded = false
        try {
            // Register before assigning pendingRequest — see the invariant documented in the class KDoc.
            val registeredLauncher = activity.activityResultRegistry.register(
                REGISTRY_KEY,
                LaunchAuthTab(),
                ::handleResult,
            )
            launcher = registeredLauncher
            pendingRequest = PendingRequest.from(options)

            // Arm recreation BEFORE launching: if this throws after the tab is already visible, we'd
            // report failure on a visibly-open checkout and could lose or leak the result.
            activity.savedStateRegistry.runOnNextRecreation(AuthTabRecreator::class.java)
            registeredLauncher.launch(options)
            setupSucceeded = true
        } finally {
            if (!setupSucceeded) {
                // Roll back the saved-state provider too, not just the launcher, so a retry on the
                // same Activity isn't rejected by a stale "already pending" check.
                cleanup()
            }
        }
    }

    /**
     * Recreation path: lifecycle-aware registration on the restored Activity's `ON_CREATE`. Called by
     * [AuthTabRecreator] ONLY when a [PendingRequest] was actually restored — callers must not invoke
     * this when nothing is pending, since `runOnNextRecreation` cannot be un-armed and a stray
     * lifecycle registration under the fixed [REGISTRY_KEY] would interfere with a later non-lifecycle
     * registration for a new checkout on the same Activity instance.
     */
    @MainThread
    fun registerForRecreation(owner: ComponentActivity, restoredRequest: PendingRequest) {
        pendingRequest = restoredRequest
        owner.savedStateRegistry.registerSavedStateProvider(SAVED_STATE_KEY) {
            pendingRequest?.toBundle() ?: Bundle()
        }
        savedStateRegistry = owner.savedStateRegistry

        var setupSucceeded = false
        try {
            launcher = owner.activityResultRegistry.register(
                REGISTRY_KEY,
                owner,
                LaunchAuthTab(),
                ::handleResult,
            )
            owner.savedStateRegistry.runOnNextRecreation(AuthTabRecreator::class.java)
            setupSucceeded = true
        } finally {
            if (!setupSucceeded) {
                cleanup()
            }
        }
    }

    private fun handleResult(result: AuthTabResult) {
        val request = pendingRequest ?: return
        try {
            resultDispatcher.dispatch(request, result)
        } finally {
            cleanup()
        }
    }

    /** Fully nested so a throw while unregistering the launcher still unregisters the provider. */
    private fun cleanup() {
        pendingRequest = null
        try {
            try {
                launcher?.unregister()
            } finally {
                launcher = null
            }
        } finally {
            try {
                savedStateRegistry?.unregisterSavedStateProvider(SAVED_STATE_KEY)
            } finally {
                savedStateRegistry = null
            }
        }
    }

    companion object {
        private const val REGISTRY_KEY =
            "com.paypal.android.corepayments.browserswitch.AUTH_TAB"
        internal const val SAVED_STATE_KEY =
            "com.paypal.android.corepayments.browserswitch.AUTH_TAB_STATE"
    }
}
