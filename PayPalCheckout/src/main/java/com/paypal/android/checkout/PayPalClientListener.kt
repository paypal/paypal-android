package com.paypal.android.checkout

import com.paypal.checkout.approve.Approval
import com.paypal.checkout.error.ErrorInfo
import com.paypal.checkout.shipping.ShippingChangeActions
import com.paypal.checkout.shipping.ShippingChangeData

//might be subject to change, we could go to callback pattern
//are we going to wrap the objects PayPal returns?
interface PayPalClientListener {
    fun onPayPalApprove(approval: Approval)
    fun onPayPalError(errorInfo: ErrorInfo)
    fun onPayPalCancel()
    fun onPayPalShippingAddressChange(shippingChangeData: ShippingChangeData, shippingChangeActions: ShippingChangeActions)
}