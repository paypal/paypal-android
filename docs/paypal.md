# Pay with PayPal

Accept card payments in your Android app using the PayPal Payments SDK.

## How it works

This diagram shows how your client, your server, and PayPal interact:

// TODO - Get a diagram of the payment flow similar to [this](https://developer.paypal.com/braintree/docs/start/overview#how-it-works)

## Eligibility

PayPal is available as a payment method to merchants in multiple [countries and currencies](https://developer.paypal.com/docs/checkout/payment-methods/).

This SDK supports a minimum Android API of 23 or higher.
Android apps can be written in Kotlin or Java 8 or higher.

## How to integrate

- [Custom Integration](#technical-steps---custom-integration)

### Know before you code

You will need to set up authorization to use the PayPal Payments SDK. 
Follow the steps in [Get Started](https://developer.paypal.com/api/rest/#link-getstarted) to create a client ID and generate an access token. 
Follow the steps in [Enable the SDK](https://developer.paypal.com/sdk/in-app/android/#link-enablethesdk) to set up your account and application for PayPal payments.

You will need a server integration to create an order and capture the funds using [PayPal Orders v2 API](https://developer.paypal.com/docs/api/orders/v2). 
To test your initial integration and account setup, the `curl` commands below can be used in place of a server SDK.

### Technical steps - custom integration

#### 1. Add the Payments SDK  to your app

In your `build.gradle` file, add the following dependency:

```groovy
dependencies {
   implementation "com.paypal.android:paypal-checkout:1.0.0"
}
```

Add the following maven credentials to your `build.gradle`:
```groovy
maven {
    url  "https://cardinalcommerceprod.jfrog.io/artifactory/android"
    credentials {
        username 'paypal_sgerritz'
        password 'AKCp8jQ8tAahqpT5JjZ4FRP2mW7GMoFZ674kGqHmupTesKeAY2G8NcmPKLuTxTGkKjDLRzDUQ'
    }
}
```

#### 2. Create a PayPal button 

Add a `PayPalButton` to your layout XML:

```xml
<com.paypal.android.checkout.paymentbutton.PayPalButton
    android:id="@+id/payPalButton"
    android:layout_width="match_parent"
    android:layout_height="wrap_content" />
```

#### 3. Initiate the Payments SDK

Create a `CoreConfig` using your client ID obtained from the PayPal Developer Portal:

```kotlin
val config = CoreConfig("<CLIENT_ID>", environment = Environment.SANDBOX)
```

Configure a return URL using your application ID:

```kotlin
val returnUrl = "<APPLICATION_ID>" + "://paypalpay"
```

Create a `PayPalClient` to approve an order with a PayPal payment method:

```kotlin
val payPalClient = PayPalClient(application, config, returnUrl)
```

#### 4. Create an order

When a user enters the payment flow, call `v2/checkout/orders` to create an order an obtain an order ID:

```bash
curl --location --request POST 'https://api.sandbox.paypal.com/v2/checkout/orders/' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer <ACCESS_TOKEN>' \
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

The `id` field of the response contains the order ID to pass to your client.

#### 5. Approve the order through the Payments SDK

When a user taps the PayPal button created above, approve the order using your `PayPalClient`.

Attach your PayPal button to the PayPal payment flow:

```kotlin
findViewById<View>(R.id.payPalButton).setOnClickListener {
    launchPayPal(orderID)
}
```

Call `PayPalClient#checkout` to approve the order, and handle results:

```kotlin
fun launchPayPal(orderID: String) {
    payPalClient.checkout(orderID) { result ->
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

```bash
curl --location --request POST 'https://api.sandbox.paypal.com/v2/checkout/orders/<ORDER_ID>/authorize' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer <ACCESS_TOKEN>' \
--data-raw ''
```

Call `capture` to capture funds immediately:

```bash
curl --location --request POST 'https://api.sandbox.paypal.com/v2/checkout/orders/<ORDER_ID>/capture' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer <ACCESS_TOKEN>' \
--data-raw ''
```

### Testing your integration

Follow the [Create sandbox accounts](https://developer.paypal.com/api/rest/#link-createsandboxaccounts) instructions to create PayPal test accounts.
When prompted to login with PayPal during the payment flow on your mobile app, you can log in with the test account credentials created above to complete the Sandbox payment flow. 

### Go live

Follow [these instructions](https://developer.paypal.com/api/rest/production/) to prepare your integration to go live.