package io.monkeypatch.mobk.utils
import platform.Foundation.*

actual fun isMainThread(): Boolean =
    NSThread.isMainThread()