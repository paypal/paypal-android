package com.paypal.android.paymentbuttons

sealed class PaymentButtonEdges {
    data object Sharp : PaymentButtonEdges()
    data object Soft : PaymentButtonEdges()
    data object Pill : PaymentButtonEdges()
    class Custom(val cornerRadius: Float) : PaymentButtonEdges()
}
