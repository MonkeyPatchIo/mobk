package io.monkeypatch.mobk.core

class Action(
    private val body: () -> Unit,
    private val context: ReactiveContext = ReactiveContext.main
) {

    fun runAction() {
        val prevDerivation = context.startUntracked()
        context.startBatch()
        val prevAllowStateChanges = context.startAllowStateChanges(true)

        try {
            body()
        } finally {
            context.endUntracked(prevDerivation)
            context.endBatch()
            context.endAllowStateChanges(prevAllowStateChanges)
        }
    }
}
