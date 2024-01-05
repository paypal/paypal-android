package com.paypal.android.uishared.state

sealed class ActionState<out S, out E> {
    object Ready : ActionState<Nothing, Nothing>()
    object Loading : ActionState<Nothing, Nothing>()
    data class Success<S>(val value: S) : ActionState<S, Nothing>()
    data class Failure<E>(val value: E) : ActionState<Nothing, E>()
}
