package com.paypal.android.usecase

import com.paypal.android.uishared.state.ActionState

sealed class UseCaseResult<out S, out E> {
    data class Success<S>(val value: S) : UseCaseResult<S, Nothing>()
    data class Failure<E>(val value: E) : UseCaseResult<Nothing, E>()

    fun mapToActionState(): ActionState<S, E> = when (this) {
        is Success -> ActionState.Success(value)
        is Failure -> ActionState.Failure(value)
    }
}
