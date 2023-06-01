# Accepting PayPal Native Payments
 
The PayPal Native Payments module in the PayPal SDK enables PayPal payments in your app via the checkout flow.

Follow these steps to add PayPal Native payments:

1. [Setup a PayPal Developer Account](#setup-a-paypal-developer-account)
2. [Add PayPal Native Checkout Module](#add-paypal-native-checkout-module)
3. After initial setup, follow instructions [here](#billing-agreement) for Billing Agreements
4. [Test and go live](#test-and-go-live)

## Setup a PayPal Developer Account

You will need to set up authorization to use the PayPal Payments SDK.
Follow the steps in [Get Started](https://developer.paypal.com/api/rest/#link-getstarted) to create a client ID.

The SDK requires a server-side integration to create an order to capture funds using the [PayPal Orders v2 API](https://developer.paypal.com/docs/api/orders/v2).
The order created on your server will be used to authorize or capture funds. For initial setup, the `curl` commands below can be used as a reference for making server-side RESTful API calls.

## Add PayPal Native Payments Module

### 1. Add the PayPal Native Payments module to your app

In your `build.gradle` file, add the following dependency:

```groovy
dependencies {
   implementation "com.paypal.android:paypal-native-payments:<CURRENT-VERSION>"
}
```

### 2. Sample App Preparation

In order to integrate PayPal native payments, you will need:

1. A PayPal client ID and corresponding secret. This is to generate an access token that will allow you to create payment tokens, capture funds and authorize customers to place orders.
2. Setting a return URL.

A return URL is required for redirecting users back to the app after authenticating. Please reference our [developer documentation](https://developer.paypal.com/limited-release/paypal-mobile-checkout/android/#link-enablethesdk) to create said url and also to learn about how to create a new PayPal application as well.

### 3. Initiate PayPal Native checkout

Create a `CoreConfig` using an [client id](https://developer.paypal.com/api/rest/):

```kotlin
val config = CoreConfig("<CLIENT_ID>", environment = Environment.SANDBOX)
```

Create a `PayPalNativeCheckoutClient` with your `RETURN_URL` created above::
```kotlin
val payPalNativeClient = PayPalNativeCheckoutClient(
   application = requireActvitiy().application,
   coreConfig = coreConfig,
   returnUrl = "<RETURN_URL>" 
)
```

Set the required `listener` on the `PayPalNativeCheckoutClient` to receive result notifications for the SDK.

```kotlin
payPalNativeClient.listener = object : PayPalNativeCheckoutListener {

    override fun onPayPalCheckoutStart() {
        // the PayPal paysheet is about to appear
    }
    
    override fun onPayPalSuccess(result: PayPalNativeCheckoutResult) {
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

### 4. Inspect shipping details

:warning: Only implement `PayPalNativeShippingListener` if your order ID was created with [`shipping_preference`](https://developer.paypal.com/docs/api/orders/v2/#definition-order_application_context) = `GET_FROM_FILE`. If you created your order ID with [`shipping_preference`](https://developer.paypal.com/docs/api/orders/v2/#definition-order_application_context) = `NO_SHIPPING` or `SET_PROVIDED_ADDRESS`, **skip this step**.

Setting this `shippingListener` on the `PayPalNativeCheckoutClient` notifies your app when the user updates their shipping address or shipping method. You are required to `PATCH` the order details on your server if the shipping method (or amount) changes. Do this with the [PayPal Orders API - Update order](https://developer.paypal.com/docs/api/orders/v2/#orders_patch) functionality.

```kotlin
payPalNativeClient.shippingListener = object : PayPalNativeShippingListener {

    override fun onPayPalNativeShippingAddressChange(
        actions: PayPalNativePaysheetActions,
        shippingAddress: PayPalNativeShippingAddress
    ) {
        // called when the user updates their chosen shipping address

        // REQUIRED: you must call actions.approve() or actions.reject() in this callback
        actions.approve()

        // OPTIONAL: you can optionally patch your order. Once complete, call actions.approve() if successful or actions.reject() if not.
    }

    override fun onPayPalNativeShippingMethodChange(
        actions: PayPalNativePaysheetActions,
        shippingMethod: PayPalNativeShippingMethod
    ) {
        // called when the user updates their chosen shipping method

        // REQUIRED: patch your order server-side with the updated shipping amount.
        // Once complete, call `actions.approve()` or `actions.reject()`
        try {
            patchOrder()
            actions.approve()
        } else {
            actions.reject()
        }
    }
}
```

### 5. Create an order

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

### 6. Start the PayPal Native checkout flow

To start the PayPal Native checkout flow, call the `startCheckout` function on `PayPalNativeCheckoutClient`, with a `PayPalNativeCheckoutRequest` using your order ID from [step 4](#4-create-an-order):

```kotlin
val request = PayPalNativeCheckoutRequest("<ORDER_ID>")
payPalClient.startCheckout(request)
```
When a user completes the PayPal payment flow successfully, the result will be returned to the listener set in [step 3](#3-initiate-paypal-native-checkout).

### 7. Capture/Authorize the order

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

## Billing Agreement

### 1. Create Billing Agreement

**Request**

```bash
curl --location --request POST 'https://api.sandbox.paypal.com/v2/checkout/orders/' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer <ACCESS_TOKEN>' \
--data-raw '{
  "description": "Billing Agreement",
  "shipping_address":
  {
    "line1": "1350 North First Street",
    "city": "San Jose",
    "state": "CA",
    "postal_code": "95112",
    "country_code": "US",
    "recipient_name": "John Doe"
  },
  "payer":
  {
    "payment_method": "PAYPAL"
  },
  "plan":
  {
    "type": "MERCHANT_INITIATED_BILLING",
  }
}'
```

**Response**

```json
{
   "token_id": "<BILLING_AGREEMENT_TOKEN>"
}
```

### 2. Set Billing Agreement

```kotlin
paypalNativeClient.startCheckout(CreateOrder { createOrderActions ->
   createOrderActions.setBillingAgreementId(BILLING_AGREEMENT_TOKEN)
})
```

### 3. Start checkout

Follow steps here to [Initiate PayPal Native Checkout](#3-initiate-paypal-native-checkout)

## Test and go live

### 1. Test the PayPal integration

Follow the [Create sandbox account](https://developer.paypal.com/api/rest/#link-createsandboxaccounts) instructions to create a PayPal test account.
When prompted to login with PayPal during the payment flow on your mobile app, you can log in with the test account credentials created above to complete the Sandbox payment flow.

### 2. Go live with your integration

Follow [these instructions](https://developer.paypal.com/api/rest/production/) to prepare your integration to go live.
