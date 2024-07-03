package net.ttiimm.morsecode.ui

import java.time.Instant

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
