package com.paypal.android.core

/**
 * Error returned from the SDK
 *
 * @property code The error code
 * @property errorDescription A description of the error that occurred
 */
open class CoreSDKError(
    val code: Int?,
    val errorDescription: String?
) : Exception("Error: $code - Description: $errorDescription")
