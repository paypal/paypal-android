package com.paypal.android.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

// SDKSampleServerAPI and CustomEnvironmentRepository both use @Inject constructors and
// @Singleton, so Hilt provides them automatically — no manual @Provides needed here.
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule
