package com.paypal.android.corepayments.browserswitch

import android.net.Uri
import org.json.JSONObject

data class BrowserSwitchOptions(
    val targetUri: Uri,
    val requestCode: Int,
    val returnUrlScheme: String,
    val metadata: JSONObject? = null
)
