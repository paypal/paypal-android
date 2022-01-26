package com.paypal.android.checkout.paymentbutton.error

import com.paypal.android.core.CoreSDKError

class PayPalSDKError(errorDescription: String?): CoreSDKError(0, errorDescription)