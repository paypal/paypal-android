package com.paypal.android.cardpayments

import androidx.annotation.MainThread

fun interface CardApproveOrderCallback {
    /**
     * Called when the order is approved.
     *
     * @param result [CardResult.ApproveOrder] result with details
     */
    @MainThread
    fun onApproveOrderResult(result: CardApproveOrderResult)
}