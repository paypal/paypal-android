package com.paypal.android.utils

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity

// Ref: https://stackoverflow.com/a/68423182
fun Context.getActivityOrNull(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.getActivityOrNull()
    else -> null
}
