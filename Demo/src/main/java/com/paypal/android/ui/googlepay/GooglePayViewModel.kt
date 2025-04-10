package com.paypal.android.ui.googlepay

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.tasks.Task
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.contract.ApiTaskResult
import com.paypal.android.api.services.SDKSampleServerResult
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.GooglePayClient
import com.paypal.android.usecase.GetClientIdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

@HiltViewModel
class GooglePayViewModel @Inject constructor(
    val getClientIdUseCase: GetClientIdUseCase,
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
            googlePayClient!!.confirmOrder(result)
        }
    }
}