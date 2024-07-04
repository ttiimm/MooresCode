package net.ttiimm.morsecode

import android.content.Context
import android.hardware.camera2.CameraManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import net.ttiimm.morsecode.ui.ChatViewModel
import net.ttiimm.morsecode.ui.Message
import net.ttiimm.morsecode.ui.MessageState
import net.ttiimm.morsecode.ui.theme.MorseCodeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MorseCodeTheme {
                Scaffold (
                    topBar = { MorseCodeTopBar() },
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Surface( modifier = Modifier.padding(innerPadding)) {
                        MorseCodeApp()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MorseCodeTopBar(modifier: Modifier = Modifier) {
    CenterAlignedTopAppBar(
        title = {
            Row {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.displayMedium
                )
            }
        },
        modifier = modifier
    )
}

private const val BACK_CAMERA_IDX = 0

@Composable
fun MorseCodeApp(
    chatViewModel: ChatViewModel = viewModel(),
    context: Context = LocalContext.current
) {
    val chatUiState by chatViewModel.uiState.collectAsState()
    val scrollState = rememberLazyListState()

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.5F)
        ) {
            MessageBubbles(
                messages = chatUiState.messages,
                scrollState = scrollState
            )
        }
        val context = LocalContext.current
        Row {
            MessageInput(
                message = chatViewModel.currentMessage,
                onMessageChange = { chatViewModel.updateCurrentMessage(it) },
                doSend = {
                    chatViewModel.transmit(context)
                },
                modifier = Modifier
                    .padding(top = 16.dp)
                    .fillMaxWidth()
            )
        }

        val checkedStatus = remember { mutableStateOf(false) }
        Row {
            Switch(
                checked = checkedStatus.value,
                onCheckedChange = {
                    checkedStatus.value = it
                    val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
                    val cameraId = cameraManager.cameraIdList[BACK_CAMERA_IDX]
                    if (checkedStatus.value) {
                        cameraManager.setTorchMode(cameraId, true)
                    } else {
                        cameraManager.setTorchMode(cameraId, false)
                    }
                }
            )
        }
    }
}

@Composable
fun MessageBubbles(
    messages: List<Message>,
    scrollState: LazyListState,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        state = scrollState,
    ) {
        items(messages) {
            MessageBubble(
                message = it,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

@Composable
fun MessageBubble(message: Message, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.statusBarsPadding()
    ) {
        Card (
            modifier = modifier,
            colors = if (
                    message.status == MessageState.SENDING ||
                    message.status == MessageState.SENT
                ) {
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer // Set background color
                    )
                } else {
                    CardDefaults.cardColors()
                },
            shape = if (message.status == MessageState.SENDING ||
                        message.status == MessageState.SENT
                ) {
                    RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 20.dp,
                        bottomEnd = 20.dp,
                    )
                } else {
                    RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 20.dp,
                        bottomStart = 20.dp,
                    )
                }
        ) {
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyMedium,
                modifier = modifier.padding(8.dp)
            )
        }
        Text(
            text = message.datetime.toString(),
            style = MaterialTheme.typography.labelSmall
        )
        Text(
            text = message.status.toString(),
            style = MaterialTheme.typography.labelSmall
        )
    }
}


@Composable
fun MessageInput(
    message: String,
    onMessageChange: (String) -> Unit,
    doSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    TextField(
        label = { Text(stringResource(R.string.enter_message)) },
        value = message,
        onValueChange = onMessageChange,
        modifier = modifier,
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Send
        ),
        keyboardActions = KeyboardActions(
            onSend =  {
                focusManager.clearFocus()
                doSend()
            }
        ),
    )
}

@Preview(showBackground = true)
@Composable
fun MorseCodeAppPreview() {
    MorseCodeApp()
}

@Preview(showBackground = true)
@Composable
fun MorseCodeAppDarkThemePreview() {
    MorseCodeTheme(darkTheme = true) {
        MorseCodeApp()
    }
}
