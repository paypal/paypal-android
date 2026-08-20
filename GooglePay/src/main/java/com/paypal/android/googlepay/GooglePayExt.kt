package com.paypal.android.googlepay

import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.Executor
import kotlin.coroutines.resume

// Ref: https://github.com/google-pay/android-quickstart/blob/f1caab23fdaadd8d133d31aeb612ecbdbdeda5ed/kotlin/app/src/main/java/com/google/android/gms/samples/pay/viewmodel/CheckoutViewModel.kt#L152
// Ref: https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/suspend-cancellable-coroutine.html
@OptIn(ExperimentalCoroutinesApi::class)
suspend fun <T> Task<T>.awaitTask(): Task<T> = suspendCancellableCoroutine { continuation ->
    // Run the callback directly to avoid unnecessarily scheduling on the main thread.
    addOnCompleteListener { task ->
        continuation.resume(task)
    }
}
