package com.paypal.android.cardpayments

import androidx.annotation.MainThread

fun interface CardApproveOrderCallback {
    /**
     * Called when the order is approved.
     *
     * @param result [CardApproveOrderResult] result with details
     */
    @MainThread
    fun onCardApproveOrderResult(result: CardApproveOrderResult)
}
