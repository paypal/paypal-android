# PayPal SDK Migration Guide

This guide highlights how to migrate to the latest version of the PayPal SDK.

> For evolution of this guide, see the MIGRATION_GUIDE for [v2.0.0-beta1](https://github.com/paypal/paypal-android/blob/4afcf913ae8ae91c741faa4a7d49f8ba44765117/v2_MIGRATION_GUIDE.md).

## Table of Contents

1. [Card Payments](#card-payments)
1. [PayPal Web Payments](#paypal-web-payments)
1. [PayPal Native Payments](#paypal-native-payments)

## Card Payments

We refactored the `CardClient` API to improve the developer experience. Use this diff to guide your migration from `v1` to `v2`:

```diff
-class SampleActivity: ComponentActivity(), ApproveOrderListener, CardVaultListener {
+class SampleActivity: ComponentActivity() {

  val config = CoreConfig("<CLIENT_ID>", environment = Environment.LIVE)
- val cardClient = CardClient(requireActivity(), config)
+ val cardClient = CardClient(requireContext(), config)
+ var authState: String? = null

- init {
-   cardClient.approveOrderListener = this
-   cardClient.vaultListener = this
- }

+ override fun onResume() {
+   super.onResume()
+   // Manually attempt auth challenge completion (via host activity intent deep link)
+   checkForCardAuthCompletion(intent)
+ }

+ override fun onNewIntent(newIntent: Intent) {
+   super.onNewIntent(newIntent)
+   // Manually attempt auth challenge completion (via new intent deep link)
+   checkForCardAuthCompletion(newIntent)
+ }

  fun approveOrder() {
    val cardRequest: CardRequest = TODO("Create a card request.")
-   cardClient.approveOrder(this, cardRequest)
+   when (val approveOrderResult = cardClient.approveOrder(cardRequest)) {
+     is CardApproveOrderResult.Success -> TODO("Capture or authorize order on your server.")
+     is CardApproveOrderResult.Failure -> TODO("Handle approve order failure.")
+     is CardApproveOrderResult.AuthorizationRequired -> presentAuthChallenge(result.authChallenge)
+   }
  }
  
  fun vaultCard() {
    val cardVaultRequest: CardVaultRequest = TODO("Create a card vault request.")
-   cardClient.vault(this, cardVaultRequest)
+   when (val vaultResult = cardClient.vault(cardVaultRequest)) {
+     is CardVaultResult.Success -> TODO("Create payment token on your server.")
+     is CardVaultResult.Failure -> TODO("Handle card vault failure.")
+     is CardVaultResult.AuthorizationRequired -> presentAuthChallenge(result.authChallenge)
+   }
  }

+ fun presentAuthChallenge(authChallenge: CardAuthChallenge) {
+   // Manually present auth challenge
+   when (val result = cardClient.presentAuthChallenge(this, authChallenge)) {
+     is CardPresentAuthChallengeResult.Success -> {
+       // Capture auth state for balancing call to finishApproveOrder()/finishVault() when
+       // the merchant application re-enters the foreground
+       authState = result.authState
+     }
+     is CardPresentAuthChallengeResult.Failure -> TODO("Handle Present Auth Challenge Failure")
+   }
+ }

+ fun checkForCardAuthCompletion(intent: Intent) = authState?.let { state ->
+   // check for approve order completion
+   when (val approveOrderResult = cardClient.finishApproveOrder(intent, state)) {
+     is CardFinishApproveOrderResult.Success -> {
+       TODO("Capture or authorize order on your server.")
+       authState = null // Discard auth state when done
+     }
+
+     is CardFinishApproveOrderResult.Failure -> {
+       TODO("Handle approve order failure.")
+       authState = null // Discard auth state when done
+     }
+
+     CardFinishApproveOrderResult.Canceled -> {
+       TODO("Give user the option to restart the flow.")
+       authState = null // Discard auth state when done
+     }
+
+     CardFinishApproveOrderResult.NoResult -> {
+       // there isn't enough information to determine the state of the auth challenge for this payment method
+     }
+   }
+
+   // check for vault completion
+   when (val vaultResult = cardClient.finishVault(intent, state)) {
+     is CardFinishVaultResult.Success -> {
+       TODO("Create payment token on your server.")
+       authState = null // Discard auth state when done
+     }
+     is CardFinishVaultResult.Failure -> {
+       TODO("Handle card vault failure.")
+       authState = null // Discard auth state when done
+     }
+     CardFinishVaultResult.Canceled -> {
+       TODO("Give user the option to restart the flow.")
+       authState = null // Discard auth state when done
+     }
+     CardFinishVaultResult.NoResult -> {
+       // there isn't enough information to determine the state of the auth challenge for this payment method
+     }
+   }
+ }

- override fun onApproveOrderSuccess(result: CardResult) {
-   TODO("Capture or authorize order on your server.")
- }

- override fun onApproveOrderFailure(error: PayPalSDKError) {
-   TODO("Handle approve order failure.")
- }

- override fun onVaultSuccess(result: CardVaultResult) {
-   val authChallenge = result.authChallenge
-   if (authChallenge != null) {
-     cardClient?.presentAuthChallenge(activity, authChallenge)
-   } else {
-     TODO("Create payment token on your server.")
-   }
- }
  
- override fun onVaultFailure(error: PayPalSDKError) {
-   TODO("Handle card vault failure.")
- }

- override fun onDestroy() {
-   super.onDestroy()
-   cardClient.removeObservers()
- }
}
```

<details>
<summary><b>Notes on Changes to Card Payments in v2</b></summary>

Here are some detailed notes on the changes made to Card Payments in v2:

### Activity Reference no Longer Required in CardClient Constructor

- In `v1` the activity reference is only truly needed when the call to `CardClient#approveOrder()` or `CardClient#vault()` is made (to open a Chrome Custom Tab in the current Task).
- In `v2` the `CardClient` constructor no longer requires an activity reference.
- The goal of this change is to increase flexibility of `CardClient` instantiation.

### Moving from Implicit (Automatic) to Manual Completion of Auth Challenges

- In `v1` the SDK registers a lifecycle observer to parse incoming deep links when the host application comes into the foreground.
- In `v2` the host application is responsible for calling `CardClient#completeAuthChallenge()` to attempt completion of an auth challenge.
- The goal of this change is to make the SDK less opinionated and give host applications more control over the auth challenge user experience.

### Migration from Listener Patern to Result Types

- In `v1` the SDK would notify the host application of success, failure, etc. events using a registered listener
- In `v2` the host application will receive a sealed class `Result` type in response to each method invocation
- The goal of this change is to prevent having to retain listener references and to make auth challenge presentation more explicit
- Sealed class result types also have the added benefit of explicitly calling out all possible outcomes of a related method inovcation

</details>

## PayPal Web Payments

We refactored the `PayPalWebClient` API to improve the developer experience. Use this diff to guide your migration from `v1` to `v2`:

```diff
-class SampleActivity: ComponentActivity(), PayPalWebCheckoutListener, PayPalWebVaultListener {
+class SampleActivity: ComponentActivity() {

  val config = CoreConfig("<CLIENT_ID>", environment = Environment.LIVE)
- val payPalClient = PayPalWebCheckoutClient(requireActivity(), config, "my-deep-link-url-scheme")
+ val payPalClient = PayPalWebCheckoutClient(requireContext(), config, "my-deep-link-url-scheme")
+ var authState: String? = null

- init {
-   payPalClient.listener = this
-   payPalClient.vaultListener = this
- }

+ override fun onResume() {
+   super.onResume()
+   // Manually attempt auth challenge completion (via host activity intent deep link)
+   checkForPayPalAuthCompletion(intent)
+ }

+ override fun onNewIntent(newIntent: Intent) {
+   super.onNewIntent(newIntent)
+   // Manually attempt auth challenge completion (via new intent deep link)
+   checkForPayPalAuthCompletion(newIntent)
+ }

- override fun onDestroy() {
-   super.onDestroy()
-   payPalClient.removeObservers()
- }

  private fun launchPayPalCheckout() {
    val checkoutRequest: PayPalWebCheckoutRequest = TODO("Create a PayPal checkout request.")
-   payPalClient.start(checkoutRequest)
+   when (val result = paypalClient.start(this, checkoutRequest)) {
+     is PayPalPresentAuthChallengeResult.Success -> {
+       // Capture auth state for balancing call to finishStart() when
+       // the merchant application re-enters the foreground
+       authState = result.authState
+     }
+     is PayPalPresentAuthChallengeResult.Failure -> TODO("Handle Present Auth Challenge Failure")
+   }
  }
  
  private fun launchPayPalVault() {
    val vaultRequest: PayPalWebVaultRequest = TODO("Create a card vault request.")
-   payPalClient.vault(vaultRequest)
+   when (val result = paypalClient.vault(this, vaultRequest)) {
+     is PayPalPresentAuthChallengeResult.Success -> {
+       // Capture auth state for balancing call to finishVault() when
+       // the merchant application re-enters the foreground
+       authState = result.authState
+     }
+     is PayPalPresentAuthChallengeResult.Failure -> TODO("Handle Present Auth Challenge Failure")
+   }
  }

+ fun checkForPayPalAuthCompletion(intent: Intent) = authState?.let { state ->
+   // check for checkout completion
+   when (val checkoutResult = payPalClient.finishStart(intent, state)) {
+     is PayPalWebCheckoutFinishStartResult.Success -> {
+       TODO("Capture or authorize order on your server.")
+       authState = null // Discard auth state when done
+     }
+
+     is PayPalWebCheckoutFinishStartResult.Failure -> {
+       TODO("Handle approve order failure.")
+       authState = null // Discard auth state when done
+     }
+
+     is PayPalWebCheckoutFinishStartResult.Canceled -> {
+       TODO("Notify user PayPal checkout was canceled.")
+       authState = null // Discard auth state when done
+     }
+
+     PayPalWebCheckoutFinishStartResult.NoResult -> {
+       // there isn't enough information to determine the state of the auth challenge for this payment method
+     }
+   }
+
+   // check for vault completion
+   when (val vaultResult = payPalClient.finishVault(intent, state)) {
+     is PayPalWebCheckoutFinishVaultResult.Success -> {
+       TODO("Create payment token on your server.")
+       authState = null // Discard auth state when done
+     }
+
+     is PayPalWebCheckoutFinishVaultResult.Failure -> {
+       TODO("Handle card vault failure.")
+       authState = null // Discard auth state when done
+     }
+
+     is PayPalWebCheckoutFinishVaultResult.Canceled -> {
+       TODO("Notify user PayPal vault was canceled.")
+       authState = null // Discard auth state when done
+     }
+
+     PayPalWebCheckoutFinishVaultResult.NoResult -> {
+       // there isn't enough information to determine the state of the auth challenge for this payment method
+     }
+   }
+ }
  
- override fun onPayPalWebSuccess(result: PayPalWebCheckoutResult) {
-   TODO("Capture or authorize order on your server.")
- }

- override fun onPayPalWebFailure(error: PayPalSDKError) {
-   TODO("Handle approve order failure.")
- }

- override fun onPayPalWebCanceled() {
-   TODO("Notify user PayPal checkout was canceled.")
- }
  
- fun onPayPalWebVaultSuccess(result: PayPalWebVaultResult) {
-   TODO("Create payment token on your server.")
- }
  
- fun onPayPalWebVaultFailure(error: PayPalSDKError) {
-   TODO("Handle card vault failure.")
- }
  
- fun onPayPalWebVaultCanceled() {
-   TODO("Notify user PayPal vault was canceled.")
- }
}
```

<details>
<summary><b>Notes on Changes to PayPal Web Payments in v2</b></summary>

Here are some detailed notes on the changes made to PayPal Web Payments in v2:

### Activity Reference no Longer Required in PayPalWebCheckoutClient Constructor

- In `v1` the activity reference is only truly needed when the call to `PayPalWebCheckoutClient#start()` or `PayPalWebCheckoutClient#vault()` is made (to open a Chrome Custom Tab in the current Task).
- In `v2` the `PayPalWebCheckoutClient` constructor no longer requires an activity reference.
- The goal of this change is to increase flexibility of `PayPalWebCheckoutClient` instantiation.

### Moving from Implicit (Automatic) to Manual Completion of Auth Challenges

- In `v1` the SDK registers a lifecycle observer to parse incoming deep links when the host application comes into the foreground.
- In `v2` the host application is responsible for calling `PayPalWebCheckoutClient#completeAuthChallenge()` to attempt completion of an auth challenge.
- The goal of this change is to make the SDK less opinionated and give host applications more control over the auth challenge user experience.

### Migration from Listener Patern to Result Types

- In `v1` the SDK would notify the host application of success, failure, etc. events using a registered listener
- In `v2` the host application will receive a sealed class `Result` type in response to each method invocation
- The goal of this change is to prevent having to retain listener references and to make auth challenge presentation more explicit
- Sealed class result types also have the added benefit of explicitly calling out all possible outcomes of a related method inovcation

</details>

## PayPal Native Payments

We have removed `PayPalNativeClient` and all associated classes. The PayPal Native Checkout dependency this module uses has been discontinued.
