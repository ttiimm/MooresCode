package net.ttiimm.morsecode.ui

import android.graphics.Bitmap
import net.ttiimm.morsecode.data.Message

data class CameraUiState(
    val isShowingAnalysis: Boolean = false,
    val previewImage: Bitmap? = null,
    val showCameraPreview: Boolean = false,
    val needsCamera: Boolean = false,
    val toSend: List<Message> = mutableListOf(),
    var receiving: String = "",
    val received: List<Message> = mutableListOf(),
) {

}
