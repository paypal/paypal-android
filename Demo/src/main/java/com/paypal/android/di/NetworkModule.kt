package com.paypal.android.di

import com.paypal.android.api.services.SDKSampleServerApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

private const val CONNECT_TIMEOUT_IN_SEC = 20L
private const val READ_TIMEOUT_IN_SEC = 30L
private const val WRITE_TIMEOUT_IN_SEC = 30L

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val SAMPLE_SERVER_BASE_URL = "https://sdk-sample-merchant-server.herokuapp.com/"
    private const val SANDBOX_BASE_URL = "https://api.sandbox.paypal.com"

    private fun provideRetrofitService(
        baseUrl: String,
        vararg interceptors: Interceptor
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(initHttpClientAndInterceptor(*interceptors))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    fun provideSDKSampleServerApi(): SDKSampleServerApi {
        return provideApi(provideRetrofitService(SAMPLE_SERVER_BASE_URL))
    }

    private fun initHttpClientAndInterceptor(vararg interceptors: Interceptor): OkHttpClient {
        val okHttpBuilder = OkHttpClient.Builder()
        val httpLoggingInterceptor = HttpLoggingInterceptor()
        httpLoggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
        // Timeouts
        okHttpBuilder.connectTimeout(CONNECT_TIMEOUT_IN_SEC, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_IN_SEC, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT_IN_SEC, TimeUnit.SECONDS)
        interceptors.forEach { interceptor -> okHttpBuilder.addInterceptor(interceptor) }
        okHttpBuilder.addInterceptor(httpLoggingInterceptor)
        return okHttpBuilder.build()
    }
}

inline fun <reified T> provideApi(retrofit: Retrofit): T {
    return retrofit.create(T::class.java)
}
