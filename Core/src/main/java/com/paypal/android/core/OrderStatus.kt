package com.paypal.android.core

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
    COMPLETED
}
