package com.paypal.android.utils

import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Parcelable

// Ref: https://stackoverflow.com/a/73311814
inline fun <reified T : Parcelable> Bundle.parcelable(key: String): T? = when {
    SDK_INT >= Build.VERSION_CODES.TIRAMISU -> getParcelable(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelable(key) as? T
}
