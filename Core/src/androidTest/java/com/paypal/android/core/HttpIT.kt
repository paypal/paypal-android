package com.paypal.android.core

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HttpIT {

    @ExperimentalCoroutinesApi
    lateinit var testDispatcher: TestCoroutineDispatcher

    @ExperimentalCoroutinesApi
    @Before
    fun beforeEach() {
        testDispatcher = TestCoroutineDispatcher()
    }

    @ExperimentalCoroutinesApi
    @After
    fun afterEach() {
        testDispatcher.cleanupTestCoroutines()
    }

    @ExperimentalCoroutinesApi
    @Test
    fun itMakesAnHttpRequest() = runBlockingTest {
        val request = HttpRequest().apply {
            method = "GET"
            host = "www.google.com"
            path = "/"
        }

        val result = http.send(request, testDispatcher)
        assertEquals(200, result.responseCode)
    }
}
