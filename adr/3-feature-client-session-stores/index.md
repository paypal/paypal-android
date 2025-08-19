# Feature Client Session Stores

**Status: Pending**

## Context

TODO: Add a description of why this decision is being made

## Decision

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
    // the merchant app no longer needs to provide "auth state" to the finishStart() method
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

TODO: Add a description of the decision itself

## Consequences

TODO: Add a description of the consequences related to the decision

