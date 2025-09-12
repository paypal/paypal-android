package com.paypal.android.api.services

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Converter

class KotlinSerializationConverterFactory private constructor() {
    companion object {
        fun create(
            json: Json = Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            }
        ): Converter.Factory {
            val contentType = "application/json".toMediaType()
            return json.asConverterFactory(contentType)
        }
    }
}