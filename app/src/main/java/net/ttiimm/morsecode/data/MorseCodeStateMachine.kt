package net.ttiimm.morsecode.data

import android.util.Log
import net.ttiimm.morsecode.ui.DASH_TIME_UNIT
import net.ttiimm.morsecode.ui.LETTER_PAUSE_TIME_UNIT
import net.ttiimm.morsecode.ui.MESSAGE_PAUSE_TIME_UNIT
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
        State("idle") to listOf(Guard(State("receiving")) { it.isOn && it.isStart }),

        // receiving
        State("receiving") to listOf(
            Guard(State("dot")) { it.isOn && it.isEnd && it.duration.get() < DASH_TIME_UNIT },
            Guard(State("dash")) { it.isOn && it.isEnd && it.duration.get() >= DASH_TIME_UNIT },
            Guard(State("idle")) { it.isOff && it.isEnd && it.duration.get() >= MESSAGE_PAUSE_TIME_UNIT },
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
            Guard(State("pause-symbol")) { it.isOff && it.isEnd && it.duration.get() >= SYMBOL_PAUSE_TIME_UNIT && it.duration.get() < LETTER_PAUSE_TIME_UNIT },
            Guard(State("pause-letter")) { it.isOff && it.isEnd && it.duration.get() >= LETTER_PAUSE_TIME_UNIT && it.duration.get() < WORD_PAUSE_TIME_UNIT },
            Guard(State("pause-word")) { it.isOff && it.isEnd && it.duration.get() >= WORD_PAUSE_TIME_UNIT && it.duration.get() < MESSAGE_PAUSE_TIME_UNIT },
            Guard(State("idle")) { it.isOff && it.isEnd && it.duration.get() >= MESSAGE_PAUSE_TIME_UNIT },
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

    fun onSignal(signal: Signal) {
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