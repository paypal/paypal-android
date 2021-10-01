package com.paypal.android.di

import android.annotation.SuppressLint
import com.paypal.android.api.services.PayPalDemoApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

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
            .client(getUnsafeOkHttpClient().build())
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

    private fun getUnsafeOkHttpClient(): OkHttpClient.Builder {
        // Create a trust manager that does not validate certificate chains
        val trustAllCerts: Array<TrustManager> = arrayOf(
            @SuppressLint("CustomX509TrustManager")
            object : X509TrustManager {
                @Throws(CertificateException::class)
                override fun checkClientTrusted(
                    chain: Array<X509Certificate?>?,
                    authType: String?
                ) = Unit

                @Throws(CertificateException::class)
                override fun checkServerTrusted(
                    chain: Array<X509Certificate?>?,
                    authType: String?
                ) = Unit

                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            }
        )
        // Install the all-trusting trust manager
        val sslContext: SSLContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, SecureRandom())
        // Create an ssl socket factory with our all-trusting manager
        val sslSocketFactory: SSLSocketFactory = sslContext.socketFactory
        val builder = OkHttpClient.Builder()
        builder.sslSocketFactory(
            sslSocketFactory,
            trustAllCerts[0] as X509TrustManager
        )
        builder.hostnameVerifier { _, _ -> true }
        return builder
    }
}

inline fun <reified T> provideApi(retrofit: Retrofit): T {
    return retrofit.create(T::class.java)
}
