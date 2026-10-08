package com.paypal.android

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.paypal.android.customenvironment.SettingsViewModel
import com.paypal.android.ui.paypal.PayPalCheckoutViewModel

internal class CustomTabViewModelFactory(
    private val applicationContext: Context,
    private val dependencies: CustomTabActivityDependencies,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when (modelClass) {
        PayPalCheckoutViewModel::class.java -> PayPalCheckoutViewModel(
            applicationContext = applicationContext,
            createOrderUseCase = dependencies.createOrderUseCase(),
            completeOrderUseCase = dependencies.completeOrderUseCase(),
            customEnvironmentRepository = dependencies.customEnvironmentRepository(),
        ) as T
        SettingsViewModel::class.java -> SettingsViewModel(
            customEnvironmentRepository = dependencies.customEnvironmentRepository(),
        ) as T
        else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
