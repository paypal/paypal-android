package com.paypal.android.corepayments

import androidx.annotation.RestrictTo

/**
 * The status of an order.
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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
