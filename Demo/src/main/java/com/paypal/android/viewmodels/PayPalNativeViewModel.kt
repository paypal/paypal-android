package com.paypal.android.viewmodels

import android.app.Application
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
import com.paypal.android.paypalnativepayments.PayPalNativePaysheetActions
import com.paypal.android.paypalnativepayments.PayPalNativeShippingAddress
import com.paypal.android.paypalnativepayments.PayPalNativeShippingListener
import com.paypal.android.paypalnativepayments.PayPalNativeShippingMethod
import com.paypal.android.ui.paypal.ShippingPreferenceType
import com.paypal.android.usecase.GetClientIdUseCase
import com.paypal.android.usecase.GetOrderIdUseCase
import com.paypal.android.usecase.UpdateOrderUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import okio.IOException
import javax.inject.Inject

@HiltViewModel
class PayPalNativeViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    @Inject
    lateinit var getClientIdUseCase: GetClientIdUseCase

    @Inject
    lateinit var getOrderIdUseCase: GetOrderIdUseCase

    @Inject
    lateinit var updateOrderUseCase: UpdateOrderUseCase

    private var orderId: String? = null

    private val payPalListener = object : PayPalNativeCheckoutListener {
        override fun onPayPalCheckoutStart() {
            internalState.postValue(NativeCheckoutViewState.CheckoutStart)
        }

        override fun onPayPalCheckoutSuccess(result: PayPalNativeCheckoutResult) {
            result.apply {
                internalState.postValue(NativeCheckoutViewState.CheckoutComplete(
                    payerId,
                    orderId
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
            actions: PayPalNativePaysheetActions,
            shippingAddress: PayPalNativeShippingAddress
        ) {
            if (shippingAddress.adminArea1.isNullOrBlank() || shippingAddress.adminArea1 == "NV") {
                actions.reject()
            } else {
                actions.approve()
            }
        }

        override fun onPayPalNativeShippingMethodChange(
            actions: PayPalNativePaysheetActions,
            shippingMethod: PayPalNativeShippingMethod
        ) {

            viewModelScope.launch(exceptionHandler) {
                orderId?.also {
                    try {
                        updateOrderUseCase(it, shippingMethod)
                        actions.approve()
                    } catch (e: IOException) {
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

    private var clientId = ""

    private val exceptionHandler = CoroutineExceptionHandler { _, e ->
        internalState.postValue(NativeCheckoutViewState.CheckoutError(message = e.message))
    }

    fun fetchClientId() {
        internalState.postValue(NativeCheckoutViewState.FetchingClientId)
        viewModelScope.launch(exceptionHandler) {
            clientId = getClientIdUseCase()
            initPayPalClient()
            internalState.postValue(NativeCheckoutViewState.ClientIdFetched(clientId))
        }
    }

    fun orderIdCheckout(shippingPreferenceType: ShippingPreferenceType) {
        internalState.postValue(NativeCheckoutViewState.CheckoutInit)
        viewModelScope.launch(exceptionHandler) {
            orderId = getOrderIdUseCase(shippingPreferenceType)
            orderId?.also {
                payPalClient.startCheckout(PayPalNativeCheckoutRequest(it))
            }
        }
    }

    fun reset() {
        clientId = ""
        internalState.postValue(NativeCheckoutViewState.Initial)
    }

    private fun initPayPalClient() {
        val clientId = "AcXwOk3dof7NCNcriyS8RVh5q39ozvdWUF9oHPrWqfyrDS4AwVdKe32Axuk2ADo6rI_31Vv6MGgOyzRt"
        payPalClient = PayPalNativeCheckoutClient(
            getApplication(),
            CoreConfig(clientId),
            "${BuildConfig.APPLICATION_ID}://paypalpay"
        )
        payPalClient.listener = payPalListener
        payPalClient.shippingListener = shippingListener
    }
}
