package com.paypal.android.checkout;

import androidx.annotation.NonNull;

/**
 * Interface for receiving results of the PayPal payment flow.
 */
public interface PayPalCheckoutListener {
    void onPayPalCheckoutResult(@NonNull PayPalCheckoutResult result);
}
