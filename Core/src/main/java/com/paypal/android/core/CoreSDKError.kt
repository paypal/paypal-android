package com.paypal.android.core

open class CoreSDKError(
    val code: Int?,
    val errorDescription: String?
) : Exception("Error: $code - Description: $errorDescription")
