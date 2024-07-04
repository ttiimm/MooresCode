package net.ttiimm.morsecode.data

import java.time.Instant

data class Message(
    val content: String,
    val datetime: Instant,
    var status: MessageState
)

enum class MessageState {
    SENDING,
    SENT,
    RECEIVING,
    RECEIVED
}
