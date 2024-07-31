package net.ttiimm.morsecode.data

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import net.ttiimm.morsecode.ui.Signal
import org.junit.Test

class MorseCodeStateMachineTest {
    @Test
    fun initialState() {
        val stateMachine = MorseCodeStateMachine()
        assertEquals(State("idle"), stateMachine.current)
    }

    @Test
    fun idleToMaybeReceiving() {
        val stateMachine = MorseCodeStateMachine(State("idle"), isProd = false)
        stateMachine.onSignal(Signal(1))
        assertEquals(State("maybe"), stateMachine.current)
    }

    @Test
    fun maybeReceivingToIdleNotLongEnough() {
        val now = System.currentTimeMillis()
        val stateMachine = MorseCodeStateMachine(State("maybe"), isProd = false)
        stateMachine.onSignal(Signal(1, ts = now))
        assertEquals(State("maybe"), stateMachine.current)
        stateMachine.onSignal(Signal(0, ts = now + 1_000))
        assertEquals(State("maybe"), stateMachine.current)
        stateMachine.onSignal(Signal(0, ts = now + 5_000))
        assertEquals(State("maybe"), stateMachine.current)
    }

    @Test
    fun maybeReceivingToIdle() {
        val now = System.currentTimeMillis()
        val stateMachine = MorseCodeStateMachine(State("maybe"), isProd = false)
        stateMachine.onSignal(Signal(1, ts = now))
        assertEquals(State("maybe"), stateMachine.current)

        stateMachine.onSignal(Signal(0, ts = now + 1_000))
        stateMachine.onSignal(Signal(0, ts = now + 6_000))
        assertEquals(State("idle"), stateMachine.current)
    }

    @Test
    fun maybeReceivingToReceiving() {
        val now = System.currentTimeMillis()
        var result = false
        val stateMachine = MorseCodeStateMachine(
            State("maybe"),
            entrances = mapOf(State("receiving") to { result = true }),
            isProd = false
        )
        stateMachine.onSignal(Signal(1, ts = now))
        assertEquals(State("maybe"), stateMachine.current)
        stateMachine.onSignal(Signal(1, ts = now + 300))
        assertEquals(State("receiving"), stateMachine.current)
        assertTrue(result)
    }

    @Test
    fun receivingToMaybeReceiving() {
        val now = System.currentTimeMillis()
        var result = false
        val stateMachine = MorseCodeStateMachine(
            State("receiving"),
            exits = mapOf(State("receiving") to { result = true }),
            isProd = false
        )
        stateMachine.onSignal(Signal(0, ts = now))
        assertEquals(State("receiving"), stateMachine.current)
        stateMachine.onSignal(Signal(0, ts = now + 4_000))
        assertEquals(State("maybe"), stateMachine.current)
        assertTrue(result)
    }

    @Test
    fun receivingToSymbol() {
        val now = System.currentTimeMillis()
        val stateMachine = MorseCodeStateMachine(State("receiving"), isProd = false)
        stateMachine.onSignal(Signal(1, ts = now))
        assertEquals(State("receiving"), stateMachine.current)
        stateMachine.onSignal(Signal(1, ts = now + 500))
        assertEquals(State("symbol"), stateMachine.current)
    }

    @Test
    fun receivingToDot() {
        val now = System.currentTimeMillis()
        val stateMachine = MorseCodeStateMachine(State("receiving"), isProd = false)
        stateMachine.onSignal(Signal(1, ts = now))
        assertEquals(State("receiving"), stateMachine.current)
        stateMachine.onSignal(Signal(1, ts = now + 500))
        assertEquals(State("symbol"), stateMachine.current)
        stateMachine.onSignal(Signal(1, ts = now + 1_000))
        assertEquals(State("dot"), stateMachine.current)
    }

    @Test
    fun receivingToDash() {
        val now = System.currentTimeMillis()
        val stateMachine = MorseCodeStateMachine(State("receiving"), isProd = false)
        stateMachine.onSignal(Signal(1, ts = now))
        assertEquals(State("receiving"), stateMachine.current)
        stateMachine.onSignal(Signal(1, ts = now + 500))
        assertEquals(State("symbol"), stateMachine.current)
        stateMachine.onSignal(Signal(1, ts = now + 1_500))
        assertEquals(State("dash"), stateMachine.current)
    }

    @Test
    fun dotToPause() {
        val now = System.currentTimeMillis()
        val stateMachine = MorseCodeStateMachine(State("dot"), isProd = false)
        stateMachine.onSignal(Signal(0, now))
        assertEquals(State("pause"), stateMachine.current)
    }

    @Test
    fun dashToPause() {
        val now = System.currentTimeMillis()
        val stateMachine = MorseCodeStateMachine(State("dash"), isProd = false)
        stateMachine.onSignal(Signal(0, now))
        assertEquals(State("pause"), stateMachine.current)
    }

    @Test
    fun pauseToSymbolPause() {
        val now = System.currentTimeMillis()
        val stateMachine = MorseCodeStateMachine(State("pause"), isProd = false)
        stateMachine.onSignal(Signal(0, now))
        assertEquals(State("pause"), stateMachine.current)
        stateMachine.onSignal(Signal(0, ts = now + 500))
        assertEquals(State("pause-symbol"), stateMachine.current)
        stateMachine.onSignal(Signal(0, ts = now + 1499))
        assertEquals(State("receiving"), stateMachine.current)
    }

    @Test
    fun pauseToLetterPause() {
        val now = System.currentTimeMillis()
        val stateMachine = MorseCodeStateMachine(State("pause"), isProd = false)
        stateMachine.onSignal(Signal(0, now))
        assertEquals(State("pause"), stateMachine.current)
        stateMachine.onSignal(Signal(0, ts = now + 1500))
        assertEquals(State("pause-letter"), stateMachine.current)
        stateMachine.onSignal(Signal(0, ts = now + 3499))
        assertEquals(State("receiving"), stateMachine.current)
    }

    @Test
    fun pauseToWordPause() {
        val now = System.currentTimeMillis()
        val stateMachine = MorseCodeStateMachine(State("pause"), isProd = false)
        stateMachine.onSignal(Signal(0, now))
        assertEquals(State("pause"), stateMachine.current)
        stateMachine.onSignal(Signal(0, ts = now + 3500))
        assertEquals(State("pause-word"), stateMachine.current)
        stateMachine.onSignal(Signal(0, ts = now + 4000))
        assertEquals(State("receiving"), stateMachine.current)
    }

}
