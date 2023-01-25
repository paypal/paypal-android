package com.paypal.android.corepayments

/**
 * The status of an order.
 */
enum class OrderStatus {

    /**
     * The order was created
     */
    CREATED,

    /**
     * The order was approved with a valid payment source
     */
    APPROVED,

    /**
     * The order is completed and paid
     */
    COMPLETED,

    /**
     * The payer is required to take additional action before the order can be approved
     */
    PAYER_ACTION_REQUIRED
}
