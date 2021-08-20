package io.monkeypatch.mobk.api

import io.monkeypatch.mobk.core.ReactiveContext
import kotlin.test.BeforeTest
import kotlin.test.Test


class ApiTest {
    @BeforeTest
    fun setupContext() {
        ReactiveContext.main.config = ReactiveContext.main.config.copy(enforceWriteOnMainThread = false)
    }

    @Test
    fun simpleTest() {
        var x by observable(0)
        var y by observable(0)

        autorun {
            println("x = $x, y = $y")
        }

        x = x + 10

        action {
            x = 42
            y = y + 10
        }
    }

    @Test
    fun simpleComutedTest() {
        var x by observable(0)
        var y by observable(0)
        val total by computed { x + y }

        val disposer = autorun {
            println("x = $x, y = $y, total = $total")
        }

        x = x + 10

        action {
            x = 42
            y = y + 10
        }

        disposer()

        action {
            x = -1
            y = -1
        }
    }
}