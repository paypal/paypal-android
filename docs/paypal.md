# Pay with PayPal

## How it works

## Eligibility

## Integration methods


## How to integrate

### Know before you code

### Technical steps

#### 1. Add the Payments SDK  to your app

In your `build.gradle` file, add the following dependency:

```groovy
dependencies {
   implementation "com.paypal.android.checkout:1.0.0"
}
```

#### 2. Create a PayPal button 

Add a `PayPalButton` to your layout XML.

```xml
<com.paypal.android.checkout.paymentbutton.PayPalButton
    android:id="@+id/payPalButton"
    android:layout_width="match_parent"
    android:layout_height="wrap_content" />
```



#### 3. Initiate the Payments SDK

Create a `CoreConfig` using your clientID obtained from PayPal application portal.

```kotlin
val config = CoreConfig("<CLIENT_ID>", environment = Environment.SANDBOX)
```

Configure a return URL using your application ID.

```kotlin
val returnUrl = "<APPLICATION_ID>" + "://paypalpay"
```

Create a `PayPalClient` to approve an order with a PayPal payment method.

```kotlin
val payPalClient = PayPalClient(requireActivity().application, config, returnUrl)
```

#### 4. Create an order

When a user enters the payment flow, call `v2/checkout/orders` to create an order an obtain an order ID

```
curl --location --request POST 'https://api.sandbox.paypal.com/v2/checkout/orders/' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer <access_token>' \
--data-raw '{
    "intent": "CAPTURE",
    "purchase_units": [
        {
            "amount": {
                "currency_code": "USD",
                "value": "5.00"
            }
        }
    ]
}'
```

#### 5. Approve the order through the Payments SDK

When a user taps the PayPal button created above, approve the order using your `PayPalClient`.

Attach your PayPal button to the PayPal payment flow:

```kotlin
view.findViewById<View>(R.id.payPalButton).setOnClickListener {
    launchPayPal(orderID)
}
```

Call `PayPalClient#checkout` to approve the order, and handle results:

```kotlin
fun launchPayPal(orderID: String) {
    payPalClient.checkout(orderId) { result ->
        when (result) {
            is PayPalCheckoutResult.Success -> {
                // capture/authorize the order (see step 6)
            } 
            is PayPalCheckoutResult.Failure -> {
                // handle the error by accessing `result.error`
            } 
            is PayPalCheckoutResult.Cancellation -> {
                // the user canceled
            } 
        }
    }
}
```

#### 6. Capture/authorize the order

If you receive a successful result in the client-side flow, you can then capture or authorize the order. 

Call `authorize` to place funds on hold:

```
curl --location --request POST 'https://api.sandbox.paypal.com/v2/checkout/orders/<orderID>/authorize' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer <access_token>' \
--data-raw ''
```

Call `capture` to capture funds immediately:

```
curl --location --request POST 'https://api.sandbox.paypal.com/v2/checkout/orders/<orderID>/capture' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer <access_token>' \
--data-raw ''