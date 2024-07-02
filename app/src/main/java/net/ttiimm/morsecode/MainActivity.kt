package net.ttiimm.morsecode

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.ttiimm.morsecode.data.FakeExampleMessages
import net.ttiimm.morsecode.model.Message
import net.ttiimm.morsecode.ui.theme.MorseCodeTheme
import java.time.Instant

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MorseCodeTheme {
                Scaffold (
                    topBar = {
                        MorseCodeTopBar()
                    },
                    modifier = Modifier
                        .fillMaxSize()
                    ) { innerPadding ->
                    Surface(
                        modifier = Modifier.padding(innerPadding)
                    ) {
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

@Composable
fun MorseCodeApp(modifier: Modifier = Modifier) {
    var messageInput by remember { mutableStateOf("") }
    val messages by remember {
        mutableStateOf(FakeExampleMessages)
    }
    val scrollState = rememberLazyListState()

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.5F)
        ) {
            MessageBubbles(
                messages = messages,
                scrollState = scrollState
            )
        }

        Row {
            MessageInput(
                message = messageInput,
                onMessageChange = { messageInput = it },
                doSend = {
                    messages.add(Message(messageInput, Instant.now(), true))
                    messageInput = ""
                },
                modifier = modifier
                    .padding(top = 16.dp)
                    .fillMaxWidth()
            )
        }
    }
}

@Composable
fun MessageBubbles(
    messages: MutableList<Message>,
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
        modifier = modifier
            .statusBarsPadding()
            .padding(horizontal = 40.dp)
    ) {
        Card (modifier = modifier) {
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
