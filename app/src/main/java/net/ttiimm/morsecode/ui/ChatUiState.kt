package net.ttiimm.morsecode.ui

import net.ttiimm.morsecode.data.FakeExampleMessages
import java.time.Instant

data class ChatUiState(
//    val messages: MutableList<Message> = mutableStateListOf(),
    val messages: MutableList<Message> = FakeExampleMessages,
)

data class Message(
    val content: String,
    val datetime: Instant,
    val status: MessageState
)

enum class MessageState {
    SENDING,
    SENT,
    RECEIVING,
    RECEIVED
}
