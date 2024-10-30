# PayPal SDK Migration Guide

This guide highlights how to migrate to the latest version of the PayPal SDK.

## Table of Contents

1. CardPayments
1. PayPalWebPayments
1. PayPalNativePayments

### CardPayments

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
class Activity: ComponentActivity(), ApproveOrderListener {

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
+     // preserve auth state for balancing call to completeAuthChallenge() in onResume()
+     is CardPresentAuthChallengeResult.Success -> authState = result.authState
+     is CardPresentAuthChallengeResult.Failure -> TODO("Handle Present Auth Challenge Failure")
+   }
+ }
}
```

<details>
<summary><b>Details: Approve Order v2</b></summary>
</details>

TODO: add notes on approve order v2

<details>

<summary><b>Details: Approve Order v1</b></summary>

// in this version, presentAuthChallenge() is called internally by the SDK
// in v1, completeAuthChallenge() is called internally to handle deep links

</details>

### PayPalWebPayments

We have refactored the `PayPalWebClient` API to improve the developer experience.

### PayPalNativePayments

We have removed `PayPalNativeClient` and all associated classes because the PayPal Native Checkout dependency this module uses has been sunset.
