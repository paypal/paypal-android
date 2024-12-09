package com.paypal.android.cardpayments

sealed class CardStatus {

    class UnknownError(val error: Throwable) : CardStatus()
    data object NoResult : CardStatus()
}
