package com.paypal.android.corepayments.browserswitch

import android.content.Context
import androidx.browser.customtabs.CustomTabsServiceConnection

internal class SessionBinding(
    private val context: Context,
    private val connection: CustomTabsServiceConnection,
    private val adapter: CustomTabsAdapter,
    private val onTabShown: () -> Unit,
    private val onSessionEnded: () -> Unit
) {
    private val lock = Any()
    private var disposed = false
    private var bindCompleted = false
    private var isBound = false
    private var unbound = false

    val isDisposed: Boolean get() = synchronized(lock) { disposed }

    fun onBindResult(didBind: Boolean) {
        val shouldUnbind = synchronized(lock) {
            bindCompleted = true
            isBound = didBind
            markUnbindIfNeeded()
        }
        if (shouldUnbind) unbind()
    }

    fun notifyTabShown() {
        runIfNotDisposed(onTabShown)
    }

    fun notifySessionEnded() {
        if (dispose()) onSessionEnded()
    }

    internal fun runIfNotDisposed(action: () -> Unit): Boolean = synchronized(lock) {
        if (disposed) {
            false
        } else {
            action()
            true
        }
    }

    fun dispose(): Boolean {
        val result = synchronized(lock) {
            if (disposed) {
                false to false
            } else {
                disposed = true
                true to markUnbindIfNeeded()
            }
        }
        if (result.second) unbind()
        return result.first
    }

    private fun markUnbindIfNeeded(): Boolean {
        val bindingActive = bindCompleted && isBound
        val shouldUnbind = disposed && bindingActive && !unbound
        if (shouldUnbind) unbound = true
        return shouldUnbind
    }

    private fun unbind() {
        try {
            adapter.unbind(context, connection)
        } catch (_: IllegalArgumentException) {
            // The browser service may disconnect before local cleanup runs.
        }
    }
}
