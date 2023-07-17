package com.paypal.android.cardpayments

import androidx.annotation.MainThread

interface VaultListener {

    @MainThread
    fun onVaultSuccess(result: VaultResult)
}
