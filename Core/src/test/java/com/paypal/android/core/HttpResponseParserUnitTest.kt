package com.paypal.android.core

import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.HttpURLConnection.HTTP_OK
import java.util.zip.GZIPOutputStream

@RunWith(Enclosed::class)
class HttpResponseParserUnitTest {

    @RunWith(Parameterized::class)
    class HttpSuccessTest(
        private val responseCode: Int,
        private val contentEncoding: String?,
        private val inputStream: InputStream,
        private val expectedBody: String
    ) {
        // TODO: consider -> https://github.com/Pragmatists/JUnitParams
        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "Parses Response Code {0} with Encoding equal to: {1}")
            fun responseScenarios() = listOf(
                arrayOf(HTTP_OK, "gzip", createGzippedInputStream("200_ok_gzip"), "200_ok_gzip"),
                arrayOf(HTTP_OK, null, createPlainTextInputStream("200_ok_plaintext"), "200_ok_plaintext")
            )
        }

        @Test
        fun parse() {
            val connection = mockk<HttpURLConnection>()
            every { connection.responseCode } returns responseCode
            every { connection.contentEncoding } returns contentEncoding
            every { connection.inputStream } returns inputStream

            val sut = HttpResponseParser()
            val result = sut.parse(connection)
            assertEquals(HTTP_OK, result.status)
            assertEquals(expectedBody, result.body)
        }
    }
}

private fun createGzippedInputStream(input: String): InputStream {
    val byteArrayOutputStream = ByteArrayOutputStream(input.length)
    val gzipOutputStream = GZIPOutputStream(byteArrayOutputStream)
    gzipOutputStream.write(input.toByteArray(Charsets.UTF_8))
    gzipOutputStream.close()
    val gzippedBytes = byteArrayOutputStream.toByteArray()
    byteArrayOutputStream.close()
    return ByteArrayInputStream(gzippedBytes)
}

private fun createPlainTextInputStream(input: String): InputStream {
    return ByteArrayInputStream(input.toByteArray(Charsets.UTF_8))
}
