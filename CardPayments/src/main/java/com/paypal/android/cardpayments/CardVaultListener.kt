package com.paypal.android.cardpayments

/**
 * @suppress
 *
 * Listener to receive callbacks form [CardClient.vault].
 */
fun interface CardVaultListener {
    fun onCardVaultResult(result: CardVaultResult)
}
