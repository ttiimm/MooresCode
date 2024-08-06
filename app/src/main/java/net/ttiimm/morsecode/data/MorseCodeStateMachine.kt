package net.ttiimm.morsecode.data

import android.util.Log
import net.ttiimm.morsecode.ui.DASH_TIME_UNIT
import net.ttiimm.morsecode.ui.DOT_TIME_UNIT
import net.ttiimm.morsecode.ui.MESSAGE_PAUSE_TIME_UNIT
import net.ttiimm.morsecode.ui.Signal
import net.ttiimm.morsecode.ui.WORD_PAUSE_TIME_UNIT

private const val TAG = "MorseCodeStateMachine"


private const val DOT_MIN = DOT_TIME_UNIT - 100
private const val DASH_MIN = DASH_TIME_UNIT - 100

private const val SYMBOL_PAUSE_MIN = DOT_MIN
private const val LETTER_PAUSE_MIN = DASH_MIN
private const val WORD_PAUSE_MIN = WORD_PAUSE_TIME_UNIT - 100
private const val MESSAGE_PAUSE_MIN = MESSAGE_PAUSE_TIME_UNIT - 100


class MorseCodeStateMachine(
    initState: State = State("idle"),
    private val entrances: Map<State, () -> Unit> = mapOf(),
    private val exits: Map<State, () -> Unit> = mapOf(),
    private val isProd: Boolean = true,
) {


    // 400 to 1400
    private val dotRange: LongRange = DOT_MIN ..< DASH_MIN
    // dash range >= 1400

    // 400 to 1400
    private val pauseSymbolRange: LongRange = SYMBOL_PAUSE_MIN ..< LETTER_PAUSE_MIN
    // 1400 to 3400
    private val pauseLetterRange: LongRange = LETTER_PAUSE_MIN ..< WORD_PAUSE_MIN
    // 3400 to 3800
    private val pauseWordRange: LongRange = WORD_PAUSE_MIN .. MESSAGE_PAUSE_MIN

    private val transitions: Map<State, List<Guard>> = mapOf(
        // idle
        State("idle") to listOf(Guard(State("receiving")) { it.isOn && it.isStart }),

        // receiving
        State("receiving") to listOf(
            Guard(State("dot")) { it.isOn && it.isEnd && it.duration.get() in dotRange },
            Guard(State("dash")) { it.isOn && it.isEnd && it.duration.get() >= DASH_MIN },
            Guard(State("idle")) { it.isOff && it.isEnd && it.duration.get() >= MESSAGE_PAUSE_MIN },
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
            Guard(State("pause-symbol")) { it.isOff && it.isEnd && it.duration.get() in pauseSymbolRange },
            Guard(State("pause-letter")) { it.isOff && it.isEnd && it.duration.get() in pauseLetterRange },
            Guard(State("pause-word")) { it.isOff && it.isEnd && it.duration.get() in pauseWordRange },
            Guard(State("idle")) { it.isOff && it.isEnd && it.duration.get() >= MESSAGE_PAUSE_MIN },
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
                // XXX: figure how to properly ignore this in tests
                if (isProd) {
                    Log.d(TAG, "signal = $signal")
                    Log.d(TAG, "$_current -> ${transition.next}")
                }
                entrances[transition.next]?.invoke()
                exits[_current]?.invoke()
                _current = transition.next
                return
            }
        }
    }
}

data class Guard(val next: State, val test: (Signal) -> Boolean)

data class State(val name: String)