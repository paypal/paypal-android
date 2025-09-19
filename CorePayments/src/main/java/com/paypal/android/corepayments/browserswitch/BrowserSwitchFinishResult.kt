package com.paypal.android.corepayments.browserswitch

import android.net.Uri
import org.json.JSONObject

sealed class BrowserSwitchFinishResult() {

    data class Success(
        val returnUrl: Uri,
        val requestCode: Int,
        val requestUrl: Uri,
        val requestMetadata: JSONObject?,
    ) : BrowserSwitchFinishResult()

    class Failure internal constructor(val error: Exception) : BrowserSwitchFinishResult()

    /**
     * No browser switch result was found. This is the expected result when a user cancels the
     * browser switch flow without completing by closing the browser, or navigates back to the app
     * without completing the browser switch flow.
     */
    object NoResult : BrowserSwitchFinishResult()
}
