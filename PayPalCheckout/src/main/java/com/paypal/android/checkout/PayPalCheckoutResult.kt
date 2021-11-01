package com.paypal.android.checkout

import com.paypal.android.checkout.pojo.Approval
import com.paypal.android.checkout.pojo.ErrorInfo
import com.paypal.android.checkout.pojo.ShippingChangeData

sealed class PayPalCheckoutResult {
    class Success(val approval: Approval) : PayPalCheckoutResult()
    class Failure(val error: ErrorInfo) : PayPalCheckoutResult()
    class ShippingChange(val shippingChangeData: ShippingChangeData) : PayPalCheckoutResult()
    object Cancellation : PayPalCheckoutResult()
}
