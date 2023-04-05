package com.paypal.android.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.paypal.android.BuildConfig
import com.paypal.android.paypalnativepayments.PayPalNativeCheckoutError
import com.paypal.android.paypalnativepayments.PayPalNativeCheckoutListener
import com.paypal.android.paypalnativepayments.PayPalNativeCheckoutResult
import com.paypal.android.paypalnativepayments.PayPalNativeCheckoutClient
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.paypalnativepayments.PayPalNativeCheckoutRequest
import com.paypal.android.paypalnativepayments.PayPalNativeShippingAddress
import com.paypal.android.paypalnativepayments.PayPalNativeShippingListener
import com.paypal.android.paypalnativepayments.PayPalNativeShippingMethod
import com.paypal.android.ui.paypal.ShippingPreferenceType
import com.paypal.android.usecase.GetAccessTokenUseCase
import com.paypal.android.usecase.GetOrderIdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PayPalNativeViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    @Inject
    lateinit var getAccessTokenUseCase: GetAccessTokenUseCase
    @Inject
    lateinit var getOrderIdUseCase: GetOrderIdUseCase

    private val payPalListener = object : PayPalNativeCheckoutListener {
        override fun onPayPalCheckoutStart() {
            internalState.postValue(NativeCheckoutViewState.CheckoutStart)
        }

        override fun onPayPalCheckoutSuccess(result: PayPalNativeCheckoutResult) {
            result.apply {
                internalState.postValue(NativeCheckoutViewState.CheckoutComplete(
                    payerID,
                    orderID
                ))
            }
        }

        override fun onPayPalCheckoutFailure(error: PayPalSDKError) {
            val errorState = when (error) {
                is PayPalNativeCheckoutError -> NativeCheckoutViewState.CheckoutError(error = error.errorInfo)
                else -> NativeCheckoutViewState.CheckoutError(message = error.errorDescription)
            }
            internalState.postValue(errorState)
        }

        override fun onPayPalCheckoutCanceled() {
            internalState.postValue(NativeCheckoutViewState.CheckoutCancelled)
        }
    }

    private val shippingListener = object : PayPalNativeShippingListener {

        override fun onPayPalNativeShippingAddressChange(shippingAddress: PayPalNativeShippingAddress) {
            Log.d("PayPalNativeViewModel", "Address change")
        }

        override fun onPayPalNativeShippingMethodChange(shippingMethod: PayPalNativeShippingMethod) {
            Log.d("PayPalNativeViewModel", "Method change")
        }
    }

    private val internalState = MutableLiveData<NativeCheckoutViewState>(NativeCheckoutViewState.Initial)
    val state: LiveData<NativeCheckoutViewState> = internalState

    lateinit var payPalClient: PayPalNativeCheckoutClient

    private var accessToken = ""

    private val exceptionHandler = CoroutineExceptionHandler { _, e ->
        internalState.postValue(NativeCheckoutViewState.CheckoutError(message = e.message))
    }

    fun fetchAccessToken() {
        internalState.postValue(NativeCheckoutViewState.GeneratingToken)
        viewModelScope.launch(exceptionHandler) {
            accessToken = getAccessTokenUseCase()
            initPayPalClient(accessToken)
            internalState.postValue(NativeCheckoutViewState.TokenGenerated(accessToken))
        }
    }

    fun orderIdCheckout(shippingPreferenceType: ShippingPreferenceType) {
        internalState.postValue(NativeCheckoutViewState.CheckoutInit)
        viewModelScope.launch(exceptionHandler) {
            val orderId = getOrderIdUseCase(shippingPreferenceType)
            orderId?.also {
                payPalClient.startCheckout(PayPalNativeCheckoutRequest(it))
            }
        }
    }

    fun reset() {
        accessToken = ""
        internalState.postValue(NativeCheckoutViewState.Initial)
    }

    private fun initPayPalClient(accessToken: String) {
        payPalClient = PayPalNativeCheckoutClient(
            getApplication(),
            CoreConfig(accessToken),
            "${BuildConfig.APPLICATION_ID}://paypalpay"
        )
        payPalClient.listener = payPalListener
        payPalClient.shippingListener = shippingListener
    }
}
