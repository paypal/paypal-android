package com.paypal.android.paymentbuttons

sealed class PaymentButtonEdges {
    data object Sharp : PaymentButtonEdges()
    data object Soft : PaymentButtonEdges()
    data object Pill : PaymentButtonEdges()
    class Custom(val cornerRadius: Float) : PaymentButtonEdges()

    companion object {

        const val PAYMENT_BUTTON_EDGE_INT_VALUE_DEFAULT = -1
        private const val PAYMENT_BUTTON_EDGE_INT_VALUE_SOFT = 0
        private const val PAYMENT_BUTTON_EDGE_INT_VALUE_PILL = 1
        private const val PAYMENT_BUTTON_EDGE_INT_VALUE_SHARP = 2

        /**
         * See attrs.xml for valid values.
         */
        fun fromInt(value: Int): PaymentButtonEdges? =
            when (value) {
                PAYMENT_BUTTON_EDGE_INT_VALUE_DEFAULT,
                PAYMENT_BUTTON_EDGE_INT_VALUE_SOFT,
                    -> Soft

                PAYMENT_BUTTON_EDGE_INT_VALUE_PILL -> Pill
                PAYMENT_BUTTON_EDGE_INT_VALUE_SHARP -> Sharp
                else -> null
            }
    }
}
