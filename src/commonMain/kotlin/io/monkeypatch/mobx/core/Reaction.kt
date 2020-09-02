package io.monkeypatch.mobx.core

typealias ReactionErrorHandler = (error: Throwable, reaction: Reaction) -> Unit

interface Reaction : Derivation {
    val isDisposed: Boolean

    fun dispose()
    fun runReaction()
}

class ReactionImpl(
    private val context: ReactiveContext,
    override val name: String,
    private val onError: ReactionErrorHandler?,
    val onInvalidate: ReactionImpl.() -> Unit
) : Reaction {
    override var isDisposed: Boolean = false
    private var isScheduled = false
    private var isRunning = false

    override var newObservables: MutableSet<Atom> = mutableSetOf()

    override var observables: Set<Atom> = emptySet()

    override var dependenciesState: DerivationState = DerivationState.NOT_TRACKING

    override var errorValue: MobXException.MobXCaughtException? = null

    override fun onBecomeStale() = schedule()

    fun startTracking(): Derivation? {
        context.startBatch()
        isRunning = true
        return context.startTracking(this)
    }

    fun endTracking(previous: Derivation) {
        context.endTracking(this, previous)
        isRunning = false
        if (isDisposed) {
            context.clearObservables(this)
        }
        context.endBatch()
    }

    fun track(fn: () -> Unit) {
        context.startBatch()

        // TODO("spy ?")

        isRunning = true
        context.trackDerivation(this, fn)
        isRunning = false

        if (isDisposed) {
            context.clearObservables(this)
        }

        if (context.hasCaughtException(this)) {
            reportException(errorValue!!)
        }

        // TODO("spy ?")

        context.endBatch()
    }

    override fun runReaction() {
        if (isDisposed) return

        context.startBatch()

        isScheduled = false

        if (context.shouldCompute(this)) {
            try {
                onInvalidate()
            } catch (e: Throwable) {
                errorValue = MobXException.MobXCaughtException(e)
                reportException(errorValue!!)
            }
        }

        context.endBatch()
    }

    override fun dispose() {
        if (isDisposed) return

        isDisposed = true

        if (isRunning) return

        // TODO("spy report ?")

        context.run {
            startBatch()
            clearObservables(this@ReactionImpl)
            endBatch()
        }
    }

    override fun suspend() {
        // TODO("Suspendable support")
    }

    fun schedule() {
        if (isScheduled) return

        isScheduled = true
        context.run {
            addPendingReactions(this@ReactionImpl)
            runReactions()
        }
    }

    private fun reportException(exception: Throwable) {
        if (onError != null) {
            onError.invoke(exception, this)
            return
        }

        if (context.config.disableErrorBoundaries) {
            throw exception
        }

        // TODO("spy")

        context.notifyReactionErrorHandlers(exception, this)
    }


}