package io.monkeypatch.mobk.core

public enum class DerivationState {
    NOT_TRACKING,
    UP_TO_DATE,
    POSSIBLY_STALE,
    STALE
}

internal interface Derivation {
    val name: String
    var observables: Set<Atom>
    var newObservables: MutableSet<Atom>

    var dependenciesState: DerivationState

    var errorValue: MobXException.MobXCaughtException?

    fun onBecomeStale()
    fun suspend()
}