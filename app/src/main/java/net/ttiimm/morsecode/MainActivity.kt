package net.ttiimm.morsecode

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import net.ttiimm.morsecode.ui.MessageState
import net.ttiimm.morsecode.ui.MorseCodeApp
import net.ttiimm.morsecode.ui.theme.MorseCodeTheme

val FROM_ME_STATUS = setOf(MessageState.SENDING, MessageState.SENT)
val FROM_ME_SHAPE = RoundedCornerShape(
    topStart = 20.dp,
    topEnd = 20.dp,
    // point towards left
    bottomStart = 0.dp,
    bottomEnd = 20.dp
)
val FROM_YOU_SHAPE = RoundedCornerShape(
    topStart = 20.dp,
    topEnd = 20.dp,
    bottomStart = 20.dp,
    // point towards right
    bottomEnd = 0.dp
)

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

