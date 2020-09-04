package io.monkeypatch.mobk.core

import io.monkeypatch.mobk.utils.isMainThread
import kotlin.native.concurrent.ThreadLocal
import kotlin.properties.Delegates


/// STATE

private class ReactiveState {
    /// Current batch depth. This is used to track the depth of `transaction` / `action`.
    /// When the batch ends, we execute all the [pendingReactions]
    var batch: Int = 0

    // Monotonically increasing counter for assigning a name to an action/reaction/atom
    var nextIdCounter: Int = 0 // TODO thread safety ???

    /// Tracks the currently executing derivation (reactions or computeds).
    /// The Observables used here are linked to this derivation.
    var trackingDerivation: Derivation? = null

    /// The reactions that must be triggered at the end of a `transaction` or an `action`
    var pendingReactions: MutableList<Reaction> = mutableListOf()

    /// Are we in middle of executing the [pendingReactions].
    var isRunningReactions = false

    /// The atoms that must be disconnected from their observed reactions. This happens
    /// if a reaction has been disposed during a batch
    var pendingUnobservations: MutableList<Atom> = mutableListOf()

    /// Tracks if within a computed property evaluation
    var computationDepth = 0

    /// Track if observables can be mutated
    var allowStateChanges = true

    /// Are we inside an action or transaction?
    val isWithinBatch
        get() = batch > 0

    /// Are we inside a reaction or computed?
    val isWithinDerivation
        get() = trackingDerivation != null || computationDepth > 0
}

/// CONFIG

public enum class ReactiveWritePolicy {
    OBSERVED, ALWAYS, NEVER
}

public enum class ReactiveReadPolicy {
    ALWAYS, NEVER
}

public data class ReactiveConfig(
    val disableErrorBoundaries: Boolean,
    val writePolicy: ReactiveWritePolicy,
    val readPolicy: ReactiveReadPolicy,
    val maxIterations: Int = 100,
    val enforceWriteOnMainThread: Boolean = true
) {
    internal val reactionErrorHandlers: MutableSet<ReactionErrorHandler> = mutableSetOf()

    public companion object {
        public val main: ReactiveConfig = ReactiveConfig(
            disableErrorBoundaries = false,
            writePolicy = ReactiveWritePolicy.NEVER,
            readPolicy = ReactiveReadPolicy.NEVER
        )
    }
}

/// CONTEXT

public class ReactiveContext(config: ReactiveConfig = ReactiveConfig.main) {
    private var state = ReactiveState()

    public var config: ReactiveConfig by Delegates.observable(config) { _, _, newConfig ->
        state.allowStateChanges = newConfig.writePolicy == ReactiveWritePolicy.NEVER
    }

    internal fun nameFor(prefix: String): String {
        require(prefix.isNotEmpty()) { "Prefix should not be empty " }
        val nextId = ++state.nextIdCounter
        return "$prefix@$nextId"
    }

    internal fun startBatch() {
        state.batch += 1
    }

    internal fun endBatch() {
        state.batch -= 1
        if (state.batch == 0) {
            runReactions()

            for (i in 0 until state.pendingUnobservations.size) {
                val ob = state.pendingUnobservations[i]
                ob.isPendingUnobservation = false

                if (!ob.hasObservers) {
                    if (ob.isBeingObserved) {
                        ob.apply {
                            isBeingObserved = false
                            notifyOnBecomedUnobserved()
                        }
                    }

                    if (ob is Computed<*>) {
                        ob.suspend()
                    }
                }
            }

            state.pendingUnobservations = mutableListOf()
        }
    }

    internal val isWithinBatch get() = state.isWithinBatch

    internal fun enforceReadPolicy(atom: Atom) {
        require(
            (run {
                when (config.readPolicy) {
                    ReactiveReadPolicy.ALWAYS -> require(state.isWithinBatch || state.isWithinDerivation) {
                        "'Observable values cannot be read outside Actions and Reactions. Make sure to wrap them inside an action or a reaction. Tried to read: ${atom.name}"
                    }
                    ReactiveReadPolicy.NEVER -> Unit
                }
                true
            })
        )
    }

    // TODO
    internal fun enforceWritePolicy(atom: Atom) {
        require(!config.enforceWriteOnMainThread || isMainThread()) {
            "Observable values cannot be written outside main thread"
        }

        if (state.computationDepth > 0 && atom.hasObservers) {
            throw IllegalStateException("Computed values are not allowed to cause side effects by changing observables that are already being observed. Tried to modify: ${atom.name}")
        }

        when (config.writePolicy) {
            ReactiveWritePolicy.NEVER -> { }
            ReactiveWritePolicy.OBSERVED -> {
                if (atom.hasObservers) {
                    require(state.isWithinBatch) {
                        "Changing observable values outside actions is not allowed. Please wrap the code in an \"action\" if this change is intended. Tried to modify ${atom.name}"
                    }
                }
            }
            ReactiveWritePolicy.ALWAYS -> {
                require(state.isWithinBatch) {
                    "Changing observable values outside actions is not allowed. Please wrap the code in an \"action\" if this change is intended. Tried to modify ${atom.name}"
                }
            }
        }
    }

    internal fun startTracking(derivation: Derivation): Derivation? {
        val prevDerivation = state.trackingDerivation
        state.trackingDerivation = derivation
        resetDerivationState(derivation)
        derivation.newObservables = mutableSetOf()
        return prevDerivation
    }

    internal fun endTracking(currentDerivation: Derivation, prevDerivation: Derivation?) {
        state.trackingDerivation = prevDerivation
        bindDependencies(currentDerivation)
    }

    internal fun <T> trackDerivation(d: Derivation, fn: () -> T): T? {
        val prevDerivation = startTracking(d)

        val result = if (config.disableErrorBoundaries) {
            fn()
        } else {
            try {
                fn().also {
                    d.errorValue = null
                }
            } catch (t: Throwable) {
                d.errorValue = MobXException.MobXCaughtException(t)
                null
            }
        }

        endTracking(d, prevDerivation)
        return result
    }

    internal fun reportObserved(atom: Atom) {
        val derivation = state.trackingDerivation

        if (derivation != null) {
            derivation.newObservables.add(atom)
            if (!atom.isBeingObserved) {
                atom.apply {
                    isBeingObserved = true
                    notifyOnBecomedObserved()
                }
            }
        }
    }

    private fun bindDependencies(derivation: Derivation) {
        val staleObservables = derivation.observables - derivation.newObservables
        val newObservables = derivation.newObservables - derivation.observables
        var lowestNewDerivationState = DerivationState.UP_TO_DATE

        // Add newly found observables
        newObservables.forEach { observable ->
            observable.addObserver(derivation)

            // Computed = Observable + Derivation
            if (observable is Computed<*>) {
                if (observable.dependenciesState.ordinal >
                    lowestNewDerivationState.ordinal
                ) {
                    lowestNewDerivationState = observable.dependenciesState
                }
            }
        }

        // Remove previous observables
        staleObservables.forEach { ob ->
            ob.removeObserver(derivation)
        }

        if (lowestNewDerivationState != DerivationState.UP_TO_DATE) {
            derivation.apply {
                dependenciesState = lowestNewDerivationState
                onBecomeStale()
            }
        }

        derivation.apply {
            this.observables = derivation.newObservables
            this.newObservables = mutableSetOf()
        }
    }

    internal fun addPendingReactions(reaction: Reaction) {
        state.pendingReactions.add(reaction)
    }

    internal fun runReactions() {
        if (state.batch > 0 || state.isRunningReactions) {
            return
        }
        runReactionsInternal()
    }

    private fun runReactionsInternal() {
        state.isRunningReactions = true

        var iterations = 0
        val allReactions = state.pendingReactions

        // While running reactions, new reactions might be triggered.
        // Hence we work with two variables and check whether
        // we converge to no remaining reactions after a while.
        while (allReactions.isNotEmpty()) {
            iterations++
            if (iterations == config.maxIterations) {
                val failingReaction = allReactions[0]

                resetState()
                throw MobXException.MobXCyclicReactionException("Reaction doesn't converge to a stable state after ${config.maxIterations} iterations. Probably there is a cycle in the reactive function: $failingReaction")
            }

            val remainingReactions = allReactions.toList()
            allReactions.clear()
            remainingReactions.forEach { it.runReaction() }
        }



        state.apply {
            pendingReactions = mutableListOf()
            isRunningReactions = false
        }
    }


    internal fun propagateChanged(atom: Atom) {
        if (atom.lowestObserverState == DerivationState.STALE) return

        atom.lowestObserverState = DerivationState.STALE

        atom.observers.forEach { obs ->
            if (obs.dependenciesState == DerivationState.UP_TO_DATE) {
                obs.onBecomeStale()
            }
            obs.dependenciesState = DerivationState.STALE
        }
    }

    internal fun propagatePossibilyChanged(atom: Atom) {
        if (atom.lowestObserverState != DerivationState.UP_TO_DATE) return

        atom.lowestObserverState = DerivationState.POSSIBLY_STALE

        atom.observers.forEach { obs ->
            if (obs.dependenciesState == DerivationState.UP_TO_DATE) {
                obs.dependenciesState = DerivationState.POSSIBLY_STALE
                obs.onBecomeStale()
            }
            obs.dependenciesState = DerivationState.STALE
        }
    }

    internal fun propagateChangeConfirmed(atom: Atom) {
        if (atom.lowestObserverState == DerivationState.STALE) return

        atom.lowestObserverState = DerivationState.STALE

        atom.observers.forEach { obs ->
            if (obs.dependenciesState == DerivationState.POSSIBLY_STALE) {
                obs.dependenciesState = DerivationState.STALE
            } else if (obs.dependenciesState == DerivationState.UP_TO_DATE) {
                atom.lowestObserverState = DerivationState.UP_TO_DATE
            }
        }
    }

    internal fun clearObservables(derivation: Derivation) {
        val observables = derivation.observables
        derivation.observables = mutableSetOf()

        observables.forEach { it.removeObserver(derivation) }

        derivation.dependenciesState = DerivationState.NOT_TRACKING
    }

    internal fun enqueueForUnobservation(atom: Atom) {
        if (atom.isPendingUnobservation) {
            return
        }
        atom.isPendingUnobservation = true
        state.pendingUnobservations.add(atom)
    }


    private fun resetDerivationState(derivation: Derivation) {
        if (derivation.dependenciesState == DerivationState.UP_TO_DATE) return

        derivation.dependenciesState = DerivationState.UP_TO_DATE
        derivation.observables.forEach { it.lowestObserverState = DerivationState.UP_TO_DATE }
    }

    internal fun shouldCompute(derivation: Derivation): Boolean {
        return when (derivation.dependenciesState) {
            DerivationState.UP_TO_DATE -> false
            DerivationState.NOT_TRACKING -> true
            DerivationState.STALE -> true
            DerivationState.POSSIBLY_STALE -> untracked {
                derivation.observables.forEach { obs ->
                    if (obs is Computed<*>) {
                        // Force a computation
                        if (config.disableErrorBoundaries) {
                            obs.value
                        } else {
                            try {
                                obs.value
                            } catch (_: Throwable) {
                                return@untracked true
                            }
                        }

                        if (derivation.dependenciesState == DerivationState.STALE) {
                            return@untracked true
                        }
                    }
                }
                resetDerivationState(derivation)
                return@untracked false
            }
        }
    }

    internal fun hasCaughtException(derivation: Derivation) = derivation.errorValue is MobXException.MobXCaughtException

    internal fun isComputingDerivation() = state.trackingDerivation != null

    internal fun startUntracked(): Derivation? {
        val prevDerivation = state.trackingDerivation
        state.trackingDerivation = null
        return prevDerivation
    }

    internal fun endUntracked(prevDerivation: Derivation?) {
        state.trackingDerivation = prevDerivation
    }

    private fun <T> untracked(fn: () -> T): T {
        val prevDerivation = startUntracked()
        try {
            return fn()
        } finally {
            endUntracked(prevDerivation)
        }
    }

    internal fun onReactionError(handler: ReactionErrorHandler): Dispose {
        config.reactionErrorHandlers.add(handler);
        return {
            config.reactionErrorHandlers.remove(handler)
        }
    }

    internal fun notifyReactionErrorHandlers(exception: Throwable, reaction: Reaction) {
        config.reactionErrorHandlers.forEach { f ->
            f(exception, reaction)
        }
    }

    internal fun startAllowStateChanges(allow: Boolean): Boolean {
        val prevValue = state.allowStateChanges
        state.allowStateChanges = allow
        return prevValue
    }

    internal fun endAllowStateChanges(allow: Boolean) {
        state.allowStateChanges = allow
    }

    internal fun pushComputation() {
        state.computationDepth += 1
    }

    internal fun popComputation() {
        state.computationDepth -= 1
    }

    private fun resetState() {
        state = ReactiveState().apply {
            allowStateChanges = config.writePolicy == ReactiveWritePolicy.NEVER
        }
    }

    @ThreadLocal
    public companion object {
        public val main: ReactiveContext = ReactiveContext()
    }
}