package io.monkeypatch.mobk.core

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals


class TestReactiveContext {

    @BeforeTest
    fun setupContext() {
        ReactiveContext.main.config = ReactiveContext.main.config.copy(enforceWriteOnMainThread = false)
    }

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

    @Test
    fun testComputedOptimization() {
        val x = Observable(10)
        val isPair = Computed { x.value % 2 == 0 }

        val xHistory = mutableListOf<Int>()
        val isPairHistory = mutableListOf<Boolean>()

        createAutorun {
            xHistory.add(x.value)
        }

        createAutorun {
            isPairHistory.add(isPair.value)
        }

        x.value = 11
        x.value = 12
        x.value = 20

        assertEquals(listOf(10, 11, 12, 20), xHistory)
        assertEquals(listOf(true, false, true), isPairHistory)
    }

    @Test
    fun testReaction() {
        val x = Observable(10)
        val isPair = Computed { x.value % 2 == 0 }

        val xHistory = mutableListOf<Int>()
        var pairCounter = 1

        createAutorun {
            xHistory.add(x.value)
        }

        createReaction (
            trackingFn = {
                isPair.value
            }
        ) { value ->
            value?.let {
                pairCounter++
            }
        }

        x.value = 11
        x.value = 12
        x.value = 20

        assertEquals(listOf(10, 11, 12, 20), xHistory)
        assertEquals(3, pairCounter)
    }

    @Test
    fun testWhenReaction() {
        val x = Observable(10)

        val xHistory = mutableListOf<Int>()
        var isTwelve = 0

        createAutorun {
            xHistory.add(x.value)
        }

        createWhenReaction (
            predicate = {
                x.value == 12
            }
        ) {
                isTwelve++
        }

        x.value = 11
        x.value = 12
        x.value = 20
        x.value= 12
        x.value = 24

        assertEquals(listOf(10, 11, 12, 20,12,24), xHistory)
        assertEquals(1, isTwelve)
    }
}