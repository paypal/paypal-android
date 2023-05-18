# Dissolve Core API and HTTP Request Factories

**Status: Proposed**

## Context

- We've borrowed the Braintree Android SDK design pattern of having a core component to encapsulate the following behaviors:
  * Load Braintree Configuration
  * Initiate a Browser Switch
  * Send HTTP Requests
  * Send GraphQL Requests
  * Send Analytics

Since the Braintree Gateway is effectively a monolith, this pattern makes sense. It also allows reduces the dependencies required by Feature Clients, in most cases limiting them to a single BraintreeClient instance.

In the PayPal SDK, having a core component is less beneficial. There is a much larger diversity of web services offered by the PayPal platform. With the PayPal platform, it becomes difficult to determine which components are considered "core" components.

## Decision

While the PayPal SDK doesn't have a core client, it does have a core `API` class that is similar to `BraintreeClient`. In the PayPal SDK, each feature client has an associated API class used to execute requests over the network to a given web service.

The API pattern is a great way to shield feature clients from the complexities of making a network request, similar to how `BraintreeClient` `sendPost()` and `sendGraphQL` post do in the Braintree SDK. It allows feature clients to focus on business logic and success (or failure) tracking with Analytics.

Currently our naming convention and division of responsibility is build on a one-to-one relationship between `<FEATURE_NAME>Client` and `<FEATURE_NAME>API`. While in most cases this does result in a positive feature client implementation because, as mentioned before, there is no low-level networking code in the feature client, the set of behavior offered by each API component is arbitrary. The API components in our architecture currently do not map to a known real world entity. Feature clients may rely on multiple PayPal web services, and if multiple features rely on the same microservices, we run the risk of introducing duplication within the codebase.

A more scalable solution would be to offer feature clients a one-to-many relationship with API components. We can map each API class to its own corresponding microservice. API networking code can then be shared among different features. It will then become clear what the responsibility of each API class is, which is good for DRYness, Single Responsibility Principle, and tesability.

> <img src="./figure-multi-api-uml.png" height="500" alt="Example CardClient architecture without a Core API component">

> <img src="./figure-card-client-example.png" height="400" alt="Example CardClient architecture without a Core API component">

We also have an opportunity to implement each API component as a [Deep Module][1]. We created `HttpRequestFactory` as a temporary concept because it provided a highly testable interface for asserting `APIRequest` JSON bodies. Since API classes will have a single responsibility to execute API requests internally, we can simplify our design by making this a responsibility of the API class. Response parsing can also be done internally.

> <img src="./figure-deep-module-vs-shallow-module.png" height="400" alt="Example CardClient architecture without a Core API component">


## Consequences

Composing each API within a feature client does lead to an increased parameter count when constructing feature clients. This is unsightly for the most part. The number of dependencies for a given feature client will correspond directly to the number of APIs a given feature client interacts with.

[1]: https://nakabonne.dev/posts/depth-of-module/
