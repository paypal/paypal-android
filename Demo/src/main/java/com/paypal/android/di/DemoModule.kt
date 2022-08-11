package com.paypal.android.di

import com.paypal.android.core.CoreConfig
import com.paypal.android.core.Environment
import com.paypal.android.core.api.EligibilityAPI
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object DemoModule {

    @Provides
    fun provideEligibilityAPI(
        coreConfig: CoreConfig
    ): EligibilityAPI = EligibilityAPI(coreConfig)

    @Provides
    fun provideCoreConfig(): CoreConfig = CoreConfig(
        environment = Environment.SANDBOX
    )

}
