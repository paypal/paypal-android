package com.paypal.android.corepayments.browserswitch

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class BrowserSwitchSession internal constructor(private val disposeAction: () -> Unit) {
    fun dispose() = disposeAction()
}
