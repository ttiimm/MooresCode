package net.ttiimm.morsecode.model

import java.time.Instant

data class Message(
    val message: String,
    val datetime: Instant
)
