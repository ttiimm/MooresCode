package net.ttiimm.morsecode.model

import java.time.Instant

data class Message(
    val content: String,
    val datetime: Instant,
    val isMine: Boolean
)
