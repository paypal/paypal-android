# android-sdk
[WIP] One merchant integration point for all of PayPal's services

## Static Analysis Tools
### Detekt
This project uses [Detekt](https://github.com/detekt/detekt) for Kotlin code analysis. To run the code analysis:
```
./gradlew detekt
```
This will output a list of violations, if any. This command will also auto correct formatting issues.

Detekt rules are configured in `detekt/detekt-config.yml`.