package com.paypal.android.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

// Ref: https://stackoverflow.com/a/68423182
fun Context.getActivityOrNull(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.getActivityOrNull()
    else -> null
}
