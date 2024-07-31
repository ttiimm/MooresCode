package net.ttiimm.morsecode.data

import android.util.Log
import net.ttiimm.morsecode.ui.DASH_TIME_UNIT
import net.ttiimm.morsecode.ui.DOT_TIME_UNIT
import net.ttiimm.morsecode.ui.LETTER_PAUSE_TIME_UNIT
import net.ttiimm.morsecode.ui.MAYBE_RECEIVING
import net.ttiimm.morsecode.ui.MESSAGE_PAUSE_TIME_UNIT
import net.ttiimm.morsecode.ui.RECEIVING_STOPPED
import net.ttiimm.morsecode.ui.SYMBOL_PAUSE_TIME_UNIT
import net.ttiimm.morsecode.ui.Signal
import net.ttiimm.morsecode.ui.WORD_PAUSE_TIME_UNIT

private const val TAG = "MorseCodeStateMachine"

class MorseCodeStateMachine(
    initState: State = State("idle"),
    private val entrances: Map<State, () -> Unit> = mapOf(),
    private val exits: Map<State, () -> Unit> = mapOf(),
    private val isProd: Boolean = true,
) {

    private val transitions: Map<State, List<Guard>> = mapOf(
        // idle
        State("idle") to listOf(Guard(State("maybe")) { it.isOn }),
        // maybe
        State("maybe") to listOf(
            Guard(State("idle")) { it.isOff && duration >= RECEIVING_STOPPED },
            Guard(State("receiving")) { it.isOn && duration >= MAYBE_RECEIVING },
        ),
        // receiving
        State("receiving") to listOf(
            Guard(State("symbol")) { it.isOn && duration >= DOT_TIME_UNIT },
            Guard(State("maybe")) { it.isOff && duration >= MESSAGE_PAUSE_TIME_UNIT },
        ),
        // symbol
        State("symbol") to listOf(
            Guard(State("dot")) { it.isOn && duration < DASH_TIME_UNIT },
            Guard(State("dash")) { it.isOn && duration >= DASH_TIME_UNIT },
        ),
        // dot
        State("dot") to listOf(
            Guard(State("pause")) { it.isOff }
        ),
        // dash
        State("dash") to listOf(
            Guard(State("pause")) { it.isOff }
        ),
        // pause
        State("pause") to listOf(
            Guard(State("pause-symbol")) { it.isOff && duration >= SYMBOL_PAUSE_TIME_UNIT && duration < LETTER_PAUSE_TIME_UNIT },
            Guard(State("pause-letter")) { it.isOff && duration >= LETTER_PAUSE_TIME_UNIT && duration < WORD_PAUSE_TIME_UNIT },
            Guard(State("pause-word")) { it.isOff && duration >= WORD_PAUSE_TIME_UNIT },
        ),
        // pause-symbol
        State("pause-symbol") to listOf(
            Guard(State("receiving")) { true }
        ),
        // pause-letter
        State("pause-letter") to listOf(
            Guard(State("receiving")) { true }
        ),
        // pause-word
        State("pause-word") to listOf(
            Guard(State("receiving")) { true }
        ),
    )

    private var _current: State = initState
    val current: State
        get() = _current

    private var duration: Long = 0
    private lateinit var last: Signal

    fun onSignal(signal: Signal) {
        if (!::last.isInitialized) {
            last = signal
        }

        duration = if (signal.isOn == last.isOn) {
            duration + (signal.ts - last.ts)
        } else {
            0
        }

        last = signal

        for (transition in transitions.getOrDefault(_current, listOf())) {
            if (transition.test(signal)) {
                // XXX: figure this out
                if (isProd) {
                    Log.d(TAG, "${_current} -> ${transition.next}")
                }
                entrances[transition.next]?.invoke()
                exits[_current]?.invoke()
                _current = transition.next
            }
        }
    }
}

data class Guard(val next: State, val test: (Signal) -> Boolean)

data class State(val name: String)