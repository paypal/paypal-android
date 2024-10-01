package com.paypal.android.paypalwebpayments

import com.paypal.android.corepayments.CoreConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
sealed class PayPalAuthMetadata {

    abstract val config: CoreConfig
    abstract val trackingId: String

    @Serializable
    class Checkout(override val config: CoreConfig, override val trackingId: String, val orderId: String) :
        PayPalAuthMetadata()

    @Serializable
    class Vault(override val config: CoreConfig, override val trackingId: String, val setupTokenId: String) :
        PayPalAuthMetadata()

    fun encodeToString(): String = Json.encodeToString(this)

    companion object {
        fun decodeFromString(input: String) = tryDecodeFromString<PayPalAuthMetadata>(input)

        private inline fun <reified T> tryDecodeFromString(input: String): T? = try {
            Json.decodeFromString<T>(input)
        } catch (e: IllegalArgumentException) {
            null
        } catch (e: SerializationException) {
            null
        }
    }
}
