package com.paypal.android.corepayments.browserswitch

import android.net.Uri
import org.json.JSONObject

sealed class BrowserSwitchFinishResult() {

    data class Success(
        val returnUrl: Uri,
        val requestCode: Int,
        val requestUrl: Uri,
        val requestMetadata: JSONObject?,
    ) : BrowserSwitchFinishResult() {
        constructor(returnUrl: Uri, originalOptions: BrowserSwitchOptions) : this(
            returnUrl = returnUrl,
            requestCode = originalOptions.requestCode,
            requestUrl = originalOptions.targetUri,
            requestMetadata = originalOptions.metadata
        )
    }

    object RequestCodeDoesNotMatch : BrowserSwitchFinishResult()
    object DeepLinkNotPresent : BrowserSwitchFinishResult()
    object DeepLinkDoesNotMatch : BrowserSwitchFinishResult()
}
