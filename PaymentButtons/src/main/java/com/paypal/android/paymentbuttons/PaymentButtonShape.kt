package com.paypal.android.paymentbuttons

import com.paypal.android.paymentbuttons.error.createFormattedIllegalArgumentException

/**
 * Defines the shapes available for payment buttons. If no shape is provided then the default
 * button style will be retrieved from the applications root style.
 *
 * @see ROUNDED will render the button with rounded corners.
 * @see PILL will render the button with circular corners on either side, it looks like a pill.
 * @see RECTANGLE will render the button with sharp corners, it will look like a rectangle.
 */
enum class PaymentButtonShape(val value: Int) {
    ROUNDED(value = 0),
    PILL(value = 1),
    RECTANGLE(value = 2);

    companion object {
        /**
         * Given an [attributeIndex] this will provide the correct [PaymentButtonShape]. If an
         * invalid [attributeIndex] is provided then it will throw an [IllegalArgumentException].
         *
         * @throws [IllegalArgumentException] when an invalid index is provided.
         */
        operator fun invoke(attributeIndex: Int): PaymentButtonShape {
            return when (attributeIndex) {
                ROUNDED.value -> ROUNDED
                PILL.value -> PILL
                RECTANGLE.value -> RECTANGLE
                else -> throw createFormattedIllegalArgumentException("PaymentButtonShape", values().size)
            }
        }
    }
}
