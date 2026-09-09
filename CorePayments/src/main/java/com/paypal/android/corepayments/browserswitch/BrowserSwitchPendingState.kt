package com.paypal.android.corepayments.browserswitch

import android.util.Base64
import androidx.annotation.RestrictTo
import androidx.core.net.toUri
import org.json.JSONException
import org.json.JSONObject
import java.nio.charset.StandardCharsets

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class BrowserSwitchPendingState(val originalOptions: BrowserSwitchOptions) {

    fun toBase64EncodedJSON(): String {
        val json = JSONObject()
            .put(KEY_TARGET_URI, originalOptions.targetUri)
            .put(KEY_REQUEST_CODE, originalOptions.requestCode)
            .putOpt(KEY_RETURN_URL_SCHEME, originalOptions.returnUrlScheme)
            .putOpt(KEY_APP_LINK_URL, originalOptions.appLinkUrl)
            .putOpt(KEY_METADATA, originalOptions.metadata)
            .put(KEY_LAUNCH_MODE, originalOptions.launchMode.name)
        val jsonBytes: ByteArray? = json.toString().toByteArray(StandardCharsets.UTF_8)
        val flags = Base64.DEFAULT or Base64.NO_WRAP
        return Base64.encodeToString(jsonBytes, flags)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {

        const val KEY_TARGET_URI = "targetUri"
        const val KEY_REQUEST_CODE = "requestCode"
        const val KEY_RETURN_URL_SCHEME = "returnUrlScheme"
        const val KEY_APP_LINK_URL = "appLinkUrl"
        const val KEY_METADATA = "metadata"
        const val KEY_LAUNCH_MODE = "launchMode"

        fun fromBase64(base64EncodedJSON: String): BrowserSwitchPendingState? {
            val data = try {
                Base64.decode(base64EncodedJSON, Base64.DEFAULT)
            } catch (_: IllegalArgumentException) {
                null
            }
            val json = if (data == null) {
                null
            } else {
                try {
                    JSONObject(String(data, StandardCharsets.UTF_8))
                } catch (_: JSONException) {
                    null
                }
            }
            val requestCode = if (json == null) {
                null
            } else {
                try {
                    json.getInt(KEY_REQUEST_CODE)
                } catch (_: JSONException) {
                    null
                }
            }
            val targetUri = json?.opt(KEY_TARGET_URI) as? String
            val launchMode = json?.optString(KEY_LAUNCH_MODE)?.let { launchModeName ->
                if (launchModeName.isBlank()) {
                    BrowserSwitchLaunchMode.CUSTOM_TAB
                } else {
                    BrowserSwitchLaunchMode.entries.firstOrNull { it.name == launchModeName }
                }
            }
            return if (json == null || targetUri == null) {
                null
            } else if (requestCode == null || launchMode == null) {
                null
            } else {
                BrowserSwitchPendingState(
                    BrowserSwitchOptions(
                        targetUri = targetUri.toUri(),
                        requestCode = requestCode,
                        returnUrlScheme = json.optString(KEY_RETURN_URL_SCHEME).takeIf(String::isNotBlank),
                        appLinkUrl = json.optString(KEY_APP_LINK_URL).takeIf(String::isNotBlank),
                        metadata = json.optJSONObject(KEY_METADATA),
                        launchMode = launchMode,
                    )
                )
            }
        }
    }
}
