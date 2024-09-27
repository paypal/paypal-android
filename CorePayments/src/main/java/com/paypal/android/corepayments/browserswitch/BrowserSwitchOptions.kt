package com.paypal.android.corepayments.browserswitch

import android.net.Uri
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class BrowserSwitchOptions(
    val code: BrowserSwitchRequestCode,
    val urlToOpen: Uri,
    val returnUrl: Uri,
    val metadata: String
) {
    fun encodeToString(): String = Json.encodeToString(this)

    companion object {

        fun decodeIfRequestCodeMatches(
            input: String,
            requestCode: BrowserSwitchRequestCode
        ): BrowserSwitchOptions? {
            val options = decodeFromString(input)
            return if (options?.code == requestCode) options else null
        }

        fun decodeFromString(input: String) = tryDecodeFromString<BrowserSwitchOptions>(input)

        private inline fun <reified T> tryDecodeFromString(input: String): T? = try {
            Json.decodeFromString<T>(input)
        } catch (e: IllegalArgumentException) {
            null
        } catch (e: SerializationException) {
            null
        }
    }
}
