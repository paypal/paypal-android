package com.paypal.android.utils

import android.content.Context
import android.content.ContextWrapper
import androidx.appcompat.app.AppCompatActivity

// Ref: https://stackoverflow.com/a/68423182
fun Context.getActivityOrNull(): AppCompatActivity? = when (this) {
    is AppCompatActivity -> this
    is ContextWrapper -> baseContext.getActivityOrNull()
    else -> null
}
