package io.monkeypatch.mobk.core

public class Computed<T>(
    context: ReactiveContext = ReactiveContext.main,
    name: String = context.nameFor("Computed"),
    private val fn: () -> T
) : Atom(
    context, name
), Derivation, ObservableValue<T> {
    override var observables: Set<Atom> = emptySet()
    override var newObservables: MutableSet<Atom> = mutableSetOf()
    override var dependenciesState: DerivationState = DerivationState.NOT_TRACKING
    override var errorValue: MobXException.MobXCaughtException? = null

    private var _value: T? = null
    private var isComputing = false

    override val value: T get() {
        if (isComputing) {
            throw MobXException.MobXCyclicReactionException(
                "Cycle detected in computation $name"
            )
        }

        if (!context.isWithinBatch && observers.isEmpty()) {
            if (context.shouldCompute(this)) {
                context.startBatch()
                _value = computeValue(false)
                context.endBatch()
            }
        } else {
            reportObserved()
            if (context.shouldCompute(this)) {
                if (trackAndCompute()) {
                    context.propagateChangeConfirmed(this)
                }
            }
        }

        if (context.hasCaughtException(this)) {
            throw errorValue!!
        }

        @Suppress("UNCHECKED_CAST")
        return _value as T
    }

    private fun computeValue(track: Boolean): T? {
        isComputing = true
        context.pushComputation()

        var value: T? = null
        if (track) {
            value = context.trackDerivation(this, fn)
        } else {
            if (context.config.disableErrorBoundaries) {
                value = fn()
            } else {
                try {
                    value = fn()
                    errorValue = null
                } catch (t: Throwable) {
                    errorValue = MobXException.MobXCaughtException(t)
                }
            }
        }

        context.popComputation()
        isComputing = false

        return value
    }

    override fun suspend() {
        context.clearObservables(this)
        _value = null
    }

    override fun onBecomeStale() {
        context.propagatePossibilyChanged(this)
    }

    private fun trackAndCompute(): Boolean {
        //TODO spy

        val oldValue = _value
        val wasSuspended = dependenciesState == DerivationState.NOT_TRACKING
        val newValue = computeValue(true)

        val changed = wasSuspended || context.hasCaughtException(this) || oldValue != newValue

        if (changed) {
            _value = newValue
        }
        return changed
    }
}