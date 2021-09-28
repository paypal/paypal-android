package com.paypal.android.di

import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import org.koin.dsl.module
import java.util.concurrent.TimeUnit

private const val AUTH_HEADER_VALUE_START = "Token "

private const val CONNECT_TIMEOUT_IN_SEC = 20L
private const val READ_TIMEOUT_IN_SEC = 30L
private const val WRITE_TIMEOUT_IN_SEC = 30L

val networkModule = module {

    single {
        initHttpClientAndInterceptor()
    }

    single {
        GsonBuilder()
            // Additional params
            .create()
    }
}

private fun initHttpClientAndInterceptor(): OkHttpClient {
    val okHttpBuilder = OkHttpClient.Builder()
    // Timeouts
    okHttpBuilder.connectTimeout(CONNECT_TIMEOUT_IN_SEC, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT_IN_SEC, TimeUnit.SECONDS)
        .writeTimeout(WRITE_TIMEOUT_IN_SEC, TimeUnit.SECONDS)

    return okHttpBuilder.build()
}

private fun prepareTokenHeaderValue(token: String): String {
    return AUTH_HEADER_VALUE_START + token
}