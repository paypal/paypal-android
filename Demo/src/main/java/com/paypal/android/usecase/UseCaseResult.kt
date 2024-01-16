package com.paypal.android.usecase

sealed class UseCaseResult<out S, out E> {
    data class Success<S>(val value: S) : UseCaseResult<S, Nothing>()
    data class Failure<E>(val value: E) : UseCaseResult<Nothing, E>()
}
