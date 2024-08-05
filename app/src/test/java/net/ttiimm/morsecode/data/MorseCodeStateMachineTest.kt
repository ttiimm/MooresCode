package net.ttiimm.morsecode.data

import junit.framework.TestCase.assertEquals
import net.ttiimm.morsecode.ui.Signal
import org.junit.Test
import java.util.Optional

class MorseCodeStateMachineTest {
    @Test
    fun initialState() {
        val stateMachine = MorseCodeStateMachine()
        assertEquals(State("idle"), stateMachine.current)
    }

    @Test
    fun idleOnStart() {
        val stateMachine = MorseCodeStateMachine(State("idle"), isProd = false)
        stateMachine.onSignal(Signal(true, Optional.empty()))
        assertEquals(State("receiving"), stateMachine.current)
    }

    @Test
    fun idleOnEnd() {
        val stateMachine = MorseCodeStateMachine(State("idle"), isProd = false)
        stateMachine.onSignal(Signal(true, Optional.of(1L)))
        assertEquals(State("idle"), stateMachine.current)
    }

    @Test
    fun receivingToDot() {
        val stateMachine = MorseCodeStateMachine(State("receiving"), isProd = false)
        stateMachine.onSignal(Signal(true, Optional.of(500)))
        assertEquals(State("dot"), stateMachine.current)
    }

    @Test
    fun receivingToDash() {
        val stateMachine = MorseCodeStateMachine(State("receiving"), isProd = false)
        stateMachine.onSignal(Signal(true, Optional.of(1500)))
        assertEquals(State("dash"), stateMachine.current)
    }

    @Test
    fun receivingToIdle() {
        val stateMachine = MorseCodeStateMachine(State("receiving"), isProd = false)
        stateMachine.onSignal(Signal(false, Optional.of(4000)))
        assertEquals(State("idle"), stateMachine.current)
    }

    @Test
    fun dotToPause() {
        val stateMachine = MorseCodeStateMachine(State("dot"), isProd = false)
        stateMachine.onSignal(Signal(false, Optional.empty()))
        assertEquals(State("pause"), stateMachine.current)
    }

    @Test
    fun dashToPause() {
        val stateMachine = MorseCodeStateMachine(State("dash"), isProd = false)
        stateMachine.onSignal(Signal(false, Optional.empty()))
        assertEquals(State("pause"), stateMachine.current)
    }

    @Test
    fun pauseToSymbolPause() {
        val now = System.currentTimeMillis()
        val stateMachine = MorseCodeStateMachine(State("pause"), isProd = false)
        stateMachine.onSignal(Signal(false, Optional.empty()))
        assertEquals(State("pause"), stateMachine.current)
        stateMachine.onSignal(Signal(false, Optional.of(500)))
        assertEquals(State("pause-symbol"), stateMachine.current)
        stateMachine.onSignal(Signal(true, Optional.empty()))
        assertEquals(State("receiving"), stateMachine.current)
    }

    @Test
    fun pauseToLetterPause() {
        val now = System.currentTimeMillis()
        val stateMachine = MorseCodeStateMachine(State("pause"), isProd = false)
        stateMachine.onSignal(Signal(false, Optional.empty()))
        assertEquals(State("pause"), stateMachine.current)
        stateMachine.onSignal(Signal(false, Optional.of(1500)))
        assertEquals(State("pause-letter"), stateMachine.current)
        stateMachine.onSignal(Signal(true, Optional.empty()))
        assertEquals(State("receiving"), stateMachine.current)
    }

    @Test
    fun pauseToWordPause() {
        val now = System.currentTimeMillis()
        val stateMachine = MorseCodeStateMachine(State("pause"), isProd = false)
        stateMachine.onSignal(Signal(false, Optional.empty()))
        assertEquals(State("pause"), stateMachine.current)
        stateMachine.onSignal(Signal(false, Optional.of(3500)))
        assertEquals(State("pause-word"), stateMachine.current)
        stateMachine.onSignal(Signal(true, Optional.empty()))
        assertEquals(State("receiving"), stateMachine.current)
    }

    @Test
    fun pauseToIdle() {
        val now = System.currentTimeMillis()
        val stateMachine = MorseCodeStateMachine(State("pause"), isProd = false)
        stateMachine.onSignal(Signal(false, Optional.empty()))
        assertEquals(State("pause"), stateMachine.current)
        stateMachine.onSignal(Signal(false, Optional.of(4000)))
        assertEquals(State("idle"), stateMachine.current)
    }
}
