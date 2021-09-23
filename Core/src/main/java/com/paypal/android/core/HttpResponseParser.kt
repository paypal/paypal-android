package com.paypal.android.core

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.nio.charset.StandardCharsets
import java.util.zip.GZIPInputStream

class HttpResponseParser {

    companion object {
        private const val EOF = -1
        private const val BUFFER_SIZE = 1024
    }

    fun parse(connection: HttpURLConnection): HttpResponse {
        val status = connection.responseCode
        val body = when (status) {
            in 200..299 -> getInputStream(connection)
            else -> getErrorStream(connection)
        }?.let {
            parseInputStream(it)
        }
        return HttpResponse(status, body)
    }

    private fun parseInputStream(inputStream: InputStream): String {
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

    private fun getInputStream(connection: HttpURLConnection): InputStream? {
        return connection.inputStream?.let { inputStream ->
            if (connection.contentEncoding == "gzip") {
                return GZIPInputStream(inputStream)
            }
            return inputStream
        }
    }

    private fun getErrorStream(connection: HttpURLConnection): InputStream? {
        return connection.errorStream?.let { inputStream ->
            if (connection.contentEncoding == "gzip") {
                return GZIPInputStream(inputStream)
            }
            return inputStream
        }
    }
}