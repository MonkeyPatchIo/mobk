package io.monkeypatch.mobk.viewmodel

import dev.icerock.moko.mvvm.viewmodel.ViewModel
import io.monkeypatch.mobk.core.Reaction
import io.monkeypatch.mobk.core.ReactionDisposers
import io.monkeypatch.mobk.core.ReactionErrorHandler
import io.monkeypatch.mobk.core.ReactiveContext
import kotlin.time.Duration


interface MobkLifecycleAware {
    fun <T> reaction(
        context: ReactiveContext = ReactiveContext.main, delay: Duration? = null,
        equals: ((T?, T?) -> Boolean)? = null,
        onError: ReactionErrorHandler? = null,
        trackingFn: (Reaction) -> T, effect: (T?) -> Unit
    )
}

abstract class MobkViewModel : ViewModel(), MobkLifecycleAware {
    private val reactionDispatcher = ReactionDisposers()

    override fun <T> reaction(
        context: ReactiveContext,
        delay: Duration?,
        equals: ((T?, T?) -> Boolean)?,
        onError: ReactionErrorHandler?,
        trackingFn: (Reaction) -> T, effect: (T?) -> Unit
    ) =
        reactionDispatcher.add(
            io.monkeypatch.mobk.api.reaction(
                context = context,
                delay = delay,
                equals = equals,
                trackingFn = trackingFn,
                effect = effect
            )
        )

    override fun onCleared() {
        super.onCleared()
        reactionDispatcher.clear()
    }
}
