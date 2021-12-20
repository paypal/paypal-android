# Document – Pay with Card

## Get started

```---
title: Pay with Card
subtitle: Accept card payments in your app
layout: docs
sdkVersion: 1.0.0
keywords: PayPal, SDK, Card, Credit Card, Payments
contentType: SDK
productStatus: Current
```

Accept card payments in your app using the PayPal In-App Payments SDK.

## How it works

The In-App payments SDK allows for merchant apps to integrate easily with PayPal payment services on both the client and server.

You need to have a server integration to create an order and capture the funds using [PayPal Orders v2 API](https://developer.paypal.com/docs/api/orders/v2). The Payments SDK will allow you to approve the order in your client side using the buyer's card information.

## Eligibility
```
placeholder - Question for product (Rahul?)
what kinda merchants are we targetting?
what features do we offer right now/what features are we missing?
```

## Integration methods

- [Card fields UI integration](#card-fields-ui-integration)
- [Fully customizable integration](#fully-customizable-integration)



## Card fields UI integration
```---
title: Card fields UI integration
subtitle: 
layout: docs
sdkVersion: 1.0.0
keywords: PayPal, SDK, Card, Credit Card, Payments
contentType: SDK
productStatus: Current
```


Accept card payments using our card fields UI

`Placeholder: Include images of card fields`

**Requirements:**
Card fields UI offers a low effort integration where you can use our card fields UI, and our card fields will handle ...


### Code sample

#### 1. Create an order
Call `v2/checkout/orders` to create an order an obtain an order ID

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

#### 2. Approve the order using Payments SDK:

#### iOS
```swift
import PaymentsSDK

// Create an config object using your clientID obtained from PayPal application portal
let config = CoreConfig(clientID: "<CLIENT_ID>", evironment: .sandbox)

// Create a CardClient object to process a card payment
let cardClient = CardClient(config: config)

// Create a card object from buyer's inputs
let card = Card(
    number: "4111111111111111",
    expirationMonth: "01",
    expirationYear: "25",
    securityCode: "123"
)

// Create a CardRequest and approve the order using buyer's card when buyer submit their card info.
let cardRequest = CardRequest(orderID: "<ORDER_ID>", card: card)
cardClient.approveOrder(request: cardRequest) { result in
    switch result {
        case .success(let result):
            // Order is successfully approved with card and ready to be capture/authorize.
        case .failure(let error):
            // Encountered error when approving order.
    }
}
```

#### Android
```kotlin=
// placeholder
```

#### 3. Capture/authorize the order
Call `v2/checkout/orders/<ORDER_ID>/authorize` to place funds on hold:

```
curl --location --request POST 'https://api.sandbox.paypal.com/v2/checkout/orders/<orderID>/authorize' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer <access_token>' \
--data-raw ''
```

Call `v2/checkout/orders/<ORDER_ID>/capture` to capture funds immediately:

```
curl --location --request POST 'https://api.sandbox.paypal.com/v2/checkout/orders/<orderID>/capture' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer <access_token>' \
--data-raw ''
```


### 1. Initial code sample
Answers the question: What is the minimum code required for this integration?

#### Android
```
Code sample placeholder
```

#### iOS
```



Code sample placeholder
```

### 2. Intermediate steps

Answers the question: What are the key elements from this code sample that I need to modify for this integration?

#### Android
```
Code sample placeholder
```

#### iOS
```
Code sample placeholder
```

### 3. Complete code sample

Answers the question: What are all possible coding options for this solution?

#### Android
```
Code sample placeholder
```

#### iOS
```
Code sample placeholder
```

### 4. Test

- Use application portal + sandbox account

### 5. Go live

- Obtain live credentials






## Fully customizable integration
```---
title: Fully customizable integration
subtitle: Accept card payments in your app
layout: docs
sdkVersion: 1.0.0
keywords: PayPal, SDK, Card, Credit Card, Payments
contentType: SDK
productStatus: Current
```

The PayPal Payments SDK offers functionalities to approve an order ...

Process card payments with PayPal Payments SDK using your own UI.

**Requirements:**

### 1. Initial code sample
Answers the question: What is the minimum code required for this integration?

#### Android
```
Code sample
```

#### iOS
```
Code sample
```

### 2. Intermediate steps

Answers the question: What are the key elements from this code sample that I need to modify for this integration?

#### Android
```
Code sample
```

#### iOS
```
Code sample
```

### 3. Complete code sample

Questions:
- Should document server steps in here? (create/capture/authorize)

#### Android
```
Code sample
```

#### iOS
```swift
import PaymentsSDK

let config = CoreConfig(clientID: <your_client_id>, evironment: <environment>)
let cardClient = CardClient(config: config)

let card = Card(
    number: <card_number>,
    expirationMonth: <expiration_month>,
    expirationYear: <expiration_year>,
    securityCode: <security_code>
)

let cardRequest = CardRequest(orderID: <order_id>, card: card)

cardClient.approveOrder(request: cardRequest) { result in
    switch result {
        case .success(let result):
            // Order is successfully approved with card and ready to be capture/authorize.
        case .failure(let error):
            // Encountered error when approving order.
    }
}
```

### 4. Test

Answers the question: How do I test the integration?

> **Note:** You can point to the existing Test and Go Live guide - https://developer.paypal.com/docs/business/test-and-go-live/

For example - https://developer.paypal.com/docs/checkout/standard/

### 5. Go live

Answers the question: How do I go live with this integration?

> **Note:** You can point to the existing Test and Go Live guides:
>
> * Business: https://developer.paypal.com/docs/business/test-and-go-live/
> * Marketplaces and Platforms: https://developer.paypal.com/docs/multiparty/test-and-go-live/
