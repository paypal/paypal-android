# PayPal SDK Migration Guide

This guide highlights how to migrate to the latest version of the PayPal SDK.

## Table of Contents

1. [Card Payments](#card-payments)
1. [PayPal Web Payments](#paypal-web-payments)
1. [PayPal Native Payments](#paypal-web-payments)

### Card Payments

Reference the code diff below to guide your migration from v1 to v2:

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
+   // discard auth state when done
+   authState = null
  }

  override fun onApproveOrderFailure(error: PayPalSDKError) {
    TODO("Handle approve order failure.")
+   // discard auth state when done
+   authState = null
  }

+ // v2
+ override fun onAuthorizationRequired(authChallenge: CardAuthChallenge) {
+   val result = cardClient.presentAuthChallenge(this, authChallenge)
+   when (result) {
+     // Preserve authState for balancing call to completeAuthChallenge() in onResume()
+     is CardPresentAuthChallengeResult.Success -> authState = result.authState
+     is CardPresentAuthChallengeResult.Failure -> TODO("Handle Present Auth Challenge Failure")
+   }
+ }
}
```

We have refactored the `CardClient` API to improve the developer experience.

#### Activity Reference no Longer Required in CardClient Constructor

The `CardClient` constructor no longer requires an activity reference. In v2, the SDK will only require an activity reference when it needs to launch a Chrome Custom Tab.

```diff
val config = CoreConfig("<CLIENT_ID>", environment = Environment.LIVE)

-// v1
-val cardClient = CardClient(requireActivity(), config)
+// v2
+val cardClient = CardClient(requireContext(), config)
```

<details>
<summary><b>Details: CardClient v2</b></summary>

The new `CardClient` constructor is less restrictive.

For example, it should now be easier to create a `CardClient` instance from within a Jetpack `ViewModel`.

</details>

<details>
<summary><b>Details: CardClient v1</b></summary>

The old `CardClient` constructor requires an activity reference to register lifecycle observers so the SDK can parse incoming deep links internally when the host application comes to the foregound.

Automatic parsing of deep links can have a positive affect on the developer experience, but we've found that internal deep link parsing can be problematic for some app architectures.

</details>

#### Explicit Launch of Auth Challenge

The new `CardClient` gives more control to the host application when presenting Chrome Custom Tabs for authentication and responding to deep links.

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
+   authState?.let { state -> cardClient.completeAuthChallenge(intent, state) }
+ }

  fun approveOrder() {
    val cardRequest = ...
-   // v1
-   cardClient.approveOrder(this, cardRequest)
+   // v2
+   cardClient.approveOrder(cardRequest)
  }

  override fun onApproveOrderSuccess(result: CardResult) {
    // capture order on your server
    authState = null
  }

  override fun onApproveOrderFailure(error: PayPalSDKError) {
    TODO("Handle Approve Order Failure")
  }

+ // v2
+ override fun onAuthorizationRequired(authChallenge: CardAuthChallenge) {
+   val result = cardClient.presentAuthChallenge(this, authChallenge)
+   when (result) {
+     // Preserve authState for balancing call to completeAuthChallenge() in onResume()
+     is CardPresentAuthChallengeResult.Success -> authState = result.authState
+     is CardPresentAuthChallengeResult.Failure -> TODO("Handle Present Auth Challenge Failure")
+   }
+ }
}
```

<details>
<summary><b>Details: Approve Order v2</b></summary>

In v2, the PayPal SDK requires more explicit direction from the host application to successfully complete an authorization challenge. The host app is also responsible for preserving the auth state while the application enters the background.

Once the user has successfully completed the auth challenge via Chrome Custom Tabs, a deep link back into the host app will bring it back into the foreground. The host application can then complete the auth challenge using the deep link intent and the auth state captured when the auth challenge was initially launched.

Making these steps explicit gives the host application integration more flexibility. This added flexibility makes it easier for the SDK to work alongside more modern Jetpack architectures that use ViewModels and Compose UI.

</details>

<details>
<summary><b>Details: Approve Order v1</b></summary>

In v1, the PayPal SDK encapsulates a lot of Chrome Custom Tabs launching and deep link parsing behavior in an effort to streamline the developer experience. We've found in practice that too much encapsulation can lead to highly opinionated components that prevent developers from building apps their own way. We've decided to give merchants more control in v2 to allow app architects to build with fewer restrictions.

</details>

### PayPal Web Payments

We have refactored the `PayPalWebClient` API to improve the developer experience.

TODO: Implement migration guide for PayPalWebPayments.

### PayPal Native Payments

We have removed `PayPalNativeClient` and all associated classes because the PayPal Native Checkout dependency this module uses has been sunset.
