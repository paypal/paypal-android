package com.paypal.android.checkout

import com.paypal.android.checkout.pojo.Approval
import com.paypal.android.checkout.pojo.ErrorInfo
import com.paypal.android.checkout.pojo.ShippingChangeData
import com.paypal.android.checkout.shipping.ShippingChangeActions

//might be subject to change, we could go to callback pattern
//are we going to wrap the objects PayPal returns?
interface PayPalClientListener {
    fun onPayPalApprove(approval: Approval)
    fun onPayPalError(errorInfo: ErrorInfo)
    fun onPayPalCancel()
    fun onPayPalShippingAddressChange(shippingChangeData: ShippingChangeData, shippingChangeActions: ShippingChangeActions)
}