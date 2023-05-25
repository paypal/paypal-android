package com.paypal.android.di

import com.paypal.android.api.services.SDKSampleServerAPI
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val SAMPLE_SERVER_BASE_URL = "https://sdk-sample-merchant-server.herokuapp.com/"

    @Provides
    fun provideSDKSampleServerAPI(): SDKSampleServerAPI = SDKSampleServerAPI(SAMPLE_SERVER_BASE_URL)
}
