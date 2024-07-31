package net.ttiimm.morsecode.data

import net.ttiimm.morsecode.ui.Signal

class MorseCodeStateMachine(
    initState: State = State("idle"),
    private val entrances: Map<State, () -> Unit> = mapOf(),
    private val exits: Map<State, () -> Unit> = mapOf(),
) {

    private val transitions: Map<State, List<Guard>> = mapOf(
        // idle
        State("idle") to listOf(Guard(State("maybe")) { it.isOn }),
        // maybe
        State("maybe") to listOf(
            Guard(State("idle")) { it.isOff && duration >= 5_000 },
            Guard(State("receiving")) { it.isOn && duration >= 300 },
        ),
        // receiving
        State("receiving") to listOf(
            Guard(State("symbol")) { it.isOn && duration >= 500 },
            Guard(State("maybe")) { it.isOff && duration >= 4_000 },
        ),
        // symbol
        State("symbol") to listOf(
            Guard(State("dot")) { it.isOn && duration < 1_500 },
            Guard(State("dash")) { it.isOn && duration >= 1_500 },
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
            Guard(State("pause-symbol")) { it.isOff && duration >= 500 && duration < 1500 },
            Guard(State("pause-letter")) { it.isOff && duration >= 1500 && duration < 3500 },
            Guard(State("pause-word")) { it.isOff && duration >= 3500 },
        )
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
                entrances[transition.next]?.invoke()
                exits[_current]?.invoke()
                _current = transition.next
            }
        }
    }
}

data class Guard(val next: State, val test: (Signal) -> Boolean)

data class State(val name: String)