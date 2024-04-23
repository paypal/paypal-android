package com.paypal.android.paypalwebpayments

import androidx.fragment.app.FragmentActivity
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.corepayments.analytics.AnalyticsService
import java.lang.ref.WeakReference

// NEXT MAJOR VERSION: consider renaming this module to PayPalWebClient since
// it now offers both checkout and vaulting

/**
 * Use this client to approve an order with a [PayPalWebCheckoutRequest].
 */
class PayPalWebCheckoutClient internal constructor(
    // NEXT MAJOR VERSION: remove hardcoded activity reference
    activity: FragmentActivity,
    private val analyticsService: AnalyticsService,
    private val payPalWebLauncher: PayPalWebLauncher
) {

    /**
     * Create a new instance of [PayPalWebCheckoutClient].
     *
     * @param activity a [FragmentActivity]
     * @param configuration a [CoreConfig] object
     * @param urlScheme the custom URl scheme used to return to your app from a browser switch flow
     */
    constructor(
        activity: FragmentActivity,
        configuration: CoreConfig,
        urlScheme: String
    ) : this(
        activity,
        AnalyticsService(activity.applicationContext, configuration),
        PayPalWebLauncher(urlScheme, configuration),
    )

    /**
     * Sets a listener to receive notifications when a PayPal Checkout event occurs.
     */
    var listener: PayPalWebCheckoutListener? = null

    /**
     * Sets a listener to receive notifications when a Paypal Vault event occurs.
     */
    var vaultListener: PayPalWebVaultListener? = null

    private val activityReference = WeakReference(activity)

    private var observer = PayPalWebCheckoutLifeCycleObserver(this)

    init {
        activity.lifecycle.addObserver(observer)
        // NEXT MAJOR VERSION: remove hardcoded activity reference
    }

    /**
     * Confirm PayPal payment source for an order. Result will be delivered to your [PayPalWebCheckoutListener].
     *
     * @param request [PayPalWebCheckoutRequest] for requesting an order approval
     */
    fun start(request: PayPalWebCheckoutRequest) {
        analyticsService.sendAnalyticsEvent("paypal-web-payments:started", request.orderId)

        activityReference.get()?.let { activity ->
            payPalWebLauncher.launchPayPalWebCheckout(activity, request)?.let { launchError ->
                notifyWebCheckoutFailure(launchError, request.orderId)
            }
        }
    }

    /**
     * Vault PayPal as a payment method. Result will be delivered to your [PayPalWebVaultListener].
     *
     * @param request [PayPalWebVaultRequest] for vaulting PayPal as a payment method
     */
    fun vault(request: PayPalWebVaultRequest) {
        analyticsService.sendAnalyticsEvent("paypal-web-payments:vault-wo-purchase:started")

        activityReference.get()?.let { activity ->
            payPalWebLauncher.launchPayPalWebVault(activity, request)?.let { launchError ->
                notifyVaultFailure(launchError)
            }
        }
    }

    @Suppress("NestedBlockDepth")
    internal fun handleBrowserSwitchResult() {
        activityReference.get()?.let { activity ->
            payPalWebLauncher.deliverBrowserSwitchResult(activity)?.let { status ->
                when (status) {
                    is PayPalWebStatus.CheckoutSuccess -> notifyWebCheckoutSuccess(status.result)
                    is PayPalWebStatus.CheckoutError -> status.run {
                        notifyWebCheckoutFailure(error, orderId)
                    }

                    is PayPalWebStatus.CheckoutCanceled -> notifyWebCheckoutCancelation(status.orderId)
                    is PayPalWebStatus.VaultSuccess -> notifyVaultSuccess(status.result)
                    is PayPalWebStatus.VaultError -> notifyVaultFailure(status.error)
                    PayPalWebStatus.VaultCanceled -> notifyVaultCancelation()
                }
            }
        }
    }

    private fun notifyWebCheckoutSuccess(result: PayPalWebCheckoutResult) {
        analyticsService.sendAnalyticsEvent("paypal-web-payments:succeeded", result.orderId)
        listener?.onPayPalWebSuccess(result)
    }

    private fun notifyWebCheckoutFailure(error: PayPalSDKError, orderId: String?) {
        analyticsService.sendAnalyticsEvent("paypal-web-payments:failed", orderId)
        listener?.onPayPalWebFailure(error)
    }

    private fun notifyWebCheckoutCancelation(orderId: String?) {
        analyticsService.sendAnalyticsEvent("paypal-web-payments:browser-login:canceled", orderId)
        listener?.onPayPalWebCanceled()
    }

    private fun notifyVaultSuccess(result: PayPalWebVaultResult) {
        analyticsService.sendAnalyticsEvent("paypal-web-payments:vault-wo-purchase:succeeded")
        vaultListener?.onPayPalWebVaultSuccess(result)
    }

    private fun notifyVaultFailure(error: PayPalSDKError) {
        analyticsService.sendAnalyticsEvent("paypal-web-payments:vault-wo-purchase:failed")
        vaultListener?.onPayPalWebVaultFailure(error)
    }

    private fun notifyVaultCancelation() {
        analyticsService.sendAnalyticsEvent("paypal-web-payments:vault-wo-purchase:canceled")
        vaultListener?.onPayPalWebVaultCanceled()
    }

    /**
     * Call this method at the end of the web checkout flow
     */
    fun removeObservers() {
        activityReference.get()?.let { it.lifecycle.removeObserver(observer) }
        vaultListener = null
        listener = null
    }
}
