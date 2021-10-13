package com.paypal.android.di

import com.paypal.android.api.services.PayPalDemoApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

private const val CONNECT_TIMEOUT_IN_SEC = 20L
private const val READ_TIMEOUT_IN_SEC = 30L
private const val WRITE_TIMEOUT_IN_SEC = 30L

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    fun provideRetrofitService(): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://ppcp-sample-merchant-sand.herokuapp.com")
            .client(initHttpClientAndInterceptor())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    fun providePaypalApi(): PayPalDemoApi {
        return provideApi<PayPalDemoApi>(provideRetrofitService())
    }

    private fun initHttpClientAndInterceptor(): OkHttpClient {
        val okHttpBuilder = OkHttpClient.Builder()
        // Timeouts
        okHttpBuilder.connectTimeout(CONNECT_TIMEOUT_IN_SEC, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_IN_SEC, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT_IN_SEC, TimeUnit.SECONDS)

        return okHttpBuilder.build()
    }
}

inline fun <reified T> provideApi(retrofit: Retrofit): T {
    return retrofit.create(T::class.java)
}
