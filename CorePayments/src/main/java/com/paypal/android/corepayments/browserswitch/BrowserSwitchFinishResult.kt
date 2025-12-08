package com.paypal.android.corepayments.browserswitch

import android.net.Uri
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed class BrowserSwitchFinishResult() {
    data class Success(val deepLinkUri: Uri) : BrowserSwitchFinishResult()
    object RequestCodeDoesNotMatch : BrowserSwitchFinishResult()
    object DeepLinkNotPresent : BrowserSwitchFinishResult()
    object DeepLinkDoesNotMatch : BrowserSwitchFinishResult()
}
