package com.paypal.android.cardpayments

import androidx.annotation.MainThread

object CardCallback {
    fun interface ApproveOrder {
        /**
         * Called when the order is approved.
         */
        @MainThread
        fun onApproveOrderResult(result: CardResult.ApproveOrder)
    }
}
