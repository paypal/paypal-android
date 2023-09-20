package com.paypal.android.paymentbuttons.error

internal fun createFormattedIllegalArgumentException(enumName: String, enumValues: Int): IllegalArgumentException {
    val exceptionMessage = "Attempted to create a $enumName with an invalid index. Please use an" +
            " index that is between 0 and ${enumValues - 1} and try again."
    return IllegalArgumentException(exceptionMessage)
}
