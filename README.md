# android-sdk
[WIP] One merchant integration point for all of PayPal's services

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

