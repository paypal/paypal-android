package com.paypal.android.core

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HttpIT {

    @ExperimentalCoroutinesApi
    @Test
    fun itMakesAnHttpRequest() = runBlockingTest {
        val http = Http()
        val request = HttpRequest("GET")
        val result = http.send(request)

        assertEquals(200, result.responseCode)
    }
}
