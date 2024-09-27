package com.paypal.android.cardpayments

import com.paypal.android.corepayments.CoreConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
sealed class CardAuthMetadata {

    abstract val config: CoreConfig
    abstract val trackingId: String

    @Serializable
    class ApproveOrder(override val config: CoreConfig, override val trackingId: String, val orderId: String) :
        CardAuthMetadata()

    @Serializable
    class Vault(override val config: CoreConfig, override val trackingId: String, val setupTokenId: String) :
        CardAuthMetadata()

    fun encodeToString(): String = Json.encodeToString(this)

    companion object {
        fun decodeFromString(input: String) = tryDecodeFromString<CardAuthMetadata>(input)

        private inline fun <reified T> tryDecodeFromString(input: String): T? = try {
            Json.decodeFromString<T>(input)
        } catch (e: IllegalArgumentException) {
            null
        } catch (e: SerializationException) {
            null
        }
    }
}
