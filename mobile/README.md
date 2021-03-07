# Android Library and Demo App

Point of contact for this folder: [@lambdapioneer](https://github.com/lambdapioneer)

This folder contains a work-in-progress prototype/demo app for Android.

It demonstrates calling the API from an app and the cryptographic primitives to interact with the secure element.

## Setup

This folder can be imported as a regular AndroidStudio project. Alternatively, you can build and install it using the regular `./gradlew` commands.

For running the instrumented tests with an attached device or AVDS:

```
$ ./gradlew connectedAndroidTest

> Task :lib:connectedDebugAndroidTest
Starting 57 tests on pixel2_api28_nogoogle(AVD) - 9

BUILD SUCCESSFUL in 7s
100 actionable tasks: 2 executed, 98 up-to-date
```


## Architecture overview

The project is divided into a `lib` folder that encapsulates all important cryptographic logic for CoverDrop. It indicates how a later library _could_ look like and how it integrates with newsreader applications. Special attention is drawn to the interaction with the `SecureElement`.

The `newsreader` application is a simple newsreader mock that displays articles from the `WebApi` endpoint. At the moment, the `CoverDrop` activities are not implemented. Instead the app opens two fragments that are hard coded for a given user and reporter id.
