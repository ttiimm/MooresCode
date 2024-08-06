package net.ttiimm.morsecode.ui

import android.Manifest
import android.graphics.Bitmap
import android.util.Log
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.ttiimm.morsecode.data.Message
import net.ttiimm.morsecode.data.MessageState
import net.ttiimm.morsecode.data.MorseCodeStateMachine
import net.ttiimm.morsecode.data.State
import java.util.Optional

private const val IS_ON = true
private const val IS_OFF = false

const val DOT_TIME_UNIT = 500L
// these are all derived off of the DOT_TIME_UNIT
const val DASH_TIME_UNIT = 3 * DOT_TIME_UNIT
const val SYMBOL_PAUSE_TIME_UNIT = DOT_TIME_UNIT
const val LETTER_PAUSE_TIME_UNIT = 3 * DOT_TIME_UNIT
const val WORD_PAUSE_TIME_UNIT = 7 * DOT_TIME_UNIT
const val MESSAGE_PAUSE_TIME_UNIT = 8 * DOT_TIME_UNIT


val ALPHANUM_TO_MORSE = mapOf(
    Pair('A', ".-"),    Pair('B', "-..."),  Pair('C', "-.-."),  Pair('D', "-.."),   Pair('E', "."),
    Pair('F', "..-."),  Pair('G', "--."),   Pair('H', "...."),  Pair('I', ".."),    Pair('J', ".---"),
    Pair('K', "-.-"),   Pair('L', ".-.."),  Pair('M', "--"),    Pair('N', "-."),    Pair('O', "---"),
    Pair('P', ".--."),  Pair('Q', "--.-"),  Pair('R', ".-."),   Pair('S', "..."),   Pair('T', "-"),
    Pair('U', "..-"),   Pair('V', "...-"),  Pair('W', ".--"),   Pair('X', "-..-"),  Pair('Y', "-.--"),
    Pair('Z', "--.."),  Pair('0', "-----"), Pair('1', ".----"), Pair('2', "..---"), Pair('3', "...--"),
    Pair('4', "....-"), Pair('5', "....."), Pair('6', "-...."), Pair('7', "--..."), Pair('8', "---.."),
    Pair('9', "----."), Pair(' ', "/")
)
val MORSE_TO_ALPHANUM = ALPHANUM_TO_MORSE.entries.associateBy({ it.value } , { it.key })

private const val TAG = "CameraViewModel"


data class Frame(
    val preview: Bitmap,
    val metrics: FrameMetrics,
) {

}

data class FrameMetrics(
    val luminance: Int,
    val ts: Long = System.currentTimeMillis(),
) {
    val isOn: Boolean = luminance >= 2000
    val isOff: Boolean = !isOn

    fun isDiff(other: FrameMetrics): Boolean {
        return isOn != other.isOn
    }
}

data class Signal(
    val isOn: Boolean,
    val duration: Optional<Long>,
) {
    val isOff: Boolean = !isOn

    val isEnd: Boolean = duration.isPresent
    val isStart: Boolean = !isEnd
}

data class FpsTracker(
    var ts: Long,
    var frames: Int = 0
)

private const val ONE_SEC = 1000

class CameraViewModel(private val onReceived: (String) -> Unit) : ViewModel() {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    private val morseCodeStateMachine: MorseCodeStateMachine = MorseCodeStateMachine(
        entrances = mapOf(
            State("idle") to { receiveFinished() },
            State("receiving") to { onReceiving("") },
            State("dot") to { onReceiving(".") },
            State("dash") to { onReceiving("-") },
            State("pause-symbol") to { onReceiving(" ") },
            State("pause-letter") to { onReceiving("\t") },
            State("pause-word") to { onReceiving("\n") },
        ),
    )

    private lateinit var last: FrameMetrics
    private val fps: FpsTracker = FpsTracker(System.currentTimeMillis())

    fun onNeedsCamera(
        shouldShow: Boolean,
        cameraPermissionLauncher: ManagedActivityResultLauncher<String, Boolean>
    ): () -> Unit {
        return {
            _uiState.update { it.copy(
                showCameraPreview = it.showCameraPreview,
                needsCamera = shouldShow)
            }
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    fun onPermissionRequest(isGranted: Boolean) {
        _uiState.update { it.copy(
            showCameraPreview = isGranted && it.needsCamera,
            needsCamera = it.needsCamera)
        }
    }

    fun onSend(message: Message) {
        _uiState.update {
            it.copy(toSend = it.toSend + message)
        }
    }

    fun onCameraReady(cameraProvider: ProcessCameraProvider, camera: Camera) {
        viewModelScope.launch {
            // XXX Hack to help make sure camera is initialized before trying to use flash
            delay(LETTER_PAUSE_TIME_UNIT)
            while (cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                _uiState.collect { state ->
                    if (state.toSend.isNotEmpty()) {
                        for (message in uiState.value.toSend) {
                            val translation = convertMessageToMorse(message.content)
                            flashCode(translation, camera)
                            message.status = MessageState.SENT
                            _uiState.update {
                                it.copy(toSend = it.toSend - message)
                            }
                        }
                    } else {
                        _uiState.first { it.toSend.isNotEmpty() }
                    }
                }
            }
        }
    }

    private fun convertMessageToMorse(message: String): List<String> {
        // method to convert currentMessage to morse code (map in Alphabet.kt)
        val translation: MutableList<String> = mutableListOf()
        for(c in message) {
            translation.add(ALPHANUM_TO_MORSE[c.uppercaseChar()]!!)
        }
        return translation
    }

    private suspend fun flashCode(translation: List<String>, camera: Camera) {
        for (letter in translation) {
            for (symbol in letter) {
                when (symbol) {
                    '-' -> {
                        camera.cameraControl.enableTorch(IS_ON)
                        delay(DASH_TIME_UNIT)
                        camera.cameraControl.enableTorch(IS_OFF)
                        Log.d(TAG, "symbol_pause = $SYMBOL_PAUSE_TIME_UNIT")
                        delay(SYMBOL_PAUSE_TIME_UNIT)
                    }
                    '.' -> {
                        camera.cameraControl.enableTorch(IS_ON)
                        delay(DOT_TIME_UNIT)
                        camera.cameraControl.enableTorch(IS_OFF)
                        Log.d(TAG, "symbol_pause = $SYMBOL_PAUSE_TIME_UNIT")
                        delay(SYMBOL_PAUSE_TIME_UNIT)
                    }
                }
            }

            if (letter != "/") {
                Log.d(TAG, "letter_pause = ${LETTER_PAUSE_TIME_UNIT - SYMBOL_PAUSE_TIME_UNIT}")
                delay(LETTER_PAUSE_TIME_UNIT - SYMBOL_PAUSE_TIME_UNIT)
            } else {
                Log.d(TAG, "word_pause = ${WORD_PAUSE_TIME_UNIT - LETTER_PAUSE_TIME_UNIT}")
                delay(WORD_PAUSE_TIME_UNIT - LETTER_PAUSE_TIME_UNIT)
            }
        }
        delay(MESSAGE_PAUSE_TIME_UNIT)
    }

    fun onUsePreviewChange(isUsingAnalysis: Boolean) {
        _uiState.update {
            it.copy(isShowingAnalysis = isUsingAnalysis)
        }
    }

    fun onPreviewChange(frame: Frame) {
        _uiState.update {
            it.copy(previewImage = frame.preview)
        }


        val now = System.currentTimeMillis()
        if (!::last.isInitialized) {
            last = frame.metrics
        } else {
            val duration = frame.metrics.ts - last.ts
            // XXX: would like to get rid of the dual messages
            if (frame.metrics.isDiff(last)) {
                morseCodeStateMachine.onSignal(Signal(last.isOn, Optional.empty()))
                morseCodeStateMachine.onSignal(Signal(last.isOn, Optional.of(duration)))
                last = frame.metrics
            } else if (frame.metrics.isOff && duration > MESSAGE_PAUSE_TIME_UNIT + 100) {
                morseCodeStateMachine.onSignal(Signal(last.isOn, Optional.empty()))
                morseCodeStateMachine.onSignal(Signal(last.isOn, Optional.of(duration)))
                last = frame.metrics
            }
        }

        if (now - fps.ts > ONE_SEC) {
            Log.d(TAG, "fps=${fps.frames}")
            fps.ts = now
            fps.frames = 1
        } else {
            fps.frames++
        }
    }

    private fun onReceiving(symbol: String) {
        Log.d(TAG, "onReceiving `${symbol}`")
        _uiState.update {
            it.copy(receiving = it.receiving + symbol)
        }
    }

    private fun receiveFinished() {
        Log.d(TAG, "receiveFinished `${_uiState.value.receiving}`")
        val translated = translateBack(_uiState.value.receiving)
        Log.d(TAG, "translated = `$translated`")
        onReceived(translated)
        _uiState.update {
            it.copy(receiving = "")
        }
    }

    private fun translateBack(receiving: String): String {
        var result = ""
        for (word in receiving.split("\n")) {
            for (symbolWithSpaces in word.split("\t")) {
                val symbol = symbolWithSpaces.replace("\\s".toRegex(), "")
                result += MORSE_TO_ALPHANUM.getOrDefault(symbol, "\n< unknown symbol `$symbol` >\n")
            }
            result += " "
        }
        return result
    }
}