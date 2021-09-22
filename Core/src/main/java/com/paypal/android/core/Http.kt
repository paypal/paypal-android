package com.paypal.android.core

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection

class Http(private val dispatcher: CoroutineDispatcher = Dispatchers.IO) {

    private val httpResponseParser = HttpResponseParser()

    suspend fun send(request: HttpRequest) =
        withContext(dispatcher) {
            val url = request.url
            val connection = url.openConnection() as HttpURLConnection

            connection.requestMethod = request.method
            if (request.method == "POST") {
                connection.addRequestProperty("Content-Type", "application/json")
                try {
                    connection.doOutput = true
                    connection.outputStream.write(request.body.toByteArray())
                    connection.outputStream.flush()
                    connection.outputStream.close()
                } catch (e:IOException) {
                    //do something
                }
            }

            connection.connect()
            httpResponseParser.parse(connection)
        }
}
