package com.paypal.android.core

import java.net.URL
import java.util.Locale

data class HttpRequest(
    val url: URL,
    val method: HttpMethod,
    val body: String? = null,
    val language: String = Locale.getDefault().language
) {

    // default headers
    val headers: MutableMap<String, String> = mutableMapOf(
        "Accept-Encoding" to "gzip",
        "Accept-Language" to language
    )

    var contentType: HttpContentType? = null
        set(value) {
            field = value
            value?.let {
                headers["Content-Type"] = it.asString
            }
        }
}
