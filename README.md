# MobX for Kotlin Mobile Multiplatform #

This is a port of the core Mobx API for Kotlin Multiplatform. Mobx is a
simple and successful JS library for state management.

This library bring the benefits of transparent reactive programming to your
mobile, while leveraging modern UI toolkit such as SwiftUI and Jetpack Compose.

## Getting started ##

Add the following repository to your multiplatform project `build.gradle`.

``` gradle
repositories {
    maven {
        url "TODO URL""
    }
}
```

Add mobx as an api dependency to your commonMain dependencies :

``` gradle

kotlin {
    ...
    sourceSets {
        commonMain {
            dependencies {
                ...
                api "io.monkeypatch:mobx:$mobx_version"
            }
        }
```

For the iOS part, you need to export mobx API as a KMP framework. This is needed
so that you can consume Observable values from SwiftUI views.

``` gradle
TODO
```

If you are using the multiplatform cocoapods plugin, which will create a
framework for you, your configuration should look like this:

### SwiftUI ###

TODO

### Jetpack Compose ###

TODO
