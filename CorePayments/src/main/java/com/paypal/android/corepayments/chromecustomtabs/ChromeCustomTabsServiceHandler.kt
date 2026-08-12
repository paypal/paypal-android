package com.paypal.android.corepayments.chromecustomtabs

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.os.RemoteException
import androidx.browser.customtabs.CustomTabsCallback
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsServiceConnection
import androidx.browser.customtabs.CustomTabsSession
import androidx.browser.customtabs.EngagementSignalsCallback
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Prepares a Chrome Custom Tab session that reports when the tab is shown and when its session ends.
 *
 * The returned tab owns a live browser-service binding. If preparation cannot complete safely, this
 * handler releases the binding and returns `null` so the caller can launch an untracked tab instead.
 */
internal class ChromeCustomTabsServiceHandler(
    private val adapter: CustomTabsAdapter
) {

    private companion object {
        const val CONNECTION_TIMEOUT_MILLIS = 1_000L
    }

    suspend fun prepareTrackedTab(
        applicationContext: Context,
        onTabShown: () -> Unit,
        onSessionEnded: () -> Unit
    ): PreparedCustomTab? {
        val packageName = try {
            adapter.getPackageName(applicationContext)
        } catch (_: SecurityException) {
            null
        } catch (_: UnsupportedOperationException) {
            null
        }
        return packageName?.let {
            withTimeoutOrNull(CONNECTION_TIMEOUT_MILLIS) {
                prepareTrackedTab(applicationContext, it, onTabShown, onSessionEnded)
            }
        }
    }

    private suspend fun prepareTrackedTab(
        applicationContext: Context,
        packageName: String,
        onTabShown: () -> Unit,
        onSessionEnded: () -> Unit
    ): PreparedCustomTab? = suspendCancellableCoroutine { continuation ->
        val preparationCompleted = AtomicBoolean(false)
        lateinit var binding: SessionBinding
        val connection = object : CustomTabsServiceConnection() {
            override fun onCustomTabsServiceConnected(name: ComponentName, client: CustomTabsClient) {
                if (!continuation.isActive || binding.isDisposed) {
                    binding.dispose()
                    return
                }
                val session = try {
                    adapter.newSession(
                        client,
                        object : CustomTabsCallback() {
                            override fun onNavigationEvent(navigationEvent: Int, extras: Bundle?) {
                                if (navigationEvent == TAB_SHOWN) binding.notifyTabShown()
                            }
                        }
                    )
                } catch (_: SecurityException) {
                    null
                } catch (_: UnsupportedOperationException) {
                    null
                }
                val prepared = if (session != null && registerEngagementCallback(session, binding)) {
                    PreparedCustomTab(session, binding, packageName)
                } else {
                    null
                }
                completePreparation(binding, preparationCompleted, continuation, prepared)
            }

            override fun onServiceDisconnected(name: ComponentName) {
                binding.dispose()
                if (preparationCompleted.compareAndSet(false, true) && continuation.isActive) {
                    continuation.resume(null)
                }
            }

            override fun onNullBinding(name: ComponentName) = onServiceDisconnected(name)

            override fun onBindingDied(name: ComponentName) = onServiceDisconnected(name)
        }
        binding = SessionBinding(applicationContext, connection, adapter, onTabShown, onSessionEnded)
        continuation.invokeOnCancellation { binding.dispose() }
        val didBind = try {
            adapter.bind(applicationContext, packageName, connection)
        } catch (_: SecurityException) {
            false
        } catch (_: UnsupportedOperationException) {
            false
        }
        binding.onBindResult(didBind)
        if (!didBind) completePreparation(binding, preparationCompleted, continuation, null)
    }

    private fun registerEngagementCallback(
        session: CustomTabsSession,
        binding: SessionBinding
    ): Boolean = try {
        adapter.isEngagementSignalsApiAvailable(session) &&
            adapter.setEngagementSignalsCallback(
                session,
                object : EngagementSignalsCallback {
                    override fun onSessionEnded(didUserInteract: Boolean, extras: Bundle) {
                        binding.notifySessionEnded()
                    }
                }
            )
    } catch (_: RemoteException) {
        false
    } catch (_: SecurityException) {
        false
    } catch (_: UnsupportedOperationException) {
        false
    }

    private fun completePreparation(
        binding: SessionBinding,
        completed: AtomicBoolean,
        continuation: CancellableContinuation<PreparedCustomTab?>,
        preparedTab: PreparedCustomTab?
    ) {
        if (preparedTab == null) binding.dispose()
        if (completed.compareAndSet(false, true) && continuation.isActive) {
            continuation.resume(preparedTab)
        } else if (preparedTab != null) {
            binding.dispose()
        }
    }
}
