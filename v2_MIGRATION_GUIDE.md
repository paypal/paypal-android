# PayPal SDK Migration Guide

This guide highlights how to migrate to the latest version of the PayPal SDK.

## Table of Contents

1. [Card Payments](#card-payments)
1. [PayPal Web Payments](#paypal-web-payments)
1. [PayPal Native Payments](#paypal-web-payments)

### Card Payments

We have refactored the `CardClient` API to improve the developer experience.

Use the following code diffs to guide your migration from `v1` to `v2`:

#### CardClient: Approve Order

```diff
class SampleActivity: ComponentActivity(), ApproveOrderListener {

  val config = CoreConfig("<CLIENT_ID>", environment = Environment.LIVE)
- // v1
- val cardClient = CardClient(requireActivity(), config)
+ // v2
+ val cardClient = CardClient(requireContext(), config)
+ var authState: String? = null

  init {
    cardClient.approveOrderListener = this
  }

+ // v2
+ override fun onResume() {
+   super.onResume()
+   // Manually attempt auth challenge completion (via deep link)
+   authState?.let { state -> cardClient.completeAuthChallenge(intent, state) }
+ }

  fun approveOrder() {
    val cardRequest: CardRequest = TODO("Create a card request.")
-   // v1
-   cardClient.approveOrder(this, cardRequest)
+   // v2
+   cardClient.approveOrder(cardRequest)
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

+ // v2
+ override fun onAuthorizationRequired(authChallenge: CardAuthChallenge) {
+   // Manually present auth challenge
+   val result = cardClient.presentAuthChallenge(this, authChallenge)
+   when (result) {
+     // Preserve auth state for balancing call to completeAuthChallenge() in onResume()
+     is CardPresentAuthChallengeResult.Success -> {
+       authState = result.authState
+     }
+     is CardPresentAuthChallengeResult.Failure -> TODO("Handle Present Auth Challenge Failure")
+   }
+ }
}
```

<details>
<summary><b>Details: v2 Notes on Changes to Card Payments</b></summary>

#### Card Client: Approve Order Changes Explained

We have refactored the `CardClient` API to improve the developer experience. Here are some detailed notes on the changes:

##### Activity Reference no Longer Required in CardClient Constructor

- We noticed in `v1` that the activity reference is only needed when the call to `CardClient#approveOrder()` or `CardClient#vault()` is made (to open a Chrome Custom Tab in the current Task).
- In `v2`, the `CardClient` constructor no longer requires an activity reference.
- The goal of this change is to increase flexibility of `CardClient` instantiation.

##### Moving from Implicit (Automatic) to Manual Completion of Auth Challenges

- In `v1`, the SDK registers a lifecycle observer to parse incoming deep links when the host application comes into the foreground.
- In `v2`, the host application is responsible for calling `CardClient#completeAuthChallenge()` to attempt completion of an auth challenge.
- The goal of this change is to make the SDK less opinionated and give host applications more control over the auth challenge user experience.

</details>

### PayPal Web Payments

We have refactored the `PayPalWebClient` API to improve the developer experience.

TODO: Implement migration guide for PayPalWebPayments.

### PayPal Native Payments

We have removed `PayPalNativeClient` and all associated classes because the PayPal Native Checkout dependency this module uses has been sunset.
