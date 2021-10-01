package com.paypal.android.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // Dev  https://ppcp-sample-merchant-sand.herokuapp.com
    // Sandbox  https://api.sandbox.paypal.com
    // Live     https://api.paypal.com
    private const val BASE_URL = "https://ppcp-sample-merchant-sand.herokuapp.com"

    val instance: Retrofit
        get() {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
}
