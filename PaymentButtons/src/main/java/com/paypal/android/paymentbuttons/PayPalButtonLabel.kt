package com.paypal.android.paymentbuttons

import android.content.Context
import com.paypal.android.paymentbuttons.error.createFormattedIllegalArgumentException
import com.paypal.android.ui.R

/**
 * Defines the labels available for payment buttons. If no label is provided then it will
 * default to [PAYPAL] which will not display a label and will only display the PayPal wordmark. For
 * other labels they will have the label value itself along with a position either at the start or
 * the end of the button.
 *
 */
enum class PayPalButtonLabel(
    val value: Int,
    val position: Position? = null,
    private val stringResId: Int? = null
) {
    /**
     * Label for PayPal text
     */
    PAYPAL(value = 0),

    /**
     * Label for Checkout text
     */
    CHECKOUT(
        value = 1,
        position = Position.END,
        stringResId = R.string.paypal_checkout_smart_payment_button_label_checkout
    ),

    /**
     * Label for Buy Now text
     */
    BUY_NOW(
        value = 2,
        position = Position.END,
        stringResId = R.string.paypal_checkout_smart_payment_button_label_buy_now
    ),

    /**
     * Label for Pay text
     */
    PAY(
        value = 3,
        position = Position.START,
        stringResId = R.string.paypal_checkout_smart_payment_button_label_pay
    ),

    /**
     * Label for Pay Later text
     */
    PAY_LATER(
        value = 4,
        position = Position.END,
        stringResId = R.string.paypal_checkout_smart_payment_button_label_pay_later
    ),

    /**
     * Label for Add Money with
     */
    ADD_MONEY_WITH(
        value = 5,
        position = Position.START,
        stringResId = R.string.paypal_checkout_add_money_with_label
    ),

    /**
     * Label for Book with
     */
    BOOK_WITH(
        value = 6,
        position = Position.START,
        stringResId = R.string.paypal_checkout_book_with_label
    ),

    /**
     * Label for Buy with
     */
    BUY_WITH(
        value = 7,
        position = Position.START,
        stringResId = R.string.paypal_checkout_buy_with_label
    ),

    /**
     * Label for Buy Now with
     */
    BUY_NOW_WITH(
        value = 8,
        position = Position.START,
        stringResId = R.string.paypal_checkout_buy_now_with_label
    ),

    /**
     * Label for Checkout with
     */
    CHECKOUT_WITH(
        value = 8,
        position = Position.START,
        stringResId = R.string.paypal_checkout_checkout_with_label
    ),

    /**
     * Label for Credit with
     */
    CREDIT_WITH(
        value = 9,
        position = Position.START,
        stringResId = R.string.paypal_checkout_credit_label
    ),

    /**
     * Label for Continue with
     */
    CONTINUE_WITH(
        value = 10,
        position = Position.START,
        stringResId = R.string.paypal_checkout_continue_with_label
    ),

    /**
     * Label for Order with
     */
    ORDER_WITH(
        value = 11,
        position = Position.START,
        stringResId = R.string.paypal_checkout_order_with_label
    ),

    /**
     * Label for Pay with
     */
    PAY_WITH(
        value = 12,
        position = Position.START,
        stringResId = R.string.paypal_checkout_pay_with_label
    ),

    /**
     * Label for Pay Later with
     */
    PAY_LATER_WITH(
        value = 13,
        position = Position.START,
        stringResId = R.string.paypal_checkout_pay_later_with_label
    ),

    /**
     * Label for Reload with
     */
    RELOAD_WITH(
        value = 14,
        position = Position.START,
        stringResId = R.string.paypal_checkout_reload_with_label
    ),

    /**
     * Label for Rent with
     */
    RENT_WITH(
        value = 15,
        position = Position.START,
        stringResId = R.string.paypal_checkout_rent_with_label
    ),

    /**
     * Label for Reserve with
     */
    RESERVE_WITH(
        value = 16,
        position = Position.START,
        stringResId = R.string.paypal_checkout_reserve_with_label
    ),

    /**
     * Label for Subscribe with
     */
    SUBSCRIBE_WITH(
        value = 17,
        position = Position.START,
        stringResId = R.string.paypal_checkout_subscribe_with_label
    ),

    /**
     * Label for Support with
     */
    SUPPORT_WITH(
        value = 18,
        position = Position.START,
        stringResId = R.string.paypal_checkout_support_with_label
    ),

    /**
     * Label for Tip with
     */
    TIP_WITH(
        value = 19,
        position = Position.START,
        stringResId = R.string.paypal_checkout_tip_with_label
    ),

    /**
     * Label for Top Up with
     */
    TOP_UP_WITH(
        value = 20,
        position = Position.START,
        stringResId = R.string.paypal_checkout_top_up_with_label
    );

    fun retrieveLabel(context: Context): String? {
        return stringResId?.let { context.getString(it) }
    }

    /**
     * Defines at what position a label is displayed. A label can either be positioned at the start
     * or end of a button.
     */
    enum class Position {
        START,
        END;
    }

    companion object {
        /**
         * Given an [attributeIndex] this will provide the correct [PayPalButtonLabel].
         * If an invalid [attributeIndex] is provided then it will throw an [IllegalArgumentException].
         *
         * @throws [IllegalArgumentException] when an invalid index is provided.
         */
        operator fun invoke(attributeIndex: Int): PayPalButtonLabel {
            return when (attributeIndex) {
                PAYPAL.value -> PAYPAL
                CHECKOUT.value -> CHECKOUT
                BUY_NOW.value -> BUY_NOW
                PAY.value -> PAY
                PAY_LATER.value -> PAY_LATER
                else -> throw createFormattedIllegalArgumentException("PaymentButtonLabel", values().size)
            }
        }
    }
}
