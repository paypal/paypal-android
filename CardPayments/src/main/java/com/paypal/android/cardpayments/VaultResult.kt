package com.paypal.android.cardpayments

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Represents the result of a vault operation.
 *
 * @property status Vault status
 * @property setupTokenId Associated setup token
 */
@Parcelize
data class VaultResult(val status: String, val setupTokenId: String) : Parcelable
