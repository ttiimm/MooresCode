package net.ttiimm.morsecode.ui

import android.content.Context
import android.hardware.camera2.CameraManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.Instant

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

private const val ON = true
private const val OFF = false
private const val DOT_TIME_UNIT = 500L
// these are all derived off of the DOT_TIME_UNIT
private const val DASH_TIME_UNIT = 3 * DOT_TIME_UNIT
private const val SYMBOL_PAUSE_TIME_UNIT = DOT_TIME_UNIT
private const val LETTER_PAUSE_TIME_UNIT = 3 * DOT_TIME_UNIT
private const val WORD_PAUSE_TIME_UNIT = 7 * DOT_TIME_UNIT


class ChatViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    var currentMessage by mutableStateOf("")
        private set

    fun updateCurrentMessage(entered: String) {
        currentMessage = entered
    }

    fun transmit(context: Context) {
        _uiState.update {
            // XXX: move this out of the ViewModel
            var translation = convertCurrentMessageToMorse(currentMessage)
            flashCode(context, translation)
            it.messages.add(Message(currentMessage, Instant.now(), MessageState.SENDING))
            it
        }
        updateCurrentMessage("")
    }

    private fun convertCurrentMessageToMorse(currentMessage: String): List<String> {
        // method to convert currentMessage to morse code (map in Alphabet.kt)
        var translation: MutableList<String> = mutableListOf()
        for(c in currentMessage) {
            translation.add(ALPHANUM_TO_MORSE[c.uppercaseChar()]!!)
        }
        return translation
    }

    private fun flashCode(context: Context, translation: List<String>) {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraId = cameraManager.cameraIdList[0]
        for (symbol in translation) {
            for (char in symbol) {
                if (char == '-') {
                    cameraManager.setTorchMode(cameraId, ON)
                    Thread.sleep(DASH_TIME_UNIT)
                    cameraManager.setTorchMode(cameraId, OFF)
                } else if (char == '.'){
                    cameraManager.setTorchMode(cameraId, ON)
                    Thread.sleep(DOT_TIME_UNIT)
                    cameraManager.setTorchMode(cameraId, OFF)
                } else if (char == '/') {
                    Thread.sleep(WORD_PAUSE_TIME_UNIT)
                }
                Thread.sleep(SYMBOL_PAUSE_TIME_UNIT)
            }
            Thread.sleep(LETTER_PAUSE_TIME_UNIT)
        }
    }
}