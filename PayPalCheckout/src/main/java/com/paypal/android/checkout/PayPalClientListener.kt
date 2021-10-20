package com.paypal.android.checkout

import com.paypal.android.checkout.pojo.Approval
import com.paypal.android.checkout.pojo.ErrorInfo
import com.paypal.android.checkout.pojo.ShippingChangeData
import com.paypal.android.checkout.shipping.ShippingChangeActions

interface PayPalClientListener {
    fun onPayPalApprove(approval: Approval)
    fun onPayPalError(errorInfo: ErrorInfo)
    fun onPayPalCancel()
    fun onPayPalShippingAddressChange(shippingChangeData: ShippingChangeData, shippingChangeActions: ShippingChangeActions)
}