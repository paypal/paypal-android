package com.paypal.android.di

import com.paypal.android.api.RetrofitClient
import com.paypal.android.api.services.PayPalDemoApi
import org.koin.dsl.module
import retrofit2.Retrofit

val apiModule = module {

    single {
        provideApi<PayPalDemoApi>(RetrofitClient.instance)
    }

}

inline fun <reified T> provideApi(retrofit: Retrofit): T {
    return retrofit.create(T::class.java)
}
