# PayPal SDK Migration Guide

This guide highlights how to migrate to the latest version of the PayPal SDK.

## Table of Contents

1. [Card Payments](#card-payments)
1. [PayPal Web Payments](#paypal-web-payments)
1. [PayPal Native Payments](#paypal-web-payments)

### Card Payments

We refactored the `CardClient` API to improve the developer experience. Use this diff to guide your migration from `v1` to `v2`:

```diff
class SampleActivity: ComponentActivity(), ApproveOrderListener, CardVaultListener {

  val config = CoreConfig("<CLIENT_ID>", environment = Environment.LIVE)
- val cardClient = CardClient(requireActivity(), config)
+ val cardClient = CardClient(requireContext(), config)
+ var authState: String? = null

  init {
    cardClient.approveOrderListener = this
    cardClient.vaultListener = this
  }

+ override fun onResume() {
+   super.onResume()
+   // Manually attempt auth challenge completion (via deep link)
+   authState?.let { state -> cardClient.completeAuthChallenge(intent, state) }
+ }

  override fun onDestroy() {
    super.onDestroy()
    cardClient.removeObservers()
  }

  private fun approveOrder() {
    val cardRequest: CardRequest = TODO("Create a card request.")
-   cardClient.approveOrder(this, cardRequest)
+   cardClient.approveOrder(cardRequest)
  }
  
  private fun vaultCard() {
    val cardVaultRequest: CardVaultRequest = TODO("Create a card vault request.")
-   cardClient.vault(this, cardVaultRequest)
+   cardClient.vault(cardVaultRequest)
  }

  override fun onApproveOrderSuccess(result: CardResult) {
    TODO("Capture or authorize order on your server.")
+   // Discard auth state when done
+   authState = null
  }

  override fun onApproveOrderFailure(error: PayPalSDKError) {
    TODO("Handle approve order failure.")
+   // Discard auth state when done
+   authState = null
  }

+ override fun onApproveOrderAuthorizationRequired(authChallenge: CardAuthChallenge) {
+   // Manually present auth challenge
+   val result = cardClient.presentAuthChallenge(this, authChallenge)
+   when (result) {
+     is CardPresentAuthChallengeResult.Success -> {
+       // Capture auth state for balancing call to completeAuthChallenge() in onResume()
+       authState = result.authState
+     }
+     is CardPresentAuthChallengeResult.Failure -> TODO("Handle Present Auth Challenge Failure")
+   }
+ }

  override fun onVaultSuccess(result: CardVaultResult) {
-   val authChallenge = result.authChallenge
-   if (authChallenge != null) {
-     cardClient?.presentAuthChallenge(activity, authChallenge)
-   } else {
      TODO("Create payment token on your server.")
-   }
+   // Discard auth state when done
+   authState = null
  }
  
  override fun onVaultFailure(error: PayPalSDKError) {
    updateSetupTokenState = ActionState.Failure(error)
+   // Discard auth state when done
+   authState = null
  }
  
+ override fun onVaultAuthorizationRequired(authChallenge: CardAuthChallenge) {
+   // Manually present auth challenge
+   val result = cardClient.presentAuthChallenge(this, authChallenge)
+   when (result) {
+     is CardPresentAuthChallengeResult.Success -> {
+       // Capture auth state for balancing call to completeAuthChallenge() in onResume()
+       authState = result.authState
+     }
+     is CardPresentAuthChallengeResult.Failure -> TODO("Handle Present Auth Challenge Failure")
+   }
+ }
}
```

<details>
<summary><b>Notes on Changes to Card Payments in v2</b></summary>

Here are some detailed notes on the changes made to Card Payments in v2:

##### Activity Reference no Longer Required in CardClient Constructor

- In `v1` the activity reference is only truly needed when the call to `CardClient#approveOrder()` or `CardClient#vault()` is made (to open a Chrome Custom Tab in the current Task).
- In `v2` the `CardClient` constructor no longer requires an activity reference.
- The goal of this change is to increase flexibility of `CardClient` instantiation.

##### Moving from Implicit (Automatic) to Manual Completion of Auth Challenges

- In `v1` the SDK registers a lifecycle observer to parse incoming deep links when the host application comes into the foreground.
- In `v2` the host application is responsible for calling `CardClient#completeAuthChallenge()` to attempt completion of an auth challenge.
- The goal of this change is to make the SDK less opinionated and give host applications more control over the auth challenge user experience.

</details>

### PayPal Web Payments

We have refactored the `PayPalWebClient` API to improve the developer experience.

TODO: Implement migration guide for PayPalWebPayments.

### PayPal Native Payments

We have removed `PayPalNativeClient` and all associated classes because the PayPal Native Checkout dependency this module uses has been sunset.
