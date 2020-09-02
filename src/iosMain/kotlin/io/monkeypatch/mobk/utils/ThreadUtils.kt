package io.monkeypatch.mobk.utils
import platform.Foundation.*

internal actual fun isMainThread(): Boolean =
    NSThread.isMainThread()