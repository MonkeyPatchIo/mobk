package io.monkeypatch.mobk.core

import kotlinx.coroutines.Delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration

public interface ReactionDisposer {
    public operator fun invoke()
}

internal data class ReactionDisposerImpl(private val reaction: Reaction): ReactionDisposer {
    override operator fun invoke() {
        reaction.dispose()
    }
}

internal fun <T> createReaction(
    context: ReactiveContext = ReactiveContext.main,
    name: String = context.nameFor("Reaction"),
    delay: Duration? = null,
    equals: ((T?, T?) -> Boolean)? = null,
    onError: ReactionErrorHandler? = null,
    trackingFn: (Reaction) -> T,
    effect: (T?) -> Unit
): ReactionDisposer {
    var lastValue: T? = null
    var firstTime = true
    var runSync = delay == null
    var  rxn: ReactionImpl? = null
    val effectAction = effect

    val reactionRunner: () -> Unit = {
        val reaction = rxn
        if (reaction == null || !reaction.isDisposed) {
            var changed = false

            reaction?.run {
                track {
                    val nextValue = trackingFn(reaction)
                    val isEqual = if (equals != null) equals(nextValue, lastValue) else  lastValue == nextValue

                    changed = firstTime || !isEqual
                    lastValue = nextValue
                }
            }

            val canInvokeEffect = !firstTime && changed

            if (canInvokeEffect) {
                effectAction(lastValue)
            }

            if (firstTime) {
                firstTime = false
            }
        }
    }

    var isScheduled = false
     rxn = ReactionImpl(
        context, name, onError
    ) {
        if (firstTime || runSync) {
            reactionRunner()
        } else if(!isScheduled) {
            isScheduled = true

            context.config.reactionCoroutineScope.launch {
              if (delay != null) {
                    delay(delay)
                }

                isScheduled = false

                withContext(Dispatchers.Main) {
                reactionRunner()
                }
            }
        }
    }
    rxn.schedule()
    return ReactionDisposerImpl(rxn)
}

internal fun createWhenReaction(
    context: ReactiveContext = ReactiveContext.main,
    name: String = context.nameFor("Reaction"),
    timeout: Duration? = null,
    onError: ReactionErrorHandler? = null,
    predicate: (Reaction) -> Boolean,
    effect: () -> Unit
): ReactionDisposer {
    val effectAction = effect
    var dispose: ReactionDisposer? = null
    val rxn: ReactionImpl? = null

    if (timeout != null) {
        context.config.reactionCoroutineScope.launch {
            withTimeout(timeout) {
                val d = dispose
                val r = rxn
                if (d != null && r != null) {
                    if (!r.isDisposed) {
                        d()

                        val error = MobXException.MobXTimeoutException("WHEN_TIMEOUT")
                        if (onError != null) {
                            onError(error, r)
                        } else {
                            throw error
                        }
                    }
                }
            }
        }
    }

     dispose = createAutorun(
        context = context,
        name = name,
        onError = onError
    ) { reaction ->
        if (predicate(reaction)) {
            reaction.dispose()
            effectAction()
        }
    }
    return dispose
}