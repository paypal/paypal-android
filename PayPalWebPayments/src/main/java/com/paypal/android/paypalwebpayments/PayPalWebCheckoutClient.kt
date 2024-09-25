package com.paypal.android.paypalwebpayments

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.PayPalSDKError
import java.lang.ref.WeakReference

// NEXT MAJOR VERSION: consider renaming this module to PayPalWebClient since
// it now offers both checkout and vaulting

/**
 * Use this client to approve an order with a [PayPalWebCheckoutRequest].
 */
class PayPalWebCheckoutClient internal constructor(
    private val payPalAnalytics: PayPalAnalytics,
    private val payPalWebLauncher: PayPalWebLauncher
) {

    /**
     * Create a new instance of [PayPalWebCheckoutClient].
     *
     * @param activity a [FragmentActivity]
     * @param configuration a [CoreConfig] object
     * @param urlScheme the custom URl scheme used to return to your app from a browser switch flow
     */
    constructor(context: Context, urlScheme: String) : this(
        PayPalAnalytics(context.applicationContext),
        PayPalWebLauncher(urlScheme),
    )

    /**
     * Sets a listener to receive notifications when a PayPal Checkout event occurs.
     */
    var listener: PayPalWebCheckoutListener? = null

    /**
     * Sets a listener to receive notifications when a Paypal Vault event occurs.
     */
    var vaultListener: PayPalWebVaultListener? = null

    internal var observer = PayPalWebCheckoutLifeCycleObserver(this)

    /**
     * Confirm PayPal payment source for an order. Result will be delivered to your [PayPalWebCheckoutListener].
     *
     * @param request [PayPalWebCheckoutRequest] for requesting an order approval
     */
    fun start(
        activity: AppCompatActivity,
        request: PayPalWebCheckoutRequest
    ): PayPalWebCheckoutStartResult {
        val analytics = payPalAnalytics.createAnalyticsContext(request)
        val launchError = payPalWebLauncher.launchPayPalWebCheckout(activity, request)
        return if (launchError == null) {
            analytics.notifyWebCheckoutStarted()
            PayPalWebCheckoutStartResult.DidLaunchAuth
        } else {
            analytics.notifyWebCheckoutFailure()
            PayPalWebCheckoutStartResult.Failure(launchError)
        }
    }

    /**
     * Vault PayPal as a payment method. Result will be delivered to your [PayPalWebVaultListener].
     *
     * @param request [PayPalWebVaultRequest] for vaulting PayPal as a payment method
     */
    fun vault(
        activity: AppCompatActivity,
        request: PayPalWebVaultRequest
    ): PayPalWebCheckoutVaultResult {
        val analytics = payPalAnalytics.createAnalyticsContext(request)

        val launchError = payPalWebLauncher.launchPayPalWebVault(activity, request)
        return if (launchError == null) {
            analytics.notifyWebVaultStarted()
            PayPalWebCheckoutVaultResult.DidLaunchAuth
        } else {
            analytics.notifyVaultFailure()
            PayPalWebCheckoutVaultResult.Failure(launchError)
        }
    }

    @Suppress("NestedBlockDepth")
    internal fun handleBrowserSwitchResult() {
//        activityReference.get()?.let { activity ->
//            payPalWebLauncher.deliverBrowserSwitchResult(activity)?.let { status ->
//                when (status) {
//                    is PayPalWebStatus.CheckoutSuccess -> notifyWebCheckoutSuccess(status.result)
//                    is PayPalWebStatus.CheckoutError -> status.run {
//                        notifyWebCheckoutFailure(error, orderId)
//                    }
//
//                    is PayPalWebStatus.CheckoutCanceled -> notifyWebCheckoutCancelation(status.orderId)
//                    is PayPalWebStatus.VaultSuccess -> notifyVaultSuccess(status.result)
//                    is PayPalWebStatus.VaultError -> notifyVaultFailure(status.error)
//                    PayPalWebStatus.VaultCanceled -> notifyVaultCancelation()
//                }
//            }
//        }
    }

    private fun notifyWebCheckoutSuccess(result: PayPalWebCheckoutResult) {
//        analyticsService.sendAnalyticsEvent("paypal-web-payments:succeeded", result.orderId)
        listener?.onPayPalWebSuccess(result)
    }

    private fun notifyWebCheckoutCancelation(orderId: String?) {
//        analyticsService.sendAnalyticsEvent("paypal-web-payments:browser-login:canceled", orderId)
        listener?.onPayPalWebCanceled()
    }

    private fun notifyVaultSuccess(result: PayPalWebVaultResult) {
//        analyticsService.sendAnalyticsEvent("paypal-web-payments:vault-wo-purchase:succeeded")
        vaultListener?.onPayPalWebVaultSuccess(result)
    }

    private fun notifyVaultFailure(error: PayPalSDKError) {
//        analyticsService.sendAnalyticsEvent("paypal-web-payments:vault-wo-purchase:failed")
        vaultListener?.onPayPalWebVaultFailure(error)
    }

    private fun notifyVaultCancelation() {
//        analyticsService.sendAnalyticsEvent("paypal-web-payments:vault-wo-purchase:canceled")
        vaultListener?.onPayPalWebVaultCanceled()
    }

    /**
     * Call this method at the end of the web checkout flow to clear out all observers and listeners
     */
    fun removeObservers() {
//        activityReference.get()?.let { it.lifecycle.removeObserver(observer) }
        vaultListener = null
        listener = null
    }
}
