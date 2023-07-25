# Remove GraphQL Generics

**Status: Approved**

## Context

Our GraphQL portion of the codebase has [limited unit test coverage][1]. The current implementation follows [DRY programming][7] principles very well–however the usage of generics to decrease repetition makes the code harder to test and maintain.

The core of the current implementation is the `Query` type, an abstract base class:

> <img src="./figure-query-abstract-base-class.png" height="400" alt="Query Abstract Base Class Source Code">

The `Query` class uses a generic type `T` to allow subclasses to provide their own parsing [strategy][8]. The [FundingEligibilityQuery][2] class shows how new GraphQL queries can be created by subclassing `Query`. Generics are a powerful feature used to enforce strong typing in Kotlin, but the resulting code can be difficult to read and maintain when compared to code without generics.

The usage of generics in the Android Payments SDK is very similar to the usage of generics by the iOS Payments SDK. Generics are used on iOS to conform to a [Codable][9] interface to enable JSON parsing by first-party Apple libraries. Unfortunately, there is no `Codable` equivalent on Android.

## Decision

The Payments SDK has a [layered architecture][3]–it can be thought of as an extension to the [OSI Model][4]. The SDK adds additional layers within the OSI Application layer. Each layer communicates with the layer directly below it:

> <img src="./figure-payments-sdk-architecture.png" height="400" alt="Payments SDK Architecture Layers: Merchant App, Feature Client, API, HTTP Client">

As seen in the diagram above, the API layer is a self contained component responsible for translating merchant payment requests into HTTP requests. The API layer encapsulates all of the business logic required to communicate with either a RESTful or GraphQL web service, depending on the payment method. Through encapsulation, the feature client doesn't need to know how each API is accessed.  The API layer is responsible for making web requests and parsing each result.

`JSONObject` is an existing data type provided by the Android SDK that we can use in place of `Query`. We can keep the query at the center of our GraphQL architecture and also gain flexibility to write simpler unit tests. We can still enforce strong typing within the API layer by parsing each `HttpResponse` into an associated [value object][10] that provides transaction details (see updated [Eligibility API implementation][11]).

> <img src="./figure-graph-ql-client.png" height="400" alt="GraphQL Client Source Code">

`GraphQLClient` can be considered part of the HTTP layer since GraphQL requests are made over HTTP. GraphQL responses have a common JSON format and can be parsed entirely within the API layer. Feature Clients can inspect the value objects returned by the API layer and either make additional API calls, or forward results to the Merchant application. The primary benefit for Feature Clients within the layered architecture is the ability to make API calls without needing to know low-level networking details.

We can also follow GraphQL best practices and [provide query variables via JSON][5]. By making each query a `raw` Android resource, we can take advantage of plugins like the [IntelliJ GraphQL Plugin][6] for compile time syntax checking and auto completion of GraphQL queries.

## Consequences

The proposed GraphQL refactor will make the code less DRY in some ways, but the task to create a `JSONObject` for each GraphQL API call isn't overly complex. The tradeoff is increased repetition in the codebase in exchange for more readable code that's easy to follow. A cleaner codebase will reduce the cost of onboarding new contributors.

We will also gain more flexibility by removing abstraction through generics, since the API layer will now have access to the full `HttpRequest` object.

[1]: https://github.com/paypal/paypal-android/blob/1fa0b256c00dc0b95872c21cc4865e6f58d4dd88/CorePayments/src/test/java/com/paypal/android/corepayments/graphql/fundingEligibility/FundingEligibilityQueryTest.kt#L12
[2]: https://github.com/paypal/paypal-android/blob/1fa0b256c00dc0b95872c21cc4865e6f58d4dd88/CorePayments/src/main/java/com/paypal/android/corepayments/graphql/fundingEligibility/FundingEligibilityQuery.kt#L10
[3]: https://www.baeldung.com/cs/layered-architecture
[4]: https://www.cloudflare.com/learning/ddos/glossary/open-systems-interconnection-model-osi/
[5]: https://www.apollographql.com/docs/react/data/operation-best-practices/#use-graphql-variables-to-provide-arguments
[6]: https://plugins.jetbrains.com/plugin/8097-graphql
[7]: https://en.wikipedia.org/wiki/Don%27t_repeat_yourself
[8]: https://refactoring.guru/design-patterns/strategy
[9]: https://www.swiftbysundell.com/basics/codable/
[10]: https://martinfowler.com/bliki/ValueObject.html
[11]: https://github.com/paypal/paypal-android/blob/71dcb84d08fd9be88d1df9c404b812d636678891/CorePayments/src/main/java/com/paypal/android/corepayments/api/EligibilityAPI.kt#L21
