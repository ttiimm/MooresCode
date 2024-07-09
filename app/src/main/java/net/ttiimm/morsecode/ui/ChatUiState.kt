package net.ttiimm.morsecode.ui

import net.ttiimm.morsecode.data.FakeExampleMessages
import net.ttiimm.morsecode.data.Message

data class ChatUiState(
    val messages: MutableList<Message> = FakeExampleMessages,
)
