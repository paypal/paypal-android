package com.paypal.android.uishared.state

sealed class CompletedActionState<out S, out E> {
    data class Success<S>(val value: S) : CompletedActionState<S, Nothing>()
    data class Failure<E>(val value: E) : CompletedActionState<Nothing, E>()
}
