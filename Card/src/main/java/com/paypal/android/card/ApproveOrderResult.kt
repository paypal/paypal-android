package com.paypal.android.card

import com.paypal.android.core.OrderError

sealed class ApproveOrderResult(
    val correlationID: String?
) {

    class Success(
        val cardOrder: CardOrder,
        correlationID: String?
    ) : ApproveOrderResult(correlationID)

    class Error(
        val orderError: OrderError,
        correlationID: String?
    ) : ApproveOrderResult(correlationID)
}
