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

Add mobk-core as an api dependency to your commonMain dependencies :

``` gradle

kotlin {
    ...
    sourceSets {
        commonMain {
            dependencies {
                ...
                api "io.monkeypatch:mobk-core:$mobk_version"
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
                export("io.monkeypatch:mobk-core-iosx64:$mobk_version")
            }
        }
    }
```

If you are using the multiplatform cocoapods plugin, which will create a
framework for you to consume in your Xcode project, your configuration should
look like this:

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

You need add the following file [Observer.kt](./mobk-swift/Observer.kt) to your
Xcode project. Be sure to replace the import line with the name of your Kotlin
framework.

After that, you can use the Observer view anywhere in the hierarchy. The
observable values used in the Observer block will be tracked, and the view will
be automatically rebuilded whenever one of those values changes.

``` swift
struct ContentView: View {
    let counterStore: CounterStore
    
    var body: some View {
        Observer {
            VStack {
                Text(verbatim: self.counterStore.stateView)
                
                HStack {
                    Button(action: {
                        self.counterStore.increment()
                    }) {
                        Text("Increment")
                    }
                    Button(action: {
                        self.counterStore.decrement()
                    }) {
                        Text("Decrement")
                    }.disabled(!self.counterStore.decrementAvailable)
                }
            }
        }
    }
}
```


### Jetpack Compose ###

Add the `mobk-compose` depenency to your Android application.
Due to limitation of the Compose, the API is slightly different from SwiftUI

Inside your composables, you use Observer and pass a lambda that:
  * Get the value of all the observables/computed you wish to observe
  * Return a Render block containing the composable you want to display, based on the current values.

``` kotlin
Observer {
    // All observations should happen here, before Render
    val stateView = counterStore.stateView
    val decrementAvailable = counterStore.decrementAvailable

    Render  {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = stateView)

            Button(onClick = { counterStore.increment() }) {
                Text(text = "Increment")
            }
            Button(onClick = { counterStore.decrement() }, enabled = decrementAvailable) {
                Text(text = "Decrement")
            }
        }
    }
}
```
