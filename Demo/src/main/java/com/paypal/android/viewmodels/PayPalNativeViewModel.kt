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
import com.paypal.android.paypalnativepayments.PayPalNativeShippingActions
import com.paypal.android.paypalnativepayments.PayPalNativeShippingAddress
import com.paypal.android.paypalnativepayments.PayPalNativeShippingListener
import com.paypal.android.paypalnativepayments.PayPalNativeShippingMethod
import com.paypal.android.ui.paypal.ShippingPreferenceType
import com.paypal.android.usecase.GetAccessTokenUseCase
import com.paypal.android.usecase.GetOrderIdUseCase
import com.paypal.android.usecase.UpdateOrderUseCase
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
    @Inject
    lateinit var updateOrderUseCase: UpdateOrderUseCase

    private var orderID: String? = null

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

        override fun onPayPalNativeShippingAddressChange(
            actions: PayPalNativeShippingActions,
            shippingAddress: PayPalNativeShippingAddress
        ) {
            if (shippingAddress.adminArea1.isNullOrBlank() || shippingAddress.adminArea1 == "NV") {
                actions.reject()
            } else {
                actions.approve()
            }
        }

        override fun onPayPalNativeShippingMethodChange(
            actions: PayPalNativeShippingActions,
            shippingMethod: PayPalNativeShippingMethod
        ) {

            viewModelScope.launch(exceptionHandler) {
                orderID?.also {
                    try {
                        updateOrderUseCase(it, shippingMethod)
                        actions.approve()
                    } catch (e: Exception) {
                        actions.reject()
                        throw e
                    }
                }
            }
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
            orderID = getOrderIdUseCase(shippingPreferenceType)
            orderID?.also {
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
