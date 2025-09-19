package com.paypal.android.corepayments.browserswitch

import androidx.core.net.toUri

data class BrowserSwitchPendingState(val originalOptions: BrowserSwitchOptions) {

    fun toBase64EncodedJSON(): String {
        // TODO: implement
        return ""
    }

    companion object {
        fun fromBase64(pendingStateBase64: String): BrowserSwitchPendingState? {
            // TODO: implement
            val targetUri = "https://paypal.com".toUri()
            val options = BrowserSwitchOptions(
                targetUri = targetUri,
                requestCode = Int.MIN_VALUE
            )
            return BrowserSwitchPendingState(options)
        }
    }
}
