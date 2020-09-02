package io.monkeypatch.mobx.core

import kotlin.test.Test
import kotlin.test.assertEquals


class TestReactiveContext {

    @Test
    fun testObservables() {
        val x = Observable(10)
        val y = Observable(20)
        val total = Observable(0)

        val values = mutableListOf<Triple<Int, Int, Int>>()

        createAutorun(name="adder") { values.add(Triple(x.value, y.value, total.value)) }

        assertEquals(
            listOf(Triple(10, 20, 0)),
            values
        )

        val action = Action({
            x.value += 42
            y.value += 30
            total.value = x.value + y.value
        })
        action.runAction()

        assertEquals(
            listOf(
                Triple(10, 20, 0),
                Triple(52, 50, 102)
            ),
            values
        )
    }

    @Test
    fun testComputed() {
        val x = Observable(10)
        val y = Observable(20)
        val total = Computed { x.value + y.value }

        val values = mutableListOf<Triple<Int, Int, Int>>()

        createAutorun(name="adder") { values.add(Triple(x.value, y.value, total.value!!)) }

        assertEquals(
            listOf(Triple(10, 20, 30)),
            values
        )

        val action = Action({
            x.value += 42
            y.value += 30
        })
        action.runAction()

        assertEquals(
            listOf(
                Triple(10, 20, 30),
                Triple(52, 50, 102)
            ),
            values
        )
    }
}