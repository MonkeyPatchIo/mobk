package io.monkeypatch.mobk.core

 class ReactionDisposers {
    private val disposers = mutableListOf<ReactionDisposer>()

     fun add(disposer: ReactionDisposer) {
        disposers.add(disposer)
    }

     fun clear() {
        disposers.forEach { it() }
        disposers.clear()
    }
}