package com.paypal.android.googlepay

import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.Executor

// Ref: https://github.com/google-pay/android-quickstart/blob/f1caab23fdaadd8d133d31aeb612ecbdbdeda5ed/kotlin/app/src/main/java/com/google/android/gms/samples/pay/viewmodel/CheckoutViewModel.kt#L152
@OptIn(ExperimentalCoroutinesApi::class)
suspend fun <T> Task<T>.awaitTask(cancellationTokenSource: CancellationTokenSource? = null): Task<T> {
    return if (isComplete) this else suspendCancellableCoroutine { cont ->
        // Run the callback directly to avoid unnecessarily scheduling on the main thread.
        addOnCompleteListener(DirectExecutor) { task ->
            cont.resume(task, {
                // TODO: properly define cancelation handler
            })
        }

        cancellationTokenSource?.let { cancellationSource ->
            cont.invokeOnCancellation { cancellationSource.cancel() }
        }
    }
}

private object DirectExecutor : Executor {
    override fun execute(r: Runnable) {
        r.run()
    }
}