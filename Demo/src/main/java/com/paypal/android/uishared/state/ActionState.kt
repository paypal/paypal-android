package com.paypal.android.uishared.state

sealed class ActionState<out S, out E> {
    data object Idle : ActionState<Nothing, Nothing>()
    data object Loading : ActionState<Nothing, Nothing>()
    data class Success<S>(val value: S) : ActionState<S, Nothing>()
    data class Failure<E>(val value: E) : ActionState<Nothing, E>()

    val isComplete: Boolean
        get() = (this is Success) || (this is Failure)
}
