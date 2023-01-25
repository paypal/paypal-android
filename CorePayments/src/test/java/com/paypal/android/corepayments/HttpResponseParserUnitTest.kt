package com.paypal.android.corepayments

import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.HttpURLConnection.HTTP_ACCEPTED
import java.net.HttpURLConnection.HTTP_BAD_REQUEST
import java.net.HttpURLConnection.HTTP_CREATED
import java.net.HttpURLConnection.HTTP_FORBIDDEN
import java.net.HttpURLConnection.HTTP_INTERNAL_ERROR
import java.net.HttpURLConnection.HTTP_OK
import java.net.HttpURLConnection.HTTP_UNAUTHORIZED
import java.net.HttpURLConnection.HTTP_UNAVAILABLE
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
                arrayOf(
                    HTTP_OK,
                    null,
                    createPlainTextInputStream("200_ok_plaintext"),
                    "200_ok_plaintext"
                ),
                arrayOf(
                    HTTP_CREATED,
                    "gzip",
                    createGzippedInputStream("201_created_gzip"),
                    "201_created_gzip"
                ),
                arrayOf(
                    HTTP_CREATED,
                    null,
                    createPlainTextInputStream("201_created_plaintext"),
                    "201_created_plaintext"
                ),
                arrayOf(
                    HTTP_ACCEPTED,
                    "gzip",
                    createGzippedInputStream("202_accepted_gzip"),
                    "202_accepted_gzip"
                ),
                arrayOf(
                    HTTP_ACCEPTED,
                    null,
                    createPlainTextInputStream("202_accepted_plaintext"),
                    "202_accepted_plaintext"
                )
            )
        }

        @Test
        fun parse() {
            val connection = mockk<HttpURLConnection>(relaxed = true)
            every { connection.responseCode } returns responseCode
            every { connection.contentEncoding } returns contentEncoding
            every { connection.inputStream } returns inputStream

            val sut = HttpResponseParser()
            val result = sut.parse(connection)
            assertEquals(responseCode, result.status)
            assertEquals(expectedBody, result.body)

            verify { inputStream.close() }
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
                arrayOf(
                    HTTP_BAD_REQUEST, "gzip",
                    createGzippedInputStream("400_bad_request_gzip"),
                    "400_bad_request_gzip"
                ),
                arrayOf(
                    HTTP_BAD_REQUEST, null,
                    createPlainTextInputStream("400_bad_request_plaintext"),
                    "400_bad_request_plaintext"
                ),
                arrayOf(
                    HTTP_UNAUTHORIZED,
                    "gzip",
                    createGzippedInputStream("401_unauthorized_gzip"),
                    "401_unauthorized_gzip"
                ),
                arrayOf(
                    HTTP_UNAUTHORIZED, null,
                    createPlainTextInputStream("401_unauthorized_plaintext"),
                    "401_unauthorized_plaintext"
                ),
                arrayOf(
                    HTTP_FORBIDDEN,
                    "gzip",
                    createGzippedInputStream("403_forbidden_gzip"),
                    "403_forbidden_gzip"
                ),
                arrayOf(
                    HTTP_FORBIDDEN, null,
                    createPlainTextInputStream("403_forbidden_plaintext"),
                    "403_forbidden_plaintext"
                ),
                arrayOf(
                    HTTP_INTERNAL_ERROR,
                    "gzip",
                    createGzippedInputStream("500_internal_server_error_gzip"),
                    "500_internal_server_error_gzip"
                ),
                arrayOf(
                    HTTP_INTERNAL_ERROR, null,
                    createPlainTextInputStream("500_internal_server_error_plaintext"),
                    "500_internal_server_error_plaintext"
                ),
                arrayOf(
                    HTTP_UNAVAILABLE, "gzip",
                    createGzippedInputStream("503_unavailable_gzip"),
                    "503_unavailable_gzip"
                ),
                arrayOf(
                    HTTP_UNAVAILABLE, null,
                    createPlainTextInputStream("503_unavailable_plaintext"),
                    "503_unavailable_plaintext"
                ),
            )
        }

        @Test
        fun parse() {
            val connection = mockk<HttpURLConnection>(relaxed = true)
            every { connection.responseCode } returns responseCode
            every { connection.contentEncoding } returns contentEncoding
            every { connection.inputStream } throws IOException("http unsuccessful")
            every { connection.errorStream } returns errorStream

            val sut = HttpResponseParser()
            val result = sut.parse(connection)
            assertEquals(responseCode, result.status)
            assertEquals(expectedBody, result.body)

            verify { errorStream.close() }
        }
    }

    class HttpNonParameterizedTest {

        private lateinit var subject: HttpResponseParser
        private val connection = mockk<HttpURLConnection>(relaxed = true)

        private val headers = mapOf(
            "key_1" to listOf("value_1", "value_2"),
            "key_2" to listOf("value_3")
        )

        private val expectedHeaders = mapOf(
            "key_1" to "value_1, value_2",
            "key_2" to "value_3"
        )

        @Before
        fun setUp() {
            every { connection.responseCode } returns 200
            every { connection.contentEncoding } returns "gzip"
            every { connection.inputStream } returns createGzippedInputStream("200_ok_gzip")
            every { connection.headerFields } returns headers

            subject = HttpResponseParser()
        }

        @Test
        fun `when parse is called for a success, headers are returned in the HttpResponse`() {
            val result = subject.parse(connection)

            assertEquals(expectedHeaders, result.headers)
        }

        @Test
        fun `when parse is called for an error, headers are returned in the HttpResponse`() {
            every { connection.errorStream } returns createGzippedInputStream("500_internal_server_error_gzip")
            every { connection.inputStream } throws IOException("http unsuccessful")

            val result = subject.parse(connection)

            assertEquals(expectedHeaders, result.headers)
        }

        @Test
        fun `when parse is called, getHeaderFields is called on connection`() {
            subject.parse(connection)

            verify { connection.headerFields }
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
    return spyk(ByteArrayInputStream(gzippedBytes))
}

private fun createPlainTextInputStream(input: String): InputStream {
    return spyk(ByteArrayInputStream(input.toByteArray(Charsets.UTF_8)))
}
