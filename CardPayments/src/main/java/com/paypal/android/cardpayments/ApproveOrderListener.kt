package com.paypal.android.cardpayments

import androidx.annotation.MainThread
import com.paypal.android.corepayments.PayPalSDKError

/**
 * Implement this callback to receive results from [CardClient].
 */
fun interface ApproveOrderListener {
    fun onApproveOrderResult(result: CardApproveOrderResult)
}
