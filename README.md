# MobK: MobX for Kotlin Mobile Multiplatform #

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

Add mobk as an api dependency to your commonMain dependencies :

``` gradle

kotlin {
    ...
    sourceSets {
        commonMain {
            dependencies {
                ...
                api "io.monkeypatch:mobk:0.0.3"
            }
        }
```

For the iOS part, you need to export mobx API as a KMP framework. This is needed
so that you can consume Observable values from SwiftUI views.

``` gradle
kotlin {
    android()
    ios {
        binaries {
            framework {
                baseName = "..."
                export("io.monkeypatch:mobx-iosx64:0.0.1")
            }
        }
    }
```

If you are using the multiplatform cocoapods plugin, which will create a
framework for you, your configuration should look like this:

``` gradle
kotlin {
    ...
    ios {
        def iosArch = System.getenv('SDK_NAME')?.startsWith("iphoneos") ? "iosarm64" : "iosx64"
        binaries.forEach {
            if (it instanceof org.jetbrains.kotlin.gradle.plugin.mpp.Framework) {
                it.export("io.monkeypatch:mobk-$iosArch:$mobk_version")
            }
        }
    }
```

### SwiftUI ###

TODO

### Jetpack Compose ###

TODO
