package net.ttiimm.morsecode

import android.app.Application
import net.ttiimm.morsecode.data.AppContainer
import net.ttiimm.morsecode.data.DefaultAppContainer

class MorseCodeApplication : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(applicationContext)
    }
}