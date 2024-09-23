package com.paypal.android.cardpayments

/**
 * Implement this callback to receive results from [CardClient].
 */
fun interface CardApproveOrderListener {
    fun onCardApproveOrderResult(result: CardApproveOrderResult)
}
