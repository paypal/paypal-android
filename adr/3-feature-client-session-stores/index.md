# Feature Client Session Stores

**Status: Pending**

## Context

TODO: Add a description of why this decision is being made

## Decision

```kotlin
class MyActivity: ComponentActivity() {
    
    private val urlScheme =  "my.custom.url.scheme"
    private val config = CoreConfig(clientId = "MY_CLIENT_ID")
    private val payPalWebCheckoutClient =
        PayPalWebCheckoutClient(applicationContext, config, urlScheme)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.getString("pay_pal_instance_state")?.let { payPalState ->
            payPalWebCheckoutClient.restore(payPalState)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("pay_pal_instance_state", payPalWebCheckoutClient.instanceState)
    }
}
```


TODO: Add a description of the decision itself

## Consequences

TODO: Add a description of the consequences related to the decision

