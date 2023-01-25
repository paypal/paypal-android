package com.paypal.android.corepayments

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertThrows

inline fun <reified T : Throwable> assertThrows(
    noinline executable: suspend () -> Unit
) = assertThrows(T::class.java) {
    runBlocking {
        executable()
    }
}
