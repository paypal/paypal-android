package com.paypal.android.cardpayments

import androidx.annotation.MainThread
import com.paypal.android.corepayments.PayPalSDKError

interface VaultListener {

    @MainThread
    fun onVaultSuccess(result: VaultResult)

    @MainThread
    fun onVaultFailure(error: PayPalSDKError)
}
