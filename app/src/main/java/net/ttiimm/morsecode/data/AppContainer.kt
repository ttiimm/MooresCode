package net.ttiimm.morsecode.data

import android.content.Context

interface AppContainer {
    val chatRepository: ChatRepository
}

class DefaultAppContainer(val context: Context) : AppContainer {
    override val chatRepository: ChatRepository
        get() = CameraChatRepository(context)

}