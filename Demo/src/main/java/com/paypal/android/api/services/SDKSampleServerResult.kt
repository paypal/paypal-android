package com.paypal.android.api.services

import com.paypal.android.uishared.state.ActionState

sealed class SDKSampleServerResult<out S, out E> {
    data class Success<S>(val value: S) : SDKSampleServerResult<S, Nothing>()
    data class Failure<E>(val value: E) : SDKSampleServerResult<Nothing, E>()

    fun mapToActionState(): ActionState<S, E> = when (this) {
        is Success -> ActionState.Success(value)
        is Failure -> ActionState.Failure(value)
    }
}
