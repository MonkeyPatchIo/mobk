package io.monkeypatch.mobk.ui

import androidx.compose.runtime.*
import io.monkeypatch.mobk.api.autorun

data class Render(val composable: (@Composable () -> Unit))

@Composable
fun Observer(observe: () -> Render) {
    val (state, setState) = remember { mutableStateOf<Render?>(null) }

    onCommit(observe) {
        val disposer = autorun {
            val render = observe()
            setState(render)
        }

        onDispose {
            disposer.invoke()
        }
    }

    state?.composable?.invoke()
}