package io.monkeypatch.mobk.core

/// An Exception class to capture MobX specific exceptions
public sealed class MobXException(message: String): Exception(message) {
    /// This exception would be fired when an reaction has a cycle and does
    /// not stabilize in [ReactiveConfig.maxIterations] iterations
    public class MobXCyclicReactionException(message: String): MobXException(message)

    /// This captures the stack trace when user-land code throws an exception
    public class MobXCaughtException(public val exception: Throwable): MobXException("MobXCaughtException: $exception")

    public class MobXTimeoutException(message: String): MobXException(message)
}