package io.monkeypatch.mobk.core

public interface ReactionDisposer {
    public operator fun invoke()
}

internal data class ReactionDisposerImpl(private val reaction: Reaction): ReactionDisposer {
    override operator fun invoke() {
        reaction.dispose()
    }
}

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