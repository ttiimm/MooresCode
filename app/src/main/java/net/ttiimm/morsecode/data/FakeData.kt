package net.ttiimm.morsecode.data

import net.ttiimm.morsecode.model.Message
import java.time.Instant

private val now = Instant.now()

val FakeExampleMessages = mutableListOf(
    Message(
        "hello",
        datetime = now.plusSeconds(1),
        isMine = true
    ),
    Message(
        "hi",
        datetime = now.plusSeconds(2),
        isMine = false
    ),
    Message(
        "how are you?",
        datetime = now.plusSeconds(3),
        isMine = true
    ),
    Message(
        "good",
        datetime = now.plusSeconds(4),
        isMine = false
    ),
    Message(
        "how are you?",
        datetime = now.plusSeconds(5),
        isMine = true
    ),
    Message(
        "good",
        datetime = now.plusSeconds(6),
        isMine = false
    ),
    Message(
        "good",
        datetime = now.plusSeconds(7),
        isMine = true
    ),
    Message(
        "thanks for the convo",
        datetime = now.plusSeconds(8),
        isMine = false
    ),
    Message(
        "you're welcome",
        datetime = now.plusSeconds(9),
        isMine = true
    ),
    Message(
        "thank you",
        datetime = now.plusSeconds(10),
        isMine = false
    ),
    Message(
        "ok",
        datetime = now.plusSeconds(11),
        isMine = true
    )
)