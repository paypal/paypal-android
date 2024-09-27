package com.paypal.android.cardpayments

import com.paypal.android.corepayments.CoreConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
sealed class CardAuthMetadata(val config: CoreConfig) {

    @Serializable
    class ApproveOrder(config: CoreConfig, val orderId: String) : CardAuthMetadata(config)

    @Serializable
    class Vault(config: CoreConfig, val setupTokenId: String) : CardAuthMetadata(config)

    fun encodeToString(): String = Json.encodeToString(this)

    companion object {
        fun decodeFromString(input: String): CardAuthMetadata? =
            tryDecodeFromString<ApproveOrder>(input) ?: tryDecodeFromString<Vault>(input)

        private inline fun <reified T> tryDecodeFromString(input: String): T? = try {
            Json.decodeFromString<T>(input)
        } catch (e: IllegalArgumentException) {
            null
        } catch (e: SerializationException) {
            null
        }
    }
}
