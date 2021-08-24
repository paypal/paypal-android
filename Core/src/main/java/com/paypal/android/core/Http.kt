package com.paypal.android.core

import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection

class Http {

    suspend fun send(request: HttpRequest) = send(request, Dispatchers.IO)

    @VisibleForTesting
    suspend fun send(request: HttpRequest, dispatcher: CoroutineDispatcher) =
        withContext(dispatcher) {
            val url = request.url
            val connection = url.openConnection() as HttpURLConnection

            connection.requestMethod = "GET"
            val responseCode = connection.responseCode

            HttpResult(responseCode)
        }
}