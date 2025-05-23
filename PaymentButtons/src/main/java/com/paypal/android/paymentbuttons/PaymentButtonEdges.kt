package com.paypal.android.paymentbuttons

sealed class PaymentButtonEdges {
    data object Sharp : PaymentButtonEdges()
    data object Soft : PaymentButtonEdges()
    data object Pill : PaymentButtonEdges()
    class Custom(val cornerRadius: Float) : PaymentButtonEdges()

    companion object {

        const val PAYMENT_BUTTON_EDGE_DEFAULT_INT_VALUE = -1

        /**
         * See attrs.xml for valid values.
         */
        fun fromInt(value: Int): PaymentButtonEdges? =
            when (value) {
                0, PAYMENT_BUTTON_EDGE_DEFAULT_INT_VALUE -> Soft
                1 -> Pill
                2 -> Sharp
                else -> null
            }
    }
}
