package net.ttiimm.morsecode.ui

import android.Manifest
import android.graphics.Bitmap
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

private const val ON = true
private const val OFF = false
private const val DOT_TIME_UNIT = 300L
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

class CameraViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

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
        for (symbol in translation) {
            for (char in symbol) {
                if (char == '-') {
                    camera.cameraControl.enableTorch(ON)
                    delay(DASH_TIME_UNIT)
                    camera.cameraControl.enableTorch(OFF)
                } else if (char == '.'){
                    camera.cameraControl.enableTorch(ON)
                    delay(DOT_TIME_UNIT)
                    camera.cameraControl.enableTorch(OFF)
                } else if (char == '/') {
                    delay(WORD_PAUSE_TIME_UNIT)
                }
                delay(SYMBOL_PAUSE_TIME_UNIT)
            }
            delay(LETTER_PAUSE_TIME_UNIT)
        }
    }

    fun onUsePreviewChange(isUsingAnalysis: Boolean) {
        _uiState.update {
            it.copy(isShowingAnalysis = isUsingAnalysis)
        }
    }

    fun onPreviewChange(image: Bitmap) {
        _uiState.update {
            it.copy(previewImage = image)
        }
    }
}