# Feature Client Session Stores

**Status: Pending**

## Context

In the past, specifically with the Braintree Android SDK, we made an attempt to improve the developer experience by providing our own solution for process recovery. We later found that our solution for process recovery was too opinonated for a small minority of merchant apps. The fact that many valid Android app architectures exist prevents us from providing a one-size-fits-all solution for process recovery–we simply cannot support every possible Android app architecture.

Our current solution to allow merchants to recover from an Android process kill requires merchants to keep a reference to an `authState` value during a browser-switched flows. The merchant app is fully responsible for restoring itself after its process has been terminated by the Android OS.

While the current solution does prevent the SDK having an opinion on how the merchant app should be architected, it is somewhat error prone. We not only require merchants to keep a reference to a pending `authState` value, we also require them to discard the same reference when an app switch has completed. This is necessary to prevent the SDK from attempting to handle an app switch that has already completed.

## Decision

We can simplify merchant integrations by automatically managing authentication state. We can also prevent the SDK from becoming opinionated in regards to process kill by offering merchant apps a way to restore a Feature Client from a previous state.

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
    // instance state can be captured in any persistance store that supports strings;
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

