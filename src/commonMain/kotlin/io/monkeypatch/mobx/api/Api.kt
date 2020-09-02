package io.monkeypatch.mobx.api

import io.monkeypatch.mobx.core.*
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty


public fun autorun(body: () -> Unit): ReactionDisposer =
    createAutorun { body() }

fun action(body: () -> Unit) {
    Action(body).runAction()
}

fun <T> observable(initialValue: T) = ObservableDelegate(initialValue)

fun <T> computed(body: () -> T) = ComputedDelegate(body)

class ObservableDelegate<T>(value: T) : ReadWriteProperty<Any?, T> {

    private val observable = Observable(value)

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return observable.value
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        observable.value = value
    }
}

class ComputedDelegate<T>(body: () -> T) : ReadOnlyProperty<Any?, T> {

    private val computed = Computed(fn = body)

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return computed.value
    }
}