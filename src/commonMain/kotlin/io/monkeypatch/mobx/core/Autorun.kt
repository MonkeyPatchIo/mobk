package io.monkeypatch.mobx.core

data class ReactionDisposer(private val reaction: Reaction) {
    operator fun invoke() {
        reaction.dispose()
    }
}

fun createAutorun(
    context: ReactiveContext = ReactiveContext.main,
    name: String = context.nameFor("Autorun"),
    onError: ReactionErrorHandler? = null,
    trackingFn: (Reaction) -> Unit
): ReactionDisposer {
    val rxn = ReactionImpl(
        context, name, onError
    ) {
        track { trackingFn(this) }
    }
    rxn.schedule()
    return ReactionDisposer(rxn)
}