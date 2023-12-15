package com.paypal.android.uishared.state

sealed class ActionButtonState<out S, out E> {
    object Ready : ActionButtonState<Nothing, Nothing>()
    object Loading : ActionButtonState<Nothing, Nothing>()
    data class Success<S>(val value: S) : ActionButtonState<S, Nothing>()
    data class Failure<E>(val value: E) : ActionButtonState<Nothing, E>()
}
