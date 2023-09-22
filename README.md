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

You need add the following file [Observer.swift](./mobk-swift/Observer.swift) to your
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

### MobK ViewModel ###

#### Dependency ####
Add the mobk-viewmodel dependency to your commonMain dependencies :

``` gradle
kotlin {
    ...
    sourceSets {
        commonMain {
            dependencies {
                ...
                api "io.monkeypatch:mobk-viewmodel:$mobk_version"
            }
        }
```   

#### Usage ####

The ViewModel class is a simple class that can be used to store state and logic for your application.
It is designed to be used with SwiftUI and Jetpack Compose, but can be used with any UI toolkit.

It provides reactions, that respect lifecycle of the ViewModel, and can be used to perform side effects.

``` kotlin
class CounterViewModel: MobKViewModel {
    var counter by observable(0)

    val counterWatch = reaction(
        delay = 5.seconds,
        trackingFn = { counter }) { counter ->
        if (counter != null && counter > 10) {
            message = "Counter is too high"
        }
    }

    val warningWatch = whenReaction(predicate = { counter > 12 }) {
        message = "Counter is really too high"
    }

    fun increment() {
        counter++
    }
    
    @override
    fun onCleared() {
       super.onCleared()
         // Do some cleanup here
    }
}
```

#### SwiftUI ####

You need add the following file [MobKViewModel.swift](./mobk-swift/MobKViewModel.swift) to your Xcode project. Be sure to replace the import line with the name of your Kotlin framework.

After that, you can use the @VM property wrapper to manage the lifecycle of your ViewModel. When component is destroyed, the ViewModel will be cleared. 
Usually, you may also use the @StateObject property wrapper to manage the persistence of your ViewModel.

``` swift
struct ContentView: View {
   @StateObject @VM var counterViewModel: CounterViewModel = CounterViewModel()
    
    var body: some View {
        VStack {
            Text(verbatim: self.counterViewModel.counter)
            
            HStack {
                Button(action: {
                    self.counterViewModel.increment()
                }) {
                    Text("Increment")
                }
            }
        }
    }
}
```

If your view model has a dependency on a parameter, you have to use the following syntax:

``` swift
struct ContentView: View {
   @StateObject @VM var counterViewModel: CounterViewModel
    
    init(counter: Int) {
        _counterViewModel = asStateObject(CounterViewModel(counter: counter))
    }
    
    var body: some View {
        VStack {
            Text(verbatim: self.counterViewModel.counter)
            
            HStack {
                Button(action: {
                    self.counterViewModel.increment()
                }) {
                    Text("Increment")
                }
            }
        }
    }
}
```