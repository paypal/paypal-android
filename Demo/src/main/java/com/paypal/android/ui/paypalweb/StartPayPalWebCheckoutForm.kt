package com.paypal.android.ui.paypalweb

// Intentionally left empty. The "FUNDING SOURCE" form previously defined here (Step 2 of the
// PayPal Checkout demo) was removed since it was dead code: startCheckoutWithOrderId calls the
// 2-arg PayPalWebCheckoutClient.start(activity, orderId) overload, which never consumed the
// selected funding source. Payment method selection now happens in Step 1 via
// PayPalUiState.paymentMethodOption, which is sent as payment_source.paypal.experience_context
// .payment_method_selected on order creation.
