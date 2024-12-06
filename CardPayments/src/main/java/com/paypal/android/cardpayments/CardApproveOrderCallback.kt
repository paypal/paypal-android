package com.paypal.android.cardpayments

import androidx.annotation.MainThread

fun interface CardApproveOrderCallback {
    /**
     * Called when the order is approved.
     */
    @MainThread
    fun onApproveOrderResult(result: CardResult.ApproveOrder)
}