package net.ttiimm.morsecode.ui

import net.ttiimm.morsecode.data.FakeExampleMessages

data class ChatUiState(
//    val messages: MutableList<Message> = mutableStateListOf(),
    val messages: MutableList<Message> = FakeExampleMessages,
)
