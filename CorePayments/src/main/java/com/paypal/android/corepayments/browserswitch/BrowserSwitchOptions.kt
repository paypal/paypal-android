package com.paypal.android.corepayments.browserswitch

import android.net.Uri
import androidx.annotation.RestrictTo
import org.json.JSONObject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class BrowserSwitchOptions(
    val targetUri: Uri,
    val requestCode: Int,
    val returnUrlScheme: String?,
    val appLinkUrl: String?,
    val metadata: JSONObject? = null
)
