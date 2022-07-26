Welcome to the PayPal Android SDK. This library will help you accept Card, PayPal, and Venmo payments in your Android app.

![Maven Central](https://img.shields.io/maven-central/v/com.paypal.android/card?style=for-the-badge) ![Sonatype Nexus (Snapshots)](https://img.shields.io/nexus/s/com.paypal.android/card?server=https%3A%2F%2Foss.sonatype.org&style=for-the-badge)

## FAQ
### Availability
The SDK is currently in the development process. This product is being developed fully open source - throughout the development process, we welcome any and all feedback. Aspects of the SDK _will likely_ change as we develop the SDK. We recommend using the SDK in the sandbox environment until an official release is available. This README will be updated with an official release date once it is generally available.

### Contribution
As the SDK is moved to general availability, we will be adding a contribution guide for developers that would like to contribute to the SDK. If you have suggestions for features that you would like to see in future iterations of the SDK, please feel free to open an issue, PR, or discussion with suggestions. If you want to open a PR but are unsure about our testing strategy, we are more than happy to work with you to add tests to any PRs before work is merged.

## Support
The PayPal Android SDK is available for Android SDK 21+. See our [Client Deprecation policy](https://developer.paypal.com/braintree/docs/guides/client-sdk/deprecation-policy/android/v4) to plan for updates.

## Languages
This SDK is written in Kotlin and supports both Kotlin and Java integrations. See the [Java Demo App](#TODO: link demo when created) and [Kotlin Demo App](/Demo) for sample integrations. 

## Including the SDK
You can support a specific payment method by adding its corresponding feature module as a dependency in your app's `build.gradle` file.
For example, to support both Card and PayPalWebCheckout payments in your app include the following dependencies with the current version:

![Maven Central](https://img.shields.io/maven-central/v/com.paypal.android/card?style=for-the-badge)
```groovy
dependencies {
  implementation 'com.paypal.android:card:<CURRENT-VERSION>'
  implementation 'com.paypal.android:paypal-web-checkout:<CURRENT-VERSION>'
}
```

Snapshot builds of the latest SDK features are published from the `main` branch weekly. The snapshot builds can be used to test upcoming features before they have been released. To include a snapshot build, first add the repository to the top `build.gradle` file in your project.

![Sonatype Nexus (Snapshots)](https://img.shields.io/nexus/s/com.paypal.android/card?server=https%3A%2F%2Foss.sonatype.org&style=for-the-badge)
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
  implementation 'com.paypal.android:card:<CURRENT-VERSION>-SNAPSHOT'
}
```

## Usage
To know how to use each dependency of the SDK, go to any of the following:
- [Card](docs/Card)
- [PayPalUI](docs/PayPalUI)
- [PayPal Web Checkout](docs/PayPalWebCheckout)

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

### Jacoco

This project uses [Jacoco](https://www.jacoco.org/jacoco/) for gathering code coverage metrics. We leverage the 3rd-party [jacoco-android-gradle-plugin](https://github.com/arturdm/jacoco-android-gradle-plugin) to integrate Jacoco into our project.

To run code coverage analysis:

```
./gradlew jacocoTestReport
```

The results are then generated in each module's respective `build/jacoco` folder (e.g. `Card/build/jacoco`).

## Contributing

See our [GitHub Guidelines](#TODO: determine where this document will live and update link) for git practices followed in this project.
