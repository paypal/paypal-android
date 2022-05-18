package com.paypal.android.di

import com.paypal.android.core.CoreConfig
import com.paypal.android.core.Environment
import com.paypal.android.core.api.EligibilityAPI
import com.paypal.android.core.graphql.common.GraphQlClient
import com.paypal.android.core.graphql.common.GraphQlClientImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object DemoModule {

    @Provides
    fun provideEligibilityAPI(
        graphQlClient: GraphQlClient,
        coreConfig: CoreConfig
    ): EligibilityAPI = EligibilityAPI(graphQlClient, coreConfig)

    @Provides
    fun provideGraphQlClient(): GraphQlClient = GraphQlClientImpl()

    @Provides
    fun provideCoreConfig(): CoreConfig = CoreConfig(
        clientId = com.paypal.android.BuildConfig.CLIENT_ID,
        environment = Environment.SANDBOX
    )
}
