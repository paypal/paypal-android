package com.paypal.android.paypalwebpayments.compose.internal

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity

/**
 * Recursively searches for a ComponentActivity in the Context hierarchy.
 *
 * @return The ComponentActivity if found, null otherwise
 */
internal fun Context.getActivityOrNull(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.getActivityOrNull()
    else -> null
}
