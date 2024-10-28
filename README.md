Welcome to the PayPal Android SDK. This library will help you accept Card, PayPal, and Venmo payments in your Android app.

![Maven Central](https://img.shields.io/maven-central/v/com.paypal.android/card-payments?style=for-the-badge) ![Sonatype Nexus (Snapshots)](https://img.shields.io/nexus/s/com.paypal.android/card-payments?server=https%3A%2F%2Foss.sonatype.org&style=for-the-badge)

## FAQ

### Contribution
If you have suggestions for features that you would like to see in future iterations of the SDK, please feel free to open an issue, PR, or discussion with suggestions. This product is fully open source. We welcome any and all feedback.

## Support
The PayPal Android SDK is available for Android SDK 23+. See our [Client Deprecation policy](https://developer.paypal.com/braintree/docs/guides/client-sdk/deprecation-policy/android/v4) to plan for updates.

## Languages
This SDK is written in Kotlin and supports both Kotlin and Java integrations. See the [Java Demo App](#TODO: link demo when created) and [Kotlin Demo App](/Demo) for sample integrations. 

## Including the SDK
You can support a specific payment method by adding its corresponding feature module as a dependency in your app's `build.gradle` file.
For example, to support both CardPayments and PayPalWebPayments in your app include the following dependencies with the current version:

![Maven Central](https://img.shields.io/maven-central/v/com.paypal.android/card-payments?style=for-the-badge)
```groovy
dependencies {
  implementation 'com.paypal.android:card-payments:<CURRENT-VERSION>'
  implementation 'com.paypal.android:paypal-web-payments:<CURRENT-VERSION>'
}
```

Snapshot builds of the latest SDK features are published from the `main` branch weekly. The snapshot builds can be used to test upcoming features before they have been released. To include a snapshot build, first add the repository to the top `build.gradle` file in your project.

![Sonatype Nexus (Snapshots)](https://img.shields.io/nexus/s/com.paypal.android/card-payments?server=https%3A%2F%2Foss.sonatype.org&style=for-the-badge)
```groovy
repositories {
    maven {
        url 'https://oss.sonatype.org/content/repositories/snapshots/'
    }
}
```

Then, add the dependency:

```groovy
dependencies {
  implementation 'com.paypal.android:card-payments:<CURRENT-VERSION>-SNAPSHOT'
}
```

## Client ID

The PayPal SDK uses a client ID for authentication. This can be found in your [PayPal Developer Dashboard](https://developer.paypal.com/api/rest/#link-getstarted).

## Documentation

Documentation for the project can be found [here](https://developer.paypal.com/docs/checkout/advanced/android/).

## Release Process
This SDK follows [Semantic Versioning](https://semver.org/). This SDK is published to Maven Central. The release process is automated via GitHub Actions.

## Testing

This repository includes unit tests, integration tests, and end-to-end tests.

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

## Contributing

See our [GitHub Guidelines](#TODO: determine where this document will live and update link) for git practices followed in this project.
