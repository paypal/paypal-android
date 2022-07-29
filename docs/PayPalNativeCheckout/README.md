# Accepting PayPal Native Checkout Payments

The PayPal Native Checkout module in the PayPal SDK enables PayPal payments in your app

Follow these steps to add PayPal Native Checkout payments:

1. [Setup a PayPal Developer Account](#setup-a-paypal-developer-account)
1. [Add PayPal Native Checkout Module](#add-paypal-native-checkout-module)
1. [Test and go live](#test-and-go-live)

## Setup a PayPal Developer Account

You will need to set up authorization to use the PayPal Payments SDK.
Follow the steps in [Get Started](https://developer.paypal.com/api/rest/#link-getstarted) to create a client ID and secret, to generate access tokens.

You will need a server integration to create an order to capture funds using the [PayPal Orders v2 API](https://developer.paypal.com/docs/api/orders/v2).
For initial setup, the `curl` commands below can be used as a reference for making server-side RESTful API calls.

## Add PayPal Native Checkout Module

### 1. Add the PayPal Native checkout module to your app

In your `build.gradle` file, add the following dependency:

```groovy
dependencies {
   implementation "com.paypal.android:paypal-native-checkout:<CURRENT-VERSION>"
}
```

### 2. Sample App Preparation

In order to integrate PayPalNative checkout, you will need:

1. An app client ID and corresponding secret. This is to generate and access token that will allow you to create payment tokens, capture funds and authorize
   customers to place orders.
2. Setting a return URL.

A return URL is required for redirecting users back to the app after authenticating. Please reference our [developer documentation](https://developer.paypal.com/docs/business/native-checkout/android/)
to create said url and also to learn about how to create a new PayPal application as well.

### 3. Initiate PayPal Native Checkout

Create a `CoreConfig` using with an `ACCESS_TOKEN` created with the `CLIENT_ID` and `SECRET`:

```kotlin
 val coreConfig = CoreConfig("<ACCESS_TOKEN>", environment= Environment.SANDBOX)
```

Create a `PayPalClient` with your `RETURN_URL` created above:
```kotlin
val payPalClient = PayPalClient(
   application = requireActvitiy().application,
   coreConfig = coreConfig,
   returnUrl = "<RETURN_URL>"
)
```

Set a listener on the client to receive payment flow callbacks:

```kotlin
payPalClient.listener = object : PayPalNativeCheckoutListener {

    override fun onPayPalSuccess(result: PayPalCheckoutResult) {
       // order was successfully approved and is ready to be captured/authorized (see step 6)
    }

    override fun onPayPalFailure(error: PayPalSDKError) {
       // handle the error
    }

    override fun onPayPalCanceled() {
       // the user canceled the flow
    }
}
```

### 4. Create an order

When a user initiates a payment flow, call `v2/checkout/orders` to create an order and obtain an order ID:

**Request**
```bash
curl --location --request POST 'https://api.sandbox.paypal.com/v2/checkout/orders/' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer <ACCESS_TOKEN>' \
--data-raw '{
    "intent": "<CAPTURE|AUTHORIZE>",
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

**Response**
```json
{
   "id":"<ORDER_ID>",
   "status":"CREATED"
}
```

The `id` field of the response contains the order ID to pass to your client.

### 5. Start the PayPal Native Checkout flow

Start the PayPal Native checkout flow with the order ID generated in [step 4](#4-create-an-order):

```kotlin
paypalNativeClient.startCheckout(orderId)
```
When a user completes the PayPal payment flow successfully, the result will be returned to the listener set in [step 3](#3-initiate-paypal-native-checkout).

### 6. Capture/Authorize the order

After receiving a successful result from the `onPayPalSuccess()` callback, you can now capture or authorize the order.

Call `capture` to capture funds immediately:

```bash
curl --location --request POST 'https://api.sandbox.paypal.com/v2/checkout/orders/<ORDER_ID>/capture' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer <ACCESS_TOKEN>' \
--data-raw ''
```

Call `authorize` to place funds on hold:

```bash
curl --location --request POST 'https://api.sandbox.paypal.com/v2/checkout/orders/<ORDER_ID>/authorize' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer <ACCESS_TOKEN>' \
--data-raw ''
```

## Test and go live

### 1. Test the PayPal integration

Follow the [Create sandbox account](https://developer.paypal.com/api/rest/#link-createsandboxaccounts) instructions to create a PayPal test account.
When prompted to login with PayPal during the payment flow on your mobile app, you can log in with the test account credentials created above to complete the Sandbox payment flow.

### 2. Go live with your integration

Follow [these instructions](https://developer.paypal.com/api/rest/production/) to prepare your integration to go live.
