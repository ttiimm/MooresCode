package net.ttiimm.morsecode.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import net.ttiimm.morsecode.data.Message
import net.ttiimm.morsecode.data.MessageState
import java.time.Instant

class ChatViewModel() : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    var currentMessage by mutableStateOf("")
        private set

    fun updateCurrentMessage(entered: String) {
        currentMessage = entered
    }

    fun onSend(): Message {
        val message = Message(currentMessage, Instant.now(), MessageState.SENDING)
        _uiState.update {
            it.messages.add(message)
            it
        }
        updateCurrentMessage("")
        return message
    }

    fun onReceived(receiving: String) {
        if (receiving.isNotBlank()) {
            val received = Message(receiving, Instant.now(), MessageState.RECEIVED)
            _uiState.update {
                it.messages.add(received)
                it
            }
        }
    }
}