package net.ttiimm.morsecode.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.ttiimm.morsecode.MorseCodeApplication
import net.ttiimm.morsecode.data.ChatRepository
import net.ttiimm.morsecode.data.Message
import net.ttiimm.morsecode.data.MessageState
import java.time.Instant

class ChatViewModel(private val chatRepository: ChatRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    var currentMessage by mutableStateOf("")
        private set

    fun updateCurrentMessage(entered: String) {
        currentMessage = entered
    }

    fun doTransmit() {
        val message = Message(currentMessage, Instant.now(), MessageState.SENDING)
        _uiState.update {
            it.messages.add(message)
            it
        }
        updateCurrentMessage("")

        viewModelScope.launch {
            chatRepository.transmit(message)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as MorseCodeApplication)
                val chatRepository = application.container.chatRepository
                ChatViewModel(chatRepository = chatRepository)
            }
        }
    }
}