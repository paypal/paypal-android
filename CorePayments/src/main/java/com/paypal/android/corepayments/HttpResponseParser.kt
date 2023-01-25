package com.paypal.android.corepayments

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.nio.charset.StandardCharsets
import java.util.zip.GZIPInputStream

internal class HttpResponseParser {

    companion object {
        private const val EOF = -1
        private const val BUFFER_SIZE = 1024
    }

    fun parse(connection: HttpURLConnection): HttpResponse {
        val status = connection.responseCode

        val headers = connection.headerFields.mapValues {
            it.value.joinToString(", ")
        }

        val inputStream = runCatching {
            getInputStream(connection)
        }.recover {
            getErrorStream(connection)
        }.getOrNull()

        var body: String? = null
        inputStream?.let {
            body = parseInputStream(it)
            try {
                it.close()
            } catch (ignored: Exception) {
            }
        }

        return HttpResponse(status, headers, body)
    }

    private fun parseInputStream(inputStream: InputStream?): String? {
        if (inputStream == null) return null

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
                GZIPInputStream(inputStream)
            } else {
                inputStream
            }
        }
    }

    private fun getErrorStream(connection: HttpURLConnection): InputStream? {
        return connection.errorStream?.let { inputStream ->
            if (connection.contentEncoding == "gzip") {
                GZIPInputStream(inputStream)
            } else {
                inputStream
            }
        }
    }
}
