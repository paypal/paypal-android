package com.paypal.android.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.paypal.android.PayPalConfigConstants
import com.paypal.android.checkout.PayPalCheckoutError
import com.paypal.android.checkout.PayPalCheckoutListener
import com.paypal.android.checkout.PayPalCheckoutResult
import com.paypal.android.checkout.PayPalClient
import com.paypal.android.core.CoreConfig
import com.paypal.android.core.PayPalSDKError
import com.paypal.android.usecase.GetAccessTokenUseCase
import com.paypal.android.usecase.GetOrderIdUseCase
import com.paypal.checkout.createorder.CreateOrder
import com.paypal.checkout.shipping.ShippingChangeActions
import com.paypal.checkout.shipping.ShippingChangeData
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
    lateinit var payPalConstants: PayPalConfigConstants

    private val payPalListener = object : PayPalCheckoutListener {
        override fun onPayPalCheckoutStart() {
            internalState.postValue(NativeCheckoutViewState.CheckoutStart)
        }

        override fun onPayPalCheckoutSuccess(result: PayPalCheckoutResult) {
            result.approval.data.apply {
                internalState.postValue(NativeCheckoutViewState.CheckoutComplete(
                    payerId,
                    orderId,
                    paymentId,
                    billingToken
                ))
            }
        }

        override fun onPayPalCheckoutFailure(error: PayPalSDKError) {
            val errorState = when (error) {
                is PayPalCheckoutError -> NativeCheckoutViewState.CheckoutError(error = error.errorInfo)
                else -> NativeCheckoutViewState.CheckoutError(message = error.errorDescription)
            }
            internalState.postValue(errorState)
        }

        override fun onPayPalCheckoutCanceled() {
            internalState.postValue(NativeCheckoutViewState.CheckoutCancelled)
        }

        override fun onPayPalCheckoutShippingChange(
            shippingChangeData: ShippingChangeData,
            shippingChangeActions: ShippingChangeActions
        ) {
            // implement
        }
    }

    private val internalState = MutableLiveData<NativeCheckoutViewState>(NativeCheckoutViewState.Initial)
    val state: LiveData<NativeCheckoutViewState> = internalState

    lateinit var payPalClient: PayPalClient

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

    fun orderIdCheckout() {
        internalState.postValue(NativeCheckoutViewState.CheckoutInit)
        viewModelScope.launch(exceptionHandler) {
            val orderId = getOrderIdUseCase()
            orderId?.also {
                startCheckoutFlow(CreateOrder { createOrderActions ->
                    createOrderActions.set(it)
                    internalState.postValue(NativeCheckoutViewState.OrderCreated(it))
                })
            }
        }
    }

    fun reset() {
        accessToken = ""
        internalState.postValue(NativeCheckoutViewState.Initial)
    }

    private fun startCheckoutFlow(createOrder: CreateOrder) {
        payPalClient.startCheckout(createOrder)
    }

    private fun initPayPalClient(accessToken: String) {
        payPalClient = PayPalClient(
            getApplication(),
            CoreConfig(accessToken),
            payPalConstants.returnUrl
        )
        payPalClient.listener = payPalListener
    }
}
