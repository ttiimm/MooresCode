package net.ttiimm.morsecode.ui

import net.ttiimm.morsecode.data.Message

data class CameraUiState(
    val showCameraPreview: Boolean = false,
    val needsCamera: Boolean = false,
    val toSend: List<Message> = mutableListOf()
)
