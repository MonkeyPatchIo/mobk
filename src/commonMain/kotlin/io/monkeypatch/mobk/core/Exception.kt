package io.monkeypatch.mobk.core

/// An Exception class to capture MobX specific exceptions
sealed class MobXException(message: String): Exception(message) {
    /// This exception would be fired when an reaction has a cycle and does
    /// not stabilize in [ReactiveConfig.maxIterations] iterations
    class MobXCyclicReactionException(message: String): MobXException(message)

    /// This captures the stack trace when user-land code throws an exception
    class MobXCaughtException(val exception: Throwable): MobXException("MobXCaughtException: $exception")
}