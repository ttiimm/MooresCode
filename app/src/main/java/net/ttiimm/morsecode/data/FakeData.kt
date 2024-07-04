package net.ttiimm.morsecode.data

import androidx.compose.runtime.mutableStateListOf
import net.ttiimm.morsecode.ui.Message
import net.ttiimm.morsecode.ui.MessageState
import java.time.Instant

private val now = Instant.now()

val FakeExampleMessages = mutableStateListOf(
    Message(
        "hello",
        datetime = now.plusSeconds(1),
        status = MessageState.RECEIVED
    ),
    Message(
        "hi",
        datetime = now.plusSeconds(2),
        status = MessageState.SENT
    ),
    Message(
        "how are you?",
        datetime = now.plusSeconds(3),
        status = MessageState.RECEIVED
    ),
    Message(
        "good",
        datetime = now.plusSeconds(4),
        status = MessageState.SENT
    ),
    Message(
        "how are you?",
        datetime = now.plusSeconds(5),
        status = MessageState.RECEIVED
    ),
    Message(
        "good",
        datetime = now.plusSeconds(6),
        status = MessageState.SENT
    ),
    Message(
        "good",
        datetime = now.plusSeconds(7),
        status = MessageState.RECEIVED
    ),
    Message(
        "thanks for the convo",
        datetime = now.plusSeconds(8),
        status = MessageState.SENT
    ),
    Message(
        "you're welcome",
        datetime = now.plusSeconds(9),
        status = MessageState.RECEIVED
    ),
    Message(
        "thank you",
        datetime = now.plusSeconds(10),
        status = MessageState.SENT
    ),
    Message(
        "ok",
        datetime = now.plusSeconds(11),
        status = MessageState.RECEIVED
    )
)