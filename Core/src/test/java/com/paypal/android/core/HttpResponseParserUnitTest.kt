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
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.HttpURLConnection.*
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

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "Parses Response Code {0} with Encoding equal to: {1}")
            fun responseScenarios() = listOf(
                arrayOf(HTTP_OK, "gzip", createGzippedInputStream("200_ok_gzip"), "200_ok_gzip"),
                arrayOf(HTTP_OK, null, createPlainTextInputStream("200_ok_plaintext"), "200_ok_plaintext"),
                arrayOf(HTTP_CREATED, "gzip", createGzippedInputStream("201_created_gzip"), "201_created_gzip"),
                arrayOf(HTTP_CREATED, null, createPlainTextInputStream("201_created_plaintext"), "201_created_plaintext"),
                arrayOf(HTTP_ACCEPTED, "gzip", createGzippedInputStream("202_accepted_gzip"), "202_accepted_gzip"),
                arrayOf(HTTP_ACCEPTED, null, createPlainTextInputStream("202_accepted_plaintext"), "202_accepted_plaintext")
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
            assertEquals(responseCode, result.status)
            assertEquals(expectedBody, result.body)
        }
    }

    @RunWith(Parameterized::class)
    class HttpErrorTest(
        private val responseCode: Int,
        private val contentEncoding: String?,
        private val errorStream: InputStream,
        private val expectedBody: String
    ) {

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "Parses Response Code {0} with Encoding equal to: {1}")
            fun responseScenarios() = listOf(
                arrayOf(HTTP_BAD_REQUEST, "gzip", createGzippedInputStream("400_bad_request_gzip"), "400_bad_request_gzip"),
                arrayOf(HTTP_BAD_REQUEST, null, createPlainTextInputStream("400_bad_request_plaintext"), "400_bad_request_plaintext"),
                arrayOf(HTTP_UNAUTHORIZED, "gzip", createGzippedInputStream("401_unauthorized_gzip"), "401_unauthorized_gzip"),
                arrayOf(HTTP_UNAUTHORIZED, null, createPlainTextInputStream("401_unauthorized_plaintext"), "401_unauthorized_plaintext"),
                arrayOf(HTTP_FORBIDDEN, "gzip", createGzippedInputStream("403_forbidden_gzip"), "403_forbidden_gzip"),
                arrayOf(HTTP_FORBIDDEN, null, createPlainTextInputStream("403_forbidden_plaintext"), "403_forbidden_plaintext"),
                arrayOf(HTTP_INTERNAL_ERROR, "gzip", createGzippedInputStream("500_internal_server_error_gzip"), "500_internal_server_error_gzip"),
                arrayOf(HTTP_INTERNAL_ERROR, null, createPlainTextInputStream("500_internal_server_error_plaintext"), "500_internal_server_error_plaintext"),
                arrayOf(HTTP_UNAVAILABLE, "gzip", createGzippedInputStream("503_unavailable_gzip"), "503_unavailable_gzip"),
                arrayOf(HTTP_UNAVAILABLE, null, createPlainTextInputStream("503_unavailable_plaintext"), "503_unavailable_plaintext"),
            )
        }

        @Test
        fun parse() {
            val connection = mockk<HttpURLConnection>()
            every { connection.responseCode } returns responseCode
            every { connection.contentEncoding } returns contentEncoding
            every { connection.inputStream } throws IOException("http unsuccessful")
            every { connection.errorStream } returns errorStream

            val sut = HttpResponseParser()
            val result = sut.parse(connection)
            assertEquals(responseCode, result.status)
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
