package com.paypal.android.corepayments.browserswitch

import android.content.Intent
import android.util.Base64
import androidx.annotation.RestrictTo
import androidx.core.net.toUri
import org.json.JSONObject
import java.nio.charset.StandardCharsets

const val KEY_TARGET_URI = "targetUri"
const val KEY_REQUEST_CODE = "requestCode"
const val KEY_RETURN_URL_SCHEME = "returnUrlScheme"
const val KEY_APP_LINK_URL = "appLinkUrl"
const val KEY_METADATA = "metadata"

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class BrowserSwitchPendingState(val originalOptions: BrowserSwitchOptions) {

    fun toBase64EncodedJSON(): String {
        val json = JSONObject()
            .put(KEY_TARGET_URI, originalOptions.targetUri)
            .put(KEY_REQUEST_CODE, originalOptions.requestCode)
            .putOpt(KEY_RETURN_URL_SCHEME, originalOptions.returnUrlScheme)
            .putOpt(KEY_APP_LINK_URL, originalOptions.appLinkUrl)
            .putOpt(KEY_METADATA, originalOptions.metadata)
        val jsonBytes: ByteArray? = json.toString().toByteArray(StandardCharsets.UTF_8)
        val flags = Base64.DEFAULT or Base64.NO_WRAP
        return Base64.encodeToString(jsonBytes, flags)
    }

    // TODO: consider renaming this method; "match" sounds like it returns a boolean
    fun match(
        intent: Intent,
        requestCode: Int,
    ): BrowserSwitchFinishResult {
        if (requestCode != originalOptions.requestCode) {
            return BrowserSwitchFinishResult.RequestCodeDoesNotMatch
        }

        val deepLinkUri = intent.data
        if (deepLinkUri == null) {
            return BrowserSwitchFinishResult.DeepLinkNotPresent
        }

        val deepLinkScheme = deepLinkUri.scheme.orEmpty()
        val isMatchingDeepLink =
            deepLinkScheme.equals(originalOptions.returnUrlScheme, ignoreCase = true)
        return if (isMatchingDeepLink) {
            BrowserSwitchFinishResult.Success(deepLinkUri = deepLinkUri)
        } else {
            BrowserSwitchFinishResult.DeepLinkDoesNotMatch
        }
    }

    companion object {
        fun fromBase64(base64EncodedJSON: String): BrowserSwitchPendingState? {
            val data = Base64.decode(base64EncodedJSON, Base64.DEFAULT)
            val requestJSONString = String(data, StandardCharsets.UTF_8)
            val json = JSONObject(requestJSONString)
            val options = BrowserSwitchOptions(
                targetUri = json.getString(KEY_TARGET_URI).toUri(),
                requestCode = json.getInt(KEY_REQUEST_CODE),
                returnUrlScheme = json.optString(KEY_RETURN_URL_SCHEME),
                appLinkUrl = json.optString(KEY_APP_LINK_URL),
                metadata = json.optJSONObject(KEY_METADATA)
            )
            return BrowserSwitchPendingState(options)
        }
    }
}
