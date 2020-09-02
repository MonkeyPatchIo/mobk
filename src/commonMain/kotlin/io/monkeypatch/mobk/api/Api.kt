package io.monkeypatch.mobk.api

import io.monkeypatch.mobk.core.*
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty


public fun autorun(body: () -> Unit): ReactionDisposer =
    createAutorun { body() }

public fun action(body: () -> Unit) {
    Action(body).runAction()
}

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