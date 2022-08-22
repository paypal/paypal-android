package com.paypal.android.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.paypal.android.usecase.GetAccessTokenUseCase
import com.paypal.android.usecase.GetApprovalSessionIdActionUseCase
import com.paypal.android.usecase.GetBillingAgreementTokenUseCase
import com.paypal.android.usecase.GetOrderIdUseCase
import com.paypal.android.usecase.GetOrderUseCase
import com.paypal.android.PayPalConfigConstants
import com.paypal.android.checkout.PayPalCheckoutError
import com.paypal.android.checkout.PayPalCheckoutListener
import com.paypal.android.checkout.PayPalCheckoutResult
import com.paypal.android.checkout.PayPalClient
import com.paypal.android.core.CoreConfig
import com.paypal.android.core.PayPalSDKError
import com.paypal.android.usecase.GetBillingAgreementTokenWithoutPurchaseUseCase
import com.paypal.android.utils.OrderUtils.asValueString
import com.paypal.android.utils.OrderUtils.getAmount
import com.paypal.checkout.createorder.CreateOrder
import com.paypal.checkout.order.Options
import com.paypal.checkout.order.Order
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

    @Inject
    lateinit var getBillingAgreementTokenUseCase: GetBillingAgreementTokenUseCase
    @Inject
    lateinit var getBillingAgreementTokenWithoutPurchaseUseCase: GetBillingAgreementTokenWithoutPurchaseUseCase
    @Inject
    lateinit var getAccessTokenUseCase: GetAccessTokenUseCase
    @Inject
    lateinit var getOrderIdUseCase: GetOrderIdUseCase
    @Inject
    lateinit var getApprovalSessionIdActionUseCase: GetApprovalSessionIdActionUseCase
    @Inject
    lateinit var getOrderUseCase: GetOrderUseCase
    @Inject
    lateinit var payPalConstants: PayPalConfigConstants

    private lateinit var order: Order

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
                                value = ((it.amount?.value?.toFloat() ?: 0f) + 10f).asValueString()
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
                        value = order.purchaseUnitList.first().amount.value,
                        shippingValue = updatedShippingAmount ?: "0.00"
                    )
                )
            )
            shippingChangeActions.patchOrder(patchRequest) {
                internalState.postValue(NativeCheckoutViewState.OrderPatched)
            }
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

    fun billingAgreementCheckout() {
        internalState.postValue(NativeCheckoutViewState.CheckoutInit)
        viewModelScope.launch(exceptionHandler) {
            val order = getBillingAgreementTokenUseCase()
            order.id?.also { orderId ->
                startCheckoutFlow(CreateOrder { createOrderActions ->
                    createOrderActions.setBillingAgreementId(orderId)
                    internalState.postValue(NativeCheckoutViewState.OrderCreated(orderId))
                })
            }
        }
    }

    fun billingAgreementWithoutPurchase() {
        internalState.postValue(NativeCheckoutViewState.CheckoutInit)
        viewModelScope.launch(exceptionHandler) {
            val token = getBillingAgreementTokenWithoutPurchaseUseCase(accessToken)
            startCheckoutFlow(CreateOrder { createOrderActions ->
                createOrderActions.setBillingAgreementId(token)
                internalState.postValue(NativeCheckoutViewState.OrderCreated(token))
            })
        }
    }

    fun orderCheckout() {
        internalState.postValue(NativeCheckoutViewState.CheckoutInit)
        order = getOrderUseCase()
        startCheckoutFlow(CreateOrder { createOrderActions ->
            createOrderActions.create(order) {
                internalState.postValue(NativeCheckoutViewState.OrderCreated(it))
            }
        })
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

    fun vaultCheckout() {
        internalState.postValue(NativeCheckoutViewState.CheckoutInit)
        viewModelScope.launch(exceptionHandler) {
            val sessionId = getApprovalSessionIdActionUseCase(accessToken)
            sessionId?.also {
                startCheckoutFlow(CreateOrder { createOrderActions ->
                    createOrderActions.setVaultApprovalSessionId(it)
                    internalState.postValue(NativeCheckoutViewState.OrderCreated(sessionId))
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
