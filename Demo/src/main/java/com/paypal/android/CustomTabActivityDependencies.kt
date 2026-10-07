package com.paypal.android

import com.paypal.android.customenvironment.CustomEnvironmentRepository
import com.paypal.android.usecase.CompleteOrderUseCase
import com.paypal.android.usecase.CreateOrderUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface CustomTabActivityDependencies {
    fun customEnvironmentRepository(): CustomEnvironmentRepository
    fun createOrderUseCase(): CreateOrderUseCase
    fun completeOrderUseCase(): CompleteOrderUseCase
}
