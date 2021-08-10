# PayPal Android SDK
Welcome to the PayPal Android SDK. This library will help you accept Card, PayPal, and Venmo payments in your Android app.

## Support
The PayPal Android SDK is available for Android SDK 21+. See our [Client Deprecation policy](https://developer.paypal.com/braintree/docs/guides/client-sdk/deprecation-policy/android/v4) to plan for updates.

## Languages
This SDK is written in Kotlin and supports both Kotlin and Java integrations. See the [Java Demo App](#TODO: link demo when created) and [Kotlin Demo App](/Demo) for sample integrations. 

## Including the SDK
To accept a certain payment method in your app, you only need to include that payment-specific dependency in your `build.gradle`. 
For an integration offering card and PayPal payments, include the following:

```groovy
dependencies {
  implementation 'com.paypal.android:card:1.0.0'
  implementation 'com.paypal.android:paypal:1.0.0'
}
```

## Sample Code

// TODO: Update code snippet after finalizing integration

```kotlin
// STEP 0: Fetch an ACCESS_TOKEN and ORDER_ID from your server.

// STEP 1: Create a PaymentConfiguration object
paymentConfig = PaymentConfig(ACCESS_TOKEN)

// STEP 2: Create payment method client objects
cardClient = CardClient(paymentConfig)

// STEP 3: Collect relevant payment method details
card = Card()
card.number = "4111111111111111"
card.cvv = "123"

// STEP 4: Call checkout method
cardClient.checkoutWithCard(ORDER_ID, card) { result, error _>
    error?.let {
        // handle checkout error
        return
    }
    result?.let {
        val orderID = it.orderID 
        // Send orderID to your server to process the payment
    }
}

// STEP 5: Send orderID to your server to capture/authorize
```
## Release Process
This SDK follows [Semantic Versioning](https://semver.org/). This SDK is published to Maven Central. The release process is automated via GitHub Actions.

### Snapshot Builds
Snapshot builds of the latest SDK features are published from the `main` branch weekly. The snapshot builds can be used to test upcoming features before they have been released. To include a snapshot build:

```groovy
dependencies {
  implementation 'com.paypal.android:card:1.0.0-SNAPSHOT'
}
```

## Testing

This repository includes unit tests, integration tests, and end to end tests.

// TODO: Add sections with commands for running each type of tests 

## Static Analysis Tools

### Detekt
This project uses [Detekt](https://github.com/detekt/detekt) for Kotlin code analysis. To run the code analysis:
```
./gradlew detekt
```
This will output a list of violations, if any.

Running the gradle task with the `-PdetektAutoCorrect` parameter, will automatically correct formatting issues:
```
./gradlew detekt -PdetektAutoCorrect
```

Detekt rules are configured in `detekt/detekt-config.yml`.

### Jacoco

This project uses [Jacoco](https://www.jacoco.org/jacoco/) for gathering code coverage metrics. We leverage the 3rd-party [jacoco-android-gradle-plugin](https://github.com/arturdm/jacoco-android-gradle-plugin) to integrate Jacoco into our project.

To run code coverage analysis:

```
./gradlew jacocoTestReport
```

The results are then generated in each module's respective `build/jacoco` folder (e.g. `Card/build/jacoco`).

## Contributing

See our [GitHub Guidelines](#TODO: determine where this document will live and update link) for git practices followed in this project.
