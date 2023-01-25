package com.paypal.android.corepayments

import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.lang.IllegalStateException
import java.net.HttpURLConnection
import java.net.UnknownHostException

internal class Http(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val httpResponseParser: HttpResponseParser = HttpResponseParser()
) {

    companion object {
        private val TAG = Http::class.qualifiedName
    }

    suspend fun send(httpRequest: HttpRequest): HttpResponse =
        withContext(dispatcher) {
            runCatching {
                val url = httpRequest.url
                val connection = url.openConnection() as HttpURLConnection

                connection.requestMethod = httpRequest.method.name

                // add headers
                for ((key, value) in httpRequest.headers) {
                    connection.addRequestProperty(key, value)
                }

                if (httpRequest.method == HttpMethod.POST) {
                    try {
                        connection.doOutput = true
                        connection.outputStream.write(httpRequest.body?.toByteArray())
                        connection.outputStream.flush()
                        connection.outputStream.close()
                    } catch (e: IOException) {
                        Log.d(TAG, "Error closing connection output stream:")
                        Log.d(TAG, e.stackTrace.toString())
                    }
                }

                connection.connect()
                httpResponseParser.parse(connection)
            }.recover {
                val status = when (it) {
                    is UnknownHostException -> HttpResponse.STATUS_UNKNOWN_HOST
                    is IllegalStateException -> HttpResponse.SERVER_ERROR
                    else -> HttpResponse.STATUS_UNDETERMINED
                }
                HttpResponse(status = status, error = it)
            }.getOrNull()!!
        }
}
