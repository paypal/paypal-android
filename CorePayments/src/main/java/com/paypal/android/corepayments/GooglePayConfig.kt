package com.paypal.android.corepayments

data class GooglePayConfig(
    val isEligible: Boolean,
    val allowedPaymentMethods: List<AllowedPaymentMethod>?,
    val merchantInfo: MerchantInfoData?
)
