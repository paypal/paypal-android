package com.paypal.android.corepayments.browserswitch

import androidx.activity.ComponentActivity
import androidx.annotation.MainThread
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryOwner

/**
 * Re-registers the Auth Tab result launcher on a recreated host [ComponentActivity] during its
 * `ON_CREATE`, so an in-flight Auth Tab survives Activity/process recreation without an SDK-owned
 * Activity, a `ContentProvider`, or any app-wide instrumentation.
 *
 * Reflectively constructed by `androidx.savedstate.Recreator` via a no-arg constructor (does not need
 * to be `public`; `Recreator` uses `getDeclaredConstructor()` + `setAccessible`). Deliberately holds no
 * state of its own — everything it needs is recovered from [owner]'s own `SavedStateRegistry`.
 *
 * This is a strict no-op when nothing was actually pending: `runOnNextRecreation` cannot be un-armed,
 * so a checkout that completed before a later configuration change would otherwise still invoke this
 * class with no real work to do. Registering a launcher in that case would leave a stray lifecycle-aware
 * registration under the fixed registry key, which could interfere with a later non-lifecycle
 * registration for a brand-new checkout on the same Activity instance.
 */
internal class AuthTabRecreator : SavedStateRegistry.AutoRecreated {

    @MainThread
    override fun onRecreated(owner: SavedStateRegistryOwner) {
        val activity = owner as? ComponentActivity
        val restoredRequest = activity?.let(::restoredPendingRequest)
        if (activity != null && restoredRequest != null) {
            AuthTabLauncher(AuthTabResultDispatcher(activity.applicationContext))
                .registerForRecreation(activity, restoredRequest)
        }
    }

    private fun restoredPendingRequest(activity: ComponentActivity): PendingRequest? =
        activity.savedStateRegistry
            .consumeRestoredStateForKey(AuthTabLauncher.SAVED_STATE_KEY)
            ?.let(PendingRequest::from)
}
