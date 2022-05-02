package com.paypal.android.checkout

import java.util.UUID

/**
 * @param min is the minimum value that will be generated, defaults to 0.
 * @param max is the maximum value that will be generated, defaults to 1,000.
 * @return a random Int
 */
fun generateRandomInt(min: Int = 0, max: Int = 1_000): Int = (min..max).random()

/**
 * @return a random String formatted as a [UUID]
 */
fun generateRandomString() = UUID.randomUUID().toString()

/**
 * @return a random string with only alpha characters.
 */
fun generateRandomAlphaString(): String {
    return (1..10).map { (('A'..'Z') + ('a'..'z')).random() }.joinToString("")
}

/**
 * @return a random url with the top level
 */
fun generateRandomUrl(withSsl: Boolean = true, tld: String = "com"): String {
    val scheme = if (withSsl) "https" else "http"
    val domain = generateRandomString()
    return "$scheme://$domain.$tld"
}

/**
 * @return a random [Boolean]
 */
fun generateRandomBoolean(): Boolean = (0..1).random() == 0
