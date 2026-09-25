package com.paypal.android.corepayments.browserswitch

import android.net.Uri

internal data class AuthTabResult(
    val resultCode: Int,
    val resultUri: Uri?,
)
