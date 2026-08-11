package com.paypal.android.paypalpayments

import android.content.Context
import com.paypal.android.corepayments.browserswitch.BrowserSwitchSession

internal class PayPalLaunchCancellationTracker(
    private val returnToAppLauncher: PayPalReturnToAppLauncher
) {

    private val activeLaunchLock = Any()
    private var nextGeneration = 0L
    private var activeLaunch: TrackedLaunch? = null

    fun startTracking(
        token: String,
        requestCode: Int,
        authState: String,
        context: Context,
        cancelUrl: String
    ): Launch {
        val replacement: TrackedLaunch
        val previous = synchronized(activeLaunchLock) {
            replacement = TrackedLaunch(
                ++nextGeneration,
                token,
                requestCode,
                authState,
                context,
                cancelUrl
            )
            activeLaunch.also { activeLaunch = replacement }
        }
        previous?.session?.dispose()
        return Launch(replacement.generation)
    }

    fun retainSession(launch: Launch, session: BrowserSwitchSession?) {
        val shouldDispose = synchronized(activeLaunchLock) {
            val active = activeLaunch
            if (active?.generation == launch.generation && !active.sessionEnded) {
                active.session = session
                false
            } else {
                true
            }
        }
        if (shouldDispose) session?.dispose()
    }

    fun markTabShown(launch: Launch) {
        synchronized(activeLaunchLock) {
            activeLaunch
                ?.takeIf { it.generation == launch.generation }
                ?.returnToAppObserved = false
        }
    }

    fun markSessionEnded(launch: Launch) {
        var session: BrowserSwitchSession? = null
        var cancellationReturn: (() -> Unit)? = null
        synchronized(activeLaunchLock) {
            val active = activeLaunch?.takeIf {
                it.generation == launch.generation && !it.sessionEnded
            }
            if (active != null) {
                active.sessionEnded = true
                session = active.session
                active.session = null
                cancellationReturn = createCancellationReturn(active)
            }
        }
        session?.dispose()
        cancellationReturn?.invoke()
    }

    fun handleReturnToApp(authState: String, requestCode: Int): Result =
        synchronized(activeLaunchLock) {
            val launch = activeLaunch?.takeIf {
                it.authState == authState && it.requestCode == requestCode
            }
            when {
                launch == null -> Result.NoResult
                launch.sessionEnded -> {
                    activeLaunch = null
                    Result.Canceled(launch.token)
                }
                else -> {
                    launch.returnToAppObserved = true
                    Result.NoResult
                }
            }
        }

    fun armCancellationReturn(authState: String) {
        var cancellationReturn: (() -> Unit)? = null
        synchronized(activeLaunchLock) {
            val launch = activeLaunch?.takeIf { it.authState == authState }
            if (launch != null) {
                launch.cancellationReturnArmed = true
                cancellationReturn = createCancellationReturn(launch)
            }
        }
        cancellationReturn?.invoke()
    }

    fun clear(authState: String? = null) {
        val launch = synchronized(activeLaunchLock) {
            activeLaunch?.takeIf { authState == null || it.authState == authState }
                ?.also { activeLaunch = null }
        }
        launch?.session?.dispose()
    }

    private fun createCancellationReturn(launch: TrackedLaunch): (() -> Unit)? {
        val sessionReturnObserved = launch.sessionEnded && launch.returnToAppObserved
        val canSchedule = launch.cancellationReturnArmed && !launch.cancellationReturnScheduled
        return if (sessionReturnObserved && canSchedule && launch.cancelUrl.isNotBlank()) {
            launch.cancellationReturnScheduled = true
            val action: () -> Unit = {
                returnToAppLauncher.launch(launch.context, launch.cancelUrl) {
                    synchronized(activeLaunchLock) {
                        activeLaunch?.generation == launch.generation
                    }
                }
            }
            action
        } else {
            null
        }
    }

    internal class Launch internal constructor(internal val generation: Long)

    internal sealed class Result {
        data object NoResult : Result()

        data class Canceled(val token: String) : Result()
    }

    private data class TrackedLaunch(
        val generation: Long,
        val token: String,
        val requestCode: Int,
        val authState: String,
        val context: Context,
        val cancelUrl: String,
        var session: BrowserSwitchSession? = null,
        var sessionEnded: Boolean = false,
        var returnToAppObserved: Boolean = false,
        var cancellationReturnArmed: Boolean = false,
        var cancellationReturnScheduled: Boolean = false
    )
}
