# Feature Client Instance State

**Status: Pending**

## Summary

This ADR proposes creating internal state handles for Feature Clients to offer support for process-kill recovery in browser and app-switched flows. In addition to providing an opaque snapshot of each Feature Client's internal state to merchants, the SDK will automatically manage the persistence of internal state for each Feature Client. This refactor will reduce integration complexity, improve developer experience, and maintain reliable process kill recovery without imposing an opinion on the merchant app's architecture.

## Key Terms

<dl>
  <dt><strong>Feature Client</strong></dt>
  <dd>An SDK component that serves as a merchant-facing entry point to a payment method e.g. `CardClient`, `PayPalWebCheckoutClient`, etc.</dd>
  <dt><strong>Instance State</strong></dt>
  <dd>Serialized representation of a Feature Client's internal state used for persistence and restoration.</dd>
</dl>

## Context

In the past, specifically with the Braintree Android SDK, we made an attempt to improve the developer experience by providing our own solution for process recovery. We later found that our solution for process recovery was too opinionated for a small minority of merchant apps. The fact that many Android app architectures exist prevents us from providing a one-size-fits-all solution for process recovery–we simply cannot support every possible Android app architecture.

Our current solution to allow merchants to recover from an Android process kill requires merchants to keep a reference to an `authState` value during browser-switched flows. The merchant app is fully responsible for restoring itself after its process has been terminated by the Android OS.

While the current solution does prevent the SDK having an opinion on how the merchant app should be architected, it is somewhat error prone. We not only require merchants to keep a reference to a pending `authState` value, we also require them to discard the same reference when an app switch has completed. This is necessary to prevent the SDK from attempting to handle an app switch that has already completed.

## Decision

We can simplify merchant integrations by automatically managing `authState`. We can also offer merchant apps a way to restore a Feature Client from a previous state during process recovery.

An `instanceState` property will give merchants a handle to a Feature Client's internal state as an opaque, base64 encoded string. To restore a Feature Client using `instanceState`, merchant apps can simply call `restore()` at any point in time. In practice, `restore()` and `instanceState` can be invoked in response to the lifecycle events of Android components, e.g. Activities, Fragments, ViewModels, etc.

In summary, this solution preserves merchant flexibility by allowing them to choose their own persistence strategy (Bundle, SharedPreferences, databases, etc.) while the SDK handles the complexity of Feature Client state management internally.

```diff
class MyActivity: ComponentActivity() {
    
  private val urlScheme =  "my.custom.url.scheme"
  private val config = CoreConfig(clientId = "MY_CLIENT_ID")
  private val payPalWebCheckoutClient =
      PayPalWebCheckoutClient(applicationContext, config, urlScheme)
  
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // the merchant app can restore feature clients using "instance state"
+   savedInstanceState?.getString("pay_pal_instance_state")?.let { payPalState ->
+     payPalWebCheckoutClient.restore(payPalState)
+   }
  }
 
  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    // instance state can be captured in any persistent store that supports strings;
    // this example uses saved instance state for process kill recovery
+   outState.putString("pay_pal_instance_state", payPalWebCheckoutClient.instanceState)
  }

  private fun payWithPayPal() {
    val checkoutRequest = PayPalWebCheckoutRequest(orderId = "ORDER_ID")
    when (val startResult = payPalWebCheckoutClient.start(this, checkoutRequest)) {
      is PayPalPresentAuthChallengeResult.Success -> {
        // authState is now a part of instance state; merchant apps will no longer
        // be required to capture this value
-       authState = startResult.authState
      }

      is PayPalPresentAuthChallengeResult.Failure -> {
        // handle error
      }
    }
  }

  private fun checkIfPayPalAuthComplete(intent: Intent) {
    // the merchant app no longer needs to provide "auth state" to the finishStart() method; the merchant app
    // also no longer needs to discard its reference to "auth state" when complete
-   when (val payPalAuthResult = payPalWebCheckoutClient.finishStart(intent, authState)) {
+   when (val payPalAuthResult = payPalWebCheckoutClient.finishStart(intent)) {
      is PayPalWebCheckoutFinishStartResult.Success -> {
        // handle success
-       discardAuthState()
      }

      is PayPalWebCheckoutFinishStartResult.Canceled -> {
        // handle canceled
-       discardAuthState()
      }

      is PayPalWebCheckoutFinishStartResult.Failure -> {
        // handle auth failure
-       discardAuthState()
      }

      PayPalWebCheckoutFinishStartResult.NoResult -> {
        // no result; allow user to retry
      }
    }
  }
}
```

## Consequences

Here are potential positive, negative, and long term impacts that may result from this refactor:

**Positive Impacts**

1. Merchants no longer need to think about `authState` when finishing a browser-switched payment flow
1. The `finishStart(intent: Intent, authState: String)` method API is reduced to `finishStart(intent: Intent)`
1. Merchants no longer need to "discard" pending `authState`, making integration errors less likely
1. Merchant apps retain full control over their own persistence strategies while benefiting from simplified state management
1. We can deprecate the existing pattern, and merchants can be gradually migrated to this new pattern

**Negative Impacts**

1. The SDK is responsible for serialization and deserialization of internal state, which increases complexity
1. Merchants will need to update their existing integrations to take advantage of the new feature
1. Instance state may need to be versioned to avoid schema collisions with new instance state formats
1. Instance state may become arbitrarily large as more internal state is preserved
1. We need to update the MIGRATION_GUIDE for this new pattern

**Long-term Impact**

1. Future Feature Clients with browser and app-switched flows should follow this pattern for consistency
1. The SDK team must maintain robust testing around state serialization to prevent restoration errors
