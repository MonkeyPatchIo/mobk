package io.monkeypatch.mobk.api

import io.monkeypatch.mobk.core.*
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlin.time.Duration


public fun autorun(body: () -> Unit): ReactionDisposer =
    createAutorun { body() }

public fun action(body: () -> Unit) {
    Action(body).runAction()
}

public fun <T> reaction(context: ReactiveContext = ReactiveContext.main,
                        delay: Duration? = null,
                        equals: ((T?, T?) -> Boolean)? = null,
                        onError: ReactionErrorHandler? = null,trackingFn: (Reaction) -> T, effect: (T?) -> Unit): ReactionDisposer =
    createReaction(
        context = context,
        delay = delay,
        equals = equals,
        onError = onError,
        trackingFn = trackingFn, effect = effect)

public fun whenReaction(context: ReactiveContext = ReactiveContext.main,
                            timeout: Duration? = null,
                            onError: ReactionErrorHandler? = null,
                            predicate: (Reaction) -> Boolean,
                            effect: () -> Unit) = createWhenReaction(context = context, timeout = timeout, onError = onError, predicate = predicate, effect = effect)

public fun <T> observable(initialValue: T): ReadWriteProperty<Any?, T> = ObservableDelegate(initialValue)

public fun <T> computed(body: () -> T): ReadOnlyProperty<Any?, T> = ComputedDelegate(body)

internal class ObservableDelegate<T>(value: T) : ReadWriteProperty<Any?, T> {

    private val observable = Observable(value)

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return observable.value
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        observable.value = value
    }
}

internal class ComputedDelegate<T>(body: () -> T) : ReadOnlyProperty<Any?, T> {

    private val computed = Computed(fn = body)

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return computed.value
    }
}