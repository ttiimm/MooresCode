package net.ttiimm.morsecode

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import net.ttiimm.morsecode.ui.MessageState
import net.ttiimm.morsecode.ui.MorseCodeApp
import net.ttiimm.morsecode.ui.theme.MorseCodeTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MorseCodeTheme {
                MorseCodeApp()
            }
        }
    }
}

