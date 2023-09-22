package io.monkeypatch.mobk.core


internal fun createAutorun(
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
    return ReactionDisposerImpl(rxn)
}