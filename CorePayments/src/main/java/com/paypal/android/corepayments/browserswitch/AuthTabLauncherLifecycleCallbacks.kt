package com.paypal.android.corepayments.browserswitch

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity

/** Installs an Activity-owned Auth Tab launcher before each ComponentActivity reaches STARTED. */
@Suppress("TooManyFunctions") // ActivityLifecycleCallbacks contributes seven required callbacks.
internal class AuthTabLauncherLifecycleCallbacks : Application.ActivityLifecycleCallbacks {

    private var registeredApplication: Application? = null

    @Synchronized
    fun initialize(application: Application) {
        if (registeredApplication === application) return

        registeredApplication?.unregisterActivityLifecycleCallbacks(this)
        application.registerActivityLifecycleCallbacks(this)
        registeredApplication = application
    }

    fun dispose() {
        registeredApplication?.unregisterActivityLifecycleCallbacks(this)
        registeredApplication = null
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (activity is ComponentActivity) {
            AuthTabLauncher.attach(activity)
        }
    }

    override fun onActivityDestroyed(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityResumed(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
}
