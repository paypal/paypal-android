package com.paypal.android.ui.googlepay

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.tasks.Task
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.contract.ApiTaskResult
import com.paypal.android.api.model.OrderIntent
import com.paypal.android.api.services.SDKSampleServerResult
import com.paypal.android.corepayments.ApproveGooglePayPaymentResult
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.GooglePayClient
import com.paypal.android.models.OrderRequest
import com.paypal.android.usecase.CreateOrderUseCase
import com.paypal.android.usecase.GetClientIdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GooglePayViewModel @Inject constructor(
    val getClientIdUseCase: GetClientIdUseCase,
    val createOrderUseCase: CreateOrderUseCase,
) : ViewModel() {

    private var googlePayClient: GooglePayClient? = null

    suspend fun launchGooglePay(activity: ComponentActivity): Task<PaymentData> {
        when (val clientIdResult = getClientIdUseCase()) {
            is SDKSampleServerResult.Failure -> TODO("handle failure")
            is SDKSampleServerResult.Success -> {
                val coreConfig = CoreConfig(clientIdResult.value)
                googlePayClient = GooglePayClient(activity, coreConfig)
                return googlePayClient!!.start()
            }
        }
    }

    fun completeGooglePayLaunch(result: ApiTaskResult<PaymentData>) {
        viewModelScope.launch {
            val orderRequest = OrderRequest(intent = OrderIntent.CAPTURE, shouldVault = false)
            when (val createOrderResult = createOrderUseCase(orderRequest)) {
                is SDKSampleServerResult.Success -> {
                    val orderId = createOrderResult.value.id!!
                    confirmOrderGooglePayOrder(orderId, result)
                }
                is SDKSampleServerResult.Failure -> {
                    // TODO: handle error
                }
            }
        }
    }

    private suspend fun confirmOrderGooglePayOrder(orderId: String, result: ApiTaskResult<PaymentData>) {
        when (val confirmOrderResult = googlePayClient!!.confirmOrder(orderId, result)) {
            is ApproveGooglePayPaymentResult.Success -> {
                val status = confirmOrderResult.status
                Log.d("GooglePayViewModel", "Status: $status")
            }
            is ApproveGooglePayPaymentResult.Failure -> {
                // TODO: handle error
            }
        }
    }
}