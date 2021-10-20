package com.paypal.android.checkout.pojo

import com.paypal.checkout.approve.Approval


data class Approval(
    val approvalData: ApprovalData? = null,
) {
    internal constructor(approval: Approval) : this(
        approvalData = ApprovalData(approval.data),
    )
}


data class ApprovalData(
    val payerId: String?,
    val orderId: String?,
    val paymentId: String?,
    val payer: Buyer? = null,
    val cart: Cart? = null
) {
    internal constructor(approvalData: com.paypal.checkout.approve.ApprovalData) : this(
        payerId = approvalData.payerId,
        orderId = approvalData.orderId,
        paymentId = approvalData.paymentId,
        payer = Buyer(approvalData.payer),
        cart = Cart(approvalData.cart)
    )
}


