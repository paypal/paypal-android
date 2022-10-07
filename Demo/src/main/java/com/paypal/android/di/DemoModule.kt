package com.paypal.android.di

import com.paypal.android.core.CoreConfig
import com.paypal.android.core.Environment
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object DemoModule {

    @Provides
    fun provideCoreConfig(): CoreConfig = CoreConfig(
        environment = Environment.SANDBOX
    )
}
