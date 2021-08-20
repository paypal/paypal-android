package com.paypal.android.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.resume

class Http {

    suspend fun send(request: HttpRequest) = withContext(Dispatchers.IO) {
        val url = URL("https://www.google.com")
        val connection = url.openConnection() as HttpURLConnection

        connection.requestMethod = "GET"
        val responseCode = connection.responseCode

        HttpResult(responseCode)
    }
}