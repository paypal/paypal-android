package com.paypal.android.core

import android.util.Base64

internal fun String.base64encoded(): String {
    val bytes = toByteArray(Charsets.UTF_8)
    return Base64.encodeToString(bytes, Base64.NO_WRAP)
}
