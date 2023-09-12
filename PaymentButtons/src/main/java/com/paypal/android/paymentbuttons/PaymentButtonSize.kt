package com.paypal.android.paymentbuttons

import com.paypal.android.paymentbuttons.error.createFormattedIllegalArgumentException
import com.paypal.android.ui.R

/**
 * Defines the sizes available for payment buttons. If no size is provided then it will
 * default to medium. Button size is used for setting properties like minimum width and height to
 * allow for the use of wrap_content where necessary along with modifying the size of the wordmark
 * and adjusting padding within the button.
 *
 * @see SMALL used when a smaller button is desired, the minimum height will be 48dp with the wordmark
 * having a height of 15dp and width of 61dp.
 * @see MEDIUM this is the default button size, the minimum height will be 48dp with the wordmark having
 * a height of 20dp and width of 79dp.
 * @see LARGE this is the largest button size, the minimum height will be 26dp with the wordmark having
 * a height of 26dp and width of 103dp.
 */
enum class PaymentButtonSize(
    val value: Int,
    val minHeightResId: Int,
    val verticalPaddingResId: Int,
    val labelTextSizeResId: Int
) {
    SMALL(
        value = 0,
        minHeightResId = R.dimen.paypal_payment_button_min_height,
        verticalPaddingResId = R.dimen.paypal_payment_button_vertical_padding,
        labelTextSizeResId = R.dimen.paypal_payment_button_label_text_size
    ),
    MEDIUM(
        value = 1,
        minHeightResId = R.dimen.paypal_payment_button_min_height,
        verticalPaddingResId = R.dimen.paypal_payment_button_vertical_padding,
        labelTextSizeResId = R.dimen.paypal_payment_button_label_text_size
    ),
    LARGE(
        value = 2,
        minHeightResId = R.dimen.paypal_payment_button_min_height_large,
        verticalPaddingResId = R.dimen.paypal_payment_button_vertical_padding_large,
        labelTextSizeResId = R.dimen.paypal_payment_button_label_text_size_large
    );

    companion object {
        /**
         * Given an [attributeIndex] this will provide the correct [PaymentButtonSize].
         * If an invalid [attributeIndex] is provided then it will throw an [IllegalArgumentException].
         *
         * @throws [IllegalArgumentException] when an invalid index is provided.
         */
        operator fun invoke(attributeIndex: Int): PaymentButtonSize {
            return when (attributeIndex) {
                SMALL.value -> SMALL
                MEDIUM.value -> MEDIUM
                LARGE.value -> LARGE
                else -> throw createFormattedIllegalArgumentException("PaymentButtonSize", values().size)
            }
        }
    }
}
