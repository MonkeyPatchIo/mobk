package io.monkeypatch.mobk.utils

import android.os.Looper

internal actual fun isMainThread(): Boolean =
    Looper.myLooper() == Looper.getMainLooper()