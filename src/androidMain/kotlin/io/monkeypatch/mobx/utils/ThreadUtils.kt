package io.monkeypatch.mobx.utils

import android.os.Looper

actual fun isMainThread(): Boolean =
    Looper.myLooper() == Looper.getMainLooper()