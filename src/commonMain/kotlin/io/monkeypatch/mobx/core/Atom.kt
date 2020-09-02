package io.monkeypatch.mobx.core


enum class ListenerKind {
    ON_BECOME_OBSERVED, ON_BECOME_UNOBSERVED
}

open class Atom(
    protected val context: ReactiveContext = ReactiveContext.main,
    val name: String = context.nameFor("Atom"),
    onObserved: (() -> Unit)? = null,
    onUnobserved: (() -> Unit)? = null
) {
    internal var isPendingUnobservation = false
    internal var isBeingObserved = false
    internal var lowestObserverState = DerivationState.NOT_TRACKING
    internal val observers = mutableSetOf<Derivation>()

    private val observationListeners = mutableMapOf<ListenerKind, MutableSet<() -> Unit>>()

    val hasObservers get() = observers.isNotEmpty()

    init {
        if (onObserved != null) {
            onBecomeObserved(onObserved)
        }

        if (onUnobserved != null) {
            onBecomeUnobserved(onUnobserved)
        }
    }

    fun reportObserved() {
        context.reportObserved(this)
    }

    fun reportChanged() {
        context.apply {
            startBatch()
            propagateChanged(this@Atom)
            endBatch()
        }
    }

    internal fun addObserver(d: Derivation) {
        observers.add(d)
        if (lowestObserverState.ordinal > d.dependenciesState.ordinal) {
            lowestObserverState = d.dependenciesState
        }
    }

    internal fun removeObserver(d: Derivation) {
        observers.remove(d)
        if (observers.isEmpty()) {
            context.enqueueForUnobservation(this)
        }
    }

    internal fun notifyOnBecomedObserved() {
        val listeners = observationListeners[ListenerKind.ON_BECOME_OBSERVED]
        listeners?.forEach { it() }
    }

    internal fun notifyOnBecomedUnobserved() {
        val listeners = observationListeners[ListenerKind.ON_BECOME_UNOBSERVED]
        listeners?.forEach { it() }
    }

    private fun onBecomeObserved(onObserved: () -> Unit) {
        addListener(ListenerKind.ON_BECOME_OBSERVED, onObserved)
    }

    private fun onBecomeUnobserved(onUnobserved: () -> Unit) {
        addListener(ListenerKind.ON_BECOME_UNOBSERVED, onUnobserved)
    }

    private fun addListener(kind: ListenerKind, fn: () -> Unit): () -> Unit {
        observationListeners.getOrPut(kind) { mutableSetOf() }
            .add(fn)

        return {
            val listeners = observationListeners[kind]
            if (listeners != null) {
                listeners.remove(fn)
                if (listeners.isEmpty()) {
                    observationListeners.remove(kind)
                }
            }
        }
    }
}