package com.paypal.android.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.paypal.android.nativepayments.PayPalNativeCheckoutError
import com.paypal.android.nativepayments.PayPalNativeCheckoutListener
import com.paypal.android.nativepayments.PayPalNativeCheckoutResult
import com.paypal.android.nativepayments.PayPalNativeCheckoutClient
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.ui.paypal.ShippingPreferenceType
import com.paypal.android.usecase.GetAccessTokenUseCase
import com.paypal.android.usecase.GetOrderIdUseCase
import com.paypal.android.utils.OrderUtils.asValueString
import com.paypal.android.utils.OrderUtils.getAmount
import com.paypal.checkout.createorder.CreateOrder
import com.paypal.checkout.order.Options
import com.paypal.checkout.order.patch.PatchOrderRequest
import com.paypal.checkout.order.patch.fields.PatchAmount
import com.paypal.checkout.order.patch.fields.PatchShippingOptions
import com.paypal.checkout.shipping.ShippingChangeActions
import com.paypal.checkout.shipping.ShippingChangeData
import com.paypal.checkout.shipping.ShippingChangeType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PayPalNativeViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    private companion object {
        private const val SHIPPING_METHOD_INCREASE = 10f
    }

    @Inject
    lateinit var getAccessTokenUseCase: GetAccessTokenUseCase
    @Inject
    lateinit var getOrderIdUseCase: GetOrderIdUseCase

    private val payPalListener = object : PayPalNativeCheckoutListener {
        override fun onPayPalCheckoutStart() {
            internalState.postValue(NativeCheckoutViewState.CheckoutStart)
        }

        override fun onPayPalCheckoutSuccess(result: PayPalNativeCheckoutResult) {
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
                is PayPalNativeCheckoutError -> NativeCheckoutViewState.CheckoutError(error = error.errorInfo)
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
            val options: List<Options>
            val updatedShippingAmount: String?

            when (shippingChangeData.shippingChangeType) {
                ShippingChangeType.OPTION_CHANGE -> {

                    options = shippingChangeData.shippingOptions
                    updatedShippingAmount = shippingChangeData.selectedShippingOption?.amount?.value
                }
                ShippingChangeType.ADDRESS_CHANGE -> {
                    options = shippingChangeData.shippingOptions.map {
                        it.copy(
                            amount = it.amount?.copy(
                                value = ((it.amount?.value?.toFloat() ?: 0f) + SHIPPING_METHOD_INCREASE).asValueString()
                            )
                        )
                    }
                    updatedShippingAmount = options.find { it.selected }?.amount?.value
                }
            }

            val patchRequest = PatchOrderRequest(
                PatchShippingOptions.Replace(
                    purchaseUnitReferenceId = "PUHF",
                    options = options
                ),
                PatchAmount.Replace(
                    purchaseUnitReferenceId = "PUHF",
                    amount = getAmount(
                        value = "100.0",
                        shippingValue = updatedShippingAmount ?: "0.00"
                    )
                )
            )
            // TODO: patch order will fail because of bug in NXO. Ticket: https://paypal.atlassian.net/browse/DTNOR-607
            // issue reported at NXO: https://paypal.atlassian.net/browse/MXO-279
            shippingChangeActions.patchOrder(patchRequest) {
                internalState.postValue(NativeCheckoutViewState.OrderPatched)
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
            val orderId = getOrderIdUseCase(shippingPreferenceType)
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
        payPalClient = PayPalNativeCheckoutClient(
            getApplication(),
            CoreConfig(accessToken)
        )
        payPalClient.listener = payPalListener
    }
}
