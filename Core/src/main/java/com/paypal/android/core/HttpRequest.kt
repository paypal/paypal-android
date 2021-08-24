package com.paypal.android.core

import java.net.URL

data class HttpRequest(var method: String, var host: String, var path: String) {

    constructor() : this("GET", "", "")

    val url: URL
        get() {
            return URL("https://${host}${path}")
        }
}