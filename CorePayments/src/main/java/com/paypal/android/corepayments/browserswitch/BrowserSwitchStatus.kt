package com.paypal.android.corepayments.browserswitch

import android.net.Uri

sealed class BrowserSwitchStatus {
    class Complete(val deepLinkUri: Uri, val options: BrowserSwitchOptions) : BrowserSwitchStatus()

    object NoResult : BrowserSwitchStatus()
}
