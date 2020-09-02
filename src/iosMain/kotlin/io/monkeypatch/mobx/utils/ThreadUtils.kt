package io.monkeypatch.mobx.utils
import platform.Foundation.*

actual fun isMainThread(): Boolean =
    NSThread.isMainThread()