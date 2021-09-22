package com.paypal.android.core

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection

class Http(private val dispatcher: CoroutineDispatcher = Dispatchers.IO) {

    private val httpResponseParser = HttpResponseParser()

    suspend fun send(httpRequest: HttpRequest) =
        withContext(dispatcher) {
            val url = httpRequest.url
            val connection = url.openConnection() as HttpURLConnection

            connection.requestMethod = httpRequest.method.asString

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
                } catch (e:IOException) {
                    //do something
                }
            }

            connection.connect()
            httpResponseParser.parse(connection)
        }
}
