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
import net.ttiimm.morsecode.data.Signal
import net.ttiimm.morsecode.data.SignalStateMachine

private const val IS_ON = true
private const val IS_OFF = false
private const val DOT_TIME_UNIT = 500L
// these are all derived off of the DOT_TIME_UNIT
private const val DASH_TIME_UNIT = 3 * DOT_TIME_UNIT
private const val SYMBOL_PAUSE_TIME_UNIT = DOT_TIME_UNIT
private const val LETTER_PAUSE_TIME_UNIT = 3 * DOT_TIME_UNIT
private const val WORD_PAUSE_TIME_UNIT = 7 * DOT_TIME_UNIT

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

private const val TAG = "MorseCodeAnalyzer"


data class Capture(
    val preview: Bitmap,
    val sum: Int,
)

data class FpsTracker(
    var ts: Long,
    var frames: Int = 0
)

data class AverageLightTracker(
    var sumOfSums: Int = 0,
    var sumOfFrames: Int = 0,
)

private const val DIFF_THRESHOLD = 100
private const val DIFF_MIN = 10F

private const val ONE_SEC = 1000
private const val MAX_AVG_FRAMES = 270

class CameraViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()
    private val stateMachine: SignalStateMachine = SignalStateMachine()
    private var state: Signal = Signal(false, System.currentTimeMillis(), 0L, 0F)

    private val fps: FpsTracker = FpsTracker(System.currentTimeMillis())
    private val avg: AverageLightTracker = AverageLightTracker()

    private var averageSum: Float = 0F


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
                        delay(SYMBOL_PAUSE_TIME_UNIT)
                    }
                    '.' -> {
                        camera.cameraControl.enableTorch(IS_ON)
                        delay(DOT_TIME_UNIT)
                        camera.cameraControl.enableTorch(IS_OFF)
                        delay(SYMBOL_PAUSE_TIME_UNIT)
                    }
                    '/' -> {
                        delay(WORD_PAUSE_TIME_UNIT)
                    }
                }
            }
            delay(LETTER_PAUSE_TIME_UNIT)
        }
    }

    fun onUsePreviewChange(isUsingAnalysis: Boolean) {
        _uiState.update {
            it.copy(isShowingAnalysis = isUsingAnalysis)
        }
    }

    fun onPreviewChange(capture: Capture) {
        _uiState.update {
            it.copy(previewImage = capture.preview)
        }

        val now = System.currentTimeMillis()
        val lightDiff = capture.sum - averageSum

        if (lightDiff in 0.001F..Float.MAX_VALUE) {
            Log.d(TAG, "lightDiff = ${lightDiff} sum = ${capture.sum} averageSum = ${averageSum}")
        }

        if (!state.isLight && lightDiff > DIFF_THRESHOLD) {
            state.duration = now - state.ts

            if (state.duration in 3400.. 3600) {
                onReceiving("\t")
                Log.i(TAG, "recording end-of-word")
            } else if (state.duration in 1400..1600) {
                onReceiving("  ")
                Log.i(TAG, "recording end-of-symbol")
            }

            Log.i(TAG, state.toString())
            state = Signal(true, now, 0, lightDiff)
        } else if (state.isLight && lightDiff < 0) {
            state.duration = now - state.ts

            if (state.duration in 400..600) {
                onReceiving(".")
                Log.i(TAG, "recording .")
            } else if (state.duration in 1400..1600) {
                onReceiving("-")
                Log.i(TAG, "recording -")
            }

            Log.i(TAG, state.toString())
            state = Signal(false, now, 0, lightDiff)
        }

        if (now - fps.ts > ONE_SEC) {
            Log.d(TAG, "fps=${fps.frames}")
            averageSum = avg.sumOfSums.toFloat() / avg.sumOfFrames
            if (avg.sumOfFrames > MAX_AVG_FRAMES) {
                avg.sumOfSums = capture.sum
                avg.sumOfFrames = 1
            }

            fps.ts = now
            fps.frames = 1
        } else {
            fps.frames++
            avg.sumOfFrames++
            avg.sumOfSums += capture.sum
        }
    }

    fun onReceiving(symbol: String) {
        _uiState.update {
            it.copy(receiving = it.receiving + symbol)
        }
    }
}