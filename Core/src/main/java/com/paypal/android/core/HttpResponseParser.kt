package com.paypal.android.core

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.nio.charset.StandardCharsets
import java.util.zip.GZIPInputStream

class HttpResponseParser {

    val EOF = -1
    val BUFFER_SIZE = 1024

    fun parse(connection: HttpURLConnection): HttpResponse {
        val status = connection.responseCode
        val body = parseBody(connection)
        return HttpResponse(connection.responseCode, body)
    }

    private fun parseBody(connection: HttpURLConnection): String? {
        return getInputStream(connection)?.let { inputStream ->
            val outputStream = ByteArrayOutputStream()

            val buffer = ByteArray(BUFFER_SIZE)
            while (true) {
                val count = inputStream.read(buffer)
                if (count == EOF) {
                    break
                }
                outputStream.write(buffer, 0, count)
            }
            return String(outputStream.toByteArray(), StandardCharsets.UTF_8)
        }
    }

    private fun getInputStream(connection: HttpURLConnection): InputStream? {
        return connection.inputStream?.let { inputStream ->
            if (connection.contentEncoding == "gzip") {
                return GZIPInputStream(inputStream)
            }
            return inputStream
        }
    }
}