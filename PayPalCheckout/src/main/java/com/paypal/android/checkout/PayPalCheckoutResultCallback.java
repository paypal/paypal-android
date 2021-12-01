package com.paypal.android.checkout;

import androidx.annotation.NonNull;

public interface PayPalCheckoutResultCallback {
    void onPayPalCheckoutResult(@NonNull PayPalCheckoutResult result);
}
