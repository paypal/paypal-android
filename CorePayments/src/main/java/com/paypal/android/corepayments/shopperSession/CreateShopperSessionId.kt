package com.paypal.android.corepayments.shopperSession

import android.util.Log
import com.paypal.android.corepayments.PayPalSDK
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

class CreateShopperSessionId {

    suspend operator fun invoke(): String {
        PayPalSDK.coreConfig
            ?: throw IllegalStateException("CoreConfig must be set before creating a shopper session ID.")
        delay(1000.milliseconds) // Simulate network delay
        val timestamp =
            java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.US).format(java.util.Date())
        Log.d("PayPalSDK", "[$timestamp] Shopper session ID created successfully.")
        return "mock-shopper-session-id"
    }
}